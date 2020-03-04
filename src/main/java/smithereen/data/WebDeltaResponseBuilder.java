package smithereen.data;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

public class WebDeltaResponseBuilder{
	private JSONArray commands=new JSONArray();

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

	public JSONArray json(){
		return commands;
	}

	public enum ElementInsertionMode{
		BEFORE_BEGIN,
		AFTER_BEGIN,
		BEFORE_END,
		AFTER_END
	}
}
