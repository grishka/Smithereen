package smithereen.data;

import java.net.URI;

public class NonCachedRemoteImage implements SizedImage{

	private final Args args;
	private final Dimensions origDimensions;

	public NonCachedRemoteImage(Args args, Dimensions origDimensions){
		this.args=args;
		this.origDimensions=origDimensions;
	}

	@Override
	public URI getUriForSizeAndFormat(Type size, Format format){
		UriBuilder builder=UriBuilder.local().path("system", "downloadExternalMedia");
		args.addToUriBuilder(builder);
		builder.queryParam("size", size.suffix()).queryParam("format", format.fileExtension());
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
}
