package smithereen.routes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.PrivacySetting;
import smithereen.model.SessionInfo;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.WebDeltaResponse;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.MediaStorageUtils;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.text.FormattedTextFormat;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class PhotosRoutes{
	public static Object myAlbums(Request req, Response resp, Account self, ApplicationContext ctx){
		return photoAlbums(req, resp, self.user, self, ctx);
	}

	public static Object userAlbums(Request req, Response resp){
		ApplicationContext ctx=context(req);
		SessionInfo session=sessionInfo(req);
		return photoAlbums(req, resp, ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id"))), session!=null ? session.account : null, ctx);
	}

	public static Object groupAlbums(Request req, Response resp){
		ApplicationContext ctx=context(req);
		SessionInfo session=sessionInfo(req);
		return photoAlbums(req, resp, ctx.getGroupsController().getGroupOrThrow(safeParseInt(req.params(":id"))), session!=null ? session.account : null, ctx);
	}

	private static Object photoAlbums(Request req, Response resp, Actor owner, Account self, ApplicationContext ctx){
		List<PhotoAlbum> albums=ctx.getPhotosController().getAllAlbums(owner, self==null ? null : self.user);
		Templates.addJsLangForPrivacySettings(req);
		Set<Long> needPhotos=albums.stream().map(a->a.coverID).filter(id->id!=0).collect(Collectors.toSet());
		Map<Long, Photo> covers=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		Lang l=lang(req);
		return new RenderedTemplateResponse("photo_albums", req)
				.with("albums", albums)
				.with("owner", owner)
				.with("covers", covers)
				.pageTitle(switch(owner){
					case User u when self!=null && u.id==self.user.id -> l.get("my_photo_albums");
					case User u -> l.get("user_photo_albums", Map.of("name", u.getFirstLastAndGender()));
					case Group g when g.isEvent() -> l.get("event_photo_albums");
					case Group g -> l.get("group_photo_albums");
					default -> throw new IllegalStateException("Unexpected value: " + owner);
				});
	}

	public static Object createAlbumForm(Request req, Response resp, Account self, ApplicationContext ctx){
		int ownerID=safeParseInt(req.queryParams("owner"));
		Actor owner;
		if(ownerID<0){
			Group group=ctx.getGroupsController().getGroupOrThrow(-ownerID);
			owner=group;
			ctx.getGroupsController().enforceUserAdminLevel(group, self.user, Group.AdminLevel.MODERATOR);
		}else{
			owner=self.user;
		}
		Map<String, Object> args=Map.of("owner", owner, "defaultPrivacy", PrivacySetting.DEFAULT);
		if(isMobile(req)){
			if(isMobile(req)){
				Templates.addJsLangForPrivacySettings(req);
			}
			return new RenderedTemplateResponse("photo_album_create_form", req)
					.pageTitle(lang(req).get("create_photo_album"))
					.withAll(args);
		}
		return wrapForm(req, resp, "photo_album_create_form", "/my/albums/create", lang(req).get("create_photo_album"), "create", "createAlbum",
				List.of(), null, null, args);
	}

	public static Object createAlbum(Request req, Response resp, Account self, ApplicationContext ctx){
		int ownerID=safeParseInt(req.queryParams("owner"));
		requireQueryParams(req, "title");
		long id;
		if(ownerID<0){
			Group group=ctx.getGroupsController().getGroupOrThrow(-ownerID);
			id=ctx.getPhotosController().createAlbum(self.user, group, req.queryParams("title"), req.queryParams("description"),
					"on".equals(req.queryParams("disableCommenting")), "on".equals(req.queryParams("restrictUploads")));
		}else{
			requireQueryParams(req, "viewPrivacy", "commentPrivacy");
			id=ctx.getPhotosController().createAlbum(self.user, req.queryParams("title"), req.queryParams("description"),
					PrivacySetting.fromJson(req.queryParams("viewPrivacy")), PrivacySetting.fromJson(req.queryParams("commentPrivacy")));
		}
		return ajaxAwareRedirect(req, resp, "/albums/"+encodeLong(XTEA.obfuscateObjectID(id, ObfuscatedObjectIDType.PHOTO_ALBUM)));
	}

	public static Object album(Request req, Response resp){
		SessionInfo session=sessionInfo(req);
		User self=session!=null && session.account!=null ? session.account.user : null;
		ApplicationContext ctx=context(req);
		PhotoAlbum album=ctx.getPhotosController().getAlbum(XTEA.deobfuscateObjectID(decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO_ALBUM), self);
		RenderedTemplateResponse model=new RenderedTemplateResponse("photo_album_view", req);
		model.with("album", album).pageTitle(album.title);
		Actor owner;
		if(album.ownerID>0)
			owner=ctx.getUsersController().getUserOrThrow(album.ownerID);
		else
			owner=ctx.getGroupsController().getGroupOrThrow(-album.ownerID);
		model.with("owner", owner);
		PaginatedList<Photo> photos=ctx.getPhotosController().getAlbumPhotos(self, album, offset(req), 100);
		model.paginate(photos);

		if(isAjax(req)){
			String paginationID=req.queryParams("pagination");
			if(StringUtils.isNotEmpty(paginationID)){
				WebDeltaResponse r=new WebDeltaResponse(resp)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("photosInner"));
				if(photos.offset+photos.perPage>=photos.total){
					r.remove("ajaxPagination_"+paginationID);
				}else{
					r.setAttribute("ajaxPaginationLink_"+paginationID, "href", req.pathInfo()+"?offset="+(photos.offset+photos.perPage));
				}
				return r;
			}
		}

		jsLangKey(req, "drop_files_here", "release_files_to_upload", "uploading_photo_X_of_Y", "add_more_photos", "photo_description", "photo_description_saved", "uploading_photos", "you_uploaded_X_photos");
		return model;
	}

	public static Object uploadPhoto(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		PhotoAlbum album=ctx.getPhotosController().getAlbum(XTEA.deobfuscateObjectID(decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO_ALBUM), info.account.user);
		if(!info.permissions.canUploadToPhotoAlbum(album))
			throw new UserActionNotAllowedException();

		LocalImage photo=MediaStorageUtils.saveUploadedImage(req, resp, info.account, false);
		long photoID=ctx.getPhotosController().createPhoto(info.account.user, album, photo.fileID, null, null);
		SizedImage.Type sizeType=SizedImage.Type.PHOTO_THUMB_SMALL;
		SizedImage.Dimensions size=photo.getDimensionsForSize(sizeType);

		return new JsonObjectBuilder()
				.add("id", encodeLong(XTEA.obfuscateObjectID(photoID, ObfuscatedObjectIDType.PHOTO)))
				.add("html", photo.generateHTML(SizedImage.Type.PHOTO_THUMB_SMALL, null, null, size.width, size.height, true))
				.build();
	}

	public static Object updatePhotoDescription(Request req, Response resp, Account self, ApplicationContext ctx){
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.deobfuscateObjectID(decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO));
		ctx.getPhotosController().updatePhotoDescription(self.user, photo, req.queryParams("description"), Objects.requireNonNullElse(enumValueOpt(req.queryParams("format"), FormattedTextFormat.class), self.prefs.textFormat));
		return "";
	}

	public static Object editAlbumForm(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		PhotoAlbum album=ctx.getPhotosController().getAlbum(XTEA.deobfuscateObjectID(decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO_ALBUM), info.account.user);
		if(!info.permissions.canEditPhotoAlbum(album))
			throw new UserActionNotAllowedException();
		RenderedTemplateResponse model=new RenderedTemplateResponse("photo_album_edit", req)
				.with("album", album)
				.pageTitle(album.title+" | "+lang(req).get("editing_photo_album"));
		Actor owner;
		if(album.ownerID>0)
			owner=ctx.getUsersController().getUserOrThrow(album.ownerID);
		else
			owner=ctx.getGroupsController().getGroupOrThrow(-album.ownerID);
		model.with("owner", owner);
		if(album.coverID!=0){
			model.with("cover", ctx.getPhotosController().getPhotoIgnoringPrivacy(album.coverID));
		}
		Templates.addJsLangForPrivacySettings(req);
		jsLangKey(req, "photo_description", "photo_description_saved");

		PaginatedList<Photo> photos=ctx.getPhotosController().getAlbumPhotos(info.account.user, album, offset(req), 100);
		model.paginate(photos);
		model.with("descriptionSources", ctx.getPhotosController().getPhotoDescriptionSources(photos.list.stream().map(p->p.id).collect(Collectors.toSet())));

		Set<Integer> needUsers=new HashSet<>();
		if(album.ownerID>0){
			needUsers.addAll(album.viewPrivacy.exceptUsers);
			needUsers.addAll(album.viewPrivacy.allowUsers);
			needUsers.addAll(album.commentPrivacy.exceptUsers);
			needUsers.addAll(album.commentPrivacy.allowUsers);
		}
		model.with("users", ctx.getUsersController().getUsers(needUsers));

		return model;
	}

	public static Object editAlbum(Request req, Response resp, Account self, ApplicationContext ctx){
		PhotoAlbum album=ctx.getPhotosController().getAlbum(XTEA.deobfuscateObjectID(decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO_ALBUM), self.user);
		requireQueryParams(req, "title");
		if(album.ownerID<0){
			ctx.getPhotosController().updateGroupAlbum(self.user, album, req.queryParams("title"), req.queryParams("description"),
					"on".equals(req.queryParams("disableCommenting")), "on".equals(req.queryParams("restrictUploads")));
		}else{
			requireQueryParams(req, "viewPrivacy", "commentPrivacy");
			ctx.getPhotosController().updateUserAlbum(self.user, album,  req.queryParams("title"), req.queryParams("description"),
					PrivacySetting.fromJson(req.queryParams("viewPrivacy")), PrivacySetting.fromJson(req.queryParams("commentPrivacy")));
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.show("albumEditMessage")
					.setContent("albumEditMessage", lang(req).get("photo_album_updated"));
		}
		return "";
	}

	public static Object confirmDeleteAlbum(Request req, Response resp, Account self, ApplicationContext ctx){
		return wrapConfirmation(req, resp, lang(req).get("delete_photo_album"), lang(req).get("delete_photo_album_confirm"), "/albums/"+req.params(":id")+"/delete");
	}

	public static Object deleteAlbum(Request req, Response resp, Account self, ApplicationContext ctx){
		PhotoAlbum album=ctx.getPhotosController().getAlbum(XTEA.deobfuscateObjectID(decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO_ALBUM), self.user);
		ctx.getPhotosController().deleteAlbum(self.user, album);
		if(album.ownerID>0)
			return ajaxAwareRedirect(req, resp, "/my/albums");
		return ajaxAwareRedirect(req, resp, "/groups/"+(-album.ownerID)+"/albums");
	}
}
