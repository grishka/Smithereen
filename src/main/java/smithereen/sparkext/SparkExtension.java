package smithereen.sparkext;

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
}
