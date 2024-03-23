package smithereen.util;

public interface TranslatableEnum<E extends Enum<E>>{
	String getLangKey();
}
