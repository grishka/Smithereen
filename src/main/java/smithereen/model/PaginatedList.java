package smithereen.model;

import java.util.Collections;
import java.util.List;

public class PaginatedList<T>{
	public List<T> list;
	public int total;

	public transient int offset;
	public transient int perPage;

	public PaginatedList(List<T> list, int total){
		this.list=list;
		this.total=total;
	}

	public PaginatedList(List<T> list, int total, int offset, int perPage){
		this.list=list;
		this.total=total;
		this.offset=offset;
		this.perPage=perPage;
	}

	public static <R> PaginatedList<R> emptyList(int perPage){
		return new PaginatedList<>(Collections.emptyList(), 0, 0, perPage);
	}
}
