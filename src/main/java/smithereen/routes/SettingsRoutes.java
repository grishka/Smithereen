package smithereen.routes;

import org.json.JSONObject;
import org.jtwig.JtwigModel;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.LocalImage;
import smithereen.data.Account;
import smithereen.data.PhotoSize;
import smithereen.data.SessionInfo;
import smithereen.data.User;
import smithereen.lang.Lang;
import smithereen.libvips.VImage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;
import spark.Session;

public class SettingsRoutes{
	public static Object settings(Request req, Response resp, Account self) throws SQLException{
		JtwigModel model=JtwigModel.newModel();
		model.with("invitations", UserStorage.getInvites(self.id, true));
		model.with("languages", Lang.list).with("selectedLang", Utils.lang(req));
		Session s=req.session();
		if(s.attribute("settings.nameMessage")!=null){
			model.with("nameMessage", s.attribute("settings.nameMessage"));
			s.removeAttribute("settings.nameMessage");
		}
		if(s.attribute("settings.passwordMessage")!=null){
			model.with("passwordMessage", s.attribute("settings.passwordMessage"));
			s.removeAttribute("settings.passwordMessage");
		}
		if(s.attribute("settings.inviteMessage")!=null){
			model.with("inviteMessage", s.attribute("settings.inviteMessage"));
			s.removeAttribute("settings.inviteMessage");
		}
		if(s.attribute("settings.profilePicMessage")!=null){
			model.with("profilePicMessage", s.attribute("settings.profilePicMessage"));
			s.removeAttribute("settings.profilePicMessage");
		}
		return Utils.renderTemplate(req, "settings", model);
	}

	public static Object createInvite(Request req, Response resp, Account self) throws SQLException{
		byte[] code=new byte[16];
		new Random().nextBytes(code);
		UserStorage.putInvite(self.id, code, 1);
		req.session().attribute("settings.inviteMessage", Utils.lang(req).get("invitation_created"));
		resp.redirect("/settings/");
		return "";
	}

	public static Object updatePassword(Request req, Response resp, Account self) throws SQLException{
		String current=req.queryParams("current");
		String new1=req.queryParams("new");
		String new2=req.queryParams("new2");
		if(!new1.equals(new2)){
			req.session().attribute("settings.passwordMessage", Utils.lang(req).get("err_passwords_dont_match"));
		}else if(new1.length()<4){
			req.session().attribute("settings.passwordMessage", Utils.lang(req).get("err_password_short"));
		}else if(!SessionStorage.updatePassword(self.id, current, new1)){
			req.session().attribute("settings.passwordMessage", Utils.lang(req).get("err_old_password_incorrect"));
		}else{
			req.session().attribute("settings.passwordMessage", Utils.lang(req).get("password_changed"));
		}
		resp.redirect("/settings/");
		return "";
	}

	public static Object updateName(Request req, Response resp, Account self) throws SQLException{
		String first=req.queryParams("first_name");
		String last=req.queryParams("last_name");
		if(first.length()<2){
			req.session().attribute("settings.nameMessage", Utils.lang(req).get("err_name_too_short"));
		}else{
			UserStorage.changeName(self.user.id, first, last);
			req.session().attribute("settings.nameMessage", Utils.lang(req).get("name_changed"));
		}
		resp.redirect("/settings/");
		self.user=UserStorage.getById(self.user.id);
		return "";
	}

	public static Object updateProfilePicture(Request req, Response resp, Account self) throws SQLException{
		try{
			req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(null, 5*1024*1024, -1L, 0));
			Part part=req.raw().getPart("pic");
			if(part.getSize()>5*1024*1024){
				throw new IOException("file too large");
			}

			byte[] key=MessageDigest.getInstance("MD5").digest((self.user.username+","+System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
			String keyHex=Utils.byteArrayToHexString(key);

			File tmpDir = new File(System.getProperty("java.io.tmpdir"));
			File temp=new File(tmpDir, keyHex);
			//part.write(temp.getAbsolutePath());
			part.write(keyHex);
			VImage img=new VImage(temp.getAbsolutePath());
			if(img.getWidth()!=img.getHeight()){
				VImage cropped;
				if(img.getHeight()>img.getWidth()){
					cropped=img.crop(0, 0, img.getWidth(), img.getWidth());
				}else{
					cropped=img.crop(img.getWidth()/2-img.getHeight()/2, 0, img.getHeight(), img.getHeight());
				}
				img.release();
				img=cropped;
			}

			LocalImage ava=new LocalImage();
			File profilePicsDir=new File(Config.uploadPath, "avatars");
			profilePicsDir.mkdirs();
			try{
				MediaStorageUtils.writeResizedImages(img, new int[]{50, 100, 200, 400}, new PhotoSize.Type[]{PhotoSize.Type.SMALL, PhotoSize.Type.MEDIUM, PhotoSize.Type.LARGE, PhotoSize.Type.XLARGE},
						85, 80, keyHex, profilePicsDir, Config.uploadURLPath+"/avatars", ava.sizes);

				if(self.user.icon!=null){
					for(PhotoSize size : ((LocalImage) self.user.icon.get(0)).sizes){
						String path=size.src.getPath();
						String name=path.substring(path.lastIndexOf('/')+1);
						File file=new File(profilePicsDir, name);
						if(file.exists()){
							System.out.println("deleting: "+file.getAbsolutePath());
							file.delete();
						}
					}
				}

				self.user.icon=Collections.singletonList(ava);
				UserStorage.getById(self.user.id).icon=self.user.icon;
				UserStorage.updateProfilePicture(self.user.id, keyHex);
				temp.delete();
			}finally{
				img.release();
			}

			req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("avatar_updated"));
			resp.redirect("/settings/");
		}catch(IOException|ServletException|NoSuchAlgorithmException x){
			x.printStackTrace();
			req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("image_upload_error"));
			resp.redirect("/settings/");
		}
		return "";
	}

	public static Object setLanguage(Request req, Response resp) throws SQLException{
		String lang=req.queryParams("lang");
		SessionInfo info=req.session().attribute("info");
		if(info==null){
			req.session().attribute("info", info=new SessionInfo());
		}
		if(info.account!=null){
			info.account.prefs.locale=Locale.forLanguageTag(lang);
			SessionStorage.updatePreferences(info.account.id, info.account.prefs);
		}else{
			info.preferredLocale=Locale.forLanguageTag(lang);
		}
		resp.redirect("/settings/");
		return "";
	}

	public static Object setTimezone(Request req, Response resp) throws SQLException{
		String tz=req.queryParams("tz");
		SessionInfo info=req.session().attribute("info");
		if(info==null){
			req.session().attribute("info", info=new SessionInfo());
		}
		if(info.account!=null){
			info.account.prefs.timeZone=TimeZone.getTimeZone(tz);
			SessionStorage.updatePreferences(info.account.id, info.account.prefs);
		}else{
			info.timeZone=TimeZone.getTimeZone(tz);
		}
		if(req.queryParams("_ajax")!=null)
			return "";
		resp.redirect("/settings/");
		return "";
	}
}
