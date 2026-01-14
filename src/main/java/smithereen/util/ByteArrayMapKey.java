package smithereen.util;

import java.util.Arrays;
import java.util.Objects;

public record ByteArrayMapKey(byte[] key){
	@Override
	public int hashCode(){
		return Arrays.hashCode(key);
	}

	@Override
	public boolean equals(Object o){
		if(!(o instanceof ByteArrayMapKey(byte[] key1))) return false;
		return Objects.deepEquals(key, key1);
	}
}
