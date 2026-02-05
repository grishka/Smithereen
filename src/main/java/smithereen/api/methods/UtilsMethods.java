package smithereen.api.methods;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.api.ApiCallContext;
import smithereen.api.ApiErrorException;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiValidationError;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.exceptions.RemoteObjectFetchException;
import smithereen.model.Group;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.util.CryptoUtils;

public class UtilsMethods{
	public static Object getServerTime(ApplicationContext ctx, ApiCallContext actx){
		return System.currentTimeMillis()/1000L;
	}

	public static Object loadRemoteObject(ApplicationContext ctx, ApiCallContext actx){
		String q=actx.requireParamString("q");

		record RemoteObjectResult(String type, String id, String parentType, String parentId){}

		try{
			Object obj=ctx.getSearchController().loadRemoteObject(actx.self.user, q);
			return switch(obj){
				case Post post when post.getReplyLevel()>0 -> new RemoteObjectResult("wall_comment", String.valueOf(post.id), "wall_post", String.valueOf(post.replyKey.getFirst()));
				case Post post -> new RemoteObjectResult("wall_post", String.valueOf(post.id), null, null);
				case User user -> new RemoteObjectResult("user", String.valueOf(user.id), null, null);
				case Group group -> new RemoteObjectResult("group", String.valueOf(group.id), null, null);
				case PhotoAlbum album -> new RemoteObjectResult("photo_album", album.getIdString(), null, null);
				case Photo photo -> new RemoteObjectResult("photo", photo.getIdString(), null, null);
				case Comment comment -> new RemoteObjectResult("comment", comment.getIDString(), comment.parentObjectID.getApiType(), comment.parentObjectID.getIdString());
				case BoardTopic topic -> new RemoteObjectResult("topic", topic.getIdString(), null, null);
				default -> throw new RemoteObjectFetchException(RemoteObjectFetchException.ErrorType.UNSUPPORTED_OBJECT_TYPE, null);
			};
		}catch(RemoteObjectFetchException x){
			throw actx.error(switch(x.error){
				case UNSUPPORTED_OBJECT_TYPE -> ApiErrorType.REMOTE_FETCH_UNSUPPORTED_TYPE;
				case TIMEOUT -> ApiErrorType.REMOTE_FETCH_TIMEOUT;
				case NETWORK_ERROR -> ApiErrorType.REMOTE_FETCH_NETWORK_ERROR;
				case NOT_FOUND -> ApiErrorType.REMOTE_FETCH_NOT_FOUND;
				case OTHER_ERROR -> ApiErrorType.REMOTE_FETCH_OTHER_ERROR;
			}, x.getCause()!=null ? x.getCause().getMessage() : x.getMessage());
		}
	}

	public static Object resolveScreenName(ApplicationContext ctx, ApiCallContext actx){
		String screenName=actx.requireParamString("screen_name");
		ObjectLinkResolver.UsernameResolutionResult res=ctx.getObjectLinkResolver().resolveUsernameLocally(screenName);
		record ScreenNameResult(String type, long id){}
		return new ScreenNameResult(switch(res.type()){
			case USER -> "user";
			case GROUP -> "group";
			case APPLICATION -> "application";
		}, res.localID());
	}

	public static Object testCaptcha(ApplicationContext ctx, ApiCallContext actx){
		ApiUtils.enforceCaptcha(ctx, actx);
		return true;
	}

	public static Object testValidation(ApplicationContext ctx, ApiCallContext actx){
		String successStr=Base64.getUrlEncoder().withoutPadding().encodeToString(CryptoUtils.sha256("Success!".getBytes(StandardCharsets.UTF_8)));
		if(!successStr.equals(actx.optParamString("validation_key")))
			throw new ApiErrorException(new ApiValidationError(ApiErrorType.VALIDATION_NEEDED, null, actx.params, Config.localURI("/api/testValidation?this_parameter=should%20be%20kept%20intact&this_one=as%20well").toString()));
		return true;
	}
}
