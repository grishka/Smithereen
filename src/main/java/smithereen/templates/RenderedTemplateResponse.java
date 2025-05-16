package smithereen.templates;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.model.PaginatedList;
import spark.Request;
import spark.utils.StringUtils;

public class RenderedTemplateResponse{
	private static final Logger LOG=LoggerFactory.getLogger(RenderedTemplateResponse.class);

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

	public static RenderedTemplateResponse ofAjaxLayer(String templateName, Request req){
		RenderedTemplateResponse model;
		if(Utils.isAjax(req)){
			if(Utils.isMobile(req)){
				model=new RenderedTemplateResponse(templateName, req);
			}else{
				model=new RenderedTemplateResponse("layer_with_title", req).with("contentTemplate", templateName);
			}
		}else{
			model=new RenderedTemplateResponse("content_wrap", req).with("contentTemplate", templateName);
		}
		return model;
	}

	public RenderedTemplateResponse with(String key, Object value){
		model.put(key, value);
		return this;
	}

	public RenderedTemplateResponse withAll(Map<String, Object> values){
		model.putAll(values);
		return this;
	}

	public RenderedTemplateResponse pageTitle(String title){
		model.put("title", title);
		return this;
	}

	public RenderedTemplateResponse mobileToolbarTitle(String title){
		model.put("toolbarTitle", title);
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

	public RenderedTemplateResponse paginate(PaginatedList<?> list, String urlPrefix, String firstPageURL){
		model.put("items", list.list);
		model.put("paginationOffset", list.offset);
		model.put("paginationPerPage", list.perPage);
		model.put("totalItems", list.total);
		model.put("paginationUrlPrefix", urlPrefix);
		if(StringUtils.isNotEmpty(firstPageURL))
			model.put("paginationFirstPageUrl", firstPageURL);
		return this;
	}

	public RenderedTemplateResponse paginate(PaginatedList<?> list){
		String pathWithQuery=req.pathInfo();
		HashSet<String> queryKeys=new HashSet<>(req.queryParams());
		queryKeys.remove("offset");
		queryKeys.remove("_ajax");
		queryKeys.remove("pagination");
		if(!queryKeys.isEmpty()){
			 pathWithQuery+='?'+queryKeys.stream().map(k->k+'='+URLEncoder.encode(req.queryParams(k), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
		}
		return paginate(list, pathWithQuery+(queryKeys.isEmpty() ? '?' : '&')+"offset=", pathWithQuery);
	}

	public void setName(String name){
		templateName=name;
	}

	public RenderedTemplateResponse addMessage(Request req, String messageKeyInSession){
		addMessage(req, messageKeyInSession, "message");
		return this;
	}

	public RenderedTemplateResponse addMessage(Request req, String messageKeyInSession, String templateKey){
		String msg=req.session().attribute(messageKeyInSession);
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute(messageKeyInSession);
			with(templateKey, msg);
		}
		return this;
	}

	public RenderedTemplateResponse headerBack(String href, String title){
		with("headerBackHref", href);
		with("headerBackTitle", title);
		return this;
	}

	public RenderedTemplateResponse headerBack(Actor actor){
		headerBack(actor.getProfileURL(), actor.getName());
		return this;
	}

	public void renderToWriter(Writer writer) throws IOException{
		try{
			template=getAndPrepareTemplate(req);
			template.evaluate(writer, model, locale);
		}catch(Throwable x){
			writer.write("<pre>");
			x.printStackTrace(new PrintWriter(writer));
			writer.write("</pre>");
			LOG.error("Error rendering template {}", templateName, x);
		}
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

	public Object get(String key){
		return model.get(key);
	}
}
