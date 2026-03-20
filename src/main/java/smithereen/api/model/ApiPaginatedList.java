package smithereen.api.model;

import java.util.List;
import java.util.function.Function;

import smithereen.model.PaginatedList;

public class ApiPaginatedList<T>{
	public long count;
	public List<T> items;

	public ApiPaginatedList(){}

	public ApiPaginatedList(long count, List<T> items){
		this.count=count;
		this.items=items;
	}

	public ApiPaginatedList(PaginatedList<T> list){
		count=list.total;
		items=list.list;
	}

	public <S> ApiPaginatedList(PaginatedList<S> list, Function<S, T> transformFn){
		count=list.total;
		items=list.list.stream().map(transformFn).toList();
	}
}
