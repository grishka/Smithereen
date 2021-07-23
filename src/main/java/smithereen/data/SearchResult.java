package smithereen.data;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class SearchResult{
	public User user;
	public Group group;
	@NotNull
	public Type type;
	public int id;
	public URI url;

	public SearchResult(@NotNull Type type, int id){
		this.type=type;
		this.id=id;
	}

	public enum Type{
		USER,
		GROUP,
		URL
	}
}
