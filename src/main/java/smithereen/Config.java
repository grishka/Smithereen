package smithereen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

public class Config{
	public static String dbHost;
	public static String dbUser;
	public static String dbPassword;
	public static String dbName;

	public static String domain;

	public static String serverIP;
	public static int serverPort;

	public static File uploadPath;
	public static File mediaCachePath;
	public static long mediaCacheMaxSize;
	public static long mediaCacheFileSizeLimit;
	public static String uploadURLPath;
	public static String mediaCacheURLPath;
	public static boolean useHTTP;

	private static URI localURI;

	public static void load(String filePath) throws IOException{
		FileInputStream in=new FileInputStream(filePath);
		Properties props=new Properties();
		props.load(in);
		in.close();

		dbHost=props.getProperty("db.host");
		dbUser=props.getProperty("db.user");
		dbPassword=props.getProperty("db.password");
		dbName=props.getProperty("db.name");

		domain=props.getProperty("domain");

		uploadPath=new File(props.getProperty("upload.path"));
		uploadURLPath=props.getProperty("upload.urlpath");
		mediaCachePath=new File(props.getProperty("media_cache.path"));
		mediaCacheMaxSize=Utils.parseFileSize(props.getProperty("media_cache.max_size"));
		mediaCacheFileSizeLimit=Utils.parseFileSize(props.getProperty("media_cache.file_size_limit"));
		mediaCacheURLPath=props.getProperty("media_cache.urlpath");

		useHTTP=Boolean.parseBoolean(props.getProperty("use_http_scheme.i_know_what_i_am_doing", "false"));
		localURI=URI.create("http"+(useHTTP ? "" : "s")+"://"+domain+"/");

		serverIP=props.getProperty("server.ip", "127.0.0.1");
		serverPort=Utils.parseIntOrDefault(props.getProperty("server.port", "4567"), 4567);
	}

	public static URI localURI(String path){
		return localURI.resolve(path);
	}

	public static boolean isLocal(URI uri){
		if(domain.contains(":")){
			return (uri.getHost()+":"+uri.getPort()).equalsIgnoreCase(domain);
		}
		return uri.getHost().equalsIgnoreCase(domain);
	}
}
