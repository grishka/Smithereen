package smithereen.sparkext;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import smithereen.model.admin.UserRole;
import smithereen.model.fasp.FASPCapability;
import spark.Route;

import static spark.Spark.*;

public class SparkExtension{

	private static Gson gson=new GsonBuilder().disableHtmlEscaping().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

	public static void getLoggedIn(String path, LoggedInRoute route){
		get(path, route);
	}

	public static void postLoggedIn(String path, LoggedInRoute route){
		post(path, route);
	}

	public static void getLoggedIn(String path, LoggedInSimpleRoute route){
		get(path, route);
	}

	public static void postLoggedIn(String path, LoggedInSimpleRoute route){
		post(path, route);
	}

	public static void getWithCSRF(String path, CSRFRoute route){
		get(path, route);
	}

	public static void postWithCSRF(String path, CSRFRoute route){
		post(path, route);
	}

	public static void getWithCSRF(String path, CSRFSimpleRoute route){
		get(path, route);
	}

	public static void postWithCSRF(String path, CSRFSimpleRoute route){
		post(path, route);
	}

	public static void getRequiringPermission(String path, UserRole.Permission permission, LoggedInRoute route){
		get(path, new AdminRouteAdapter(route, permission, false));
	}

	public static void getRequiringPermissionWithCSRF(String path, UserRole.Permission permission, LoggedInRoute route){
		get(path, new AdminRouteAdapter(route, permission, true));
	}

	public static void postRequiringPermissionWithCSRF(String path, UserRole.Permission permission, LoggedInRoute route){
		post(path, new AdminRouteAdapter(route, permission, true));
	}

	public static void getRequiringPermission(String path, UserRole.Permission permission, LoggedInSimpleRoute route){
		get(path, new AdminRouteAdapter(route, permission, false));
	}

	public static void getRequiringPermissionWithCSRF(String path, UserRole.Permission permission, LoggedInSimpleRoute route){
		get(path, new AdminRouteAdapter(route, permission, true));
	}

	public static void postRequiringPermissionWithCSRF(String path, UserRole.Permission permission, LoggedInSimpleRoute route){
		post(path, new AdminRouteAdapter(route, permission, true));
	}

	public static void getActivityPub(String path, Route route){
		get(path, "application/activity+json", route);
		get(path, "application/ld+json", route);
	}

	public static void getActivityPubCollection(String path, int perPage, ActivityPubCollectionRoute route){
		getActivityPub(path, new ActivityPubCollectionRouteAdapter(route, perPage));
	}

	public static void getApi(String path, Route route){
		get(path, route, model->gson.toJson(model));
	}

	public static void getFaspAPI(String path, FASPCapability cap, FaspApiRoute route){
		get(path, new FaspApiRouteAdapter(route, cap));
	}

	public static void postFaspAPI(String path, FASPCapability cap, FaspApiRoute route){
		post(path, new FaspApiRouteAdapter(route, cap));
	}

	public static void deleteFaspAPI(String path, FASPCapability cap, FaspApiRoute route){
		delete(path, new FaspApiRouteAdapter(route, cap));
	}

	public static <T> void responseTypeSerializer(Class<T> type, ResponseSerializer<T> serializer){
		ExtendedStreamingSerializer.typeSerializers.add(new ExtendedStreamingSerializer.Entry<>(type, serializer));
	}
}
