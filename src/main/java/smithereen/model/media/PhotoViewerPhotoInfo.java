package smithereen.model.media;

import java.util.EnumSet;
import java.util.List;

public record PhotoViewerPhotoInfo(String id, String authorURL, String authorName, String albumID, String albumTitle,
								   String html, String topHTML, EnumSet<AllowedAction> actions, List<SizedImageURLs> urls, Interactions interactions, String originalURL,
								   String historyURL, String apURL, String saveURL){
	public enum AllowedAction{
		DELETE,
		EDIT_DESCRIPTION,
		SET_AS_COVER,
		REPORT,
		SAVE_TO_ALBUM,
		ROTATE,
		ADD_TAGS,
		MANAGE_TAGS
	}
	public record Interactions(int likes, boolean isLiked, int comments){}
}
