package smithereen.model.media;

import java.util.EnumSet;
import java.util.List;

public record PhotoViewerPhotoInfo(String id, String authorURL, String authorName, String albumID, String albumTitle, String html, EnumSet<AllowedAction> actions, List<SizedImageURLs> urls, Interactions interactions, String originalURL){
	public enum AllowedAction{
		// TODO
	}
	public record Interactions(int likes, boolean isLiked){}
}
