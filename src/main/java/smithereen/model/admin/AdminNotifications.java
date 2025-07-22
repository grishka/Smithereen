package smithereen.model.admin;

import org.jetbrains.annotations.Nullable;

import smithereen.Utils;
import spark.Request;

public class AdminNotifications{
	private static AdminNotifications instance;

	public int signupRequestsCount;
	public int openReportsCount;

	public static AdminNotifications getInstance(@Nullable Request req){
		if(instance==null && req!=null){
			instance=new AdminNotifications();
			instance.signupRequestsCount=Utils.context(req).getUsersController().getSignupInviteRequestCount();
			instance.openReportsCount=Utils.context(req).getModerationController().getViolationReportsCount(true);
		}
		return instance;
	}
}
