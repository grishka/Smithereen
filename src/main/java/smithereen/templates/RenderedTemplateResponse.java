package smithereen.templates;

import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
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
