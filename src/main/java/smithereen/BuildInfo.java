package smithereen;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildInfo{
	static{
		Properties props=new Properties();
		try(InputStream in=BuildInfo.class.getResourceAsStream("/version.properties")){
			props.load(in);
		}catch(IOException ignore){}
		if(props.containsKey("build.version")){
			VERSION=props.getProperty("build.version");
		}else{
			VERSION="unknown";
		}
	}

	public static final String VERSION;
}
