package smithereen.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class NodeInfo{
	public static final int CAP_FRIEND_REQUESTS=1;

	public String host;
	public String softwareName;
	public String softwareVersion;
	public long capabilities;

	public NodeInfo(JSONObject o, String host){
		this.host=host;
		if(o==null)
			return;
		JSONObject software=o.optJSONObject("software");
		if(software!=null){
			softwareName=software.optString("name", null);
			softwareVersion=software.optString("version", null);
		}
		JSONObject meta=o.optJSONObject("metadata");
		if(meta!=null){
			JSONArray caps=meta.optJSONArray("capabilities");
			if(caps!=null){
				for(Object _o:caps){
					if(!(_o instanceof String))
						continue;
					String c=(String)_o;
					switch(c){
						case "friendRequests":
							capabilities|=CAP_FRIEND_REQUESTS;
							break;
					}
				}
			}
		}
	}

	public NodeInfo(ResultSet res) throws SQLException{
		host=res.getString("host");
		softwareName=res.getString("software");
		softwareVersion=res.getString("version");
		capabilities=res.getLong("capabilities");
	}

	@Override
	public String toString(){
		return "NodeInfo{"+
				"host='"+host+'\''+
				", softwareName='"+softwareName+'\''+
				", softwareVersion='"+softwareVersion+'\''+
				", capabilities="+capabilities+
				'}';
	}
}
