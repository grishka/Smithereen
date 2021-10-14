package smithereen.templates;

import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import smithereen.Utils;
import spark.Request;
import spark.Response;

public class RenderedTemplateResponse{
	String templateName;
	final HashMap<String, Object> model=new HashMap<>();
	private PebbleTemplate template;
	private Locale locale;
	private Request req;
	private List<NavBarItem> navBarItems;

	public RenderedTemplateResponse(String templateName, Request req){
		this.templateName=templateName;
		locale=Utils.localeForRequest(req);
		this.req=req;
		req.attribute("isTemplate", Boolean.TRUE);
	}

	public RenderedTemplateResponse with(String key, Object value){
		model.put(key, value);
		return this;
	}

	public RenderedTemplateResponse pageTitle(String title){
		model.put("title", title);
		return this;
	}

	public RenderedTemplateResponse addNavBarItem(String title, String href, String auxText){
		if(navBarItems==null)
			navBarItems=new ArrayList<>();
		navBarItems.add(new NavBarItem(title, href, auxText));
		return this;
	}

	public RenderedTemplateResponse addNavBarItem(String title, String href){
		return addNavBarItem(title, href, null);
	}

	public RenderedTemplateResponse addNavBarItem(String title){
		return addNavBarItem(title, null, null);
	}

	public void setName(String name){
		templateName=name;
	}

	public void renderToWriter(Writer writer) throws IOException{
		template=getAndPrepareTemplate(req);
		template.evaluate(writer, model, locale);
	}

	public String renderToString(){
		StringWriter writer=new StringWriter();
		try{
			renderToWriter(writer);
		}catch(IOException ignore){}
		return writer.toString();
	}

	private PebbleTemplate getAndPrepareTemplate(Request req){
		PebbleTemplate template=Templates.getTemplate(req, templateName);
		if(template==null)
			throw new IllegalStateException("Template with name '"+templateName+"' not found");
		Templates.addGlobalParamsToTemplate(req, this);
		if(navBarItems!=null)
			model.put("navBarItems", navBarItems);
		return template;
	}

	public String renderContentBlock(){
		return renderBlock("content");
	}

	public String renderBlock(String name){
		StringWriter writer=new StringWriter();
		try{
			template=getAndPrepareTemplate(req);
			template.evaluateBlock(name, writer, model, locale);
		}catch(IOException ignore){}
		return writer.toString();
	}
}
