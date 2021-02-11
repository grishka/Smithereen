package smithereen.sparkext;

import smithereen.data.Account;
import spark.Route;

import static spark.Spark.*;

public class SparkExtension{
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
}
