package smithereen.routes;

import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static smithereen.Utils.*;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.PropertyValue;
import smithereen.controllers.FriendsController;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.PaginatedList;
import smithereen.data.SessionInfo;
import smithereen.data.SizedImage;
import smithereen.data.User;
import smithereen.data.UserInteractions;
import smithereen.data.UserPrivacySettingKey;
import smithereen.data.WebDeltaResponse;
import smithereen.data.viewmodel.PostViewModel;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.util.Whitelist;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class ProfileRoutes{
	public static Object profile(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		ApplicationContext ctx=context(req);
		String username=req.params(":username");
		Lang l=lang(req);
		ObjectLinkResolver.UsernameResolutionResult ur;
		try{
			ur=ctx.getObjectLinkResolver().resolveUsernameLocally(username);
		}catch(ObjectNotFoundException x){
			throw new ObjectNotFoundException("err_user_not_found", x);
		}
		return switch(ur.type()){
			case USER -> {
				User user=ctx.getUsersController().getUserOrThrow(ur.localID());
				boolean isSelf=self!=null && self.user.id==user.id;
				int offset=offset(req);
				HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();

				boolean canSeeOthers=ctx.getPrivacyController().checkUserPrivacy(self!=null ? self.user : null, user, UserPrivacySettingKey.WALL_OTHERS_POSTS);
				boolean canPost=canSeeOthers && self!=null && ctx.getPrivacyController().checkUserPrivacy(self.user, user, UserPrivacySettingKey.WALL_POSTING);

				PaginatedList<PostViewModel> wall=PostViewModel.wrap(ctx.getWallController().getWallPosts(user, !canSeeOthers, offset, 20));
				RenderedTemplateResponse model=new RenderedTemplateResponse("profile", req)
						.pageTitle(user.getFullName())
						.with("user", user)
						.with("own", self!=null && self.user.id==user.id)
						.with("postCount", wall.total)
						.with("canPostOnWall", canPost)
						.with("canSeeOthersPosts", canSeeOthers)
						.paginate(wall);

				if(req.attribute("mobile")==null){
					ctx.getWallController().populateCommentPreviews(wall.list);
				}

				Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(wall.list, self!=null ? self.user : null);
				model.with("postInteractions", interactions);

				PostViewModel.collectActorIDs(wall.list, needUsers, needGroups);
				model.with("users", ctx.getUsersController().getUsers(needUsers));

				PaginatedList<User> friends=ctx.getFriendsController().getFriends(user, 0, 6, FriendsController.SortOrder.RANDOM);
				model.with("friendCount", friends.total).with("friends", friends.list);

				if(self!=null && user.id!=self.user.id){
					PaginatedList<User> mutualFriends=ctx.getFriendsController().getMutualFriends(user, self.user, 0, 3, FriendsController.SortOrder.RANDOM);
					model.with("mutualFriendCount", mutualFriends.total).with("mutualFriends", mutualFriends.list);
				}

				ArrayList<PropertyValue> profileFields=new ArrayList<>();
				if(user.birthDate!=null)
					profileFields.add(new PropertyValue(l.get("birth_date"), l.formatDay(user.birthDate)));
				if(StringUtils.isNotEmpty(user.summary))
					profileFields.add(new PropertyValue(l.get("profile_about"), user.summary));
				if(user.attachment!=null)
					user.attachment.stream().filter(o->o instanceof PropertyValue).forEach(o->profileFields.add((PropertyValue) o));
				model.with("profileFields", profileFields);

				if(info!=null && self!=null){
					model.with("draftAttachments", info.postDraftAttachments);
				}
				if(self!=null){
					if(user.id==self.user.id){
						// lang keys for profile picture update UI
						jsLangKey(req, "update_profile_picture", "save", "profile_pic_select_square_version", "drag_or_choose_file", "choose_file",
								"drop_files_here", "picture_too_wide", "picture_too_narrow", "ok", "error", "error_loading_picture",
								"remove_profile_picture", "confirm_remove_profile_picture", "choose_file_mobile");
					}else{
						FriendshipStatus status=ctx.getFriendsController().getFriendshipStatus(self.user, user);
						if(status==FriendshipStatus.FRIENDS){
							model.with("isFriend", true);
							model.with("friendshipStatusText", Utils.lang(req).get("X_is_your_friend", Map.of("name", user.firstName)));
						}else if(status==FriendshipStatus.REQUEST_SENT){
							model.with("friendRequestSent", true);
							model.with("friendshipStatusText", Utils.lang(req).get("you_sent_friend_req_to_X", Map.of("name", user.getFirstAndGender())));
						}else if(status==FriendshipStatus.REQUEST_RECVD){
							model.with("friendRequestRecvd", true);
							model.with("friendshipStatusText", Utils.lang(req).get("X_sent_you_friend_req", Map.of("gender", user.gender, "name", user.firstName)));
						}else if(status==FriendshipStatus.FOLLOWING){
							model.with("following", true);
							model.with("friendshipStatusText", Utils.lang(req).get("you_are_following_X", Map.of("name", user.getFirstAndGender())));
						}else if(status==FriendshipStatus.FOLLOWED_BY){
							model.with("followedBy", true);
							model.with("friendshipStatusText", Utils.lang(req).get("X_is_following_you", Map.of("gender", user.gender, "name", user.firstName)));
						}else if(status==FriendshipStatus.FOLLOW_REQUESTED){
							model.with("followRequested", true);
							model.with("friendshipStatusText", Utils.lang(req).get("waiting_for_X_to_accept_follow_req", Map.of("gender", user.gender, "name", user.firstName)));
						}
						model.with("isBlocked", ctx.getUsersController().isUserBlocked(self.user, user));
						model.with("isSelfBlocked", ctx.getUsersController().isUserBlocked(user, self.user));
						jsLangKey(req, "block", "unblock", "unfollow", "remove_friend");
					}
				}else{
					HashMap<String, String> meta=new LinkedHashMap<>();
					meta.put("og:type", "profile");
					meta.put("og:site_name", Config.serverDisplayName);
					meta.put("og:title", user.getFullName());
					meta.put("og:url", user.url.toString());
					meta.put("og:username", user.getFullUsername());
					if(StringUtils.isNotEmpty(user.firstName))
						meta.put("og:first_name", user.firstName);
					if(StringUtils.isNotEmpty(user.lastName))
						meta.put("og:last_name", user.lastName);
					String descr=l.get("X_friends", Map.of("count", friends.total))+", "+l.get("X_posts", Map.of("count", wall.total));
					if(StringUtils.isNotEmpty(user.summary))
						descr+="\n"+Jsoup.clean(user.summary, Whitelist.none());
					meta.put("og:description", descr);
					if(user.gender==User.Gender.MALE)
						meta.put("og:gender", "male");
					else if(user.gender==User.Gender.FEMALE)
						meta.put("og:gender", "female");
					if(user.hasAvatar()){
						URI img=user.getAvatar().getUriForSizeAndFormat(SizedImage.Type.LARGE, SizedImage.Format.JPEG);
						if(img!=null){
							SizedImage.Dimensions size=user.getAvatar().getDimensionsForSize(SizedImage.Type.LARGE);
							meta.put("og:image", img.toString());
							meta.put("og:image:width", size.width+"");
							meta.put("og:image:height", size.height+"");
						}
					}
					model.with("metaTags", meta);
					model.with("moreMetaTags", Map.of("description", descr));
				}

				if(user instanceof ForeignUser)
					model.with("noindex", true);
				model.with("activityPubURL", user.activityPubID);

				model.addNavBarItem(user.getFullName(), null, isSelf ? l.get("this_is_you") : null);

				model.with("groups", ctx.getGroupsController().getUserGroups(user, self!=null ? self.user : null, 0, 100).list);
				jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "remove_friend", "cancel", "delete");
				Templates.addJsLangForNewPostForm(req);
				yield model;
			}
			case GROUP -> GroupsRoutes.groupProfile(req, resp, ctx.getGroupsController().getGroupOrThrow(ur.localID()));
		};
	}

	public static Object confirmBlockUser(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=getUserOrThrow(req);
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_block_user_X", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/users/"+user.id+"/block?_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object confirmUnblockUser(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=getUserOrThrow(req);
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_unblock_user_X", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/users/"+user.id+"/unblock?_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object blockUser(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=getUserOrThrow(req);
		ctx.getFriendsController().blockUser(self.user, user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object unblockUser(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=getUserOrThrow(req);
		ctx.getFriendsController().unblockUser(self.user, user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}


	private static User getUserOrThrow(Request req){
		int id=parseIntOrDefault(req.params(":id"), 0);
		return context(req).getUsersController().getUserOrThrow(id);
	}

	public static Object syncRelationshipsCollections(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=getUserOrThrow(req);
		user.ensureRemote();
		ctx.getActivityPubWorker().fetchActorRelationshipCollections(user);
		Lang l=lang(req);
		return new WebDeltaResponse(resp).messageBox(l.get("sync_friends_and_groups"), l.get("sync_started"), l.get("ok"));
	}

	public static Object syncProfile(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=getUserOrThrow(req);
		user.ensureRemote();
		ctx.getObjectLinkResolver().resolve(user.activityPubID, ForeignUser.class, true, true, true);
		return new WebDeltaResponse(resp).refresh();
	}

	public static Object syncContentCollections(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=getUserOrThrow(req);
		user.ensureRemote();
		ctx.getActivityPubWorker().fetchActorContentCollections(user);
		Lang l=lang(req);
		return new WebDeltaResponse(resp).messageBox(l.get("sync_content"), l.get("sync_started"), l.get("ok"));
	}
}
