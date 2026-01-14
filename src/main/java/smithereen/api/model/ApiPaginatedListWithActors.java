package smithereen.api.model;

import java.util.List;
import java.util.function.Function;

import smithereen.model.PaginatedList;

public class ApiPaginatedListWithActors<T> extends ApiPaginatedList<T>{
	public List<ApiUser> profiles;
	public List<ApiGroup> groups;

	public ApiPaginatedListWithActors(){
		super();
	}

	public ApiPaginatedListWithActors(long count, List<T> items){
		super(count, items);
	}

	public ApiPaginatedListWithActors(PaginatedList<T> list){
		super(list);
	}

	public <S> ApiPaginatedListWithActors(PaginatedList<S> list, Function<S, T> transformFn){
		super(list, transformFn);
	}
}
