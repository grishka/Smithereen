package smithereen.model;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import smithereen.Utils;
import spark.Response;

public class WebDeltaResponse{
	private final ArrayList<Command> commands=new ArrayList<>();

	public WebDeltaResponse(){

	}

	public WebDeltaResponse(Response resp){
		resp.type("application/json");
	}

	public WebDeltaResponse setContent(@NotNull String containerID, @NotNull String html){
		commands.add(new SetContentCommand(containerID, html));
		return this;
	}

	public WebDeltaResponse remove(@NotNull String... ids){
		commands.add(new RemoveElementsCommand(ids));
		return this;
	}

	public WebDeltaResponse messageBox(@NotNull String title, @NotNull String msg, @NotNull String button){
		commands.add(new MessageBoxCommand(title, msg, button));
		return this;
	}

	public WebDeltaResponse box(@NotNull String title, @NotNull String content, @Nullable String id, boolean scrollable){
		commands.add(new BoxCommand(title, content, id, scrollable, null));
		return this;
	}

	public WebDeltaResponse box(@NotNull String title, @NotNull String content, @Nullable String id, int width){
		commands.add(new BoxCommand(title, content, id, null, width));
		return this;
	}

	public WebDeltaResponse formBox(@NotNull String title, @NotNull String content, @NotNull String formAction, @NotNull String button){
		commands.add(new FormBoxCommand(content, title, button, formAction));
		return this;
	}

	public WebDeltaResponse show(@NotNull String... ids){
		commands.add(new ShowHideElementsCommand(true, ids));
		return this;
	}

	public WebDeltaResponse hide(@NotNull String... ids){
		commands.add(new ShowHideElementsCommand(false, ids));
		return this;
	}

	public WebDeltaResponse insertHTML(@NotNull ElementInsertionMode mode, @NotNull String id, @NotNull String html){
		commands.add(new InsertHtmlCommand(id, html, mode));
		return this;
	}

	public WebDeltaResponse setInputValue(@NotNull String id, @NotNull String value){
		commands.add(new SetInputValueCommand(id, value));
		return this;
	}

	public WebDeltaResponse refresh(){
		commands.add(new RefreshCommand());
		return this;
	}

	public WebDeltaResponse replaceLocation(String url){
		commands.add(new ReplaceLocationCommand(url));
		return this;
	}

	public WebDeltaResponse addClass(@NotNull String id, @NotNull String className){
		commands.add(new AddRemoveClassCommand(true, id, className));
		return this;
	}

	public WebDeltaResponse removeClass(@NotNull String id, @NotNull String className){
		commands.add(new AddRemoveClassCommand(false, id, className));
		return this;
	}

	public WebDeltaResponse setAttribute(@NotNull String id, @NotNull String name, @NotNull String value){
		commands.add(new SetAttributeCommand(id, name, value));
		return this;
	}

	public WebDeltaResponse runScript(@NotNull String script){
		commands.add(new RunScriptCommand(script));
		return this;
	}

	public WebDeltaResponse keepBox(){
		commands.add(new KeepBoxCommand());
		return this;
	}

	public WebDeltaResponse showSnackbar(String text){
		commands.add(new ShowSnackbarCommand(text));
		return this;
	}

	public WebDeltaResponse setURL(String url){
		commands.add(new SetURLCommand(url));
		return this;
	}

	public String json(){
		return Utils.gson.toJson(commands);
	}

	@Override
	public String toString(){
		return json();
	}

	public List<Command> commands(){
		return commands;
	}

	public enum ElementInsertionMode{
		@SerializedName("bb")
		BEFORE_BEGIN,
		@SerializedName("ab")
		AFTER_BEGIN,
		@SerializedName("be")
		BEFORE_END,
		@SerializedName("ae")
		AFTER_END
	}

	public static abstract class Command{
		@SerializedName("a")
		public String action;

		public Command(String action){
			this.action=action;
		}
	}

	private static class SetContentCommand extends Command{
		@SerializedName("id")
		public String containerID;
		@SerializedName("c")
		public String content;

