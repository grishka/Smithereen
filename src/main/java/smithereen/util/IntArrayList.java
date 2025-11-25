package smithereen.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

public class IntArrayList{
	private static final int INITIAL_CAPACITY=8;

	private int[] elements=new int[INITIAL_CAPACITY];
	private int count=0;

	public int size(){
		return count;
	}

	public void add(int value){
		if(elements.length<count+1){
			elements=Arrays.copyOf(elements, elements.length*2);
		}
		elements[count++]=value;
	}

	public int get(int index){
		if(index>=count || index<0)
			throw new ArrayIndexOutOfBoundsException();
		return elements[index];
	}

	public void set(int index, int value){
		if(index>=count || index<0)
			throw new ArrayIndexOutOfBoundsException();
		elements[index]=value;
	}

	public void remove(int index){
		if(index>=count || index<0)
			throw new ArrayIndexOutOfBoundsException();
		if(index==count-1){
			count--;
			return;
		}
		System.arraycopy(elements, index+1, elements, index, count-index);
		count--;
	}

	public void clear(){
		if(count>0){
			count=0;
			if(elements.length>INITIAL_CAPACITY)
				elements=new int[INITIAL_CAPACITY];
		}
	}

	public int indexOf(int value){
		for(int i=0;i<count;i++){
			if(elements[i]==value)
				return i;
		}
		return -1;
	}

	public int lastIndexOf(int value){
		for(int i=count-1;i>=0;i--){
			if(elements[i]==value)
				return i;
		}
		return -1;
	}

	public IntStream stream(){
		return Arrays.stream(elements, 0, count);
	}

	public int[] toArray(){
		return Arrays.copyOf(elements, count);
	}

	@Override
	public boolean equals(Object obj){
		return obj instanceof IntArrayList other && count==other.count && Arrays.equals(elements, 0, count, other.elements, 0, count);
	}
}
