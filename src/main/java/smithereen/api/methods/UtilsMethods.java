package smithereen.api.methods;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;

public class UtilsMethods{
	public static Object getServerTime(ApplicationContext ctx, ApiCallContext actx){
		return System.currentTimeMillis()/1000L;
	}
}
