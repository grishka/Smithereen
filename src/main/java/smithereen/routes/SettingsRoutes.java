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
import java.sql.SQLException;
import java.util.Random;

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
import smithereen.data.Account;
import smithereen.data.User;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;
import spark.Session;

public class SettingsRoutes{
	public static Object settings(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp)){
			Account self=req.session().attribute("account");
			JtwigModel model=JtwigModel.newModel();
			model.with("invitations", UserStorage.getInvites(self.user.id, true));
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
		return "";
	}

	public static Object createInvite(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)){
			Account self=req.session().attribute("account");
			byte[] code=new byte[16];
			new Random().nextBytes(code);
			UserStorage.putInvite(self.id, code, 1);
			req.session().attribute("settings.inviteMessage", Utils.lang(req).get("invitation_created"));
			resp.redirect("/settings/");
		}
		return "";
	}

	public static Object updatePassword(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)){
			Account self=req.session().attribute("account");
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
		}
		return "";
	}

	public static Object updateName(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)){
			Account self=req.session().attribute("account");
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
		}
		return "";
	}

	public static Object updateProfilePicture(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp)){
			Account self=req.session().attribute("account");
			try{
				req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
				Part part=req.raw().getPart("pic");
				if(part.getSize()>5*1024*1024){
					throw new IOException("file too large");
				}
				try(InputStream in=part.getInputStream()){
				/*byte[] buf=new byte[10240];
				int read;
				while((read=in.read(buf))>0){

				}*/
					BufferedImage image=ImageIO.read(in);
					if(image.getWidth()!=image.getHeight()){
						int size=Math.min(image.getWidth(), image.getHeight());
						BufferedImage square=new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
						Graphics2D g=square.createGraphics();
						g.drawImage(image, size/2-image.getWidth()/2, size/2-image.getHeight()/2, image.getWidth(), image.getHeight(), null);
						g.dispose();
						image=square;
					}

					BufferedImage resized50=Utils.getScaledInstance(image, 50, 50, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
					BufferedImage resized100=Utils.getScaledInstance(image, 100, 100, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
					BufferedImage resized200=Utils.getScaledInstance(image, 200, 200, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
					BufferedImage resized400=Utils.getScaledInstance(image, 400, 400, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);

					File profilePicsDir=new File(Config.uploadPath, "avatars");
					profilePicsDir.mkdirs();

					ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
					JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
					jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					jpegParams.setCompressionQuality(0.95f);

					File destFile=new File(profilePicsDir, self.user.username.toLowerCase()+"_50.jpg");
					try(FileImageOutputStream out=new FileImageOutputStream(destFile)){
						writer.setOutput(out);
						writer.write(null, new IIOImage(resized50, null, null), jpegParams);
					}

					destFile=new File(profilePicsDir, self.user.username.toLowerCase()+"_100.jpg");
					try(FileImageOutputStream out=new FileImageOutputStream(destFile)){
						writer.setOutput(out);
						writer.write(null, new IIOImage(resized100, null, null), jpegParams);
					}

					destFile=new File(profilePicsDir, self.user.username.toLowerCase()+"_200.jpg");
					try(FileImageOutputStream out=new FileImageOutputStream(destFile)){
						writer.setOutput(out);
						writer.write(null, new IIOImage(resized200, null, null), jpegParams);
					}

					destFile=new File(profilePicsDir, self.user.username.toLowerCase()+"_400.jpg");
					try(FileImageOutputStream out=new FileImageOutputStream(destFile)){
						writer.setOutput(out);
						writer.write(null, new IIOImage(resized400, null, null), jpegParams);
					}

					String path50="/s/uploads/avatars/"+self.user.username.toLowerCase()+"_50.jpg";
					String path100="/s/uploads/avatars/"+self.user.username.toLowerCase()+"_100.jpg";
					String path200="/s/uploads/avatars/"+self.user.username.toLowerCase()+"_200.jpg";
					String path400="/s/uploads/avatars/"+self.user.username.toLowerCase()+"_400.jpg";


					self.user.avatar=new User.Avatar();
					self.user.avatar.hasSizes=true;
					self.user.avatar.jpeg50=path50;
					self.user.avatar.jpeg100=path100;
					self.user.avatar.jpeg200=path200;
					self.user.avatar.jpeg400=path400;
					UserStorage.getById(self.user.id).avatar=self.user.avatar;
					UserStorage.updateProfilePicture(self.user.id, self.user.avatar.asJSON());

					req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("avatar_updated"));
					resp.redirect("/settings/");
				}
			}catch(IOException|ServletException x){
				req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("image_upload_error"));
				resp.redirect("/settings/");
			}
		}
		//return "Success";
		return "";
	}
}
