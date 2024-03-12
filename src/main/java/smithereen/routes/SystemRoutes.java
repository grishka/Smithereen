package smithereen.routes;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import smithereen.ApplicationContext;
import smithereen.BuildInfo;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UnsupportedRemoteObjectTypeException;
import smithereen.lang.Lang;
import smithereen.libvips.VipsImage;
import smithereen.model.Account;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.AttachmentHostContentObject;
import smithereen.model.CachedRemoteImage;
import smithereen.model.CaptchaInfo;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.MailMessage;
import smithereen.model.OwnedContentObject;
import smithereen.model.Poll;
import smithereen.model.PollOption;
import smithereen.model.Post;
import smithereen.model.ReportableContentObject;
import smithereen.model.SearchResult;
import smithereen.model.SessionInfo;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.WebDeltaResponse;
import smithereen.model.attachments.GraffitiAttachment;
import smithereen.model.media.ImageMetadata;
import smithereen.model.media.MediaFileMetadata;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.media.MediaFileType;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.storage.GroupStorage;
import smithereen.storage.MediaCache;
import smithereen.storage.MediaStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.PostStorage;
import smithereen.storage.SearchStorage;
import smithereen.storage.UserStorage;
import smithereen.storage.media.MediaFileStorageDriver;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.util.BlurHash;
import smithereen.util.CaptchaGenerator;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.NamedMutexCollection;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class SystemRoutes{
	private static final Logger LOG=LoggerFactory.getLogger(SystemRoutes.class);
	private static final NamedMutexCollection downloadMutex=new NamedMutexCollection();

	private static ActivityPubObject verifyObjectAndGetAttachment(int index, String type, Object obj){
		ActivityPubRepresentable apr=(ActivityPubRepresentable) obj;
		if(obj==null || Config.isLocal(apr.getActivityPubID())){
			LOG.warn("downloading {}: post not found or is local", type);
			return null;
		}
		List<ActivityPubObject> attachments=((AttachmentHostContentObject)obj).getAttachments();
		if(index>=attachments.size() || index<0){
			LOG.warn("downloading {}: index {} out of bounds {}", type, index, attachments.size());
			return null;
		}
		ActivityPubObject att=attachments.get(index);
		if(!(att instanceof Document)){
			LOG.warn("downloading {}: attachment {} is not a Document", type, att.getClass().getName());
			return null;
		}
		return att;
	}

	public static Object downloadExternalMedia(Request req, Response resp) throws SQLException{
		requireQueryParams(req, "type", "format", "size");
		MediaCache cache=MediaCache.getInstance();
		String type=req.queryParams("type");
		String mime;
		URI uri=null;
		MediaCache.ItemType itemType;
		SizedImage.Type sizeType;
		SizedImage.Format format=switch(req.queryParams("format")){
			case "jpeg", "jpg" -> SizedImage.Format.JPEG;
			case "webp" -> SizedImage.Format.WEBP;
			case "png" -> {
				if("post_photo".equals(type)){
					yield SizedImage.Format.PNG;
				}else{
					throw new BadRequestException();
				}
			}
			default -> throw new BadRequestException();
		};

		sizeType=SizedImage.Type.fromSuffix(req.queryParams("size"));
		if(sizeType==null)
			return "";

		float[] cropRegion=null;
		User user=null;
		Group group=null;
		boolean isGraffiti=false;

		boolean isPostPhoto="post_photo".equals(type);

		if("user_ava".equals(type)){
			itemType=MediaCache.ItemType.AVATAR;
			mime="image/jpeg";
			int userID=Utils.parseIntOrDefault(req.queryParams("user_id"), 0);
			user=UserStorage.getById(userID);
			if(user==null || Config.isLocal(user.activityPubID)){
				LOG.warn("downloading user_ava: user {} not found or is local", userID);
				return "";
			}
			Image im=user.getBestAvatarImage();
			if(im!=null && im.url!=null){
				cropRegion=user.getAvatarCropRegion();
				uri=im.url;
				if(StringUtils.isNotEmpty(im.mediaType))
					mime=im.mediaType;
				else
					mime="image/jpeg";
			}
		}else if("group_ava".equals(type)){
			itemType=MediaCache.ItemType.AVATAR;
			mime="image/jpeg";
			int groupID=Utils.parseIntOrDefault(req.queryParams("group_id"), 0);
			group=GroupStorage.getById(groupID);
			if(group==null || Config.isLocal(group.activityPubID)){
				LOG.warn("downloading group_ava: group {} not found or is local", groupID);
				return "";
			}
			Image im=group.getBestAvatarImage();
			if(im!=null && im.url!=null){
				cropRegion=group.getAvatarCropRegion();
				uri=im.url;
				if(StringUtils.isNotEmpty(im.mediaType))
					mime=im.mediaType;
				else
					mime="image/jpeg";
			}
		}else if("post_photo".equals(type) || "message_photo".equals(type)){
			itemType=MediaCache.ItemType.PHOTO;
			ApplicationContext ctx=context(req);
			SessionInfo sess=sessionInfo(req);
			Object contentObj=switch(type){
				case "post_photo" -> {
					int postID=parseIntOrDefault(req.queryParams("post_id"), 0);
					yield ctx.getWallController().getPostOrThrow(postID);
				}
				case "message_photo" -> {
					requireQueryParams(req, "msg_id");
					if(sess==null || sess.account==null)
						yield null;
					long msgID=decodeLong(req.queryParams("msg_id"));
					yield context(req).getMailController().getMessage(sess.account.user, msgID, false);
				}
				default -> throw new IllegalStateException("Unexpected value: "+type);
			};

			if(contentObj instanceof OwnedContentObject oco){
				ctx.getPrivacyController().enforceObjectPrivacy(sess==null || sess.account==null ? null : sess.account.user, oco);
			}

			int index=safeParseInt(req.queryParams("index"));
			ActivityPubObject att=verifyObjectAndGetAttachment(index, type, contentObj);
			if(att==null)
				return "";

			if(att.mediaType==null){
				if(att instanceof Image){
					mime="image/jpeg";
				}else{
					LOG.warn("downloading post_photo: media type is null and attachment type {} isn't Image", att.getClass().getName());
					return "";
				}
			}else if(!att.mediaType.startsWith("image/")){
				LOG.warn("downloading post_photo: attachment media type {} is invalid", att.mediaType);
				return "";
			}else{
				mime=att.mediaType;
			}
			if(format==SizedImage.Format.PNG && (!(att instanceof Image img) || !img.isGraffiti)){
				LOG.warn("downloading post_photo: requested png but the attachment is not a graffiti");
				throw new BadRequestException();
			}
			isGraffiti=att instanceof Image img && img.isGraffiti;
			uri=att.url;
		}else{
			LOG.warn("unknown external file type {}", type);
			return "";
		}

		if(uri!=null){
			final String uriStr=uri.toString();
			downloadMutex.acquire(uriStr);
			LOG.trace("downloadExternalMedia: after mutex acquire {}", uri);
			try{
				MediaCache.Item existing=cache.get(uri);
				if(mime.startsWith("image/")){
					if(existing!=null){
						LOG.debug("downloadExternalMedia: found existing {}", uri);
						resp.redirect(new CachedRemoteImage((MediaCache.PhotoItem) existing, cropRegion).getUriForSizeAndFormat(sizeType, format).toString());
						return "";
					}
					try{
						if(sessionInfo(req)==null){ // Only download attachments for logged-in users. Prevents crawlers from causing unnecessary churn in the media cache
							resp.redirect(uri.toString());
							return "";
						}
						LOG.debug("downloadExternalMedia: downloading {}", uri);
						MediaCache.PhotoItem item;
						if(isGraffiti)
							item=(MediaCache.PhotoItem) cache.downloadAndPut(uri, mime, itemType, true, GraffitiAttachment.WIDTH, GraffitiAttachment.HEIGHT);
						else
							item=(MediaCache.PhotoItem) cache.downloadAndPut(uri, mime, itemType, false, 0, 0);
						if(item==null){
							if(itemType==MediaCache.ItemType.AVATAR && req.queryParams("retrying")==null){
								if(user!=null){
									ForeignUser updatedUser=context(req).getObjectLinkResolver().resolve(user.activityPubID, ForeignUser.class, true, true, true);
									resp.redirect(Config.localURI("/system/downloadExternalMedia?type=user_ava&user_id="+updatedUser.id+"&size="+sizeType.suffix()+"&format="+format.fileExtension()+"&retrying").toString());
									return "";
								}else if(group!=null){
									ForeignGroup updatedGroup=context(req).getObjectLinkResolver().resolve(group.activityPubID, ForeignGroup.class, true, true, true);
									resp.redirect(Config.localURI("/system/downloadExternalMedia?type=group_ava&user_id="+updatedGroup.id+"&size="+sizeType.suffix()+"&format="+format.fileExtension()+"&retrying").toString());
									return "";
								}
							}
							LOG.debug("downloadExternalMedia: redirecting to original url {}", uri);
							resp.redirect(uri.toString());
						}else{
							LOG.debug("downloadExternalMedia: download finished {}", uri);
							resp.redirect(new CachedRemoteImage(item, cropRegion).getUriForSizeAndFormat(sizeType, format).toString());
						}
						return "";
					}catch(IOException x){
						LOG.warn("Exception while downloading external media file from {}", uri, x);
					}
					resp.redirect(uri.toString());
				}
			}finally{
				downloadMutex.release(uriStr);
			}
		}
		return "";
	}

	public static Object uploadPostPhoto(Request req, Response resp, Account self, ApplicationContext ctx){
		boolean isGraffiti=req.queryParams("graffiti")!=null;
		return uploadPhotoAttachment(req, resp, self, isGraffiti);
	}

	public static Object uploadMessagePhoto(Request req, Response resp, Account self, ApplicationContext ctx){
		return uploadPhotoAttachment(req, resp, self, false);
	}

	private static Object uploadPhotoAttachment(Request req, Response resp, Account self, boolean isGraffiti){
		try{
			req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(null, 10*1024*1024, -1L, 0));
			Part part=req.raw().getPart("file");
			if(part.getSize()>10*1024*1024){
				resp.status(413); // Payload Too Large
				return "File too large";
			}

			String mime=part.getContentType();
			if(!mime.startsWith("image/")){
				resp.status(415); // Unsupported Media Type
				return "Unsupported mime type";
			}

			File temp=File.createTempFile("SmithereenUpload", null);
			VipsImage img;
			try{
				try(FileOutputStream out=new FileOutputStream(temp)){
					copyBytes(part.getInputStream(), out);
				}
				img=new VipsImage(temp.getAbsolutePath());
			}catch(IOException x){
				throw new BadRequestException(x.getMessage(), x);
			}
			if(img.hasAlpha()){
				VipsImage flat=img.flatten(255, 255, 255);
				img.release();
				img=flat;
			}

			if(isGraffiti && (img.getWidth()!=GraffitiAttachment.WIDTH || img.getHeight()!=GraffitiAttachment.HEIGHT)){
				LOG.warn("Unexpected graffiti size {}x{}", img.getWidth(), img.getHeight());
				throw new BadRequestException();
			}

			LocalImage photo=new LocalImage();
			int width, height;
			MediaFileRecord fileRecord;
			try{
				File resizedFile=File.createTempFile("SmithereenUploadResized", ".webp");
				int[] outSize={0,0};
				MediaStorageUtils.writeResizedWebpImage(img, 2560, 0, isGraffiti ? MediaStorageUtils.QUALITY_LOSSLESS : 93, resizedFile, outSize);
				MediaFileMetadata meta=new ImageMetadata(width=outSize[0], height=outSize[1], BlurHash.encode(img, 4, 4), null);
				fileRecord=MediaStorage.createMediaFileRecord(isGraffiti ? MediaFileType.IMAGE_GRAFFITI : MediaFileType.IMAGE_PHOTO, resizedFile.length(), self.user.id, meta);
				photo.fileID=fileRecord.id().id();
				photo.fillIn(fileRecord);
				MediaFileStorageDriver.getInstance().storeFile(resizedFile, fileRecord.id());

				temp.delete();
			}finally{
				img.release();
			}

			if(isAjax(req)){
				resp.type("application/json");
				return new JsonObjectBuilder()
						.add("id", fileRecord.id().getIDForClient())
						.add("width", width)
						.add("height", height)
						.add("thumbs", new JsonObjectBuilder()
								.add("jpeg", photo.getUriForSizeAndFormat(SizedImage.Type.SMALL, SizedImage.Format.JPEG).toString())
								.add("webp", photo.getUriForSizeAndFormat(SizedImage.Type.SMALL, SizedImage.Format.WEBP).toString())
						).build();
			}
			resp.redirect(Utils.back(req));
		}catch(IOException|ServletException|SQLException x){
			throw new InternalServerErrorException(x);
		}
		return "";
	}

	public static Object aboutServer(Request req, Response resp) throws SQLException{
		ApplicationContext ctx=context(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("about_server", req);
		model.with("title", lang(req).get("about_server"));
		model.with("serverPolicy", Config.serverPolicy)
				.with("serverAdmins", UserStorage.getAdmins())
				.with("serverAdminEmail", Config.serverAdminEmail)
				.with("totalUsers", UserStorage.getLocalUserCount())
				.with("totalPosts", PostStorage.getLocalPostCount(false))
				.with("totalGroups", GroupStorage.getLocalGroupCount())
				.with("serverVersion", BuildInfo.VERSION)
				.with("restrictedServers", ctx.getModerationController().getAllServers(0, 10000, null, true, null).list);

		return model;
	}

	public static Object quickSearch(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String query=req.queryParams("q");
		if(StringUtils.isEmpty(query) || query.length()<2)
			return "";

		List<User> users=Collections.emptyList();
		List<Group> groups=Collections.emptyList();
		List<URI> externalObjects=Collections.emptyList();
		if(isURL(query)){
			if(!query.startsWith("http:") && !query.startsWith("https:"))
				query="https://"+query;
			query=normalizeURLDomain(query);
			URI uri=URI.create(query);
			try{
				ActivityPubObject obj=ctx.getObjectLinkResolver().resolve(uri, ActivityPubObject.class, false, false, false);
				if(obj instanceof User){
					users=Collections.singletonList((User)obj);
				}else if(obj instanceof Group){
					groups=Collections.singletonList((Group)obj);
				}else{
					externalObjects=Collections.singletonList(uri);
				}
			}catch(ObjectNotFoundException x){
				if(!Config.isLocal(uri)){
					try{
						Actor actor=ctx.getObjectLinkResolver().resolve(uri, Actor.class, false, false, false);
						if(actor instanceof User){
							users=Collections.singletonList((User)actor);
						}else if(actor instanceof Group){
							groups=Collections.singletonList((Group)actor);
						}else{
							throw new AssertionError();
						}
					}catch(ObjectNotFoundException|IllegalStateException xx){
						externalObjects=Collections.singletonList(uri);
					}
				}
			}
		}else if(isUsernameAndDomain(query)){
			Matcher matcher=Utils.USERNAME_DOMAIN_PATTERN.matcher(query);
			matcher.find();
			String username=matcher.group(1);
			String domain=matcher.group(2);
			String full=username;
			if(domain!=null)
				full+='@'+domain;
			User user=UserStorage.getByUsername(full);
			SearchResult sr;
			if(user!=null){
				users=Collections.singletonList(user);
			}else{
				Group group=GroupStorage.getByUsername(full);
				if(group!=null){
					groups=Collections.singletonList(group);
				}else{
					externalObjects=Collections.singletonList(URI.create(full));
				}
			}
		}else{
			List<SearchResult> results=SearchStorage.search(query, self.user.id, 10);
			users=new ArrayList<>();
			groups=new ArrayList<>();
			for(SearchResult result:results){
				switch(result.type){
					case USER -> users.add(result.user);
					case GROUP -> groups.add(result.group);
				}
			}
		}
		return new RenderedTemplateResponse("quick_search_results", req).with("users", users).with("groups", groups).with("externalObjects", externalObjects).with("avaSize", req.attribute("mobile")!=null ? 48 : 30);
	}

	public static Object loadRemoteObject(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String _uri=req.queryParams("uri");
		if(StringUtils.isEmpty(_uri))
			throw new BadRequestException();
		Object obj=null;
		URI uri=null;
		Matcher matcher=USERNAME_DOMAIN_PATTERN.matcher(_uri);
		if(matcher.find() && matcher.start()==0 && matcher.end()==_uri.length()){
			String username=matcher.group(1);
			String domain=matcher.group(2);
			try{
				uri=ActivityPub.resolveUsername(username, domain);
			}catch(IOException x){
				LOG.debug("Error getting remote user", x);
				String error=lang(req).get("remote_object_network_error");
				return new JsonObjectBuilder().add("error", error).build();
			}
		}
		if(uri==null){
			try{
				uri=new URI(_uri);
			}catch(URISyntaxException x){
				throw new BadRequestException(x);
			}
		}
		try{
			obj=ctx.getObjectLinkResolver().resolve(uri, ActivityPubObject.class, true, false, false, (JsonObject) null, false);
		}catch(UnsupportedRemoteObjectTypeException x){
			LOG.debug("Unsupported remote object", x);
			return new JsonObjectBuilder().add("error", lang(req).get("unsupported_remote_object_type")).build();
		}catch(ObjectNotFoundException x){
			LOG.debug("Remote object not found", x);
			return new JsonObjectBuilder().add("error", lang(req).get("remote_object_not_found")).build();
		}catch(Exception x){
			LOG.debug("Other remote fetch exception", x);
			String errMessage=lang(req).get("remote_object_loading_error");
			String exMessage=x.getMessage();
			if(StringUtils.isNotEmpty(exMessage)){
				errMessage+="<br><br>"+Utils.escapeHTML(exMessage);
			}
			return new JsonObjectBuilder().add("error", errMessage).build();
		}
		if(obj instanceof ForeignUser user){
			UserStorage.putOrUpdateForeignUser(user);
			return new JsonObjectBuilder().add("success", user.getProfileURL()).build();
		}else if(obj instanceof ForeignGroup group){
			group.storeDependencies(ctx);
			GroupStorage.putOrUpdateForeignGroup(group);
			return new JsonObjectBuilder().add("success", group.getProfileURL()).build();
		}else if(obj instanceof NoteOrQuestion post){
			if(post.inReplyTo==null){
				Post nativePost=post.asNativePost(ctx);
				PostStorage.putForeignWallPost(nativePost);
				try{
					ctx.getActivityPubWorker().fetchAllReplies(nativePost).get(30, TimeUnit.SECONDS);
				}catch(Throwable x){
					x.printStackTrace();
				}
				return new JsonObjectBuilder().add("success", Config.localURI("/posts/"+nativePost.id).toString()).build();
			}else{
				Future<List<Post>> future=ctx.getActivityPubWorker().fetchReplyThread(post);
				try{
					List<Post> posts=future.get(30, TimeUnit.SECONDS);
					ctx.getActivityPubWorker().fetchAllReplies(posts.get(0)).get(30, TimeUnit.SECONDS);
					Post nativePost=post.asNativePost(ctx);
					return new JsonObjectBuilder().add("success", Config.localURI("/posts/"+posts.get(0).id+"#comment"+nativePost.id).toString()).build();
				}catch(InterruptedException ignore){
				}catch(ExecutionException e){
					LOG.trace("Error fetching remote object", e);
					Throwable x=e.getCause();
					String error;
					if(x instanceof UnsupportedRemoteObjectTypeException)
						error=lang(req).get("unsupported_remote_object_type");
					else if(x instanceof ObjectNotFoundException)
						error=lang(req).get("remote_object_not_found");
					else if(x instanceof IOException)
						error=lang(req).get("remote_object_network_error");
					else
						error=x.getLocalizedMessage();
					return new JsonObjectBuilder().add("error", error).build();
				}catch(TimeoutException e){
					e.printStackTrace();
					return "";
				}
			}
		}else{
			return new JsonObjectBuilder().add("error", lang(req).get("unsupported_remote_object_type")).build();
		}
		return "";
	}

	public static Object votePoll(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		int id=parseIntOrDefault(req.queryParams("id"), 0);
		if(id==0)
			throw new ObjectNotFoundException();
		Poll poll=PostStorage.getPoll(id, null);
		if(poll==null)
			throw new ObjectNotFoundException();

		Actor owner;
		if(poll.ownerID>0){
			User _owner=UserStorage.getById(poll.ownerID);
			ensureUserNotBlocked(self.user, _owner);
			owner=_owner;
		}else{
			Group _owner=ctx.getGroupsController().getGroupOrThrow(-poll.ownerID);
			ensureUserNotBlocked(self.user, _owner);
			ctx.getPrivacyController().enforceUserAccessToGroupContent(self.user, _owner);
			owner=_owner;
		}

		String[] _options=req.queryMap("option").values();
		if(_options.length<1)
			throw new BadRequestException("options param is empty");
		if(_options.length!=1 && !poll.multipleChoice)
			throw new BadRequestException("invalid option count");
		if(_options.length>poll.options.size())
			throw new BadRequestException("invalid option count");
		int[] optionIDs=new int[_options.length];
		List<PollOption> options=new ArrayList<>(_options.length);
		for(int i=0;i<_options.length;i++){
			int optID=parseIntOrDefault(_options[i], 0);
			if(optID<=0)
				throw new BadRequestException("invalid option id '"+_options[i]+"'");
			PollOption option=null;
			for(PollOption opt:poll.options){
				if(opt.id==optID){
					option=opt;
					break;
				}
			}
			if(option==null)
				throw new BadRequestException("option with id "+optID+" does not exist in this poll");
			if(options.contains(option))
				throw new BadRequestException("option with id "+optID+" seen more than once");
			optionIDs[i]=optID;
			options.add(option);
		}

		if(poll.isExpired())
			return wrapError(req, resp, "err_poll_expired");

		int[] voteIDs=PostStorage.voteInPoll(self.user.id, poll.id, optionIDs);
		if(voteIDs==null)
			return wrapError(req, resp, "err_poll_already_voted");

		poll.numVoters++;
		for(PollOption opt:options)
			opt.numVotes++;

		ctx.getActivityPubWorker().sendPollVotes(self.user, poll, owner, options, voteIDs);
		int postID=PostStorage.getPostIdByPollId(id);
		Post post;
		if(postID>0){
			post=ctx.getWallController().getPostOrThrow(postID);
			post.poll=poll; // So the last vote time is as it was before the vote
			ctx.getWallController().sendUpdateQuestionIfNeeded(post);
		}else{
			post=null;
		}

		if(isAjax(req)){
			UserInteractions interactions=new UserInteractions();
			interactions.pollChoices=Arrays.stream(optionIDs).boxed().collect(Collectors.toList());
			RenderedTemplateResponse model=new RenderedTemplateResponse("poll", req).with("poll", poll).with("interactions", interactions);
			model.with("post", new PostViewModel(post));
			return new WebDeltaResponse(resp).setContent("poll"+poll.id, model.renderBlock("inner"));
		}

		resp.redirect(back(req));
		return "";
	}

	public static Object reportForm(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "type", "id");
		RenderedTemplateResponse model=new RenderedTemplateResponse("report_form", req);
		String rawID=req.queryParams("id");
		Actor actorForAvatar;
		String title, subtitle, boxTitle, textareaPlaceholder, titleText, otherServerDomain;
		Lang l=lang(req);
		String type=req.queryParams("type");
		switch(type){
			case "post" -> {
				int id=safeParseInt(rawID);
				Post post=ctx.getWallController().getPostOrThrow(id);
				User postAuthor=ctx.getUsersController().getUserOrThrow(post.authorID);
				actorForAvatar=postAuthor;
				title=postAuthor.getCompleteName();
				subtitle=truncateOnWordBoundary(post.text, 200);
				boxTitle=l.get(post.getReplyLevel()>0 ? "report_title_comment" : "report_title_post");
				textareaPlaceholder=l.get("report_placeholder_content");
				titleText=l.get(post.getReplyLevel()>0 ? "report_text_comment" : "report_text_post");
				otherServerDomain=Config.isLocal(post.getActivityPubID()) ? null : post.getActivityPubID().getHost();
			}
			case "user" -> {
				int id=safeParseInt(rawID);
				User user=ctx.getUsersController().getUserOrThrow(id);
				actorForAvatar=user;
				title=user.getCompleteName();
				subtitle="";
				boxTitle=l.get("report_title_user");
				titleText=l.get("report_text_user");
				textareaPlaceholder=l.get("report_placeholder_profile");
				otherServerDomain=user instanceof ForeignUser fu ? fu.domain : null;
			}
			case "group" -> {
				int id=safeParseInt(rawID);
				Group group=ctx.getGroupsController().getGroupOrThrow(id);
				actorForAvatar=group;
				title=group.name;
				subtitle="";
				boxTitle=l.get(group.isEvent() ? "report_title_event" : "report_title_group");
				titleText=l.get(group.isEvent() ? "report_text_event" : "report_text_group");
				textareaPlaceholder=l.get("report_placeholder_profile");
				otherServerDomain=group instanceof ForeignGroup fg ? fg.domain : null;
			}
			case "message" -> {
				long id=decodeLong(rawID);
				MailMessage msg=ctx.getMailController().getMessage(self.user, id, false);
				User user=ctx.getUsersController().getUserOrThrow(msg.senderID);
				actorForAvatar=user;
				title=user.getCompleteName();
				subtitle=truncateOnWordBoundary(msg.text, 200);
				boxTitle=l.get("report_title_message");
				titleText=l.get("report_text_message");
				textareaPlaceholder=l.get("report_placeholder_content");
				otherServerDomain=user instanceof ForeignUser fu ? fu.domain : null;
			}
			default -> throw new BadRequestException();
		}
		model.with("actorForAvatar", actorForAvatar)
				.with("reportTitle", title)
				.with("reportSubtitle", subtitle)
				.with("textAreaPlaceholder", textareaPlaceholder)
				.with("reportTitleText", titleText)
				.with("otherServerDomain", otherServerDomain);
		return wrapForm(req, resp, "report_form", "/system/submitReport?type="+type+"&id="+rawID, boxTitle, "send", model);
	}

	public static Object submitReport(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "type", "id");
		String rawID=req.queryParams("id");
		String type=req.queryParams("type");
		String comment=req.queryParamOrDefault("reportText", "");
		boolean forward="on".equals(req.queryParams("forward"));

		Actor target;
		List<ReportableContentObject> content;

		switch(type){
			case "post" -> {
				int id=safeParseInt(rawID);
				Post post=ctx.getWallController().getPostOrThrow(id);
				content=List.of(post);
				target=ctx.getUsersController().getUserOrThrow(post.authorID);
			}
			case "user" -> {
				int id=safeParseInt(rawID);
				target=ctx.getUsersController().getUserOrThrow(id);
				content=null;
			}
			case "group" -> {
				int id=safeParseInt(rawID);
				target=ctx.getGroupsController().getGroupOrThrow(id);
				content=null;
			}
			case "message" -> {
				long id=decodeLong(rawID);
				MailMessage msg=ctx.getMailController().getMessage(self.user, id, false);
				target=ctx.getUsersController().getUserOrThrow(msg.senderID);
				content=List.of(msg);
			}
			default -> throw new BadRequestException("invalid type");
		}

		ctx.getModerationController().createViolationReport(self.user, target, content, comment, forward);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).showSnackbar(lang(req).get("report_submitted"));
		}
		return "";
	}

	public static Object captcha(Request req, Response resp) throws IOException{
		requireQueryParams(req, "sid");
		String sid=req.queryParams("sid");
		if(sid.length()>16)
			sid=sid.substring(0, 16);

		CaptchaGenerator.Captcha c=CaptchaGenerator.generate();
		LruCache<String, CaptchaInfo> captchas=req.session().attribute("captchas");
		if(captchas==null){
			captchas=new LruCache<>(10);
			req.session().attribute("captchas", captchas);
		}
		captchas.put(sid, new CaptchaInfo(c.answer(), Instant.now()));

		resp.type("image/png");
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		ImageIO.write(c.image(), "png", out);
		return out.toByteArray();
	}
}
