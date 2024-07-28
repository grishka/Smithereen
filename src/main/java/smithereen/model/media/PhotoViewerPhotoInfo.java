package smithereen.model.media;

import java.util.EnumSet;
import java.util.List;

public record PhotoViewerPhotoInfo(String id, String authorURL, String authorName, String albumID, String albumTitle, String html, EnumSet<AllowedAction> actions, List<SizedImageURLs> urls){
	public enum AllowedAction{
		// TODO
	}
}
