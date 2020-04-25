package smithereen.data.feed;

public abstract class NewsfeedEntry{
	public int id;
	public Type type;
	public int objectID;

	@Override
	public String toString(){
		return "NewsfeedEntry{"+
				"id="+id+
				", type="+type+
				", objectID="+objectID+
				'}';
	}

	public enum Type{
		POST,
		RETOOT
	}
}
