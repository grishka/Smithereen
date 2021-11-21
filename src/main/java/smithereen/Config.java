package smithereen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import smithereen.storage.DatabaseConnectionManager;
import smithereen.util.TopLevelDomainList;
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
	public static boolean useHTTP;
	public static String staticFilesPath;
	public static final boolean DEBUG=System.getProperty("smithereen.debug")!=null || System.getenv("SMITHEREEN_DEBUG")!=null;

	public static String imgproxyLocalUploads;
	public static String imgproxyLocalMediaCache;
	public static String imgproxyUrl;
	public static byte[] imgproxyKey;
	public static byte[] imgproxySalt;

	private static URI localURI;

	// following fields are kept in the config table in database and some are configurable from /settings/admin

	public static int dbSchemaVersion;
	public static String serverDisplayName;
	public static String serverDescription;
	public static String serverShortDescription;
	public static String serverPolicy;
	public static String serverAdminEmail;
	public static SignupMode signupMode=SignupMode.CLOSED;

	public static String mailFrom;
	public static String smtpServerAddress;
	public static int smtpPort;
	public static String smtpUsername;
	public static String smtpPassword;
	public static boolean smtpUseTLS;

	public static PrivateKey serviceActorPrivateKey;
	public static PublicKey serviceActorPublicKey;

	private static final Logger LOG=LoggerFactory.getLogger(Config.class);

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
		mediaCachePath=new File(props.getProperty("media_cache.path"));
		mediaCacheMaxSize=Utils.parseFileSize(props.getProperty("media_cache.max_size"));
		mediaCacheFileSizeLimit=Utils.parseFileSize(props.getProperty("media_cache.file_size_limit"));

		useHTTP=Boolean.parseBoolean(props.getProperty("use_http_scheme.i_know_what_i_am_doing", "false"));
		localURI=URI.create("http"+(useHTTP ? "" : "s")+"://"+domain+"/");

		serverIP=props.getProperty("server.ip", "127.0.0.1");
		serverPort=Utils.parseIntOrDefault(props.getProperty("server.port", "4567"), 4567);
		staticFilesPath=props.getProperty("web.static_files_path");

		imgproxyUrl=props.getProperty("imgproxy.url_prefix");
		imgproxyLocalUploads=props.getProperty("imgproxy.local_uploads");
		imgproxyLocalMediaCache=props.getProperty("imgproxy.local_media_cache");
		imgproxyKey=Utils.hexStringToByteArray(props.getProperty("imgproxy.key"));
		imgproxySalt=Utils.hexStringToByteArray(props.getProperty("imgproxy.salt"));
		if(imgproxyUrl.charAt(0)!='/')
			imgproxyUrl='/'+imgproxyUrl;
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
			serverShortDescription=dbValues.get("ServerShortDescription");
			serverPolicy=dbValues.get("ServerPolicy");
			serverAdminEmail=dbValues.get("ServerAdminEmail");
			String _signupMode=dbValues.get("SignupMode");
			if(StringUtils.isNotEmpty(_signupMode)){
				try{
					signupMode=SignupMode.valueOf(_signupMode);
				}catch(IllegalArgumentException ignore){}
			}

			smtpServerAddress=dbValues.getOrDefault("Mail_SMTP_ServerAddress", "127.0.0.1");
			smtpPort=Utils.parseIntOrDefault(dbValues.get("Mail_SMTP_ServerPort"), 25);
			mailFrom=dbValues.getOrDefault("MailFrom", "noreply@"+domain);
			smtpUsername=dbValues.get("Mail_SMTP_Username");
			smtpPassword=dbValues.get("Mail_SMTP_Password");
			smtpUseTLS=Utils.parseIntOrDefault(dbValues.get("Mail_SMTP_UseTLS"), 0)==1;

			String pkey=dbValues.get("ServiceActorPrivateKey");
			try{
				if(pkey==null){
					KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA");
					kpg.initialize(2048);
					KeyPair pair=kpg.generateKeyPair();
					PrivateKey priv=pair.getPrivate();
					PublicKey pub=pair.getPublic();
					updateInDatabase(Map.of(
							"ServiceActorPrivateKey", Base64.getEncoder().encodeToString(priv.getEncoded()),
							"ServiceActorPublicKey", Base64.getEncoder().encodeToString(pub.getEncoded())
					));
					serviceActorPrivateKey=priv;
					serviceActorPublicKey=pub;
				}else{
					EncodedKeySpec spec=new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pkey));
					serviceActorPrivateKey=KeyFactory.getInstance("RSA").generatePrivate(spec);
					spec=new X509EncodedKeySpec(Base64.getDecoder().decode(dbValues.get("ServiceActorPublicKey")));
					serviceActorPublicKey=KeyFactory.getInstance("RSA").generatePublic(spec);
				}
			}catch(NoSuchAlgorithmException|InvalidKeySpecException ignore){}

			TopLevelDomainList.lastUpdatedTime=Long.parseLong(dbValues.getOrDefault("TLDList_LastUpdated", "0"));
			if(TopLevelDomainList.lastUpdatedTime>0){
				TopLevelDomainList.update(dbValues.get("TLDList_Data"));
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

	public static void updateInDatabase(Map<String, String> values) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("INSERT INTO config (`key`, `value`) VALUES "+String.join(", ", Collections.nCopies(values.size(), "(?, ?)"))+" ON DUPLICATE KEY UPDATE `value`=values(`value`)");
		int i=1;
		for(Map.Entry<String, String> e: values.entrySet()){
			stmt.setString(i, e.getKey());
			stmt.setString(i+1, e.getValue());
			i+=2;
		}
		LOG.debug("{}", stmt);
		stmt.execute();
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
