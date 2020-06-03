package smithereen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import smithereen.storage.DatabaseConnectionManager;
import spark.utils.StringUtils;

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
	public static String staticFilesPath;
	public static final boolean DEBUG=System.getProperty("smithereen.debug")!=null;

	private static URI localURI;

	// following fields are kept in the config table in database and some are configurable from /settings/admin

	public static int dbSchemaVersion;
	public static String serverDisplayName;
	public static String serverDescription;
	public static String serverAdminEmail;
	public static SignupMode signupMode=SignupMode.CLOSED;

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
		staticFilesPath=props.getProperty("web.static_files_path");
	}

	public static void loadFromDatabase() throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		try(ResultSet res=conn.createStatement().executeQuery("SELECT * FROM config")){
			HashMap<String, String> dbValues=new HashMap<>();
			if(res.first()){
				do{
					dbValues.put(res.getString(1), res.getString(2));
				}while(res.next());
			}
			dbSchemaVersion=Utils.parseIntOrDefault(dbValues.get("SchemaVersion"), 0);

			serverDisplayName=dbValues.get("ServerDisplayName");
			serverDescription=dbValues.get("ServerDescription");
			serverAdminEmail=dbValues.get("ServerAdminEmail");
			String _signupMode=dbValues.get("SignupMode");
			if(StringUtils.isNotEmpty(_signupMode)){
				try{
					signupMode=SignupMode.valueOf(_signupMode);
				}catch(IllegalArgumentException ignore){}
			}
		}
	}

	public static void updateInDatabase(String key, String value) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("INSERT INTO config (`key`, `value`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `value`=values(`value`)");
		stmt.setString(1, key);
		stmt.setString(2, value);
		stmt.executeUpdate();
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

	public static String getServerDisplayName(){
		return StringUtils.isNotEmpty(serverDisplayName) ? serverDisplayName : domain;
	}

	public enum SignupMode{
		OPEN,
		CLOSED,
		INVITE_ONLY,
		MANUAL_APPROVAL
	}
}
