package smithereen.util;

import org.jetbrains.annotations.NotNull;

public record CharacterRange(int start, int end) implements Comparable<CharacterRange>{
	public boolean contains(int pos){
		return pos>=start && pos<end;
	}

	public boolean intersects(CharacterRange another){
		return contains(another.start) || contains(another.end) || another.contains(start) || another.contains(end);
	}

	@Override
	public int compareTo(@NotNull CharacterRange o){
		return Integer.compare(start, o.start);
	}
}
