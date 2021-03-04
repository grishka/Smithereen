package smithereen.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import spark.Response;

public class WebDeltaResponseBuilder{
	private JSONArray commands=new JSONArray();

	public WebDeltaResponseBuilder(){

	}

	public WebDeltaResponseBuilder(Response resp){
		resp.type("application/json");
	}

	public WebDeltaResponseBuilder setContent(@NotNull String containerID, @NotNull String html){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "setContent");
		cmd.put("id", containerID);
		cmd.put("c", html);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder remove(@NotNull String... ids){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "remove");
		cmd.put("ids", new JSONArray(ids));
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder messageBox(@NotNull String title, @NotNull String msg, @NotNull String button){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "msgBox");
		cmd.put("m", msg);
		cmd.put("t", title);
		cmd.put("b", button);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder box(@NotNull String title, @NotNull String content, @Nullable String id, boolean scrollable){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "box");
		cmd.put("t", title);
		cmd.put("c", content);
		cmd.put("s", scrollable);
		if(id!=null)
			cmd.put("i", id);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder box(@NotNull String title, @NotNull String content, @Nullable String id, int width){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "box");
		cmd.put("t", title);
		cmd.put("c", content);
		cmd.put("w", width);
		if(id!=null)
			cmd.put("i", id);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder formBox(@NotNull String title, @NotNull String content, @NotNull String formAction, @NotNull String button){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "formBox");
		cmd.put("m", content);
		cmd.put("t", title);
		cmd.put("b", button);
		cmd.put("fa", formAction);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder show(@NotNull String... ids){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "show");
		cmd.put("ids", new JSONArray(ids));
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder hide(@NotNull String... ids){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "hide");
		cmd.put("ids", new JSONArray(ids));
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder insertHTML(@NotNull ElementInsertionMode mode, @NotNull String id, @NotNull String html){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "insert");
		cmd.put("id", id);
		cmd.put("c", html);
		cmd.put("m", new String[]{"bb", "ab", "be", "ae"}[mode.ordinal()]);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder setInputValue(@NotNull String id, @NotNull String value){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "setValue");
		cmd.put("id", id);
		cmd.put("v", value);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder refresh(){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "refresh");
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder replaceLocation(String url){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "location");
		cmd.put("l", url);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder addClass(@NotNull String id, @NotNull String className){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "addClass");
		cmd.put("id", id);
		cmd.put("cl", className);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder removeClass(@NotNull String id, @NotNull String className){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "remClass");
		cmd.put("id", id);
		cmd.put("cl", className);
		commands.put(cmd);
		return this;
	}

	public WebDeltaResponseBuilder setAttribute(@NotNull String id, @NotNull String name, @NotNull String value){
		JSONObject cmd=new JSONObject();
		cmd.put("a", "setAttr");
		cmd.put("id", id);
		cmd.put("n", name);
		cmd.put("v", value);
		commands.put(cmd);
		return this;
	}

	public JSONArray json(){
		return commands;
	}

	@Override
	public String toString(){
		return commands.toString();
	}

	public enum ElementInsertionMode{
		BEFORE_BEGIN,
		AFTER_BEGIN,
		BEFORE_END,
		AFTER_END
	}
}