		public SetContentCommand(String containerID, String content){
			super("setContent");
			this.containerID=containerID;
			this.content=content;
		}
	}

	private static class RemoveElementsCommand extends Command{
		@SerializedName("ids")
		public String[] elementIDs;

		public RemoveElementsCommand(String[] elementIDs){
			super("remove");
			this.elementIDs=elementIDs;
		}
	}

	private static class MessageBoxCommand extends Command{
		@SerializedName("t")
		public String title;
		@SerializedName("m")
		public String message;
		@SerializedName("b")
		public String button;

		public MessageBoxCommand(String title, String message, String button){
			super("msgBox");
			this.title=title;
			this.message=message;
			this.button=button;
		}
	}

	public static class BoxCommand extends Command{
		@SerializedName("t")
		public String title;
		@SerializedName("c")
		public String content;
		@SerializedName("i")
		public String id;
		@SerializedName("s")
		public Boolean scrollable;
		@SerializedName("w")
		public Integer width;

		public BoxCommand(String title, String content, String id, Boolean scrollable, Integer width){
			super("box");
			this.title=title;
			this.content=content;
			this.id=id;
			this.scrollable=scrollable;
			this.width=width;
		}
	}

	public static class FormBoxCommand extends Command{
		@SerializedName("m")
		public String content;
		@SerializedName("t")
		public String title;
		@SerializedName("b")
		public String button;
		@SerializedName("fa")
		public String formAction;

		public FormBoxCommand(String content, String title, String button, String formAction){
			super("formBox");
			this.content=content;
			this.title=title;
			this.button=button;
			this.formAction=formAction;
		}
	}

	private static class ShowHideElementsCommand extends Command{
		@SerializedName("ids")
		public String[] elementIDs;

		public ShowHideElementsCommand(boolean show, String[] elementIDs){
			super(show ? "show" : "hide");
			this.elementIDs=elementIDs;
		}
	}

	private static class InsertHtmlCommand extends Command{
		@SerializedName("id")
		public String id;
		@SerializedName("c")
		public String html;
		@SerializedName("m")
		public ElementInsertionMode insertionMode;

		public InsertHtmlCommand(String id, String html, ElementInsertionMode insertionMode){
			super("insert");
			this.id=id;
			this.html=html;
			this.insertionMode=insertionMode;
		}
	}

	private static class SetInputValueCommand extends Command{
		@SerializedName("id")
		public String id;
		@SerializedName("v")
		public String value;

		public SetInputValueCommand(String id, String value){
			super("setValue");
			this.id=id;
			this.value=value;
		}
	}

	private static class RefreshCommand extends Command{
		public RefreshCommand(){
			super("refresh");
		}
	}

	public static class ReplaceLocationCommand extends Command{
		@SerializedName("l")
		public String location;

		public ReplaceLocationCommand(String location){
			super("location");
			this.location=location;
		}
	}

	public static class AddRemoveClassCommand extends Command{
		@SerializedName("id")
		public String id;
		@SerializedName("cl")
		public String className;

		public AddRemoveClassCommand(boolean add, String id, String className){
			super(add ? "addClass" : "remClass");
			this.id=id;
			this.className=className;
		}
	}

	public static class SetAttributeCommand extends Command{
		@SerializedName("id")
		public String id;
		@SerializedName("n")
		public String name;
		@SerializedName("v")
		public String value;

		public SetAttributeCommand(String id, String name, String value){
			super("setAttr");
			this.id=id;
			this.name=name;
			this.value=value;
		}
	}

	public static class RunScriptCommand extends Command{
		@SerializedName("s")
		public String script;

		public RunScriptCommand(String script){
			super("run");
			this.script=script;
		}
	}

	public static class KeepBoxCommand extends Command{

		public KeepBoxCommand(){
			super("kb");
		}
	}

	public static class ShowSnackbarCommand extends Command{
		@SerializedName("t")
		public String text;

		public ShowSnackbarCommand(String text){
			super("snackbar");
			this.text=text;
		}
	}

	public static class SetURLCommand extends Command{
		public String url;

		public SetURLCommand(String url){
			super("setURL");
			this.url=url;
		}
	}
}
