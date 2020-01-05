package smithereen.data.feed;

public abstract class NewsfeedEntry{
	public static final int TYPE_POST=1;
	public static final int TYPE_RETOOT=2;

	public int type;
	public int objectID;
}
