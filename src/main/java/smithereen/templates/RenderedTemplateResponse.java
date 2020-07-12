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

	public RenderedTemplateResponse(String templateName){
		this.templateName=templateName;
	}

	public RenderedTemplateResponse with(String key, Object value){
		model.put(key, value);
		return this;
	}

	public void setName(String name){
		templateName=name;
	}

	private void renderToWriter(Request req, Writer writer){
		PebbleTemplate template=Templates.getTemplate(req, templateName);
		if(template==null)
			throw new IllegalStateException("Template with name '"+templateName+"' not found");
		Templates.addGlobalParamsToTemplate(req, this);
		Locale locale=Utils.localeForRequest(req);
		try{
			template.evaluate(writer, model, locale);
		}catch(IOException ignore){}
	}

	public String renderToString(Request req){
		req.attribute("isTemplate", Boolean.TRUE);
		StringWriter writer=new StringWriter();
		renderToWriter(req, writer);
		return writer.toString();
	}

	public void renderToResponse(Request req, Response resp){
		try{
			req.attribute("isTemplate", Boolean.TRUE);
			resp.raw().setCharacterEncoding("UTF-8");
			resp.type("text/html; charset=UTF-8");
			renderToWriter(req, resp.raw().getWriter());
		}catch(IOException ignore){}
	}
}
