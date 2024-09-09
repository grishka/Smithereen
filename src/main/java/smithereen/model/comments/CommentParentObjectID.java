package smithereen.model.comments;

import smithereen.model.ObfuscatedObjectIDType;
import smithereen.util.XTEA;

public record CommentParentObjectID(CommentableObjectType type, long id){
	public String getHtmlElementID(){
		return switch(type){
			case PHOTO -> "photo_"+XTEA.encodeObjectID(id, ObfuscatedObjectIDType.PHOTO);
		};
	}
}
