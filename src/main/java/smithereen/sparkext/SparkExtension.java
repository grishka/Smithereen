package smithereen.sparkext;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import smithereen.data.Account;
import spark.ResponseTransformer;
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

	public static void getWithCSRF(String path, CSRFRoute route){
		get(path, route);
	}

	public static void postWithCSRF(String path, CSRFRoute route){
		post(path, route);
	}

	public static void getRequiringAccessLevel(String path, Account.AccessLevel minLevel, LoggedInRoute route){
		get(path, new AdminRouteAdapter(route, minLevel, false));
	}

	public static void getRequiringAccessLevelWithCSRF(String path, Account.AccessLevel minLevel, LoggedInRoute route){
		get(path, new AdminRouteAdapter(route, minLevel, true));
	}

	public static void postRequiringAccessLevelWithCSRF(String path, Account.AccessLevel minLevel, LoggedInRoute route){
		post(path, new AdminRouteAdapter(route, minLevel, true));
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
}
