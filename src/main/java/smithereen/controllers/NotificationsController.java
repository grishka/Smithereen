package smithereen.controllers;

import com.google.gson.JsonObject;

import org.eclipse.jetty.websocket.api.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.Mailer;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.MailMessage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.OwnedContentObject;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.PostLikeObject;
import smithereen.model.SessionInfo;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.notifications.UserNotifications;
import smithereen.model.UserPresence;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.notifications.EmailNotificationFrequency;
import smithereen.model.notifications.EmailNotificationType;
import smithereen.model.notifications.GroupedNotification;
import smithereen.model.notifications.Notification;
import smithereen.model.notifications.NotificationWrapper;
import smithereen.model.notifications.RealtimeNotification;
import smithereen.model.photos.Photo;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.SessionStorage;
import smithereen.text.TextProcessor;
import smithereen.util.BackgroundTaskRunner;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public class NotificationsController{
	private static final Logger LOG=LoggerFactory.getLogger(NotificationsController.class);

	private final ApplicationContext context;

	private final HashMap<Integer, List<WebSocketConnection>> wsConnectionsByUserID=new HashMap<>();
	private final HashMap<Session, WebSocketConnection> wsConnectionsByConnection=new HashMap<>();
	private final Object wsMapsLock=new Object();

	public NotificationsController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<NotificationWrapper> getNotifications(Account self, int offset, int count, int maxID){
		try{
			PaginatedList<Notification> page;
			List<NotificationWrapper> notifications=new ArrayList<>();
			int lastSeenID=self.prefs.lastSeenNotificationID;
			HashMap<NotificationGroupingKey, GroupedNotification> groupedNotifications=new HashMap<>();
			int actualOffset=offset;
			int totalNotifications=0;
			do{
				page=NotificationsStorage.getNotifications(self.user.id, actualOffset, 50, maxID);
				for(Notification n:page.list){
					if(n.type.canBeGrouped()){
						NotificationGroupingKey key=new NotificationGroupingKey(n.type, n.objectType, n.objectID, LocalDate.ofInstant(n.time, self.prefs.timeZone), n.id<=lastSeenID);
						GroupedNotification group=groupedNotifications.get(key);
						if(group==null){
							group=new GroupedNotification();
							groupedNotifications.put(key, group);
							notifications.add(group);
						}
						group.notifications.add(n);
					}else{
						notifications.add(n);
					}
				}
				actualOffset+=page.list.size();
				totalNotifications+=page.list.size();
			}while(notifications.size()<count && page.offset+page.perPage<page.total);
			return new PaginatedList<>(notifications, notifications.isEmpty() ? offset : (offset+totalNotifications+1), offset, totalNotifications);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setNotificationsSeen(Account self, int lastSeenID){
		try{
			if(lastSeenID>self.prefs.lastSeenNotificationID){
				self.prefs.lastSeenNotificationID=lastSeenID;
				SessionStorage.updatePreferences(self.id, self.prefs);
			}
			UserNotifications un=NotificationsStorage.getNotificationsFromCache(self.user.id);
			if(un!=null){
				un.setNotificationsViewed();
			}
			sendRealtimeCountersUpdates(self.user);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void createNotificationsForObject(Object obj){
		switch(obj){
			case Post post -> {
				Post parent;
				if(post.getReplyLevel()>0){
					try{
						parent=context.getWallController().getPostOrThrow(post.replyKey.getLast());
					}catch(ObjectNotFoundException x){
						return;
					}
				}else{
					parent=null;
				}
				OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(post);
				// For a reply to a local post, notify the parent post author about the reply
				if(parent!=null && parent.isLocal() && parent.authorID!=post.authorID){
					User parentAuthor=context.getUsersController().getUserOrThrow(parent.authorID);
					createNotification(parentAuthor, Notification.Type.REPLY, post, parent, oaa.author());
				}

				// Notify every mentioned local user, except the parent post author, if any
				for(User user:context.getUsersController().getUsers(post.mentionedUserIDs).values()){
					if(user instanceof ForeignUser)
						continue;
					if(parent!=null && user.id==parent.authorID)
						continue;
					if(user.id==post.authorID)
						continue;
					createNotification(user, Notification.Type.MENTION, post, null, oaa.author());
				}

				// Finally, if it's a wall post on a local user's wall, notify them
				if(post.getReplyLevel()==0 && post.ownerID!=post.authorID && post.ownerID>0 && !(oaa.owner() instanceof ForeignUser)){
					createNotification((User)oaa.owner(), Notification.Type.POST_OWN_WALL, post, null, oaa.author());
				}

				// If this is a quote-repost of a local post, notify its author
				if(post.repostOf!=0){
					Post firstRepost=context.getWallController().getPostOrThrow(post.repostOf);
					if(firstRepost.isLocal() && post.authorID!=firstRepost.authorID){
						User repostedPostAuthor=context.getUsersController().getUserOrThrow(firstRepost.authorID);
						createNotification(repostedPostAuthor, Notification.Type.REPOST, firstRepost, post, oaa.author());
					}
				}
			}
			case Comment comment -> {
				CommentableContentObject parent=context.getCommentsController().getCommentParentIgnoringPrivacy(comment);
				OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(parent);
				User commentAuthor=context.getUsersController().getUserOrThrow(comment.authorID);
				HashSet<Integer> notifiedUsers=new HashSet<>();
				if(comment.parentObjectID.type()==CommentableObjectType.PHOTO && parent.getAuthorID()!=comment.authorID){
					// Notify the author of the photo about the new comment
					createNotification(oaa.author(), Notification.Type.REPLY, comment, parent, commentAuthor);
					notifiedUsers.add(parent.getAuthorID());
				}

				// For replies, notify the parent author about the reply
				if(comment.getReplyLevel()>0){
					Comment parentComment=context.getCommentsController().getCommentIgnoringPrivacy(comment.replyKey.getLast());
					if(parentComment.authorID!=comment.authorID && !notifiedUsers.contains(parentComment.authorID)){
						User parentCommentAuthor=context.getUsersController().getUserOrThrow(parentComment.authorID);
						createNotification(parentCommentAuthor, Notification.Type.REPLY, comment, parent, commentAuthor);
						notifiedUsers.add(parentComment.authorID);
					}
				}

				// Notify every mentioned local user, except the parent post author, if any
				for(User user:context.getUsersController().getUsers(comment.mentionedUserIDs).values()){
					if(user instanceof ForeignUser)
						continue;
					if(notifiedUsers.contains(user.id))
						continue;
					if(user.id==comment.authorID)
						continue;
					createNotification(user, Notification.Type.MENTION, comment, parent, commentAuthor);
				}
			}
			default -> throw new IllegalStateException("Unexpected value: " + obj);
		}
	}

	public void createNotification(User owner, Notification.Type type, OwnedContentObject object, OwnedContentObject relatedObject, User actor){
		if(owner instanceof ForeignUser)
			return;
		if(context.getPrivacyController().isUserBlocked(actor, owner))
			return;
		try{
			Account acc=SessionStorage.getAccountByUserID(owner.id);
			int id=NotificationsStorage.putNotification(owner.id, type, getObjectTypeForObject(object), object==null ? 0 : object.getObjectID(),
					getObjectTypeForObject(relatedObject), relatedObject==null ? 0 : relatedObject.getObjectID(), actor.id, acc.prefs.countLikesInUnread || (type!=Notification.Type.LIKE && type!=Notification.Type.REPOST && type!=Notification.Type.RETOOT));

			sendRealtimeNotifications(owner, String.valueOf(id), switch(type){
				case REPLY -> RealtimeNotification.Type.REPLY;
				case LIKE -> RealtimeNotification.Type.LIKE;
				case MENTION -> RealtimeNotification.Type.MENTION;
				case RETOOT, REPOST -> RealtimeNotification.Type.REPOST;
				case POST_OWN_WALL -> RealtimeNotification.Type.WALL_POST;
				case INVITE_SIGNUP -> RealtimeNotification.Type.INVITE_SIGNUP;
				case FOLLOW -> RealtimeNotification.Type.FOLLOW;
				case FRIEND_REQ_ACCEPT -> RealtimeNotification.Type.FRIEND_REQUEST_ACCEPTED;
			}, object, relatedObject, actor);

		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteNotificationsForObject(@NotNull OwnedContentObject obj){
		try{
			NotificationsStorage.deleteNotificationsForObject(getObjectTypeForObject(obj), obj.getObjectID());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private Notification.ObjectType getObjectTypeForObject(OwnedContentObject obj){
		return switch(obj){
			case null -> null;
			case Post post -> Notification.ObjectType.POST;
			case Photo photo -> Notification.ObjectType.PHOTO;
			case Comment comment -> Notification.ObjectType.COMMENT;
			case BoardTopic topic -> Notification.ObjectType.BOARD_TOPIC;
			default -> throw new IllegalStateException("Unexpected value: " + obj);
		};
	}

	public void registerWebSocket(SessionInfo info, Session connection, Lang lang){
		WebSocketConnection conn=new WebSocketConnection(info, connection, lang);
		synchronized(wsMapsLock){
			wsConnectionsByUserID.computeIfAbsent(info.account.user.id, id->new ArrayList<>()).add(conn);
			wsConnectionsByConnection.put(connection, conn);
		}
		LOG.debug("Websocket {} registered for realtime notifications for user {}", connection, info.account.user.id);
	}

	public void unregisterWebSocket(Session connection){
		synchronized(wsMapsLock){
			WebSocketConnection conn=wsConnectionsByConnection.remove(connection);
			if(conn!=null){
				List<WebSocketConnection> connections=wsConnectionsByUserID.get(conn.session.account.user.id);
				connections.remove(conn);
				if(connections.isEmpty())
					wsConnectionsByUserID.remove(conn.session.account.user.id);
				LOG.debug("Websocket {} unregistered for realtime notifications for user {}", connection, conn.session.account.user.id);
			}
		}
	}

	public void sendRealtimeNotifications(User user, String id, RealtimeNotification.Type type, Object object, OwnedContentObject relatedObject, Actor actor){
		Account account=context.getUsersController().getAccountForUser(user);

		Instant dayAgo=Instant.now().minus(24, ChronoUnit.HOURS);
		if((account.prefs.emailNotificationFrequency==EmailNotificationFrequency.IMMEDIATE
				|| (account.prefs.emailNotificationFrequency==EmailNotificationFrequency.DAILY && (account.prefs.lastEmailNotification==null || account.prefs.lastEmailNotification.isBefore(dayAgo))))
				&& !account.prefs.emailNotificationTypes.isEmpty()){
			UserPresence presence=context.getUsersController().getUserPresence(user);
			if(presence==null || (!presence.isOnline() && presence.lastUpdated().isBefore(dayAgo))){
				EmailNotificationType emailType=switch(type){
					case FRIEND_REQUEST -> EmailNotificationType.FRIEND_REQUEST;
					case MAIL_MESSAGE -> EmailNotificationType.MAIL;
					case PHOTO_TAG -> EmailNotificationType.PHOTO_TAG;
					case WALL_POST -> EmailNotificationType.WALL_POST;
					case REPLY -> {
						PostLikeObject plo=(PostLikeObject) object;
						if(plo.getReplyLevel()>1){
							yield EmailNotificationType.COMMENT_REPLY;
						}else if(plo instanceof Post){
							yield EmailNotificationType.WALL_COMMENT;
						}else if(plo instanceof Comment comment){
							yield switch(comment.parentObjectID.type()){
								case PHOTO -> EmailNotificationType.PHOTO_COMMENT;
								case BOARD_TOPIC -> EmailNotificationType.COMMENT_REPLY;
							};
						}else{
							throw new IllegalStateException("Unreachable");
						}
					}
					case MENTION -> EmailNotificationType.MENTION;
					case GROUP_INVITE, EVENT_INVITE -> EmailNotificationType.GROUP_INVITE;
					default -> null;
				};
				if(emailType!=null && account.prefs.emailNotificationTypes.contains(emailType)){
					sendEmailNotification(account, emailType, object, relatedObject, actor);
				}
			}
		}

		if(account.prefs.notifierTypes!=null && !account.prefs.notifierTypes.contains(type.getSettingType()))
			return;

		List<WebSocketConnection> connections=null;
		synchronized(wsMapsLock){
			List<WebSocketConnection> actualConnections=wsConnectionsByUserID.get(user.id);
			if(actualConnections!=null)
				connections=new ArrayList<>(actualConnections);
		}

		if(connections==null)
			return;

		RealtimeNotification.ObjectType objType=object instanceof OwnedContentObject owned ? getRealtimeNotificationObjectTypeForObject(owned) : null;
		for(WebSocketConnection conn:connections){
			Thread.ofVirtual().start(()->{
				Lang l=conn.lang;
				ZoneId tz=conn.session.timeZone;
				String title=l.get(switch(type){
					case REPLY -> ((PostLikeObject)object).getReplyLevel()>1 || relatedObject instanceof BoardTopic ? "notification_title_reply" : "notification_title_comment";
					case LIKE -> switch(objType){
						case POST -> ((Post)object).getReplyLevel()>0 ? "notification_title_like_comment" : "notification_title_like_post";
						case PHOTO -> "notification_title_like_photo";
						case PHOTO_COMMENT, BOARD_COMMENT -> "notification_title_like_comment";
						case null, default -> throw new IllegalStateException("Unexpected value: " + objType);
					};
					case MENTION -> "notification_title_mention";
					case REPOST -> ((Post)object).getReplyLevel()>0 ? "notification_title_repost_comment" : "notification_title_repost_post";
					case WALL_POST -> "notification_title_wall_post";
					case INVITE_SIGNUP -> "notification_title_invite_signup";
					case FOLLOW -> "notification_title_follow";
					case FRIEND_REQUEST -> "notification_title_friend_request";
					case FRIEND_REQUEST_ACCEPTED -> "notification_title_friend_request_accepted";
					case MAIL_MESSAGE -> "notification_title_new_message";
					case GROUP_INVITE -> "notification_title_group_invite";
					case EVENT_INVITE -> "notification_title_event_invite";
					case GROUP_REQUEST_ACCEPTED -> "notification_title_group_request_accepted";
					case PHOTO_TAG -> "notification_title_photo_tag";
				});
				String content=switch(type){
					case REPLY -> {
						PostLikeObject comment=(PostLikeObject) object;
						String preview=comment.getShortTitle();
						if(StringUtils.isNotEmpty(preview))
							yield makeActorLink(actor)+" "+preview;
						String text;
						User u=(User)actor;
						String objURL="";
						if(comment.getReplyLevel()==1 && !(relatedObject instanceof BoardTopic)){
							if(comment instanceof Comment c){
								text=switch(c.parentObjectID.type()){
									case PHOTO -> l.get("notification_content_comment_photo", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender));
									case BOARD_TOPIC -> null; // Comments on a board topic itself (no reply) don't make notifications
								};
								objURL=((CommentableContentObject)relatedObject).getURL();
							}else{
								text=l.get("notification_content_comment_post", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender, "parentText", makePostPreview((Post) relatedObject, l, tz)));
							}
						}else{
							text=l.get("notification_content_reply", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender, "parentText", makePostPreview((Post) relatedObject, l, tz)));
						}
						yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL()), "object", Map.of("href", objURL)));
					}
					case LIKE -> switch(object){
						case Post post -> {
							String text;
							User u=(User)actor;
							if(post.getReplyLevel()==0)
								text=l.get("notification_content_like_post", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender, "text", makePostPreview(post, l, tz)));
							else
								text=l.get("notification_content_like_post_comment", Map.of("name", u.getFirstLastAndGender(),
										"gender", u.gender, "text", makePostPreview(post, l, tz), "parentText", makePostPreview((Post)relatedObject, l, tz)));
							yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL())));
						}
						case Photo photo -> {
							User u=(User)actor;
							String text=l.get("notification_content_like_photo", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender));
							yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL()), "photo", Map.of("href", photo.getURL())));
						}
						case Comment comment -> {
							User u=(User)actor;
							CommentableContentObject parent=(CommentableContentObject) relatedObject;
							String text=switch(parent.getCommentParentID().type()){
								case PHOTO -> l.get("notification_content_like_photo_comment", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender, "text", makePostPreview(comment, l, tz)));
								case BOARD_TOPIC -> l.get("notification_content_like_board_comment", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender, "text", makePostPreview(comment, l, tz),
										"topic", TextProcessor.truncateOnWordBoundary(((BoardTopic)parent).title, 40)));
							};
							yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL()), "object", Map.of("href", parent.getURL())));
						}
						case null, default -> throw new IllegalStateException("Unexpected value: " + objType);
					};
					case MENTION -> makeActorLink(actor)+" "+((PostLikeObject)object).getShortTitle();
					case REPOST -> {
						String text;
						User u=(User) actor;
						Post post=(Post) object;
						if(post.getReplyLevel()==0)
							text=l.get("notification_content_repost_post", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender, "text", post.getShortTitle()));
						else
							text=l.get("notification_content_repost_post_comment", Map.of("name", u.getFirstLastAndGender(),
									"gender", u.gender, "text", makePostPreview(post, l, tz), "parentText", makePostPreview((Post)relatedObject, l, tz)));
						yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL())));
					}
					case WALL_POST -> {
						Post post=(Post) object;
						String preview=post.getShortTitle();
						if(StringUtils.isNotEmpty(preview))
							yield makeActorLink(actor)+" "+preview;
						User u=(User)actor;
						String text=l.get("notification_content_comment_post", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender));
						yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL())));
					}
					case INVITE_SIGNUP -> makeActorLink(actor)+" "+l.get("notification_invite_signup", Map.of("gender", ((User)actor).gender));
					case FOLLOW -> makeActorLink(actor)+" "+l.get("notification_follow", Map.of("gender", ((User)actor).gender));
					case FRIEND_REQUEST -> {
						User u=(User)actor;
						String text=l.get("notification_content_friend_request", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender));
						yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL())));
					}
					case FRIEND_REQUEST_ACCEPTED -> makeActorLink(actor)+" "+l.get("notification_friend_req_accept", Map.of("gender", ((User)actor).gender));
					case MAIL_MESSAGE -> {
						if(conn.session.account.prefs.notifierShowMessageText){
							MailMessage msg=(MailMessage) object;
							String preview=msg.getTextPreview();
							if(StringUtils.isNotEmpty(preview))
								yield makeActorLink(actor)+" "+preview;
						}
						User u=(User)actor;
						String text=l.get("notification_content_mail_message", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender));
						yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL())));
					}
					case GROUP_INVITE -> {
						User u=(User)actor;
						Group g=(Group)object;
						String text=l.get("notification_content_group_invite", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender, "groupName", g.name));
						yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL()), "group", Map.of("href", g.getProfileURL())));
					}
					case EVENT_INVITE -> {
						User u=(User)actor;
						Group g=(Group)object;
						String text=l.get("notification_content_event_invite", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender, "eventName", g.name));
						yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL()), "event", Map.of("href", g.getProfileURL())));
					}
					case GROUP_REQUEST_ACCEPTED -> {
						String text=l.get("notification_content_group_accepted", Map.of("name", actor.getName()));
						yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL())));
					}
					case PHOTO_TAG -> {
						User u=(User)actor;
						String text=l.get("notification_content_photo_tag", Map.of("name", u.getFirstLastAndGender(), "gender", u.gender));
						yield TextProcessor.substituteLinks(text, Map.of("actor", Map.of("href", actor.getProfileURL())));
					}
				};
				String url=switch(object){
					case Post post -> post.getReplyLevel()>0 && relatedObject instanceof Post parentPost ? parentPost.getInternalURL().toString()+"#comment"+post.id : post.getInternalURL().toString();
					case Photo photo -> photo.getURL();
					case Comment comment when relatedObject instanceof CommentableContentObject parent -> parent.getURL()+"#comment"+comment.getIDString();
					case MailMessage msg -> "/my/mail/messages/"+msg.encodedID;
					case null, default -> actor.getProfileURL();
				};
				String objID=switch(object){
					case Post post -> String.valueOf(post.id);
					case Photo photo -> XTEA.encodeObjectID(photo.id, ObfuscatedObjectIDType.PHOTO);
					case Comment comment -> XTEA.encodeObjectID(comment.id, ObfuscatedObjectIDType.COMMENT);
					case MailMessage msg -> msg.encodedID;
					case null, default -> null;
				};

				RealtimeNotification.ImageURLs ava;
				if(actor.hasAvatar()){
					SizedImage actorAva=actor.getAvatar();
					ava=new RealtimeNotification.ImageURLs(
							actorAva.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_SMALL, SizedImage.Format.JPEG).toString(),
							actorAva.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_SMALL, SizedImage.Format.WEBP).toString(),
							actorAva.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_MEDIUM, SizedImage.Format.JPEG).toString(),
							actorAva.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_MEDIUM, SizedImage.Format.WEBP).toString()
					);
				}else{
					ava=null;
				}

				Map<String, String> extraAttrs=null;
				RealtimeNotification.ImageURLs extraImage;
				if(relatedObject==null || relatedObject instanceof PostLikeObject){
					if(object instanceof PostLikeObject post){
						if(post.attachments!=null && !post.attachments.isEmpty()){
							Attachment att=post.getProcessedAttachments().getFirst();
							if(att instanceof PhotoAttachment pa){
								String jpeg=pa.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_THUMB_SMALL, SizedImage.Format.JPEG).toString();
								String webp=pa.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_THUMB_SMALL, SizedImage.Format.WEBP).toString();
								extraImage=new RealtimeNotification.ImageURLs(jpeg, webp, jpeg, webp);
							}else{
								extraImage=null;
							}
						}else{
							extraImage=null;
						}
						if(post.getReplyLevel()>0 && post instanceof Post realPost){
							extraAttrs=Map.of(
									"onclick", "return openPostLayer('"+realPost.replyKey.getFirst()+"', '"+post.getObjectID()+"')"
							);
						}else{
							extraAttrs=Map.of(
									"onclick", "return openPostLayer('"+post.getObjectID()+"')"
							);
						}
					}else if(object instanceof Group group){
						SizedImage groupAva=group.getAvatar();
						extraImage=new RealtimeNotification.ImageURLs(
								groupAva.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_SMALL, SizedImage.Format.JPEG).toString(),
								groupAva.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_SMALL, SizedImage.Format.WEBP).toString(),
								groupAva.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_MEDIUM, SizedImage.Format.JPEG).toString(),
								groupAva.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_MEDIUM, SizedImage.Format.WEBP).toString()
						);
					}else if(object instanceof Photo photo){
						String jpeg=photo.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_THUMB_SMALL, SizedImage.Format.JPEG).toString();
						String webp=photo.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_THUMB_SMALL, SizedImage.Format.WEBP).toString();
						extraImage=new RealtimeNotification.ImageURLs(jpeg, webp, jpeg, webp);
						extraAttrs=Map.of(
								"onclick", "return openPhotoViewer(this)",
								"data-pv", Utils.gson.toJson(new PhotoViewerInlineData(0, "single/"+photo.getIdString(), photo.image.getURLsForPhotoViewer()))
						);
					}else{
						extraImage=null;
					}
				}else if(relatedObject instanceof Photo photo){
					String jpeg=photo.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_THUMB_SMALL, SizedImage.Format.JPEG).toString();
					String webp=photo.image.getUriForSizeAndFormat(SizedImage.Type.PHOTO_THUMB_SMALL, SizedImage.Format.WEBP).toString();
					extraImage=new RealtimeNotification.ImageURLs(jpeg, webp, jpeg, webp);
					extraAttrs=Map.of(
							"onclick", "return openPhotoViewer(this)",
							"data-pv", Utils.gson.toJson(new PhotoViewerInlineData(0, "single/"+photo.getIdString(), photo.image.getURLsForPhotoViewer()))
					);
				}else{
					extraImage=null;
				}

				RealtimeNotification rn=new RealtimeNotification(id, type, objType, objID, actor.getLocalID(), title, content, url, ava, extraImage, extraAttrs);
				conn.send(rn);
				conn.sendRaw(makeCountersWebsocketMessage(conn.session.account));
			});
		}
	}

	private String makeActorLink(Actor actor){
		return "<a href=\""+actor.getProfileURL()+"\">"+TextProcessor.escapeHTML(actor.getName())+"</a>";
	}

	private String makePostPreview(PostLikeObject post, Lang l, ZoneId timeZone){
		if(post==null)
			return "DELETED";
		String preview=post.getShortTitle(40);
		if(StringUtils.isEmpty(preview))
			return l.formatDate(post.createdAt, timeZone, false);
		return preview;
	}

	private RealtimeNotification.ObjectType getRealtimeNotificationObjectTypeForObject(OwnedContentObject obj){
		return switch(obj){
			case Post post -> RealtimeNotification.ObjectType.POST;
			case Photo photo -> RealtimeNotification.ObjectType.PHOTO;
			case Comment comment -> switch(comment.parentObjectID.type()){
				case PHOTO -> RealtimeNotification.ObjectType.PHOTO_COMMENT;
				case BOARD_TOPIC -> RealtimeNotification.ObjectType.BOARD_COMMENT;
			};
			case null, default -> null;
		};
	}

	public UserNotifications getUserCounters(Account self){
		try{
			return NotificationsStorage.getNotificationsForUser(self.user.id, self.prefs.lastSeenNotificationID, self.prefs.countLikesInUnread);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public JsonObject getUserCountersJson(Account self){
		UserNotifications un=getUserCounters(self);
		return new JsonObjectBuilder()
				.add("friends", un.getNewFriendRequestCount())
				.add("photos", un.getNewPhotoTagCount())
				.add("mail", un.getUnreadMailCount())
				.add("groups", un.getNewGroupInvitationsCount())
				.add("events", un.getNewEventInvitationsCount())
				.add("notifications", un.getNewNotificationsCount())
				.build();
	}

	private String makeCountersWebsocketMessage(Account self){
		return new JsonObjectBuilder()
				.add("type", "counters")
				.add("counters", getUserCountersJson(self))
				.build()
				.toString();
	}

	public void sendRealtimeCountersUpdates(User user){
		List<WebSocketConnection> connections=null;
		synchronized(wsMapsLock){
			List<WebSocketConnection> actualConnections=wsConnectionsByUserID.get(user.id);
			if(actualConnections!=null)
				connections=new ArrayList<>(actualConnections);
		}

		if(connections==null)
			return;

		for(WebSocketConnection conn:connections){
			Thread.ofVirtual().start(()->conn.sendRaw(makeCountersWebsocketMessage(conn.session.account)));
		}
	}

	public void recountCounters(User user){
		NotificationsStorage.removeCountersFromCache(user.id);
		sendRealtimeCountersUpdates(user);
	}

	private void sendEmailNotification(Account account, EmailNotificationType type, Object object, OwnedContentObject relatedObject, Actor actor){
		if(!(actor instanceof User user)){
			LOG.warn("Expected actor to be user here but got {} ({}, {}) instead", actor.getClass(), actor.getLocalID(), actor.activityPubID);
			return;
		}
		BackgroundTaskRunner.getInstance().submit(()->{
			try{
				Mailer.getInstance().sendNotification(account, context, type, object, relatedObject, user);
			}catch(Throwable x){
				LOG.error("Failed to send email notification", x);
			}
		});
		account.prefs.lastEmailNotification=Instant.now();
		context.getUsersController().updateUserPreferences(account);
	}

	record WebSocketConnection(SessionInfo session, Session conn, Lang lang){
		public void send(RealtimeNotification notification){
			try{
				conn.getRemote().sendString(Utils.gson.toJson(Map.of("type", "notification", "notification", notification)));
			}catch(IOException x){
				LOG.debug("Failed to send notification to websocket", x);
				conn.close();
			}
		}

		public void sendRaw(String msg){
			try{
				conn.getRemote().sendString(msg);
			}catch(IOException x){
				LOG.debug("Failed to send websocket message");
				conn.close();
			}
		}
	}

	private record NotificationGroupingKey(Notification.Type type, Notification.ObjectType objectType, long objectID, LocalDate day, boolean seen){}
}
