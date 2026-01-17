package smithereen.model;

import org.jetbrains.annotations.Nullable;

public interface BlurHashable{
	@Nullable
	String getBlurHash();

	void setBlurHash(@Nullable String blurHash);
}
