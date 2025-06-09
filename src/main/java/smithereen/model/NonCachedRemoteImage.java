package smithereen.model;

import java.net.URI;

import smithereen.Utils;
import smithereen.model.photos.Photo;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;

public final class NonCachedRemoteImage extends RemoteImage{

	private final Args args;
	private final Dimensions origDimensions;

	public NonCachedRemoteImage(Args args, Dimensions origDimensions, URI originalURL){
		super(originalURL);
		this.args=args;
		this.origDimensions=origDimensions;
	}

	@Override
	public URI getUriForSizeAndFormat(Type size, Format format, boolean is2x, boolean useFallback){
		UriBuilder builder=UriBuilder.local().path("system", "downloadExternalMedia");
		args.addToUriBuilder(builder);
		builder.queryParam("size", size.suffix()).queryParam("format", format.fileExtension());
		if(is2x)
			builder.queryParam("2x", "");
		if(useFallback)
			builder.queryParam("fb", "");
		return builder.build();
	}

	@Override
	public Dimensions getOriginalDimensions(){
		return origDimensions;
	}

	public static abstract class Args{
		protected abstract void addToUriBuilder(UriBuilder builder);
	}

	public static class UserProfilePictureArgs extends Args{

		private final int userID;

		public UserProfilePictureArgs(int userID){
			this.userID=userID;
		}

		@Override
		protected void addToUriBuilder(UriBuilder builder){
			builder.queryParam("type", "user_ava");
			builder.queryParam("user_id", userID+"");
		}
	}

	public static class GroupProfilePictureArgs extends Args{

		private final int groupID;

		public GroupProfilePictureArgs(int groupID){
			this.groupID=groupID;
		}

		@Override
		protected void addToUriBuilder(UriBuilder builder){
			builder.queryParam("type", "group_ava");
			builder.queryParam("group_id", groupID+"");
		}
	}

	public static class PostPhotoArgs extends Args{

		private final int postID, index;

		public PostPhotoArgs(int postID, int index){
			this.postID=postID;
			this.index=index;
		}

		@Override
		protected void addToUriBuilder(UriBuilder builder){
			builder.queryParam("type", "post_photo");
			builder.queryParam("post_id", postID+"");
			builder.queryParam("index", index+"");
		}
	}

	public static class MessagePhotoArgs extends Args{

		private final int index;
		private final long messageID;

		public MessagePhotoArgs(long messageID, int index){
			this.messageID=messageID;
			this.index=index;
		}

		@Override
		protected void addToUriBuilder(UriBuilder builder){
			builder.queryParam("type", "message_photo");
			builder.queryParam("msg_id", Utils.encodeLong(messageID));
			builder.queryParam("index", index+"");
		}
	}

	public static class AlbumPhotoArgs extends Args{
		private final Photo photo;

		public AlbumPhotoArgs(Photo photo){
			this.photo=photo;
		}

		@Override
		protected void addToUriBuilder(UriBuilder builder){
			builder.queryParam("type", "album_photo");
			builder.queryParam("photo_id", Utils.encodeLong(XTEA.obfuscateObjectID(photo.id, ObfuscatedObjectIDType.PHOTO)));
		}
	}
}
