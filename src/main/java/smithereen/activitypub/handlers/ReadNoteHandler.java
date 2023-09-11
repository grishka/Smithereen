package smithereen.activitypub.handlers;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Read;
import smithereen.data.ForeignUser;
import smithereen.data.MailMessage;
import smithereen.exceptions.ObjectNotFoundException;

public class ReadNoteHandler extends ActivityTypeHandler<ForeignUser, Read, NoteOrQuestion>{
	private static final Logger LOG=LoggerFactory.getLogger(ReadNoteHandler.class);

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Read activity, NoteOrQuestion object) throws SQLException{
		try{
			MailMessage msg=context.appContext.getObjectLinkResolver().resolveNative(object.activityPubID, MailMessage.class, false, false, false, (JsonObject) null, false);
			context.appContext.getMailController().markMessageRead(actor, msg);
		}catch(ObjectNotFoundException x){
			LOG.debug("Message {} not found for marking as read", object.activityPubID);
		}
	}
}
