package smithereen.routes.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.WebDeltaResponse;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class AdminCustomCSSRoutes{
	private static final Logger LOG=LoggerFactory.getLogger(AdminCustomCSSRoutes.class);

	public static Object customCSS(Request req, Response resp, Account self, ApplicationContext ctx){
		if(isMobile(req)){
			resp.redirect("/settings/admin");
			return "";
		}
		Lang l=lang(req);
		File dir=new File(Config.uploadPath, "css");
		String[] files=dir.exists() ? dir.list() : new String[0];
		return new RenderedTemplateResponse("admin_css", req)
				.with("commonCSS", Config.commonCSS)
				.with("desktopCSS", Config.desktopCSS)
				.with("mobileCSS", Config.mobileCSS)
				.with("files", files)
				.with("filesUrlPath", Config.uploadUrlPath+"/css")
				.pageTitle(l.get("admin_custom_css")+" | "+l.get("menu_admin"));
	}

	public static Object saveCustomCSS(Request req, Response resp, Account self, ApplicationContext ctx){
		Config.updateCSS(req.queryParams("common"), req.queryParams("desktop"), req.queryParams("mobile"));

		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object uploadFileForCSS(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req))
			throw new BadRequestException();

		Lang l=lang(req);

		File dir=new File(Config.uploadPath, "css");
		try{
			req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(null, 10*1024*1024, -1L, 0));
			Part part=req.raw().getPart("file");
			if(part==null)
				throw new BadRequestException();
			if(part.getSize()>10*1024*1024){
				throw new UserErrorException("err_file_upload_too_large", Map.of("maxSize", l.formatFileSize(10*1024*1024)));
			}

			String mime=part.getContentType();
			String fileName=part.getSubmittedFileName();
			int index=fileName.lastIndexOf('.');
			if(!mime.startsWith("image/") || index==-1)
				throw new UserErrorException("err_file_upload_image_format");

			String extension=fileName.substring(index+1).toLowerCase();
			if(!Set.of("jpg", "jpeg", "png", "webp", "gif", "svg", "avif").contains(extension))
				throw new UserErrorException("err_file_upload_image_format");

			String sanitizedName=fileName.substring(0, index).replaceAll("[^a-zA-Z0-9_-]", "_")+"."+extension;
			if(!dir.exists() && !dir.mkdirs())
				throw new IOException("Failed to create "+dir);

			File destination=new File(dir, sanitizedName);
			LOG.debug("Saving to {}", destination);
			try(InputStream in=part.getInputStream(); FileOutputStream out=new FileOutputStream(destination)){
				copyBytes(in, out);
			}
		}catch(IOException | ServletException x){
			throw new UserErrorException("err_file_upload", x);
		}

		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_css", req)
				.with("files", dir.list())
				.with("filesUrlPath", Config.uploadUrlPath+"/css");
		return new WebDeltaResponse(resp)
				.setContent("cssFiles", model.renderBlock("files"))
				.removeClass("cssFileButton", "loading");
	}

	public static Object deleteCssFile(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "file");
		String name=req.queryParams("file");
		int index=name.lastIndexOf('.');
		if(index==-1)
			throw new BadRequestException();

		String extension=name.substring(index+1).toLowerCase();
		String sanitizedName=name.substring(0, index).replaceAll("[^a-zA-Z0-9_-]", "_")+"."+extension;
		File dir=new File(Config.uploadPath, "css");
		File file=new File(dir, sanitizedName);
		if(!file.exists() || !file.delete())
			throw new ObjectNotFoundException();

		return new WebDeltaResponse(resp).remove("cssFile_"+sanitizedName);
	}
}
