package smithereen.routes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.activities.Like;
import smithereen.controllers.FriendsController;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.AttachmentHostContentObject;
import smithereen.model.CommentViewType;
import smithereen.model.Group;
import smithereen.model.MailMessage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.OwnedContentObject;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.PrivacySetting;
import smithereen.model.RemoteImage;
import smithereen.model.SessionInfo;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.ViolationReport;
import smithereen.model.WebDeltaResponse;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentParentObjectID;
import smithereen.model.feed.GroupedNewsfeedEntry;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.media.MediaFileRecord;
import smithereen.model.media.PhotoViewerInlineData;
import smithereen.model.media.PhotoViewerPhotoInfo;
import smithereen.model.photos.AvatarCropRects;
import smithereen.model.photos.ImageRect;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.photos.PhotoTag;
import smithereen.model.reports.ReportableContentObject;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.storage.MediaStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.text.FormattedTextFormat;
import smithereen.text.FormattedTextSource;
import smithereen.text.TextProcessor;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class PhotosRoutes{
	private static final Logger LOG=LoggerFactory.getLogger(PhotosRoutes.class);

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
		List<PhotoAlbum> albums=ctx.getPhotosController().getAllAlbums(owner, self==null ? null : self.user, true);
		Templates.addJsLangForPrivacySettings(req);
		Set<Long> needPhotos=albums.stream().map(a->a.coverID).filter(id->id!=0).collect(Collectors.toSet());
		Map<Long, Photo> covers=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		Lang l=lang(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("photo_albums", req)
				.with("albums", albums)
				.with("owner", owner)
				.with("covers", covers)
				.headerBack(owner)
				.pageTitle(switch(owner){
					case User u when self!=null && u.id==self.user.id -> l.get("my_photo_albums");
					case User u -> l.get("user_photo_albums", Map.of("name", u.getFirstLastAndGender()));
					case Group g when g.isEvent() -> l.get("event_photo_albums");
					case Group g -> l.get("group_photo_albums");
					default -> throw new IllegalStateException("Unexpected value: " + owner);
				});
		if(!isMobile(req)){
			PaginatedList<Photo> photos=ctx.getPhotosController().getAllPhotos(owner, self==null ? null : self.user, 0, 100);
			model.paginate(photos, owner.getTypeAndIdForURL()+"/allPhotos?offset=", owner.getTypeAndIdForURL()+"/albums");
			Map<Long, PhotoViewerInlineData> pvData=new HashMap<>();
			int i=0;
			for(Photo p:photos.list){
				pvData.put(p.id, new PhotoViewerInlineData(i, "all/"+owner.getOwnerID(), p.image.getURLsForPhotoViewer()));
				i++;
			}
			model.with("photoViewerData", pvData);
		}
		return model;
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
		model.with("album", album);
		Actor owner;
		if(album.ownerID>0)
			owner=ctx.getUsersController().getUserOrThrow(album.ownerID);
		else
			owner=ctx.getGroupsController().getGroupOrThrow(-album.ownerID);
		model.with("owner", owner).headerBack(owner).pageTitle(album.getLocalizedTitle(lang(req), self, owner));
		int offset=offset(req);
		PaginatedList<Photo> photos=ctx.getPhotosController().getAlbumPhotos(self, album, offset, 100, false);
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

		jsLangKey(req, "drop_files_here", "release_files_to_upload", "uploading_photo_X_of_Y", "add_more_photos", "photo_description", "photo_description_saved", "uploading_photos", "you_uploaded_X_photos",
				"delete", "delete_photo", "delete_photo_confirm");
		return model;
	}

	public static Object uploadPhoto(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		PhotoAlbum album=ctx.getPhotosController().getAlbum(XTEA.deobfuscateObjectID(decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO_ALBUM), info.account.user);
		if(album.systemType!=null || !info.permissions.canUploadToPhotoAlbum(album))
			throw new UserActionNotAllowedException();

		LocalImage photo=MediaStorageUtils.saveUploadedImage(req, resp, info.account, false);
		long photoID=ctx.getPhotosController().createPhoto(info.account.user, album, photo.fileID, null, null);
		SizedImage.Type sizeType=SizedImage.Type.PHOTO_THUMB_SMALL;
		SizedImage.Dimensions size=photo.getDimensionsForSize(sizeType);

		return new JsonObjectBuilder()
				.add("id", encodeLong(XTEA.obfuscateObjectID(photoID, ObfuscatedObjectIDType.PHOTO)))
				.add("html", photo.generateHTML(SizedImage.Type.PHOTO_THUMB_MEDIUM, null, null, size.width, size.height, true, null))
				.build();
	}

	public static Object updatePhotoDescription(Request req, Response resp, Account self, ApplicationContext ctx){
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.deobfuscateObjectID(decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO));
		ctx.getPhotosController().updatePhotoDescription(self.user, photo, req.queryParams("description"), Objects.requireNonNullElse(enumValueOpt(req.queryParams("format"), FormattedTextFormat.class), self.prefs.textFormat));
		if("viewer".equals(req.queryParams("from")) && !isMobile(req) && isAjax(req)){
			String idStr=photo.getIdString();
			WebDeltaResponse r=new WebDeltaResponse(resp)
					.remove("pvDescriptionForm_"+idStr)
					.show("pvEditDescriptionW_"+idStr);
			if(StringUtils.isNotEmpty(photo.description)){
				r.show("pvDescription_"+idStr)
						.hide("pvDescriptionPlaceholder_"+idStr)
						.setContent("pvDescription_"+idStr, TextProcessor.postprocessPostHTMLForDisplay(photo.description, false, false));
			}else{
				r.hide("pvDescription_"+idStr)
						.show("pvDescriptionPlaceholder_"+idStr);
			}
			return r;
		}
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

		PaginatedList<Photo> photos=ctx.getPhotosController().getAlbumPhotos(info.account.user, album, offset(req), 100, false);
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
			}else if("viewer".equals(from)){
				return new WebDeltaResponse(resp)
						.runScript("LayerManager.getMediaInstance().getTopLayer().dismiss();")
						.remove("photo"+photo.getIdString());
			}
		}
		return ajaxAwareRedirect(req, resp, back(req));
	}

	private static PhotoViewerPhotoInfo makePhotoInfoForAttachment(Request req, PhotoAttachment pa, User self, User author, Instant createdAt, AttachmentHostContentObject parent, int index, EnumSet<PhotoViewerPhotoInfo.AllowedAction> allowedActions){
		String html;
		String origURL;
		String saveURL;
		String saveType, saveID;
		if(allowedActions==null)
			allowedActions=EnumSet.noneOf(PhotoViewerPhotoInfo.AllowedAction.class);
		if(self==null || self.id==author.id || parent==null){
			saveURL=saveType=saveID=null;
		}else{
			saveURL=UriBuilder.local()
					.path("photos", "saveAttachmentToAlbum")
					.queryParam("type", saveType=switch(parent){
						case Post post -> "post";
						case Comment comment -> "comment";
						case MailMessage msg -> "message";
					})
					.queryParam("id", saveID=switch(parent){
						case Post post -> String.valueOf(post.id);
						case Comment comment -> comment.getIDString();
						case MailMessage msg -> msg.encodedID;
					})
					.queryParam("index", String.valueOf(index))
					.queryParam("csrf", sessionInfo(req).csrfToken)
					.build()
					.toString();
		}
		if(isMobile(req)){
			html=StringUtils.isNotEmpty(pa.description) ? TextProcessor.escapeHTML(pa.description).replace("\n", "<br/>") : "";
			origURL=pa.image.getOriginalURI().toString();
		}else{
			RenderedTemplateResponse model=new RenderedTemplateResponse("photo_viewer_info_comments", req);
			model.with("description", pa.description==null ? null : pa.description.replace("\n", "<br/>"))
					.with("author", author)
					.with("createdAt", createdAt)
					.with("originalImageURL", pa.image.getOriginalURI())
					.with("allowedActions", allowedActions.stream().map(Object::toString).collect(Collectors.toSet()));
			if(saveURL!=null){
				model.with("saveURL", saveURL)
						.with("saveElementID", saveType+"_"+saveID+"_"+index);
			}
			html=model.renderToString();
			origURL=null;
		}
		return new PhotoViewerPhotoInfo(null, author.getProfileURL(), author.getCompleteName(), null, null, html, null,
				allowedActions, pa.image.getURLsForPhotoViewer(), null, origURL, null, null, saveURL);
	}

	private static EnumSet<PhotoViewerPhotoInfo.AllowedAction> getAllowedActionsForPhoto(ApplicationContext ctx, User self, Photo photo, PhotoAlbum album){
		EnumSet<PhotoViewerPhotoInfo.AllowedAction> allowedActions=EnumSet.noneOf(PhotoViewerPhotoInfo.AllowedAction.class);
		if(ctx.getPhotosController().canManagePhoto(self, photo)){
			allowedActions.add(PhotoViewerPhotoInfo.AllowedAction.DELETE);
			allowedActions.add(PhotoViewerPhotoInfo.AllowedAction.EDIT_DESCRIPTION);
			allowedActions.add(PhotoViewerPhotoInfo.AllowedAction.ROTATE);
			if(photo.apID==null){
				allowedActions.add(PhotoViewerPhotoInfo.AllowedAction.ADD_TAGS);
				allowedActions.add(PhotoViewerPhotoInfo.AllowedAction.MANAGE_TAGS);
			}
		}
		if(ctx.getPhotosController().canManageAlbum(self, album) && album.systemType==null){
			allowedActions.add(PhotoViewerPhotoInfo.AllowedAction.SET_AS_COVER);
		}
		if(self!=null && photo.authorID!=self.id){
			allowedActions.add(PhotoViewerPhotoInfo.AllowedAction.REPORT);
			allowedActions.add(PhotoViewerPhotoInfo.AllowedAction.SAVE_TO_ALBUM);
		}
		return allowedActions;
	}

	private static PhotoViewerPhotoInfo makePhotoInfoForPhoto(Request req, Photo photo, PhotoAlbum album, Map<Integer, User> users, Map<Long, UserInteractions> interactions,
															  Account self, PaginatedList<CommentViewModel> comments, Map<Long, UserInteractions> commentsInteractions, Map<Long, List<PhotoTag>> allTags){
		ApplicationContext ctx=context(req);
		String html;
		User author=users.get(photo.authorID);
		User owner=photo.ownerID>0 ? users.get(photo.ownerID) : null;
		PhotoViewerPhotoInfo.Interactions pvInteractions;
		UserInteractions ui=interactions!=null ? interactions.get(photo.id) : null;
		String origURL;
		EnumSet<PhotoViewerPhotoInfo.AllowedAction> allowedActions=getAllowedActionsForPhoto(ctx, self!=null ? self.user : null, photo, album);
		String topHTML=null;
		List<PhotoTag> tags=allTags.getOrDefault(photo.id, List.of());
		if(isMobile(req)){
			RenderedTemplateResponse model=new RenderedTemplateResponse("photo_viewer_bottom", req);
			model.with("description", photo.description)
					.with("users", users)
					.with("author", author)
					.with("album", album)
					.with("photo", photo)
					.with("tags", tags);
			html=model.renderToString();
			if(ui!=null){
				pvInteractions=new PhotoViewerPhotoInfo.Interactions(ui.likeCount, ui.isLiked, ui.commentCount);
			}else{
				pvInteractions=null;
			}
			origURL=photo.image.getOriginalURI().toString();
		}else{
			RenderedTemplateResponse model=new RenderedTemplateResponse("photo_viewer_info_comments", req);
			model.with("description", photo.description)
					.with("users", users)
					.with("author", author)
					.with("album", album)
					.with("createdAt", photo.createdAt)
					.with("interactions", interactions!=null ? interactions.get(photo.id) : null)
					.with("photo", photo)
					.with("originalImageURL", photo.image.getOriginalURI())
					.with("allowedActions", allowedActions.stream().map(Object::toString).collect(Collectors.toSet()))
					.with("tags", tags);
			if(comments!=null){
				model.with("comments", comments)
						.with("commentViewType", self!=null ? self.prefs.commentViewType : CommentViewType.THREADED)
						.with("commentInteractions", commentsInteractions)
						.with("maxReplyDepth", PostRoutes.getMaxReplyDepth(self)-1);
			}
			html=model.renderToString();
			pvInteractions=null;
			origURL=null;
			if(self!=null){
				for(PhotoTag tag:tags){
					if(tag.userID()==self.id && !tag.approved()){
						topHTML=new RenderedTemplateResponse("photo_new_tag_confirm", req)
								.with("placer", users.get(tag.placerID()))
								.with("photo", photo)
								.with("tagID", tag.id())
								.renderToString();
						break;
					}
				}
			}
		}
		return new PhotoViewerPhotoInfo(encodeLong(XTEA.obfuscateObjectID(photo.id, ObfuscatedObjectIDType.PHOTO)), author!=null ? author.getProfileURL() : "/id"+photo.authorID,
				author!=null ? author.getCompleteName() : "DELETED", encodeLong(XTEA.obfuscateObjectID(album.id, ObfuscatedObjectIDType.PHOTO_ALBUM)), album.getLocalizedTitle(lang(req), self!=null ? self.user : null, owner),
				html, topHTML, allowedActions, photo.image.getURLsForPhotoViewer(), pvInteractions, origURL, photo.getURL(), photo.apID==null ? null : photo.getActivityPubURL().toString(), null);
	}

	private static List<PhotoViewerPhotoInfo> makePhotoInfosForPhotoList(Request req, List<Photo> photos, ApplicationContext ctx, Account self, Map<Long, PhotoAlbum> albums){
		HashSet<Integer> needUsers=new HashSet<>();
		Map<Long, UserInteractions> interactions=ctx.getUserInteractionsController().getUserInteractions(photos, self!=null ? self.user : null);
		Map<CommentParentObjectID, PaginatedList<CommentViewModel>> comments;
		Map<Long, UserInteractions> commentsInteractions;
		if(isMobile(req)){
			comments=Map.of();
			commentsInteractions=Map.of();
		}else{
			CommentViewType commentViewType=self!=null ? self.prefs.commentViewType : CommentViewType.THREADED;
			comments=ctx.getCommentsController().getCommentsForFeed(photos.stream().map(Photo::getCommentParentID).collect(Collectors.toSet()), commentViewType==CommentViewType.FLAT, 5);
			CommentViewModel.collectUserIDs(comments.values().stream().flatMap(l->l.list.stream()).toList(), needUsers);
			commentsInteractions=ctx.getUserInteractionsController().getUserInteractions(comments.values().stream().flatMap(l->l.list.stream().map(vm->vm.post)).toList(), self!=null ? self.user : null);
		}
		photos.stream().map(ph->ph.authorID).forEach(needUsers::add);
		Map<Long, List<PhotoTag>> tags=ctx.getPhotosController().getTagsForPhotos(photos.stream().map(p->p.id).collect(Collectors.toSet()));
		tags.values()
				.stream()
				.flatMap(List::stream)
				.map(PhotoTag::userID)
				.filter(id->id!=0)
				.forEach(needUsers::add);
		if(self!=null){
			tags.values()
					.stream()
					.flatMap(List::stream)
					.filter(t->!t.approved() && t.userID()==self.id)
					.map(PhotoTag::placerID)
					.forEach(needUsers::add);
		}
		Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers);
		Set<Long> needAdditionalAlbums=photos.stream().map(p->p.albumID).filter(id->!albums.containsKey(id)).collect(Collectors.toSet());
		Map<Long, PhotoAlbum> _albums;
		if(!needAdditionalAlbums.isEmpty()){
			Map<Long, PhotoAlbum> additionalAlbums=ctx.getPhotosController().getAlbumsIgnoringPrivacy(needAdditionalAlbums);
			_albums=new HashMap<>(albums);
			_albums.putAll(additionalAlbums);
		}else{
			_albums=albums;
		}
		return photos.stream().map(ph->makePhotoInfoForPhoto(req, ph, _albums.get(ph.albumID), users, interactions, self, comments.get(ph.getCommentParentID()), commentsInteractions, tags)).toList();
	}

	private static PaginatedList<PhotoViewerPhotoInfo> makePhotoInfoForAttachHostObject(Request req, AttachmentHostContentObject obj, User author, Instant createdAt, Account self){
		ApplicationContext ctx=context(req);
		List<PhotoViewerPhotoInfo> photos=new ArrayList<>();
		int i=0;
		List<Attachment> attachments=obj.getProcessedAttachments();
		Set<Long> needPhotos=attachments.stream()
				.map(a->a instanceof PhotoAttachment pa && pa.photoID!=0 ? pa.photoID : null)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Set<URI> needRemotePhotos=attachments.stream()
				.map(a->a instanceof PhotoAttachment pa && pa.image instanceof RemoteImage ri && ri.photoActivityPubID!=null ? ri.photoActivityPubID : null)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<URI, Long> remotePhotoIDs=needRemotePhotos.isEmpty() ? Map.of() : ctx.getPhotosController().getPhotoIdsByActivityPubIds(needRemotePhotos);
		if(!remotePhotoIDs.isEmpty()){
			needPhotos=new HashSet<>(needPhotos);
			needPhotos.addAll(remotePhotoIDs.values());
		}
		Map<Long, Photo> actualPhotos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		Map<Long, PhotoAlbum> albums=ctx.getPhotosController().getAlbums(actualPhotos.values().stream().map(p->p.albumID).collect(Collectors.toSet()), self!=null ? self.user : null);
		Map<Long, PhotoViewerPhotoInfo> photoInfos;
		if(!albums.isEmpty()){
			photoInfos=makePhotoInfosForPhotoList(req, new ArrayList<>(actualPhotos.values()), ctx, self, albums).stream().collect(Collectors.toMap(pi->XTEA.decodeObjectID(pi.id(), ObfuscatedObjectIDType.PHOTO), Function.identity()));
		}else{
			photoInfos=Map.of();
		}
		for(Attachment att:attachments){
			if(att instanceof PhotoAttachment pa){
				long photoID;
				if(pa.photoID!=0)
					photoID=pa.photoID;
				else if(pa.image instanceof RemoteImage ri && ri.photoActivityPubID!=null)
					photoID=remotePhotoIDs.getOrDefault(ri.photoActivityPubID, 0L);
				else
					photoID=0;

				if(photoID!=0 && photoInfos.get(photoID) instanceof PhotoViewerPhotoInfo pi){
					photos.add(pi);
				}else{
					photos.add(makePhotoInfoForAttachment(req, pa, self!=null ? self.user : null, author, createdAt, obj, i, null));
				}
			}
			i++;
		}
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

				PaginatedList<PhotoViewerPhotoInfo> info=makePhotoInfoForAttachHostObject(req, post, author, post.createdAt, selfAccount);
				total=info.total;
				title=null;
				yield info.list;
			}
			case "comments" -> {
				long id=XTEA.decodeObjectID(listParts[1], ObfuscatedObjectIDType.COMMENT);
				Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(id);
				ctx.getCommentsController().getCommentParent(self, comment); // enforces privacy
				User author=ctx.getUsersController().getUserOrThrow(comment.authorID);

				PaginatedList<PhotoViewerPhotoInfo> info=makePhotoInfoForAttachHostObject(req, comment, author, comment.createdAt, selfAccount);
				total=info.total;
				title=null;
				yield info.list;
			}
			case "messages" -> {
				if(self==null)
					throw new UserActionNotAllowedException();
				MailMessage msg=ctx.getMailController().getMessage(self, decodeLong(listParts[1]), false);
				User author=ctx.getUsersController().getUserOrThrow(msg.senderID);

				PaginatedList<PhotoViewerPhotoInfo> info=makePhotoInfoForAttachHostObject(req, msg, author, msg.createdAt, selfAccount);
				total=info.total;
				title=null;
				yield info.list;
			}
			case "albums" -> {
				long albumID=XTEA.deobfuscateObjectID(decodeLong(listParts[1]), ObfuscatedObjectIDType.PHOTO_ALBUM);
				PhotoAlbum album=ctx.getPhotosController().getAlbum(albumID, self);
				PaginatedList<Photo> _photos=ctx.getPhotosController().getAlbumPhotos(self, album, offset(req), 10, false);
				total=_photos.total;
				title=album.getLocalizedTitle(lang(req), self, ctx.getWallController().getContentAuthorAndOwner(album).owner());
				yield makePhotoInfosForPhotoList(req, _photos.list, ctx, selfAccount, Map.of(album.id, album));
			}
			case "friendsFeedGrouped" -> {
				if(self==null)
					throw new UserActionNotAllowedException();
				int id=safeParseInt(listParts[1]);
				PaginatedList<NewsfeedEntry> feed=ctx.getNewsfeedController().getFriendsFeed(selfAccount, timeZoneForRequest(req), id, 0, 100);
				if(feed.list.isEmpty() || !(feed.list.getFirst() instanceof GroupedNewsfeedEntry gne) || (gne.childEntriesType!=NewsfeedEntry.Type.ADD_PHOTO && gne.childEntriesType!=NewsfeedEntry.Type.PHOTO_TAG))
					throw new ObjectNotFoundException();
				total=gne.childEntries.size();
				title=null;
				int offset=offset(req);
				if(offset>=total)
					throw new BadRequestException();
				List<Long> photoIDs=gne.childEntries.subList(offset, Math.min(offset+10, total)).stream().map(e->e.objectID).toList();
				Map<Long, Photo> _photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(photoIDs);
				Map<Long, PhotoAlbum> albums=ctx.getPhotosController().getAlbumsIgnoringPrivacy(_photos.values().stream().map(p->p.albumID).collect(Collectors.toSet()));

				yield makePhotoInfosForPhotoList(req, photoIDs.stream().map(_photos::get).toList(), ctx, selfAccount, albums);
			}
			case "friendsFeed" -> {
				if(self==null)
					throw new UserActionNotAllowedException();
				int id=safeParseInt(listParts[1]);
				List<NewsfeedEntry> feed=ctx.getNewsfeedController().getFriendsFeed(selfAccount, timeZoneForRequest(req), id, 0, 1).list;
				if(feed.isEmpty())
					throw new ObjectNotFoundException();
				NewsfeedEntry e=feed.getFirst();
				if(e.type!=NewsfeedEntry.Type.ADD_PHOTO && e.type!=NewsfeedEntry.Type.PHOTO_TAG)
					throw new ObjectNotFoundException();
				Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(e.objectID);
				PhotoAlbum album=ctx.getPhotosController().getAlbumIgnoringPrivacy(photo.albumID);
				total=1;
				title=null;
				yield makePhotoInfosForPhotoList(req, List.of(photo), ctx, selfAccount, Map.of(album.id, album));
			}
			case "single" -> {
				long id=XTEA.deobfuscateObjectID(decodeLong(listParts[1]), ObfuscatedObjectIDType.PHOTO);
				Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(id);
				PhotoAlbum album=ctx.getPhotosController().getAlbum(photo.albumID, self);
				total=1;
				title=null;
				yield makePhotoInfosForPhotoList(req, List.of(photo), ctx, selfAccount, Map.of(album.id, album));
			}
			case "liked" -> {
				if(self==null)
					throw new UserActionNotAllowedException();
				PaginatedList<Long> photoIDs=ctx.getUserInteractionsController().getLikedObjects(self, Like.ObjectType.PHOTO, offset(req), 10);
				Map<Long, Photo> photoObjects=ctx.getPhotosController().getPhotosIgnoringPrivacy(photoIDs.list);
				Map<Long, PhotoAlbum> albums=ctx.getPhotosController().getAlbumsIgnoringPrivacy(photoObjects.values().stream().map(p->p.albumID).collect(Collectors.toSet()));
				total=photoIDs.total;
				title=lang(req).get("bookmarks_title");
				yield makePhotoInfosForPhotoList(req, photoIDs.list.stream().map(photoObjects::get).toList(), ctx, selfAccount, albums);
			}
			case "all" -> {
				int oid=safeParseInt(listParts[1]);
				Actor owner=oid>0 ? ctx.getUsersController().getUserOrThrow(oid) : ctx.getGroupsController().getGroupOrThrow(-oid);
				PaginatedList<Photo> allPhotos=ctx.getPhotosController().getAllPhotos(owner, self, offset(req), 10);
				Map<Long, PhotoAlbum> albums=ctx.getPhotosController().getAlbumsIgnoringPrivacy(allPhotos.list.stream().map(p->p.albumID).collect(Collectors.toSet()));
				total=allPhotos.total;
				title=null;
				yield makePhotoInfosForPhotoList(req, allPhotos.list, ctx, selfAccount, albums);
			}
			case "rawFile" -> {
				title=null;
				total=1;

				String[] idParts=listParts[1].split(":");
				if(idParts.length!=2)
					throw new BadRequestException();

				long fileID;
				byte[] fileRandomID;
				try{
					byte[] _fileID=Base64.getUrlDecoder().decode(idParts[0]);
					fileRandomID=Base64.getUrlDecoder().decode(idParts[1]);
					if(_fileID.length!=8 || fileRandomID.length!=18)
						throw new BadRequestException();
					fileID=XTEA.deobfuscateObjectID(Utils.unpackLong(_fileID), ObfuscatedObjectIDType.MEDIA_FILE);
				}catch(IllegalArgumentException x){
					throw new BadRequestException();
				}
				MediaFileRecord mfr;
				try{
					mfr=MediaStorage.getMediaFileRecord(fileID);
				}catch(SQLException x){
					throw new InternalServerErrorException(x);
				}
				if(mfr==null || !Arrays.equals(mfr.id().randomID(), fileRandomID))
					throw new ObjectNotFoundException();
				LocalImage img=new LocalImage();
				img.fileID=fileID;
				img.fillIn(mfr);
				PhotoAttachment fakeAttachment=new PhotoAttachment();
				fakeAttachment.image=img;
				fakeAttachment.blurHash=img.blurHash;
				yield List.of(makePhotoInfoForAttachment(req, fakeAttachment, self, self, Instant.now(), null, 0, EnumSet.of(PhotoViewerPhotoInfo.AllowedAction.EDIT_DESCRIPTION)));
			}
			case "newTags" -> {
				if(self==null)
					throw new UserActionNotAllowedException();
				PaginatedList<Photo> allPhotos=ctx.getPhotosController().getUserUnapprovedTaggedPhotos(self, offset(req), 10);
				Map<Long, PhotoAlbum> albums=ctx.getPhotosController().getAlbumsIgnoringPrivacy(allPhotos.list.stream().map(p->p.albumID).collect(Collectors.toSet()));
				total=allPhotos.total;
				title=null;
				yield makePhotoInfosForPhotoList(req, allPhotos.list, ctx, selfAccount, albums);
			}
			default -> {
				LOG.debug("Unknown photo list {}", req.queryParams("list"));
				throw new BadRequestException();
			}
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
			case "photos" -> {
				for(ReportableContentObject co:report.content){
					if(co instanceof Photo photo && photo.getIdString().equals(listParts[3]))
						yield co;
				}
				throw new ObjectNotFoundException();
			}
			case "comments" -> {
				for(ReportableContentObject co:report.content){
					if(co instanceof Comment comment && comment.id==XTEA.decodeObjectID(listParts[3], ObfuscatedObjectIDType.COMMENT))
						yield co;
				}
				throw new ObjectNotFoundException();
			}
			default -> throw new IllegalStateException("Unexpected value: " + listParts[2]);
		};
		PaginatedList<PhotoViewerPhotoInfo> info=switch(obj){
			case Post post -> makePhotoInfoForAttachHostObject(req, post, ctx.getUsersController().getUserOrThrow(post.authorID), post.createdAt, self);
			case MailMessage msg -> makePhotoInfoForAttachHostObject(req, msg, ctx.getUsersController().getUserOrThrow(msg.senderID), msg.createdAt, self);
			case Photo photo -> new PaginatedList<>(makePhotoInfosForPhotoList(req, List.of(photo), ctx, self, ctx.getPhotosController().getAlbumsIgnoringPrivacy(List.of(photo.albumID))), 1);
			case Comment comment -> makePhotoInfoForAttachHostObject(req, comment, ctx.getUsersController().getUserOrThrow(comment.authorID), comment.createdAt, self);
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

	public static Object setPhotoAsAlbumCover(Request req, Response resp, Account self, ApplicationContext ctx){
		Photo photo=getPhotoForRequest(req);
		PhotoAlbum album=ctx.getPhotosController().getAlbum(photo.albumID, self.user);
		ctx.getPhotosController().setPhotoAsAlbumCover(self.user, album, photo);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).showSnackbar(lang(req).get("photo_was_set_as_album_cover"));
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object photo(Request req, Response resp){
		ApplicationContext ctx=context(req);
		Account self=sessionInfo(req) instanceof SessionInfo si ? si.account : null;
		Photo photo=getPhotoForRequest(req);
		PhotoAlbum album=ctx.getPhotosController().getAlbum(photo.albumID, self!=null ? self.user : null);
		int index=ctx.getPhotosController().getPhotoIndexInAlbum(album, photo);
		if(req.queryParams("nojs")!=null || isMobile(req)){
			RenderedTemplateResponse model=new RenderedTemplateResponse("photo_view", req);
			EnumSet<PhotoViewerPhotoInfo.AllowedAction> allowedActions=getAllowedActionsForPhoto(ctx, self==null ? null : self.user, photo, album);
			OwnerAndAuthor oaa=ctx.getWallController().getContentAuthorAndOwner(photo);
			Photo next=ctx.getPhotosController().getAlbumPhotos(self==null ? null : self.user, album, (index+1)%album.numPhotos, 1, false).list.getFirst();
			Photo prev=ctx.getPhotosController().getAlbumPhotos(self==null ? null : self.user, album, index==0 ? album.numPhotos-1 : index-1, 1, false).list.getFirst();

			boolean canComment=switch(oaa.owner()){
				case User u -> ctx.getPrivacyController().checkUserPrivacy(self!=null ? self.user : null, u, album.commentPrivacy);
				case Group g -> !album.flags.contains(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING);
				default -> throw new IllegalStateException("Unexpected value: " + oaa.owner());
			};
			CommentViewType commentViewType=self!=null ? self.prefs.commentViewType : CommentViewType.THREADED;
			PaginatedList<CommentViewModel> comments=ctx.getCommentsController().getComments(photo, List.of(), offset(req), 100, 50, commentViewType);
			HashSet<Integer> needUsers=new HashSet<>();
			CommentViewModel.collectUserIDs(comments.list, needUsers);
			List<CommentViewModel> allComments=new ArrayList<>();
			for(CommentViewModel cvm:comments.list){
				allComments.add(cvm);
				cvm.getAllReplies(allComments);
			}
			Map<Long, UserInteractions> commentsInteractions=ctx.getUserInteractionsController().getUserInteractions(allComments.stream().map(vm->vm.post).toList(), self!=null ? self.user : null);

			List<PhotoTag> tags=ctx.getPhotosController().getTagsForPhoto(photo.id);
			PhotoTag unapprovedTag=null;
			for(PhotoTag tag:tags){
				if(self!=null && tag.userID()==self.user.id && !tag.approved()){
					unapprovedTag=tag;
					needUsers.add(tag.placerID());
				}
				needUsers.add(tag.userID());
			}

			model.with("description", photo.description)
					.with("author", oaa.author())
					.with("owner", oaa.owner())
					.headerBack(oaa.owner())
					.with("album", album)
					.with("createdAt", photo.createdAt)
					.with("interactions", ctx.getUserInteractionsController().getUserInteractions(List.of(photo), self==null ? null : self.user).get(photo.id))
					.with("photo", photo)
					.with("originalImageURL", photo.image.getOriginalURI())
					.with("allowedActions", allowedActions.stream().map(Object::toString).collect(Collectors.toSet()))
					.with("index", index)
					.with("nextURL", next.getURL()+"?nojs")
					.with("prevURL", prev.getURL()+"?nojs")
					.paginate(comments)
					.with("canComment", canComment)
					.with("users", ctx.getUsersController().getUsers(needUsers))
					.with("maxReplyDepth", PostRoutes.getMaxReplyDepth(self))
					.with("commentViewType", commentViewType)
					.with("commentInteractions", commentsInteractions)
					.with("tags", tags)
					.with("unapprovedTag", unapprovedTag)
					.pageTitle(album.title);

			if(isMobile(req)){
				model.with("photoInteractions", ctx.getUserInteractionsController().getUserInteractions(List.of(photo), self==null ? null : self.user));
			}
			jsLangKey(req, "yes", "no", "cancel", "delete_reply", "delete_reply_confirm", "delete", "post_form_cw", "post_form_cw_placeholder", "attach_menu_photo", "attach_menu_cw");

			return model;
		}

		RenderedTemplateResponse model=new RenderedTemplateResponse("photo_album_view", req);
		model.with("album", album).pageTitle(album.title);
		Actor owner;
		if(album.ownerID>0)
			owner=ctx.getUsersController().getUserOrThrow(album.ownerID);
		else
			owner=ctx.getGroupsController().getGroupOrThrow(-album.ownerID);
		model.with("owner", owner).headerBack(owner);
		int offset=offset(req);
		PaginatedList<Photo> photos=ctx.getPhotosController().getAlbumPhotos(self==null ? null : self.user, album, offset, 100, false);
		model.paginate(photos, album.getURL(), album.getURL());

		Map<Long, PhotoViewerInlineData> pvData=new HashMap<>();
		int i=0;
		for(Photo p:photos.list){
			pvData.put(p.id, new PhotoViewerInlineData(offset+i, "albums/"+album.getIdString(), p.image.getURLsForPhotoViewer()));
			i++;
		}
		model.with("photoViewerData", pvData);
		model.with("photoDataToOpenViewer", new PhotoViewerInlineData(index, "albums/"+album.getIdString(), photo.image.getURLsForPhotoViewer()));
		jsLangKey(req, "drop_files_here", "release_files_to_upload", "uploading_photo_X_of_Y", "add_more_photos", "photo_description", "photo_description_saved", "uploading_photos", "you_uploaded_X_photos",
				"delete", "delete_photo", "delete_photo_confirm");
		return model;
	}

	public static Object ajaxEditDescription(Request req, Response resp, Account self, ApplicationContext ctx){
		if(isMobile(req) || !isAjax(req))
			return "";
		Photo photo=getPhotoForRequest(req);
		ctx.getPrivacyController().enforceObjectPrivacy(self.user, photo);
		FormattedTextSource source=ctx.getPhotosController().getPhotoDescriptionSources(List.of(photo.id)).get(photo.id);
		RenderedTemplateResponse model=new RenderedTemplateResponse("photo_viewer_edit_description", req)
				.with("photo", photo)
				.with("source", source);
		String idStr=photo.getIdString();
		return new WebDeltaResponse(resp)
				.hide("pvEditDescriptionW_"+idStr)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, "pvEditDescriptionW_"+idStr, model.renderToString())
				.runScript("var ta=ge(\"pvDescriptionTextarea_"+idStr+"\"); autoSizeTextArea(ta); addSendOnCtrlEnter(ta);");
	}

	public static Object saveToAlbum(Request req, Response resp, Account self, ApplicationContext ctx){
		Photo photo=getPhotoForRequest(req);
		ctx.getPrivacyController().enforceObjectPrivacy(self.user, photo);
		ctx.getPhotosController().savePhotoToAlbum(self.user, photo);
		if(!isAjax(req)){
			resp.redirect(back(req));
			return "";
		}
		Lang l=lang(req);
		WebDeltaResponse wdr=new WebDeltaResponse(resp)
				.showSnackbar(l.get("photo_saved_to_album"));
		if(!isMobile(req)){
			String contID="photoSave_"+photo.getIdString();
			wdr.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, contID,
							"<a class=\"grayText\" style=\"pointer-events: none\">"+l.get("photo_saved_short")+"</a>")
					.remove(contID);
		}
		return wdr;
	}

	public static Object saveAttachmentToAlbum(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "type", "id", "index");
		String id=req.queryParams("id");
		String type=req.queryParams("type");
		int index=safeParseInt(req.queryParams("index"));
		if(index<0)
			throw new BadRequestException();
		AttachmentHostContentObject obj=switch(type){
			case "post" -> ctx.getWallController().getPostOrThrow(safeParseInt(id));
			case "message" -> ctx.getMailController().getMessage(self.user, decodeLong(id), false);
			case "comment" -> ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(id, ObfuscatedObjectIDType.COMMENT));
			default -> throw new BadRequestException();
		};
		if(obj instanceof OwnedContentObject owned)
			ctx.getPrivacyController().enforceObjectPrivacy(self.user, owned);
		List<Attachment> attachments=obj.getProcessedAttachments();
		if(index>=attachments.size())
			throw new BadRequestException();
		Attachment att=attachments.get(index);
		if(!(att instanceof PhotoAttachment photo))
			throw new BadRequestException();
		ctx.getPhotosController().saveImageToAlbum(self.user, photo.image);
		if(!isAjax(req)){
			resp.redirect(back(req));
			return "";
		}
		Lang l=lang(req);
		WebDeltaResponse wdr=new WebDeltaResponse(resp)
				.showSnackbar(l.get("photo_saved_to_album"));
		if(!isMobile(req)){
			String contID="photoSave_"+type+"_"+id+"_"+index;
			wdr.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, contID,
							"<a class=\"grayText\" style=\"pointer-events: none\">"+l.get("photo_saved_short")+"</a>")
					.remove(contID);
		}
		return wdr;
	}

	public static Object allUserPhotos(Request req, Response resp){
		ApplicationContext ctx=context(req);
		return allPhotos(req, resp, ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id"))), ctx);
	}

	public static Object allGroupPhotos(Request req, Response resp){
		ApplicationContext ctx=context(req);
		return allPhotos(req, resp, ctx.getGroupsController().getGroupOrThrow(safeParseInt(req.params(":id"))), ctx);
	}

	private static Object allPhotos(Request req, Response resp, Actor owner, ApplicationContext ctx){
		if(!isAjax(req))
			throw new BadRequestException();
		String paginationID=req.queryParams("pagination");
		if(StringUtils.isEmpty(paginationID))
			throw new BadRequestException();
		RenderedTemplateResponse model=new RenderedTemplateResponse("photo_album_view", req);
		SessionInfo session=sessionInfo(req);
		User self=session!=null && session.account!=null ? session.account.user : null;
		int offset=offset(req);

		PaginatedList<Photo> photos=ctx.getPhotosController().getAllPhotos(owner, self, offset, 100);
		Map<Long, PhotoViewerInlineData> pvData=new HashMap<>();
		int i=0;
		for(Photo p:photos.list){
			pvData.put(p.id, new PhotoViewerInlineData(i+offset, "all/"+owner.getOwnerID(), p.image.getURLsForPhotoViewer()));
			i++;
		}
		model.with("photoViewerData", pvData);
		model.paginate(photos);

		WebDeltaResponse r=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("photosInner"));
		if(photos.offset+photos.perPage>=photos.total){
			r.remove("ajaxPagination_"+paginationID);
		}else{
			r.setAttribute("ajaxPaginationLink_"+paginationID, "href", req.pathInfo()+"?offset="+(photos.offset+photos.perPage));
		}
		return r;
	}
	
	public static Object attachPhotosBox(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req))
			return "";
		requireQueryParams(req, "id");
		String formID=req.queryParams("id");
		if(formID.contains("'"))
			throw new BadRequestException();

		RenderedTemplateResponse model=new RenderedTemplateResponse("attach_photo_box", req);
		Lang l=lang(req);
		List<PhotoAlbum> albums=ctx.getPhotosController().getAllAlbums(self.user, self.user, true);
		Set<Long> needPhotos=albums.stream().map(a->a.coverID).filter(id->id!=0).collect(Collectors.toSet());
		Map<Long, Photo> covers=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		model.with("albums", albums)
				.with("covers", covers)
				.with("formID", formID)
				.with("showUpload", true);
		PaginatedList<Photo> photos=ctx.getPhotosController().getAllPhotos(self.user, self.user, 0, 100);
		model.paginate(photos, "/photos/attachBoxAll?id="+formID+"&offset=", "/photos/attachBox");
		WebDeltaResponse r=new WebDeltaResponse(resp);
		if(isMobile(req))
			r.box(l.get("photo_attach_title"), model.renderToString(), "photoAttach", true);
		else
			r.box(l.get("photo_attach_title"), model.renderToString(), "photoAttach", 642);
		return r.runScript("initDynamicControls(ge('photoAttach')); PostForm.initPhotoAttachBox('"+formID+"');");
	}

	public static Object attachPhotosBoxAll(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req))
			return "";
		requireQueryParams(req, "id");
		String formID=req.queryParams("id");
		if(formID.contains("'"))
			throw new BadRequestException();

		String paginationID=req.queryParams("pagination");
		if(StringUtils.isEmpty(paginationID))
			throw new BadRequestException();

		PaginatedList<Photo> photos=ctx.getPhotosController().getAllPhotos(self.user, self.user, offset(req), 100);
		RenderedTemplateResponse model=new RenderedTemplateResponse("attach_photo_box", req);
		model.paginate(photos, "/photos/attachBoxAll?id="+formID+"&offset=", "/photos/attachBox")
				.with("formID", formID);

		WebDeltaResponse r=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("photosInner"));
		if(photos.offset+photos.perPage>=photos.total){
			r.remove("ajaxPagination_"+paginationID);
		}else{
			r.setAttribute("ajaxPaginationLink_"+paginationID, "href", req.pathInfo()+"?id="+formID+"&offset="+(photos.offset+photos.perPage));
		}
		return r;
	}

	public static Object attachPhotosBoxAlbum(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req))
			return "";
		requireQueryParams(req, "id", "album");
		String formID=req.queryParams("id");
		if(formID.contains("'"))
			throw new BadRequestException();
		PhotoAlbum album=ctx.getPhotosController().getAlbum(XTEA.decodeObjectID(req.queryParams("album"), ObfuscatedObjectIDType.PHOTO_ALBUM), self.user);
		Lang l=lang(req);

		RenderedTemplateResponse model=new RenderedTemplateResponse("attach_photo_box", req);
		model.with("formID", formID);
		PaginatedList<Photo> photos=ctx.getPhotosController().getAlbumPhotos(self.user, album, offset(req), 100, false);
		model.paginate(photos, "/photos/attachBoxAlbum?id="+formID+"&album="+album.getIdString()+"&offset=", "");

		if(req.queryParams("pagination")!=null){
			String paginationID=req.queryParams("pagination");
			WebDeltaResponse r=new WebDeltaResponse(resp)
					.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderBlock("photosInner"));
			if(photos.offset+photos.perPage>=photos.total){
				r.remove("ajaxPagination_"+paginationID);
			}else{
				r.setAttribute("ajaxPaginationLink_"+paginationID, "href", req.pathInfo()+"?id="+formID+"&album="+album.getIdString()+"&offset="+(photos.offset+photos.perPage));
			}
			return r;
		}
		Actor owner;
		if(album.ownerID==self.user.id)
			owner=self.user;
		else
			owner=ctx.getWallController().getContentAuthorAndOwner(album).owner();
		String boxTitle=l.get("photo_attach_title_album", Map.of("album", album.getLocalizedTitle(l, self.user, owner)));
		WebDeltaResponse r=new WebDeltaResponse(resp);
		if(isMobile(req))
			r.box(boxTitle, model.renderToString(), "photoAttachAlbum", true);
		else
			r.box(boxTitle, model.renderToString(), "photoAttachAlbum", 642);
		return r.runScript("initDynamicControls(ge('photoAttachAlbum'));");
	}

	public static Object rotatePhoto(Request req, Response resp, Account self, ApplicationContext ctx){
		Photo photo=getPhotoForRequest(req);
		SizedImage.Rotation rotation=photo.metadata!=null && photo.metadata.rotation!=null ? photo.metadata.rotation : SizedImage.Rotation._0;
		SizedImage.Rotation newRotation;
		if(req.queryParams("cw")!=null)
			newRotation=rotation.cw();
		else if(req.queryParams("ccw")!=null)
			newRotation=rotation.ccw();
		else
			throw new BadRequestException();
		ctx.getPhotosController().setPhotoRotation(self.user, photo, newRotation);
		if(isAjax(req)){
			String from=req.queryParams("from");
			if("viewer".equals(from)){
				resp.type("application/json");
				return gson.toJson(photo.image.getURLsForPhotoViewer());
			}else if("edit".equals(from)){
				return new WebDeltaResponse(resp)
						.setContent("photoEditThumb_"+photo.getIdString(), photo.image.generateHTML(SizedImage.Type.PHOTO_THUMB_MEDIUM, null, null, photo.getWidth(), photo.getHeight(), true, null));
			}
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object updateAvatarCrop(Request req, Response resp, Account self, ApplicationContext ctx){
		Photo photo=getPhotoForRequest(req);
		AvatarCropRects cropRects=AvatarCropRects.fromString(req.queryParams("crop"));
		if(cropRects==null)
			throw new BadRequestException();
		SizedImage.Rotation rotation;
		try{
			rotation=SizedImage.Rotation.valueOf(safeParseInt(req.queryParams("rotation")));
		}catch(IllegalArgumentException x){
			throw new BadRequestException();
		}

		ctx.getPhotosController().updateAvatarCrop(self.user, photo, cropRects, rotation);

		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object getFriendsForTagging(Request req, Response resp, Account self, ApplicationContext ctx){
		List<User> friends=ctx.getFriendsController().getFriends(self.user, 0, 10_000, FriendsController.SortOrder.ID_ASCENDING).list;
		return new RenderedTemplateResponse("photo_viewer_tag_friends", req)
				.with("friends", friends);
	}

	public static Object addTag(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req))
			throw new BadRequestException();
		requireQueryParams(req, "rect");
		String[] rectStr=req.queryParams("rect").split(",");
		if(rectStr.length!=4)
			throw new BadRequestException();
		float[] rectArr=new float[4];
		for(int i=0;i<4;i++){
			try{
				float v=Float.parseFloat(rectStr[i]);
				if(v<0 || v>1)
					throw new BadRequestException();
				rectArr[i]=v;
			}catch(NumberFormatException x){
				throw new BadRequestException();
			}
		}
		if(rectArr[0]>rectArr[2] || rectArr[1]>rectArr[3])
			throw new BadRequestException();
		ImageRect rect=new ImageRect(rectArr[0], rectArr[1], rectArr[2], rectArr[3]);

		Photo photo=getPhotoForRequest(req);
		User user;
		String name;
		if(req.queryParams("user")!=null){
			user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.queryParams("user")));
			name=null;
		}else{
			user=null;
			requireQueryParams(req, "name");
			name=req.queryParams("name");
		}
		List<PhotoTag> existingTags=ctx.getPhotosController().getTagsForPhoto(photo.id);
		long tagID=ctx.getPhotosController().createPhotoTag(self.user, photo, user, name, rect);

		Lang l=lang(req);
		String tagHTML="<span id=\"pvTag_"+photo.getIdString()+"_"+tagID+"\">";
		if(!existingTags.isEmpty())
			tagHTML+=", ";
		if(user!=null)
			tagHTML+="<a href=\""+user.getProfileURL()+"\" ";
		else
			tagHTML+="<span ";
		tagHTML+="data-rect=\""+String.join(",", rectStr)+"\">"+TextProcessor.escapeHTML(user!=null ? user.getFullName() : name);
		if(user!=null)
			tagHTML+="</a>";
		else
			tagHTML+="</span>";
		tagHTML+="<a href=\"javascript:void(0)\" class=\"pvTagDelete\" data-tooltip=\""+l.get("photo_delete_tag")+"\" aria-label=\""+l.get("photo_delete_tag")
				+"\" data-confirm-message=\""+l.get("photo_tag_delete_confirm")+"\" data-confirm-title=\""+l.get("photo_delete_tag")+"\" data-confirm-action=\"/photos/"+photo.getIdString()+"/deleteTag?id="+tagID+"\"></a>";
		tagHTML+="</span>";
		return new WebDeltaResponse(resp)
				.show("pvTagsCont_"+photo.getIdString())
				.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_END, "pvTags_"+photo.getIdString(), tagHTML);
	}

	public static Object deleteTag(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req))
			throw new BadRequestException();
		requireQueryParams(req, "id");
		int tagID=safeParseInt(req.queryParams("id"));
		Photo photo=getPhotoForRequest(req);
		ctx.getPhotosController().deletePhotoTag(self.user, photo, tagID);
		return new WebDeltaResponse(resp).remove("pvTag_"+photo.getIdString()+"_"+tagID, "pvConfirmTag_"+photo.getIdString(), "photoNewTag"+photo.getIdString());
	}
	
	public static Object newTags(Request req, Response resp, Account self, ApplicationContext ctx){
		int offset=offset(req);
		PaginatedList<Photo> photos=ctx.getPhotosController().getUserUnapprovedTaggedPhotos(self.user, offset, 100);
		RenderedTemplateResponse model=new RenderedTemplateResponse("photo_new_tags", req)
				.with("owner", self.user)
				.pageTitle(lang(req).get("new_photos_of_me"))
				.paginate(photos);

		Map<Long, PhotoViewerInlineData> pvData=new HashMap<>();
		int i=0;
		for(Photo p:photos.list){
			pvData.put(p.id, new PhotoViewerInlineData(offset+i, "newTags/1", p.image.getURLsForPhotoViewer()));
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
		return model;
	}

	public static Object approveTag(Request req, Response resp, Account self, ApplicationContext ctx){
		Photo photo=getPhotoForRequest(req);
		List<PhotoTag> tags=ctx.getPhotosController().getTagsForPhoto(photo.id);
		PhotoTag myTag=null;
		for(PhotoTag tag:tags){
			if(tag.userID()==self.user.id){
				myTag=tag;
				break;
			}
		}
		if(myTag==null)
			throw new BadRequestException("You aren't tagged in this photo");
		if(!myTag.approved()){
			ctx.getPhotosController().approvePhotoTag(self.user, photo, myTag.id());
		}
		if(!isAjax(req)){
			resp.redirect(back(req));
			return "";
		}
		if(isMobile(req))
			return new WebDeltaResponse(resp).refresh();
		return new WebDeltaResponse(resp)
				.remove("pvConfirmTag_"+photo.getIdString(), "photoNewTag"+photo.getIdString());
	}
}
