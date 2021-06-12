package smithereen.data;

import java.util.List;

public class ListAndTotal<T>{
	public List<T> list;
	public int total;

	public ListAndTotal(List<T> list, int total){
		this.list=list;
		this.total=total;
	}
}
