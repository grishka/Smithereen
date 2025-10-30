package smithereen.routes;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.objects.LocalImage;
import smithereen.api.ApiCallContext;
import smithereen.api.ApiDispatcher;
import smithereen.api.ApiErrorException;
import smithereen.api.methods.ApiUtils;
import smithereen.api.model.ApiError;
import smithereen.api.model.ApiErrorResponse;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiResponse;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.FloodControlViolationException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.SizedImage;
import smithereen.model.apps.AppAccessToken;
import smithereen.model.apps.AppAuthCode;
import smithereen.model.apps.ClientApp;
import smithereen.model.apps.ClientAppPermission;
import smithereen.storage.MediaStorageUtils;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.util.CryptoUtils;
import smithereen.util.FloodControl;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class ApiRoutes{
	private static final Logger LOG=LoggerFactory.getLogger(ApiRoutes.class);
	private static final Pattern VERSION_PATTERN=Pattern.compile("^(\\d+)\\.(\\d+)$");
	public static final String MAX_VERSION="1.0";

	public static final Gson gson=new GsonBuilder()
			.disableHtmlEscaping()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
			.create();

	private static Object oauthAuthorizeError(Request req, String error){
		return new RenderedTemplateResponse("oauth_error", req)
				.with("error", error)
				.pageTitle(Config.serverDisplayName+" | "+lang(req).get("oauth_title"));
	}

	public static Object oauthAuthorize(Request req, Response resp){
		req.attribute("popup", Boolean.TRUE);
		String responseType=req.queryParams("response_type");
		String clientID=req.queryParams("client_id");
		String redirectUri=req.queryParams("redirect_uri");
		String scope=req.queryParams("scope");
		String state=req.queryParams("state");

		ApplicationContext ctx=context(req);

		ArrayList<String> emptyParams=new ArrayList<>();
		if(StringUtils.isEmpty(responseType))
			emptyParams.add("response_type");
		if(StringUtils.isEmpty(clientID))
			emptyParams.add("client_id");
		if(StringUtils.isEmpty(redirectUri))
			emptyParams.add("redirect_uri");
		if(!emptyParams.isEmpty())
			return oauthAuthorizeError(req, "the following required parameters are missing: "+String.join(", ", emptyParams));

		if(!"token".equals(responseType) && !"code".equals(responseType))
			return oauthAuthorizeError(req, "response_type has an invalid value. Allowed values are 'token' or 'code'");

		URI appApID;
		try{
			appApID=new URI(clientID);
		}catch(URISyntaxException x){
			return oauthAuthorizeError(req, "client_id is not a valid URL");
		}
		if(!"https".equals(appApID.getScheme()) || StringUtils.isEmpty(appApID.getHost()))
			return oauthAuthorizeError(req, "client_id is not a valid URL");

		URI redirect;
		try{
			redirect=new URI(redirectUri);
		}catch(URISyntaxException x){
			return oauthAuthorizeError(req, "redirect_uri is not a valid URL");
		}
		if(StringUtils.isEmpty(redirect.getScheme()) || StringUtils.isEmpty(redirect.getAuthority()))
			return oauthAuthorizeError(req, "redirect_uri must have at least scheme and authority parts");

		ClientApp app;
		try{
			app=ctx.getObjectLinkResolver().resolveNative(appApID, ClientApp.class, true, true, false, (JsonObject) null, true, true, false);
		}catch(ObjectNotFoundException x){
			return oauthAuthorizeError(req, "failed to resolve client_id to an ActivityPub Application object: "+x.getMessage());
		}
		if(!Config.isLocal(appApID)){
			if(app.lastUpdated.isBefore(Instant.now().minus(1, ChronoUnit.DAYS)) || req.queryParams("forceReload")!=null){
				try{
					app=ctx.getObjectLinkResolver().resolveNative(appApID, ClientApp.class, true, true, true, (JsonObject) null, true, true, false);
				}catch(ObjectNotFoundException ignore){}
			}
		}

		if(!app.allowedRedirectURIs.isEmpty() && !app.allowedRedirectURIs.contains(redirectUri)){
			return oauthAuthorizeError(req, "redirect_uri is not on the list of allowed redirect URIs in the application object");
		}

		Account self=currentUserAccount(req);
		RenderedTemplateResponse model;
		if(self==null){
			model=new RenderedTemplateResponse("login", req)
					.with("additionalParams", "?to="+URLEncoder.encode(req.pathInfo()+"?"+req.queryString(), StandardCharsets.UTF_8));
		}else{
			model=new RenderedTemplateResponse("oauth_prompt", req)
					.with("queryString", req.queryString());
		}

		EnumSet<ClientAppPermission> permissions=parseScope(scope);
		UriBuilder rejectUrlBuilder=new UriBuilder(redirect);
		String codeChallenge=null;
		if(responseType.equals("code")){
			rejectUrlBuilder.queryParam("error", "access_denied")
					.queryParam("error_description", "User denied access request");
			if(StringUtils.isNotEmpty(state))
				rejectUrlBuilder.queryParam("state", state);
			String codeChallengeMethod=req.queryParams("code_challenge_method");
			if(StringUtils.isNotEmpty(codeChallengeMethod)){
				if("S256".equalsIgnoreCase(codeChallengeMethod)){
					codeChallenge=req.queryParams("code_challenge");
					if(StringUtils.isEmpty(codeChallenge))
						return oauthAuthorizeError(req, "with code_challenge_method present, code_challenge is required");
				}else{
					return oauthAuthorizeError(req, "unsupported code_challenge_method, the only 'S256' is supported");
				}
			}
		}else{
			String fragment="error=access_denied&error_description="+URLEncoder.encode("User denied access request", StandardCharsets.UTF_8);
			if(StringUtils.isNotEmpty(state))
				fragment+="&state="+URLEncoder.encode(state, StandardCharsets.UTF_8);
			rejectUrlBuilder.fragment(fragment);
		}

		String reqID=randomAlphanumericString(32);
		List<OAuthRequest> reqs=req.session().attribute("oauthRequests");
		if(reqs==null){
			reqs=new ArrayList<>();
			req.session().attribute("oauthRequests", reqs);
		}else{
			Instant threshold=Instant.now().minus(10, ChronoUnit.MINUTES);
			reqs.removeIf(r->r.createdAt.isBefore(threshold));
		}
		reqs.add(new OAuthRequest(reqID, app.id, responseType, permissions, redirect, state, Instant.now(), codeChallenge));

		return model.with("app", app)
				.pageTitle(Config.serverDisplayName+" | "+lang(req).get("oauth_title"))
				.with("permissions", permissions.stream().map(Enum::toString).collect(Collectors.toSet()))
				.with("rejectURL", rejectUrlBuilder.build().toString())
				.with("requestID", reqID);
	}

	private static Object oauthTokenError(Response resp, String error, String description){
		resp.status(400);
		JsonObjectBuilder jb=new JsonObjectBuilder().add("error", error);
		if(description!=null)
			jb.add("error_description", description);
		return jb.build();
	}

	public static Object oauthToken(Request req, Response resp){
		ApplicationContext ctx=context(req);
		resp.type("application/json");
		String grantType=req.queryParams("grant_type");
		if(StringUtils.isEmpty(grantType)){
			return oauthTokenError(resp, "invalid_request", "grant_type parameter is missing");
		}else if("authorization_code".equals(grantType)){
			String code=req.queryParams("code");
			String redirectURI=req.queryParams("redirect_uri");
			String clientID=req.queryParams("client_id");
			if(StringUtils.isEmpty(code) || StringUtils.isEmpty(redirectURI) || StringUtils.isEmpty(clientID)){
				ArrayList<String> missingFields=new ArrayList<>();
				if(StringUtils.isEmpty(code))
					missingFields.add("code");
				if(StringUtils.isEmpty(redirectURI))
					missingFields.add("redirect_uri");
				if(StringUtils.isEmpty(clientID))
					missingFields.add("client_id");
				return oauthTokenError(resp, "invalid_request", "Required parameters are missing: "+String.join(", ", missingFields));
			}
			byte[] codeID;
			try{
				codeID=Base64.getUrlDecoder().decode(code);
			}catch(IllegalArgumentException x){
				return oauthTokenError(resp, "invalid_grant", null);
			}
			if(codeID.length!=64)
				return oauthTokenError(resp, "invalid_grant", null);
			AppAuthCode ac=ctx.getAppsController().getAndDeleteAuthCode(codeID);
			if(ac==null)
				return oauthTokenError(resp, "invalid_grant", null);

			ClientApp app;
			try{
				app=ctx.getObjectLinkResolver().resolveLocally(new URI(clientID), ClientApp.class);
			}catch(ObjectNotFoundException|URISyntaxException x){
				return oauthTokenError(resp, "invalid_grant", null);
			}

			if(app.id!=ac.appID() || !ac.redirectURI().equals(redirectURI) || ac.expiresAt().isBefore(Instant.now()))
				return oauthTokenError(resp, "invalid_grant", null);

			if(ac.s256CodeChallenge()!=null){
				String codeVerifier=req.queryParams("code_verifier");
				if(StringUtils.isEmpty(codeVerifier))
					return oauthTokenError(resp, "invalid_grant", "PKCE mismatch");
				String hash=Base64.getUrlEncoder().encodeToString(CryptoUtils.sha256(codeVerifier.getBytes(StandardCharsets.US_ASCII)));
				if(!hash.equals(ac.s256CodeChallenge()))
					return oauthTokenError(resp, "invalid_grant", "PKCE mismatch");
			}

			Account account=ctx.getUsersController().getAccountOrThrow(ac.accountID());
			AppAccessToken token=ctx.getAppsController().createAccessToken(account, app, ac.permissions(), req);
			JsonObjectBuilder jb=new JsonObjectBuilder()
					.add("access_token", token.getEncodedID())
					.add("token_type", "bearer")
					.add("user_id", account.user.id);
			if(token.expiresAt()!=null)
				jb.add("expires_in", token.expiresAt().getEpochSecond()-Instant.now().getEpochSecond());
			return jb.build();
		}else{
			return oauthTokenError(resp, "unsupported_grant_type", null);
		}
	}

	public static Object oauthDoAuthorize(Request req, Response resp, Account self, ApplicationContext ctx){
		List<OAuthRequest> reqs=req.session().attribute("oauthRequests");
		if(reqs==null)
			throw new UserActionNotAllowedException();
		String reqID=req.queryParams("request");
		if(StringUtils.isEmpty(reqID))
			throw new UserActionNotAllowedException();
		OAuthRequest oauthRequest=null;
		for(OAuthRequest r:reqs){
			if(r.id.equals(reqID)){
				Instant threshold=Instant.now().minus(10, ChronoUnit.MINUTES);
				if(r.createdAt.isAfter(threshold))
					oauthRequest=r;
				reqs.remove(r);
				break;
			}
		}
		if(oauthRequest==null)
			throw new UserActionNotAllowedException();

		ClientApp app=ctx.getAppsController().getAppByID(oauthRequest.appID);

		if(oauthRequest.responseType.equals("token")){
			AppAccessToken token=ctx.getAppsController().createAccessToken(self, app, oauthRequest.scope, req);
			String fragment="access_token="+token.getEncodedID()+"&token_type=bearer&user_id="+self.user.id;
			if(token.expiresAt()!=null)
				fragment+="&expires_in="+(token.expiresAt().getEpochSecond()-Instant.now().getEpochSecond());
			if(StringUtils.isNotEmpty(oauthRequest.state))
				fragment+="&state="+URLEncoder.encode(oauthRequest.state, StandardCharsets.UTF_8);
			resp.redirect(new UriBuilder(oauthRequest.redirectURI).fragment(fragment).build().toString());
		}else if(oauthRequest.responseType.equals("code")){
			byte[] code=ctx.getAppsController().createAuthCode(self, app, oauthRequest.scope, oauthRequest.s256CodeChallenge, oauthRequest.redirectURI.toString());
			UriBuilder builder=new UriBuilder(oauthRequest.redirectURI)
					.queryParam("code", Base64.getUrlEncoder().withoutPadding().encodeToString(code));
			if(StringUtils.isNotEmpty(oauthRequest.state))
				builder.queryParam("state", oauthRequest.state);
			resp.redirect(builder.build().toString());
		}

		return "";
	}

	private static EnumSet<ClientAppPermission> parseScope(String scope){
		EnumSet<ClientAppPermission> permissions=EnumSet.noneOf(ClientAppPermission.class);
		if(StringUtils.isNotEmpty(scope)){
			for(String s:scope.split(" +")){
				switch(s){
					case "friends:read" -> permissions.add(ClientAppPermission.FRIENDS_READ);
					case "friends", "friends:write" -> {
						permissions.add(ClientAppPermission.FRIENDS_READ);
						permissions.add(ClientAppPermission.FRIENDS_WRITE);
					}
					case "photos:read" -> permissions.add(ClientAppPermission.PHOTOS_READ);
					case "photos", "photos:write" -> {
						permissions.add(ClientAppPermission.PHOTOS_READ);
						permissions.add(ClientAppPermission.PHOTOS_WRITE);
					}
					case "account", "account:write" -> permissions.add(ClientAppPermission.ACCOUNT_WRITE);
					case "wall:read" -> permissions.add(ClientAppPermission.WALL_READ);
					case "wall", "wall:write" -> {
						permissions.add(ClientAppPermission.WALL_READ);
						permissions.add(ClientAppPermission.WALL_WRITE);
					}
					case "groups:read" -> permissions.add(ClientAppPermission.GROUPS_READ);
					case "groups", "groups:write" -> {
						permissions.add(ClientAppPermission.GROUPS_READ);
						permissions.add(ClientAppPermission.GROUPS_WRITE);
					}
					case "messages:read" -> permissions.add(ClientAppPermission.MESSAGES_READ);
					case "messages", "messages:write" -> {
						permissions.add(ClientAppPermission.MESSAGES_READ);
						permissions.add(ClientAppPermission.MESSAGES_WRITE);
					}
					case "likes:read" -> permissions.add(ClientAppPermission.LIKES_READ);
					case "likes", "likes:write" -> {
						permissions.add(ClientAppPermission.LIKES_READ);
						permissions.add(ClientAppPermission.LIKES_WRITE);
					}
					case "newsfeed" -> permissions.add(ClientAppPermission.NEWSFEED);
					case "notifications" -> permissions.add(ClientAppPermission.NOTIFICATIONS);
					case "offline" -> permissions.add(ClientAppPermission.OFFLINE);
				}
			}
		}
		return permissions;
	}

	private record OAuthRequest(String id, long appID, String responseType, EnumSet<ClientAppPermission> scope, URI redirectURI, String state, Instant createdAt, String s256CodeChallenge){}

	public static Object apiCall(Request req, Response resp){
		ApplicationContext ctx=context(req);
		resp.type("application/json");
		if(StringUtils.isNotEmpty(req.headers("origin"))){
			addCorsHeaders(resp);
		}
		String method=req.params(":method");
		String token=req.queryParams("access_token");
		if(StringUtils.isEmpty(token)){
			String authHeader=req.headers("authorization");
			if(StringUtils.isNotEmpty(authHeader)){
				String[] parts=authHeader.split(" ");
				if(parts.length==2 && parts[0].equalsIgnoreCase("bearer"))
					token=parts[1];
			}
		}
		TreeMap<String, String> params=new TreeMap<>();
		params.put("method", method);
		Map<String, String[]> rawQueryParams=req.queryMap().toMap();
		rawQueryParams.forEach((key, value)->params.put(key, value[0]));
		params.remove("access_token");
		Account self=null;
		AppAccessToken accessToken=null;
		int versionMajor, versionMinor;
		try{
			String version=params.get("v");
			if(StringUtils.isEmpty(version))
				throw new ApiErrorException(new ApiError(ApiErrorType.BAD_REQUEST, "version parameter \"v\" is missing", params));
			Matcher matcher=VERSION_PATTERN.matcher(version);
			if(!matcher.find())
				throw new ApiErrorException(new ApiError(ApiErrorType.BAD_REQUEST, "version parameter \"v\" must have format major.minor, e.g. 1.0", params));
			versionMajor=safeParseInt(matcher.group(1));
			versionMinor=safeParseInt(matcher.group(2));
			if(versionMajor>1 || versionMinor>0){
				throw new ApiErrorException(new ApiError(ApiErrorType.BAD_REQUEST, "version "+versionMajor+"."+versionMinor+" too new for this server. Maximum supported API version is 1.0", params));
			}else if(versionMajor==0){
				throw new ApiErrorException(new ApiError(ApiErrorType.BAD_REQUEST, "invalid API version "+versionMajor+"."+versionMinor, params));
			}
			byte[] tokenID=null;
			if(StringUtils.isNotEmpty(token)){
				try{
					tokenID=Base64.getUrlDecoder().decode(token);
					if(tokenID.length!=64)
						throw new IllegalArgumentException();
					accessToken=ctx.getAppsController().getAccessTokenOrNull(tokenID);
					if(accessToken==null)
						throw new IllegalArgumentException();
					if(accessToken.expiresAt()!=null && accessToken.expiresAt().isBefore(Instant.now()))
						throw new ApiErrorException(new ApiError(ApiErrorType.USER_AUTH_FAILED, "access token has expired", params));
					self=ctx.getUsersController().getAccountOrThrow(accessToken.accountID());
					ctx.getAppsController().updateAccessTokenLastAccess(accessToken, getRequestIP(req), Objects.requireNonNullElse(req.userAgent(), ""));
				}catch(IllegalArgumentException x){
					throw new ApiErrorException(new ApiError(ApiErrorType.USER_AUTH_FAILED, "invalid access token", params));
				}
			}
			ApiCallContext actx=new ApiCallContext(accessToken, self, params, req, versionMajor, versionMinor, self==null ? null : ctx.getUsersController().getUserPermissions(self));
			try{
				if(self!=null)
					FloodControl.API_REQUESTS.incrementOrThrow(tokenID);
				else
					FloodControl.API_REQUESTS_ANON.incrementOrThrow(getRequestIP(req));
			}catch(FloodControlViolationException x){
				throw actx.error(ApiErrorType.TOO_MANY_REQUESTS);
			}
			actx.imageFormat=switch(req.queryParams("image_format")){
				case "jpeg" -> SizedImage.Format.JPEG;
				case "webp" -> SizedImage.Format.WEBP;
				case null -> SizedImage.Format.WEBP;
				default -> throw actx.paramError("unsupported image_format value, supported values are 'webp' and 'jpeg'");
			};
			String langParam=req.queryParams("lang");
			if(StringUtils.isNotEmpty(langParam)){
				actx.lang=Lang.get(Locale.forLanguageTag(langParam));
			}else if(self!=null){
				actx.lang=Lang.get(self.prefs.locale);
			}else{
				Locale locale=req.raw().getLocale();
				if(locale==null)
					locale=Locale.US;
				actx.lang=Lang.get(locale);
			}
			return new ApiResponse(ApiDispatcher.doApiCall(method, ctx, actx));
		}catch(ApiErrorException x){
			resp.status(x.error.errorType.httpStatusCode);
			return new ApiErrorResponse(x.error);
		}catch(Throwable x){
			LOG.error("API request {}({}) failed", method, params, x);
			resp.status(500);
			return new ApiErrorResponse(new ApiError(ApiErrorType.INTERNAL_SERVER_ERROR, Config.DEBUG ? x.toString() : null, params));
		}
	}

	public static Object apiCallPreflight(Request req, Response resp){
		addCorsHeaders(resp);
		return "";
	}

	private static void addCorsHeaders(Response resp){
		resp.header("Access-Control-Allow-Origin", "*");
		resp.header("Access-Control-Allow-Methods", "POST, GET");
		resp.header("Access-Control-Max-Age", "7200");
	}

	public static Object uploadAttachmentPhoto(Request req, Response resp){
		requireQueryParams(req, "d");
		resp.type("application/json");
		JsonObject data;
		try{
			byte[] encrypted=Base64.getUrlDecoder().decode(req.queryParams("d"));
			data=JsonParser.parseString(new String(CryptoUtils.aesGcmDecrypt(encrypted, ApiUtils.UPLOAD_KEY), StandardCharsets.UTF_8)).getAsJsonObject();
		}catch(IllegalArgumentException | JsonParseException x){
			resp.status(403);
			return new JsonObjectBuilder().add("error", "Invalid upload URL. Call photos.getAttachmentUploadServer again to get a new one.").build();
		}
		int accountID=data.get("id").getAsInt();
		long created=data.get("ct").getAsLong();
		long now=System.currentTimeMillis()/1000L;
		if(now-created>60){
			resp.status(403);
			return new JsonObjectBuilder().add("error", "This upload URL has expired. Call photos.getAttachmentUploadServer again to get a new one.").build();
		}
		ApplicationContext ctx=context(req);
		Account self=ctx.getUsersController().getAccountOrThrow(accountID);
		try{
			LocalImage img=MediaStorageUtils.saveUploadedImage(req, resp, self, false, "photo");
			return new JsonObjectBuilder()
					.add("id", XTEA.encodeObjectID(img.fileID, ObfuscatedObjectIDType.MEDIA_FILE))
					.add("hash", img.fileRecord.id().getEncodedRandomID())
					.build();
		}catch(HaltException x){
			resp.status(x.statusCode());
			return new JsonObjectBuilder().add("error", x.body()).build();
		}catch(BadRequestException x){
			resp.status(400);
			return new JsonObjectBuilder().add("error", "Field 'photo' not found").build();
		}
	}
}
