package smithereen.routes;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.activities.Like;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.AttachmentHostContentObject;
import smithereen.model.Group;
import smithereen.model.MailMessage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.PrivacySetting;
import smithereen.model.ReportableContentObject;
import smithereen.model.SessionInfo;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.ViolationReport;
import smithereen.model.WebDeltaResponse;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.feed.GroupedNewsfeedEntry;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.media.PhotoViewerPhotoInfo;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.MediaStorageUtils;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.text.FormattedTextFormat;
import smithereen.text.TextProcessor;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class PhotosRoutes{
	private static Photo getPhotoForRequest(Request req){
		return context(req).getPhotosController().getPhotoIgnoringPrivacy(XTEA.deobfuscateObjectID(decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO));
	}

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
		int offset=offset(req);
		PaginatedList<Photo> photos=ctx.getPhotosController().getAlbumPhotos(self, album, offset, 100);
		model.paginate(photos);

		Map<Long, PhotoViewerInlineData> pvData=new HashMap<>();
		int i=0;
		for(Photo p:photos.list){
			pvData.put(p.id, new PhotoViewerInlineData(offset+i, "albums/"+album.getIdString(), p.image.getURLsForPhotoViewer()));
			i++;
		}
		model.with("photoViewerData", pvData);

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

	public static Object confirmDeletePhoto(Request req, Response resp, Account self, ApplicationContext ctx){
		return wrapConfirmation(req, resp, lang(req).get("delete_photo"), lang(req).get("delete_photo_confirm"), "/photos/"+req.params(":id")+"/delete");
	}

	public static Object deletePhoto(Request req, Response resp, Account self, ApplicationContext ctx){
		Photo photo=getPhotoForRequest(req);
		ctx.getPhotosController().deletePhoto(self.user, photo);
		if(isAjax(req)){
			String from=req.queryParams("from");
			if("edit".equals(from)){
				return new WebDeltaResponse(resp).remove("photoEditRow_"+photo.getIdString());
			}
		}
		return ajaxAwareRedirect(req, resp, back(req));
	}

	private static PhotoViewerPhotoInfo makePhotoInfoForAttachment(Request req, PhotoAttachment pa, User author, Instant createdAt){
		String html;
		if(isMobile(req)){
			html=StringUtils.isNotEmpty(pa.description) ? TextProcessor.escapeHTML(pa.description) : "";
		}else{
			RenderedTemplateResponse model=new RenderedTemplateResponse("photo_viewer_info_comments", req);
			model.with("description", pa.description)
					.with("author", author)
					.with("createdAt", createdAt);
			html=model.renderToString();
		}
		return new PhotoViewerPhotoInfo(null, author.getProfileURL(), author.getCompleteName(), null, null, html, EnumSet.noneOf(PhotoViewerPhotoInfo.AllowedAction.class), pa.image.getURLsForPhotoViewer(), null);
	}

	private static PhotoViewerPhotoInfo makePhotoInfoForPhoto(Request req, Photo photo, PhotoAlbum album, Map<Integer, User> users, Map<Long, UserInteractions> interactions){
		String html;
		User author=users.get(photo.authorID);
		if(isMobile(req)){
			html=StringUtils.isNotEmpty(photo.description) ? TextProcessor.postprocessPostHTMLForDisplay(photo.description, false, false) : "";
		}else{
			RenderedTemplateResponse model=new RenderedTemplateResponse("photo_viewer_info_comments", req);
			model.with("description", photo.description)
					.with("author", author)
					.with("album", album)
					.with("createdAt", photo.createdAt)
					.with("interactions", interactions!=null ? interactions.get(photo.id) : null)
					.with("photo", photo);
			html=model.renderToString();
		}
		PhotoViewerPhotoInfo.Interactions pvInteractions;
		UserInteractions ui=interactions!=null ? interactions.get(photo.id) : null;
		if(ui!=null){
			pvInteractions=new PhotoViewerPhotoInfo.Interactions(ui.likeCount, ui.isLiked);
		}else{
			pvInteractions=null;
		}
		return new PhotoViewerPhotoInfo(encodeLong(XTEA.obfuscateObjectID(photo.id, ObfuscatedObjectIDType.PHOTO)), author!=null ? author.getProfileURL() : "/id"+photo.authorID,
				author!=null ? author.getCompleteName() : "DELETED", encodeLong(XTEA.obfuscateObjectID(album.id, ObfuscatedObjectIDType.PHOTO_ALBUM)), album.title,
				html, EnumSet.noneOf(PhotoViewerPhotoInfo.AllowedAction.class), photo.image.getURLsForPhotoViewer(), pvInteractions);
	}

	private static PaginatedList<PhotoViewerPhotoInfo> makePhotoInfoForAttachHostObject(Request req, AttachmentHostContentObject obj, User author, Instant createdAt){
		List<PhotoViewerPhotoInfo> photos=obj.getProcessedAttachments().stream()
				.map(a->a instanceof PhotoAttachment pa ? makePhotoInfoForAttachment(req, pa, author, createdAt) : null)
				.filter(Objects::nonNull)
				.toList();
		return new PaginatedList<>(photos, photos.size());
	}
	
	public static Object ajaxViewerInfo(Request req, Response resp){
		Account selfAccount=sessionInfo(req) instanceof SessionInfo si ? si.account : null;
		User self=selfAccount!=null ? selfAccount.user : null;
		ApplicationContext ctx=context(req);
		requireQueryParams(req, "list", "offset");
		String[] listParts=req.queryParams("list").split("/");
		if(listParts.length<2)
			throw new BadRequestException();
		int total;
		String title;
		List<PhotoViewerPhotoInfo> photos=switch(listParts[0]){
			case "posts" -> {
				int id=safeParseInt(listParts[1]);
				Post post=ctx.getWallController().getPostOrThrow(id);
				ctx.getPrivacyController().enforceObjectPrivacy(self, post);
				User author=ctx.getUsersController().getUserOrThrow(post.authorID);

				PaginatedList<PhotoViewerPhotoInfo> info=makePhotoInfoForAttachHostObject(req, post, author, post.createdAt);
				total=info.total;
				title=null;
				yield info.list;
			}
			case "messages" -> {
				if(self==null)
					throw new UserActionNotAllowedException();
				MailMessage msg=ctx.getMailController().getMessage(self, decodeLong(listParts[1]), false);
				User author=ctx.getUsersController().getUserOrThrow(msg.senderID);

				PaginatedList<PhotoViewerPhotoInfo> info=makePhotoInfoForAttachHostObject(req, msg, author, msg.createdAt);
				total=info.total;
				title=null;
				yield info.list;
			}
			case "albums" -> {
				long albumID=XTEA.deobfuscateObjectID(decodeLong(listParts[1]), ObfuscatedObjectIDType.PHOTO_ALBUM);
				PhotoAlbum album=ctx.getPhotosController().getAlbum(albumID, self);
				PaginatedList<Photo> _photos=ctx.getPhotosController().getAlbumPhotos(self, album, offset(req), 10);
				total=_photos.total;
				title=album.title;
				Map<Integer, User> users=ctx.getUsersController().getUsers(_photos.list.stream().map(ph->ph.authorID).collect(Collectors.toSet()));
				Map<Long, UserInteractions> interactions=ctx.getUserInteractionsController().getUserInteractions(_photos.list, self);
				yield _photos.list.stream().map(ph->makePhotoInfoForPhoto(req, ph, album, users, interactions)).toList();
			}
			case "friendsFeedGrouped" -> {
				if(self==null)
					throw new UserActionNotAllowedException();
				int id=safeParseInt(listParts[1]);
				PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getFriendsFeed(selfAccount, timeZoneForRequest(req), id, 0, 100);
				if(feed.list.isEmpty() || !(feed.list.getFirst() instanceof GroupedNewsfeedEntry gne) || gne.childEntriesType!=NewsfeedEntry.Type.ADD_PHOTO)
					throw new ObjectNotFoundException();
				total=gne.childEntries.size();
				title=null;
				int offset=offset(req);
				if(offset>=total)
					throw new BadRequestException();
				List<Long> photoIDs=gne.childEntries.subList(offset, Math.min(offset+10, total)).stream().map(e->e.objectID).toList();
				Map<Long, Photo> _photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(photoIDs);
				Map<Long, PhotoAlbum> albums=ctx.getPhotosController().getAlbumsIgnoringPrivacy(_photos.values().stream().map(p->p.albumID).collect(Collectors.toSet()));
				Map<Integer, User> users=ctx.getUsersController().getUsers(_photos.values().stream().map(ph->ph.authorID).collect(Collectors.toSet()));
				Map<Long, UserInteractions> interactions=ctx.getUserInteractionsController().getUserInteractions(_photos.values().stream().toList(), self);
				yield photoIDs.stream()
						.map(pid->{
							Photo p=_photos.get(pid);
							return makePhotoInfoForPhoto(req, p, albums.get(p.albumID), users, interactions);
						})
						.toList();
			}
			case "friendsFeed" -> {
				if(self==null)
					throw new UserActionNotAllowedException();
				int id=safeParseInt(listParts[1]);
				List<NewsfeedEntry> feed=ctx.getNewsfeedController().getFriendsFeed(selfAccount, timeZoneForRequest(req), id, 0, 1).list;
				if(feed.isEmpty())
					throw new ObjectNotFoundException();
				NewsfeedEntry e=feed.getFirst();
				if(e.type!=NewsfeedEntry.Type.ADD_PHOTO)
					throw new ObjectNotFoundException();
				Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(e.objectID);
				PhotoAlbum album=ctx.getPhotosController().getAlbumIgnoringPrivacy(photo.albumID);
				Map<Integer, User> users=ctx.getUsersController().getUsers(Set.of(photo.authorID));
				Map<Long, UserInteractions> interactions=ctx.getUserInteractionsController().getUserInteractions(List.of(photo), self);
				total=1;
				title=null;
				yield List.of(makePhotoInfoForPhoto(req, photo, album, users, interactions));
			}
			case "single" -> {
				long id=XTEA.deobfuscateObjectID(decodeLong(listParts[1]), ObfuscatedObjectIDType.PHOTO);
				Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(id);
				PhotoAlbum album=ctx.getPhotosController().getAlbum(photo.albumID, self);
				Map<Integer, User> users=ctx.getUsersController().getUsers(Set.of(photo.authorID));
				Map<Long, UserInteractions> interactions=ctx.getUserInteractionsController().getUserInteractions(List.of(photo), self);
				total=1;
				title=null;
				yield List.of(makePhotoInfoForPhoto(req, photo, album, users, interactions));
			}
			case "liked" -> {
				PaginatedList<Long> photoIDs=ctx.getUserInteractionsController().getLikedObjects(self, Like.ObjectType.PHOTO, offset(req), 100);
				Map<Long, Photo> photoObjects=ctx.getPhotosController().getPhotosIgnoringPrivacy(photoIDs.list);
				Map<Long, PhotoAlbum> albums=ctx.getPhotosController().getAlbumsIgnoringPrivacy(photoObjects.values().stream().map(p->p.albumID).collect(Collectors.toSet()));
				Map<Integer, User> users=ctx.getUsersController().getUsers(photoObjects.values().stream().map(ph->ph.authorID).collect(Collectors.toSet()));
				Map<Long, UserInteractions> interactions=ctx.getUserInteractionsController().getUserInteractions(photoObjects.values().stream().toList(), self);
				total=photoIDs.total;
				title=lang(req).get("bookmarks_title");
				yield photoIDs.list.stream()
						.map(pid->{
							Photo p=photoObjects.get(pid);
							return makePhotoInfoForPhoto(req, p, albums.get(p.albumID), users, interactions);
						})
						.toList();
			}
			default -> throw new BadRequestException();
		};
		resp.type("application/json");
		HashMap<String, Object> r=new HashMap<>();
		r.put("total", total);
		r.put("photos", photos);
		if(title!=null)
			r.put("title", title);
		return gson.toJson(r);
	}

	public static Object ajaxViewerInfoForReport(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "list", "offset");
		String[] listParts=req.queryParams("list").split("/");
		if(listParts.length<4)
			throw new BadRequestException();
		if(!listParts[0].equals("reports"))
			throw new BadRequestException();
		ViolationReport report=ctx.getModerationController().getViolationReportByID(safeParseInt(listParts[1]), true);
		long objID=safeParseLong(listParts[3]);
		ReportableContentObject obj=switch(listParts[2]){
			case "posts" -> {
				for(ReportableContentObject co:report.content){
					if(co instanceof Post post && post.id==objID)
						yield co;
				}
				throw new ObjectNotFoundException();
			}
			case "messages" -> {
				for(ReportableContentObject co:report.content){
					if(co instanceof MailMessage msg && msg.id==objID)
						yield co;
				}
				throw new ObjectNotFoundException();
			}
			default -> throw new IllegalStateException("Unexpected value: " + listParts[2]);
		};
		PaginatedList<PhotoViewerPhotoInfo> info=switch(obj){
			case Post post -> makePhotoInfoForAttachHostObject(req, post, ctx.getUsersController().getUserOrThrow(post.authorID), post.createdAt);
			case MailMessage msg -> makePhotoInfoForAttachHostObject(req, msg, ctx.getUsersController().getUserOrThrow(msg.senderID), msg.createdAt);
		};
		resp.type("application/json");
		HashMap<String, Object> r=new HashMap<>();
		r.put("total", info.total);
		r.put("photos", info.list);
		return gson.toJson(r);
	}

	public static Object like(Request req, Response resp){
		return UserInteractionsRoutes.like(req, resp, getPhotoForRequest(req));
	}

	public static Object unlike(Request req, Response resp, Account self, ApplicationContext ctx){
		return UserInteractionsRoutes.setLiked(req, resp, self, ctx, getPhotoForRequest(req), false);
	}

	public static Object likePopover(Request req, Response resp){
		return UserInteractionsRoutes.likePopover(req, resp, getPhotoForRequest(req));
	}

	public static Object likeList(Request req, Response resp){
		return UserInteractionsRoutes.likeList(req, resp, getPhotoForRequest(req));
	}
}
