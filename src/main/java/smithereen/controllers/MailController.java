package smithereen.controllers;

import com.google.gson.JsonArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.model.ForeignUser;
import smithereen.model.MailMessage;
import smithereen.model.MessagesPrivacyGrant;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.UserNotifications;
import smithereen.model.UserPrivacySettingKey;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.storage.MailStorage;
import smithereen.storage.MediaCache;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.NotificationsStorage;
import smithereen.util.BackgroundTaskRunner;
import spark.utils.StringUtils;

public class MailController{
	private static final Logger LOG=LoggerFactory.getLogger(MailController.class);
	private final ApplicationContext context;
	private static final Pattern AP_ID_PATTERN=Pattern.compile("^/activitypub/objects/messages/([a-zA-Z0-9_-]+)$");

	public MailController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<MailMessage> getInbox(User self, int offset, int count){
		try{
			return MailStorage.getInbox(self.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<MailMessage> getOutbox(User self, int offset, int count){
		try{
			return MailStorage.getOutbox(self.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public MailMessage getMessage(User self, long id, boolean wantDeleted){
		try{
			MailMessage msg=MailStorage.getMessage(self.id, id, wantDeleted);
			if(msg==null)
				throw new ObjectNotFoundException();
			return msg;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long sendMessage(User self, int selfAccountID, Set<User> _to, String text, String subject, List<String> attachmentIDs, MailMessage inReplyTo){
		try{
			Set<User> to=_to.stream().filter(u->context.getPrivacyController().checkUserPrivacy(self, u, UserPrivacySettingKey.PRIVATE_MESSAGES)).collect(Collectors.toSet());
			if(to.isEmpty())
				throw new UserActionNotAllowedException();
			if(StringUtils.isNotEmpty(text)){
				text=Utils.preprocessPostHTML(text, null);
			}
			int maxAttachments=10;
			int attachmentCount=0;
			String attachments=null;
			if(!attachmentIDs.isEmpty()){
				ArrayList<ActivityPubObject> attachObjects=new ArrayList<>();
				for(String id:attachmentIDs){
					if(!id.matches("^[a-fA-F0-9]{32}$"))
						continue;
					ActivityPubObject obj=MediaCache.getAndDeleteDraftAttachment(id, selfAccountID, "mail_images");
					if(obj!=null){
						attachObjects.add(obj);
						attachmentCount++;
					}
					if(attachmentCount==maxAttachments)
						break;
				}
				if(!attachObjects.isEmpty()){
					if(attachObjects.size()==1){
						attachments=MediaStorageUtils.serializeAttachment(attachObjects.get(0)).toString();
					}else{
						JsonArray ar=new JsonArray();
						for(ActivityPubObject o:attachObjects){
							ar.add(MediaStorageUtils.serializeAttachment(o));
						}
						attachments=ar.toString();
					}
				}
			}
			Map<Integer, MailMessage.ReplyInfo> replyInfos;
			if(inReplyTo==null){
				replyInfos=Map.of();
			}else{
				replyInfos=new HashMap<>();
				replyInfos.put(self.id, new MailMessage.ReplyInfo(MailMessage.ParentObjectType.MESSAGE, inReplyTo.id));
				if(!inReplyTo.relatedMessageIDs.isEmpty()){
					for(MailMessage msg:MailStorage.getMessages(inReplyTo.relatedMessageIDs)){
						replyInfos.put(msg.ownerID, new MailMessage.ReplyInfo(MailMessage.ParentObjectType.MESSAGE, msg.id));
					}
				}
			}
			Set<Integer> localOwners=new HashSet<>();
			localOwners.add(self.id);
			boolean hasForeignRecipients=false;
			for(User user:to){
				if(user instanceof ForeignUser){
					hasForeignRecipients=true;
					continue;
				}
				localOwners.add(user.id);
				UserNotifications un=NotificationsStorage.getNotificationsFromCache(user.id);
				if(un!=null)
					un.incUnreadMailCount(1);
			}
			long id=MailStorage.createMessage(text, Objects.requireNonNullElse(subject, ""), attachments, self.id, to.stream().map(u->u.id).collect(Collectors.toSet()), null, localOwners, null, replyInfos);
			for(User user:to){
				MessagesPrivacyGrant grant=MailStorage.getPrivacyGrant(user.id, self.id);
				if(grant!=null && grant.isValid()){
					MailStorage.consumePrivacyGrant(user.id, self.id);
				}
			}
			if(hasForeignRecipients){
				MailMessage msg=MailStorage.getMessage(self.id, id, false);
				context.getActivityPubWorker().sendDirectMessage(self, msg);
			}
			BackgroundTaskRunner.getInstance().submit(()->{
				Set<User> usersToGrant=to.stream()
						.filter(u->!context.getPrivacyController().checkUserPrivacy(u, self, self.getPrivacySetting(UserPrivacySettingKey.PRIVATE_MESSAGES)))
						.collect(Collectors.toSet());
				if(usersToGrant.isEmpty())
					return;
				try{
					for(User user:usersToGrant){
						MailStorage.createOrRenewPrivacyGrant(self.id, user.id, 10);
					}
				}catch(SQLException x){
					LOG.warn("Failed to update privacy grants", x);
				}
			});
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void markMessageRead(User self, MailMessage message){
		if(message.readReceipts.contains(self.id))
			return;
		if(message.senderID==self.id)
			throw new BadRequestException("Can't mark outgoing messages as read");
		try{
			// User's own copy
			if(!(self instanceof ForeignUser)){
				MailStorage.addMessageReadReceipt(self.id, Set.of(message.id), self.id);
				UserNotifications un=NotificationsStorage.getNotificationsFromCache(self.id);
				if(un!=null)
					un.incUnreadMailCount(-1);
			}
			// Sender's copy
			if(!message.relatedMessageIDs.isEmpty()){
				MailStorage.addMessageReadReceipt(message.senderID, message.relatedMessageIDs, self.id);
			}
			if(!(self instanceof ForeignUser)){
				context.getActivityPubWorker().sendReadMessageActivity(self, message);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteMessage(User self, MailMessage message){
		try{
			MailStorage.deleteMessage(self.id, message.id);
			if(message.isUnread()){
				UserNotifications un=NotificationsStorage.getNotificationsFromCache(self.id);
				if(un!=null)
					un.incUnreadMailCount(-1);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void actuallyDeleteMessage(User self, MailMessage message, boolean deleteRelated){
		try{
			if(message.ownerID!=self.id && message.senderID!=self.id){
				throw new IllegalArgumentException("This user can't delete this message");
			}
			MailStorage.actuallyDeleteMessage(message.id);
			if(message.ownerID!=self.id){
				UserNotifications un=NotificationsStorage.getNotificationsFromCache(message.ownerID);
				if(un!=null)
					un.incUnreadMailCount(-1);
			}
			if(deleteRelated && !message.relatedMessageIDs.isEmpty()){
				for(MailMessage msg:MailStorage.getMessages(message.relatedMessageIDs)){
					if(msg.isUnread()){
						MailStorage.actuallyDeleteMessage(msg.id);
						UserNotifications un=NotificationsStorage.getNotificationsFromCache(msg.ownerID);
						if(un!=null)
							un.incUnreadMailCount(-1);
					}
				}
			}
			if(message.attachments!=null && !message.attachments.isEmpty() && MailStorage.getMessageRefCount(message.relatedMessageIDs)==0){
				MediaStorageUtils.deleteAttachmentFiles(message.attachments);
			}
			if(!(self instanceof ForeignUser) && deleteRelated && message.senderID==self.id){
				context.getActivityPubWorker().sendDeleteMessageActivity(self, message);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void restoreMessage(User self, MailMessage message){
		try{
			MailStorage.restoreMessage(self.id, message.id);
			if(message.isUnread()){
				UserNotifications un=NotificationsStorage.getNotificationsFromCache(self.id);
				if(un!=null)
					un.incUnreadMailCount(1);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public static void deleteRestorableMessages(){
		try{
			List<MailMessage> messages=MailStorage.getRecentlyDeletedMessages(Instant.now().minus(10, ChronoUnit.MINUTES));
			if(messages.isEmpty())
				return;
			MailStorage.actuallyDeleteMessages(messages.stream().map(m->m.id).collect(Collectors.toSet()));
			for(MailMessage message:messages){
				if(message.attachments!=null && !message.attachments.isEmpty() && MailStorage.getMessageRefCount(message.relatedMessageIDs)==0){
					MediaStorageUtils.deleteAttachmentFiles(message.attachments);
				}
			}
		}catch(SQLException x){
			LOG.warn("Failed to permanently delete restorable messages", x);
		}
	}

	public void putForeignMessage(MailMessage msg){
		try{
			HashSet<Integer> needUsers=new HashSet<>();
			needUsers.add(msg.senderID);
			needUsers.addAll(msg.to);
			needUsers.addAll(msg.cc);
			Map<Integer, User> users=context.getUsersController().getUsers(needUsers);
			User sender=users.remove(msg.senderID);
			Map<Integer, MailMessage.ReplyInfo> replyInfos;
			if(msg.inReplyTo==null){
				replyInfos=Map.of();
			}else{
				replyInfos=new HashMap<>();

				// Reply to a local message?
				Matcher localMatcher=AP_ID_PATTERN.matcher(msg.inReplyTo.getPath());
				if(localMatcher.find()){
					long id=Utils.decodeLong(localMatcher.group(1));
					List<MailMessage> _parent=MailStorage.getMessages(Set.of(id));
					if(!_parent.isEmpty()){
						MailMessage parent=_parent.get(0);
						replyInfos.put(parent.ownerID, new MailMessage.ReplyInfo(MailMessage.ParentObjectType.MESSAGE, parent.id));
						if(!parent.relatedMessageIDs.isEmpty()){
							for(MailMessage related:MailStorage.getMessages(parent.relatedMessageIDs)){
								replyInfos.put(related.ownerID, new MailMessage.ReplyInfo(MailMessage.ParentObjectType.MESSAGE, related.id));
							}
						}
					}
				}else{
					// Reply to a remote message?
					List<MailMessage> parentMessages=MailStorage.getMessages(msg.inReplyTo);
					if(!parentMessages.isEmpty()){
						for(MailMessage parent:parentMessages){
							replyInfos.put(parent.ownerID, new MailMessage.ReplyInfo(MailMessage.ParentObjectType.MESSAGE, parent.id));
						}
					}else{
						// Reply to an object of another type?
						try{
							Object parent=context.getObjectLinkResolver().resolveNative(msg.inReplyTo, Object.class, true, true, false, (Actor)null, false);
							if(parent instanceof Post post){
								for(int id:needUsers){
									replyInfos.put(id, new MailMessage.ReplyInfo(MailMessage.ParentObjectType.POST, post.id));
								}
							}
						}catch(ObjectNotFoundException ignore){} // Also thrown by NoteOrQuestion -> Post conversion when parent post is not loaded. TODO: maybe load reply threads here
					}
				}
			}
			Set<Integer> localOwners=users.values().stream()
					.filter(u->!(u instanceof ForeignUser))
					.filter(u->context.getPrivacyController().checkUserPrivacy(sender, u, UserPrivacySettingKey.PRIVATE_MESSAGES))
					.map(u->u.id)
					.collect(Collectors.toSet());
			if(localOwners.isEmpty())
				throw new UserActionNotAllowedException();
			MailStorage.createMessage(msg.text, msg.subject!=null ? msg.subject : "", msg.getSerializedAttachments(), msg.senderID, msg.to, msg.cc, localOwners, msg.activityPubID, replyInfos);
			for(int id:localOwners){
				MessagesPrivacyGrant grant=MailStorage.getPrivacyGrant(id, msg.senderID);
				if(grant!=null && grant.isValid())
					MailStorage.consumePrivacyGrant(id, msg.senderID);

				if(!context.getPrivacyController().checkUserPrivacy(users.get(id), sender, sender.getPrivacySetting(UserPrivacySettingKey.PRIVATE_MESSAGES))){
					MailStorage.createOrRenewPrivacyGrant(msg.senderID, id, 10);
				}

				UserNotifications un=NotificationsStorage.getNotificationsFromCache(id);
				if(un!=null)
					un.incUnreadMailCount(1);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
