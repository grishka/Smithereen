package smithereen.routes;

import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.PropertyValue;
import smithereen.controllers.FriendsController;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.CommentViewType;
import smithereen.model.ForeignUser;
import smithereen.model.FriendshipStatus;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.SessionInfo;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.WebDeltaResponse;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.text.TextProcessor;
import smithereen.text.Whitelist;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class ProfileRoutes{
	public static Object profile(Request req, Response resp){
		ApplicationContext ctx=context(req);
		String username=req.params(":username");
		ObjectLinkResolver.UsernameResolutionResult ur;
		try{
			ur=ctx.getObjectLinkResolver().resolveUsernameLocally(username);
		}catch(ObjectNotFoundException x){
			throw new ObjectNotFoundException("err_user_not_found", x);
		}
		return switch(ur.type()){
			case USER -> userProfile(req, resp, ctx.getUsersController().getUserOrThrow(ur.localID()));
			case GROUP -> GroupsRoutes.groupProfile(req, resp, ctx.getGroupsController().getGroupOrThrow(ur.localID()));
		};
	}

	public static RenderedTemplateResponse userProfile(Request req, Response resp, User user){
		ApplicationContext ctx=context(req);
		SessionInfo info=sessionInfo(req);
		@Nullable Account self=info!=null ? info.account : null;
		Lang l=lang(req);

		ctx.getPrivacyController().enforceUserProfileAccess(self!=null ? self.user : null, user);
		boolean isSelf=self!=null && self.user.id==user.id;
		int offset=offset(req);
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();

		boolean canSeeOthers=ctx.getPrivacyController().checkUserPrivacy(self!=null ? self.user : null, user, UserPrivacySettingKey.WALL_OTHERS_POSTS);
		boolean canPost=canSeeOthers && self!=null && ctx.getPrivacyController().checkUserPrivacy(self.user, user, UserPrivacySettingKey.WALL_POSTING);
		boolean canMessage=self!=null && ctx.getPrivacyController().checkUserPrivacy(self.user, user, UserPrivacySettingKey.PRIVATE_MESSAGES);

		PaginatedList<PostViewModel> wall=PostViewModel.wrap(ctx.getWallController().getWallPosts(self!=null ? self.user : null, user, !canSeeOthers, offset, 20));
		RenderedTemplateResponse model=new RenderedTemplateResponse("profile", req)
				.pageTitle(user.getFullName())
				.with("user", user)
				.with("own", self!=null && self.user.id==user.id)
				.with("postCount", wall.total)
				.with("canPostOnWall", canPost)
				.with("canSeeOthersPosts", canSeeOthers)
				.with("canMessage", canMessage)
				.paginate(wall, "/users/"+user.id+"/wall"+(canSeeOthers ? "" : "/own")+"?offset=", null);

		ctx.getWallController().populateReposts(self!=null ? self.user : null, wall.list, 2);
		CommentViewType viewType=self!=null ? self.prefs.commentViewType : CommentViewType.THREADED;
		if(req.attribute("mobile")==null){
			ctx.getWallController().populateCommentPreviews(self!=null ? self.user : null, wall.list, viewType);
		}

		Map<Integer, UserInteractions> interactions=ctx.getWallController().getUserInteractions(wall.list, self!=null ? self.user : null);
		model.with("postInteractions", interactions);
		model.with("maxReplyDepth", PostRoutes.getMaxReplyDepth(self)).with("commentViewType", viewType);

		PostViewModel.collectActorIDs(wall.list, needUsers, needGroups);
		model.with("users", ctx.getUsersController().getUsers(needUsers));

		PaginatedList<User> friends=ctx.getFriendsController().getFriends(user, 0, 6, FriendsController.SortOrder.RANDOM);
		model.with("friendCount", friends.total).with("friends", friends.list);

		if(!isMobile(req)){
			PaginatedList<User> onlineFriends=ctx.getFriendsController().getFriends(user, 0, 6, FriendsController.SortOrder.RANDOM, true, 0);
			model.with("onlineFriendCount", onlineFriends.total).with("onlineFriends", onlineFriends.list);
		}

		if(self!=null && user.id!=self.user.id){
			PaginatedList<User> mutualFriends=ctx.getFriendsController().getMutualFriends(user, self.user, 0, 3, FriendsController.SortOrder.RANDOM);
			model.with("mutualFriendCount", mutualFriends.total).with("mutualFriends", mutualFriends.list);
		}

		ArrayList<Map<String, String>> mainFields=new ArrayList<>();
		if(user.birthDate!=null)
			mainFields.add(Map.of("name", l.get("birthday"), "value", l.formatDay(user.birthDate)));
		if(user.attachment!=null)
			user.attachment.stream()
					.map(o->o instanceof PropertyValue pv ? pv : null)
					.filter(Objects::nonNull)
					.map(pv->Map.of("name", pv.name, "value", pv.value, "html", "true"))
					.forEach(mainFields::add);
		if(StringUtils.isNotEmpty(user.hometown))
			mainFields.add(Map.of("name", l.get("profile_hometown"), "value", user.hometown));
		if(user.relationship!=null){
			User partner=null;
			if(user.relationshipPartnerID!=0){
				try{
					partner=ctx.getUsersController().getUserOrThrow(user.relationshipPartnerID);
				}catch(ObjectNotFoundException ignore){}
			}
			String relationValue;
			if(partner==null || (user.relationship.needsPartnerApproval() && partner.relationshipPartnerID!=user.id)){
				relationValue=l.get(user.relationship.getLangKey(), Map.of("ownGender", user.gender));
			}else{
				relationValue=l.get(switch(user.relationship){
					case IN_RELATIONSHIP -> "profile_relationship_in_relationship_with_X";
					case ENGAGED -> "profile_relationship_engaged_with_X";
					case MARRIED -> "profile_relationship_married_to_X";
					case IN_LOVE -> "profile_relationship_in_love_with_X";
					case COMPLICATED -> "profile_relationship_complicated_with_X";
					default -> throw new IllegalStateException("Unexpected value: " + user.relationship);
				}, Map.of("ownGender", user.gender, "partnerGender", partner.gender, "partnerName", partner.getFirstLastAndGender()));
				relationValue=TextProcessor.substituteLinks(relationValue, Map.of("partner", Map.of("href", partner.getProfileURL())));
			}
			mainFields.add(Map.of("name", l.get("profile_relationship"), "value", relationValue, "html", "true"));
		}
		model.with("mainFields", mainFields);

		ArrayList<Map<String, String>> contactsFields=new ArrayList<>();
		if(StringUtils.isNotEmpty(user.location))
			contactsFields.add(Map.of("name", l.get("profile_city"), "value", user.location));
		if(StringUtils.isNotEmpty(user.website)){
			String url=TextProcessor.escapeHTML(user.website);
			contactsFields.add(Map.of("name", l.get("profile_website"), "value", "<a href=\""+url+"\" rel=\"me noopener ugc\" target=\"_blank\">"+url+"</a>", "html", "true"));
		}
		for(User.ContactInfoKey key:User.ContactInfoKey.values()){
			if(user.contacts.containsKey(key)){
				HashMap<String, String> field=new HashMap<>();
				String value=user.contacts.get(key);
				if(key==User.ContactInfoKey.GIT){
					field.put("name", switch(URI.create(value).getHost()){
						case "github.com" -> "GitHub";
						case "gitlab.com" -> "GitLab";
						case null, default -> "Git";
					});
				}else{
					field.put("name", key.isLocalizable() ? l.get(key.getLangKey()) : key.getFieldName());
				}
				String url=TextProcessor.getContactInfoValueURL(key, value);
				if(url!=null){
					if(key==User.ContactInfoKey.GIT){
						URI uri=URI.create(value);
						String path=uri.getPath();
						if(path.length()>1)
							path=path.substring(1);
						if(path.indexOf('/')==-1 && ("github.com".equals(uri.getHost()) || "gitlab.com".equals(uri.getHost()))){
							value=path;
						}
					}
					url=TextProcessor.escapeHTML(url);
					String v="<a href=\""+url+"\"";
					if(url.startsWith("http"))
						v+=" target=\"_blank\" rel=\"me noopener ugc\"";
					v+=">"+TextProcessor.escapeHTML(value)+"</a>";
					field.put("value", v);
					field.put("html", "true");
				}else{
					field.put("value", value);
				}
				contactsFields.add(field);
			}
		}
		model.with("contactsFields", contactsFields);

		ArrayList<Map<String, String>> personalFields=new ArrayList<>();
		if(user.politicalViews!=null)
			personalFields.add(Map.of("name", l.get(isMobile(req) ? "profile_political_views" : "profile_political_views_short"), "value", l.get(user.politicalViews.getLangKey())));
		if(StringUtils.isNotEmpty(user.religion))
			personalFields.add(Map.of("name", l.get(isMobile(req) ? "profile_religion" : "profile_religion_short"), "value", user.religion));
		if(user.personalPriority!=null)
			personalFields.add(Map.of("name", l.get("profile_personal_priority"), "value", l.get(user.personalPriority.getLangKey())));
		if(user.peoplePriority!=null)
			personalFields.add(Map.of("name", l.get("profile_people_priority"), "value", l.get(user.peoplePriority.getLangKey())));
		if(user.smokingViews!=null)
			personalFields.add(Map.of("name", l.get(isMobile(req) ? "profile_views_on_smoking" : "profile_views_on_smoking_short"), "value", l.get(user.smokingViews.getLangKey())));
		if(user.alcoholViews!=null)
			personalFields.add(Map.of("name", l.get(isMobile(req) ? "profile_views_on_alcohol" : "profile_views_on_alcohol_short"), "value", l.get(user.alcoholViews.getLangKey())));
		if(StringUtils.isNotEmpty(user.inspiredBy))
			personalFields.add(Map.of("name", l.get("profile_inspired_by"), "value", user.inspiredBy));
		model.with("personalFields", personalFields);

		ArrayList<Map<String, String>> interestsFields=new ArrayList<>();
		if(StringUtils.isNotEmpty(user.activities))
			interestsFields.add(Map.of("name", l.get("profile_activities"), "value", user.activities));
		if(StringUtils.isNotEmpty(user.interests))
			interestsFields.add(Map.of("name", l.get("profile_interests"), "value", user.interests));
		if(StringUtils.isNotEmpty(user.favoriteMusic))
			interestsFields.add(Map.of("name", l.get("profile_music"), "value", user.favoriteMusic));
		if(StringUtils.isNotEmpty(user.favoriteMovies))
			interestsFields.add(Map.of("name", l.get("profile_movies"), "value", user.favoriteMovies));
		if(StringUtils.isNotEmpty(user.favoriteTvShows))
			interestsFields.add(Map.of("name", l.get("profile_tv_shows"), "value", user.favoriteTvShows));
		if(StringUtils.isNotEmpty(user.favoriteBooks))
			interestsFields.add(Map.of("name", l.get("profile_books"), "value", user.favoriteBooks));
		if(StringUtils.isNotEmpty(user.favoriteGames))
			interestsFields.add(Map.of("name", l.get("profile_games"), "value", user.favoriteGames));
		if(StringUtils.isNotEmpty(user.favoriteQuotes))
			interestsFields.add(Map.of("name", l.get("profile_quotes"), "value", user.favoriteQuotes));
		if(StringUtils.isNotEmpty(user.summary))
			interestsFields.add(Map.of("name", l.get("profile_about"), "value", user.summary, "html", "true"));
		model.with("interestsFields", interestsFields);

		if(info!=null && self!=null){
			model.with("draftAttachments", info.postDraftAttachments);
		}
		if(self!=null){
			if(user.id==self.user.id){
				// lang keys for profile picture update UI
				jsLangKey(req, "update_avatar_title", "update_avatar_intro", "update_avatar_formats", "update_avatar_footer", "update_avatar_crop_title", "update_avatar_crop_explanation1",
						"update_avatar_crop_explanation2", "update_avatar_thumb_title", "update_avatar_thumb_explanation1", "update_avatar_thumb_explanation2", "choose_file", "save_and_continue", "go_back",
						"remove_profile_picture", "confirm_remove_profile_picture");
			}else{
				FriendshipStatus status=ctx.getFriendsController().getFriendshipStatus(self.user, user);
				if(status==FriendshipStatus.FRIENDS){
					ctx.getFriendsController().incrementHintsRank(self.user, user, 1);
					model.with("isFriend", true);
					model.with("friendshipStatusText", lang(req).get("X_is_your_friend", Map.of("name", user.firstName)));
				}else if(status==FriendshipStatus.REQUEST_SENT){
					model.with("friendRequestSent", true);
					model.with("friendshipStatusText", lang(req).get("you_sent_friend_req_to_X", Map.of("name", user.getFirstAndGender())));
				}else if(status==FriendshipStatus.REQUEST_RECVD){
					model.with("friendRequestRecvd", true);
					model.with("friendshipStatusText", lang(req).get("X_sent_you_friend_req", Map.of("gender", user.gender, "name", user.firstName)));
				}else if(status==FriendshipStatus.FOLLOWING){
					model.with("following", true);
					model.with("friendshipStatusText", lang(req).get("you_are_following_X", Map.of("name", user.getFirstAndGender())));
				}else if(status==FriendshipStatus.FOLLOWED_BY){
					model.with("followedBy", true);
					model.with("friendshipStatusText", lang(req).get("X_is_following_you", Map.of("gender", user.gender, "name", user.firstName)));
				}else if(status==FriendshipStatus.FOLLOW_REQUESTED){
					model.with("followRequested", true);
					model.with("friendshipStatusText", lang(req).get("waiting_for_X_to_accept_follow_req", Map.of("gender", user.gender, "name", user.firstName)));
				}
				model.with("isBlocked", ctx.getUsersController().isUserBlocked(self.user, user));
				model.with("isSelfBlocked", ctx.getUsersController().isUserBlocked(user, self.user));
				model.with("isBookmarked", ctx.getBookmarksController().isUserBookmarked(self.user, user));
				if(status==FriendshipStatus.FRIENDS || status==FriendshipStatus.FOLLOWING){
					model.with("isMuted", ctx.getFriendsController().isUserMuted(self.user, user))
							.with("canMute", true);
				}
				jsLangKey(req, "block", "unblock", "unfollow", "remove_friend");
				if(status==FriendshipStatus.FRIENDS && !isMobile(req)){
					addFriendLists(self.user, l, ctx, model);
					Set<Integer> lists=ctx.getFriendsController().getFriendListsForUsers(self.user, self.user, List.of(user.id))
							.getOrDefault(user.id, new BitSet())
							.stream()
							.map(i->i+1)
							.boxed()
							.collect(Collectors.toSet());
					model.with("userLists", lists);
				}
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
				URI img=user.getAvatar().getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_XLARGE, SizedImage.Format.JPEG);
				if(img!=null){
					SizedImage.Dimensions size=user.getAvatar().getDimensionsForSize(SizedImage.Type.AVA_SQUARE_XLARGE);
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

		if(user.movedTo>0){
			User newProfile=ctx.getUsersController().getUserOrThrow(user.movedTo);
			model.with("movedTo", newProfile);
		}

		PaginatedList<PhotoAlbum> albums;
		if(isMobile(req))
			albums=ctx.getPhotosController().getMostRecentAlbums(user, self!=null ? self.user : null, 1, true);
		else
			albums=ctx.getPhotosController().getRandomAlbumsForProfile(user, self!=null ? self.user : null, 2);
		model.with("albums", albums.list)
				.with("photoAlbumCount", albums.total)
				.with("covers", ctx.getPhotosController().getPhotosIgnoringPrivacy(albums.list.stream().map(a->a.coverID).filter(id->id!=0).collect(Collectors.toSet())));

		model.addNavBarItem(user.getFullName(), null, isSelf ? l.get("this_is_you") : null);

		model.with("groups", ctx.getGroupsController().getUserGroups(user, self!=null ? self.user : null, 0, 100).list);
		jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm", "remove_friend", "cancel", "delete",
				"mail_tab_compose", "send");
		Templates.addJsLangForNewPostForm(req);

		try{
			Photo photo=switch(user.getAvatarImage()){
				case LocalImage li when li.photoID!=0 -> ctx.getPhotosController().getPhotoIgnoringPrivacy(li.photoID);
				case Image img when img.photoApID!=null -> ctx.getObjectLinkResolver().resolveLocally(img.photoApID, Photo.class);
				case null, default -> null;
			};
			if(photo!=null){
				model.with("avatarPvInfo", new PhotoViewerInlineData(0, "albums/"+XTEA.encodeObjectID(photo.albumID, ObfuscatedObjectIDType.PHOTO_ALBUM), photo.image.getURLsForPhotoViewer()))
						.with("avatarPhoto", photo);
			}
		}catch(ObjectNotFoundException ignore){}

		PaginatedList<User> following=ctx.getFriendsController().getFollows(user, 0, 6);
		PaginatedList<User> followers=ctx.getFriendsController().getFollowers(user, 0, 6);
		model.with("followees", following)
				.with("followers", followers);

		if(!isMobile(req)){
			try{
				PhotoAlbum taggedPhotos=ctx.getPhotosController().getUserTaggedPhotosPseudoAlbum(self!=null ? self.user : null, user);
				if(taggedPhotos!=null)
					model.with("taggedPhotoCount", taggedPhotos.numPhotos);
			}catch(UserActionNotAllowedException ignore){}
		}

		model.with("presence", ctx.getUsersController().getUserPresence(user));

		return model;
	}

	public static Object confirmBlockUser(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=getUserOrThrow(req);
		Lang l=lang(req);
		String back=back(req);
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("confirm_block_user_X", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/users/"+user.id+"/block?_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object confirmUnblockUser(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=getUserOrThrow(req);
		Lang l=lang(req);
		String back=back(req);
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

	public static Object mentionHoverCard(Request req, Response resp){
		if(isMobile(req))
			return "";
		ApplicationContext ctx=context(req);
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		SessionInfo info=sessionInfo(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("user_hover_card", req).with("user", user);
		if(info!=null && info.account!=null){
			User self=info.account.user;
			if(self.id!=user.id){
				PaginatedList<User> friends=ctx.getFriendsController().getMutualFriends(self, user, 0, 6, FriendsController.SortOrder.ID_ASCENDING);
				model.with("mutualFriends", friends);
			}
		}
		return model;
	}

	public static Object muteUser(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		return setUserMuted(req, resp, info, ctx, true);
	}

	public static Object unmuteUser(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		return setUserMuted(req, resp, info, ctx, false);
	}

	private static Object setUserMuted(Request req, Response resp, SessionInfo info, ApplicationContext ctx, boolean muted){
		User user=getUserOrThrow(req);
		ctx.getFriendsController().setUserMuted(info.account.user, user, muted);
		if(isAjax(req)){
			if(isMobile(req)){
				RenderedTemplateResponse profile=ProfileRoutes.userProfile(req, resp, user);
				return new WebDeltaResponse(resp)
						.setOuterHTML("profileFriendButton", profile.renderBlock("friendButton"))
						.showSnackbar(lang(req).get(muted ? "profile_user_muted" : "profile_user_unmuted", Map.of("name", user.getFirstAndGender())));
			}else{
				return new WebDeltaResponse(resp)
						.setContent("profileMuteButtonText", lang(req).get(muted ? "profile_unmute" : "profile_mute", Map.of("name", user.getFirstAndGender())))
						.setAttribute("profileMuteButton", "href", "/users/"+user.id+"/"+(muted ? "unmute" : "mute")+"?csrf="+info.csrfToken);
			}
		}
		resp.redirect(back(req));
		return "";
	}
}
