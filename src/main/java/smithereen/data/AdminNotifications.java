package smithereen.data;

import org.jetbrains.annotations.Nullable;

import smithereen.Utils;
import spark.Request;

public class AdminNotifications{
	private static AdminNotifications instance;

	public int signupRequestsCount;

	public static AdminNotifications getInstance(@Nullable Request req){
		if(instance==null && req!=null){
			instance=new AdminNotifications();
			instance.signupRequestsCount=Utils.context(req).getUsersController().getSignupInviteRequestCount();
		}
		return instance;
	}
}
