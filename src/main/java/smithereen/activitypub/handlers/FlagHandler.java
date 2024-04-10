package smithereen.activitypub.handlers;

import com.google.gson.JsonObject;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import smithereen.Config;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Flag;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.model.ForeignUser;
import smithereen.model.ReportableContentObject;
import smithereen.model.User;
import smithereen.exceptions.BadRequestException;

public class FlagHandler extends ActivityTypeHandler<Actor, Flag, ActivityPubObject>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Flag activity, ActivityPubObject object) throws SQLException{
		User reporter;
		if(actor instanceof ForeignUser fu && !fu.isServiceActor){
			if(fu.id==0){
				context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(fu);
			}
			reporter=fu;
		}else{
			reporter=null;
		}

		if(activity.object==null)
			throw new BadRequestException("Flag.object must be present");
		for(URI uri:activity.object){
			if(!Config.isLocal(uri))
				throw new BadRequestException("Only local URIs are supported in Flag.object. This URI is not local: "+uri);
		}
		if(activity.object.isEmpty())
			throw new BadRequestException("Flag.object must contain at least one URI");

		Actor reportedActor=null;
		ArrayList<ReportableContentObject> content=new ArrayList<>();
		List<Object> objects=activity.object.stream().map(uri->context.appContext.getObjectLinkResolver().resolveNative(uri, Object.class, false, false, false, (JsonObject) null, true)).toList();
		for(Object obj:objects){
			if(obj instanceof Actor a){
				if(reportedActor==null)
					reportedActor=a;
			}else if(obj instanceof ReportableContentObject rco){
				content.add(rco);
			}
		}
		if(reportedActor==null)
			throw new BadRequestException("None of the URIs in Flag.object point to an Actor");

		context.appContext.getModerationController().createViolationReport(reporter, reportedActor, content, activity.content, actor.domain);
	}
}
