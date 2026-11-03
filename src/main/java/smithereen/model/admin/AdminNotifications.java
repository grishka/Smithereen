package smithereen.model.admin;

import org.jetbrains.annotations.Nullable;

import smithereen.Utils;
import spark.Request;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

public class AdminNotifications{
	private static AdminNotifications instance;

	private AtomicInteger signupRequestsCount;
	private AtomicInteger openReportsCount;

	@Nullable
	public synchronized static AdminNotifications getInstance(@Nullable Request req){
		if(instance==null && req!=null){
			instance=new AdminNotifications();
			instance.signupRequestsCount=new AtomicInteger(Utils.context(req).getUsersController().getSignupInviteRequestCount());
			instance.openReportsCount=new AtomicInteger(Utils.context(req).getModerationController().getViolationReportsCount(true));
		}
		return instance;
	}

	public int getSignupRequestsCount(){
		return signupRequestsCount.get();
	}

	public void setSignupRequestsCount(int newValue){
		signupRequestsCount.set(newValue);
	}

	public void updateSignupRequestsCount(IntUnaryOperator op){
		signupRequestsCount.updateAndGet(op);
	}

	public int getOpenReportsCount(){
		return openReportsCount.get();
	}

	public void setOpenReportsCount(int newValue){
		openReportsCount.set(newValue);
	}
}
