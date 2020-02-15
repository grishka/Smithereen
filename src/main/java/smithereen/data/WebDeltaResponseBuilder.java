package smithereen.data;

import org.json.JSONArray;
import org.json.JSONObject;

public class WebDeltaResponseBuilder{
	private JSONArray commands=new JSONArray();

	public WebDeltaResponseBuilder setContent(String containerID, String html){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "setContent");
		cmd.put("id", containerID);
		cmd.put("c", html);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder remove(String... ids){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "remove");
		cmd.put("ids", new JSONArray(ids));
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder messageBox(String title, String msg, String button){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "msgBox");
		cmd.put("m", msg);
		cmd.put("t", title);
		cmd.put("b", button);
		commands.put(cmd);
		return this;
	}

	public JSONArray json(){
		return commands;
	}
}
