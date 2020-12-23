package smithereen.data.attachments;

import smithereen.data.SizedImage;

public interface SizedAttachment{
	int getWidth();
	int getHeight();
	boolean isSizeKnown();
}
