package smithereen.api;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.api.methods.UsersMethods;
import smithereen.api.methods.UtilsMethods;
import smithereen.api.model.ApiErrorType;
import smithereen.model.apps.ClientAppPermission;

public class ApiDispatcher{
	private static final Map<String, MethodRecord> unprefixedMethods=new HashMap<>();
	private static final Map<String, Map<String, MethodRecord>> methods=new HashMap<>();

	static{
		Map<String, MethodRecord> m=new HashMap<>();
		m.put("get", new MethodRecord(UsersMethods::get, false));
		methods.put("users", m);

		m=new HashMap<>();
		m.put("getServerTime", new MethodRecord(UtilsMethods::getServerTime, false));
		methods.put("utils", m);
	}

	public static Object doApiCall(String method, ApplicationContext ctx, ApiCallContext actx){
		int dotIndex=method.indexOf('.');
		MethodRecord methodRecord=null;
		if(dotIndex>0){
			String prefix=method.substring(0, dotIndex);
			Map<String, MethodRecord> prefixMethods=methods.get(prefix);
			if(prefixMethods!=null){
				String name=method.substring(dotIndex+1);
				methodRecord=prefixMethods.get(name);
			}
		}else{
			methodRecord=unprefixedMethods.get(method);
		}
		if(methodRecord!=null){
			if(methodRecord.requireUser && actx.self==null){
				throw actx.error(ApiErrorType.USER_AUTH_FAILED, "no access token passed");
			}
			if(!methodRecord.requirePermissions.isEmpty() && !actx.token.permissions().containsAll(methodRecord.requirePermissions) && !actx.token.permissions().contains(ClientAppPermission.PASSWORD_GRANT_USED)){
				throw actx.error(ApiErrorType.NO_PERMISSION, "scope "+methodRecord.requirePermissions.stream().map(ClientAppPermission::getScopeValue).collect(Collectors.joining(", "))+" is required");
			}
			return methodRecord.impl.call(ctx, actx);
		}
		throw actx.error(ApiErrorType.UNKNOWN_METHOD);
	}

	private static class MethodRecord{
		public final boolean requireUser;
		public final ApiMethod impl;
		public final EnumSet<ClientAppPermission> requirePermissions;

		private MethodRecord(ApiMethod impl, boolean requireUser){
			this.requireUser=requireUser;
			this.impl=impl;
			this.requirePermissions=EnumSet.noneOf(ClientAppPermission.class);
		}

		private MethodRecord(ApiMethod impl, ClientAppPermission... permissions){
			this.requireUser=true;
			this.impl=impl;
			this.requirePermissions=EnumSet.of(permissions[0], permissions);
		}
	}
}
