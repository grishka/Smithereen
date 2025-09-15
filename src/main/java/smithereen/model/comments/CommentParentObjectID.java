package smithereen.model.comments;

import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.Server;
import smithereen.util.XTEA;

public record CommentParentObjectID(CommentableObjectType type, long id){
	public String getHtmlElementID(){
		return switch(type){
			case PHOTO -> "photo_"+XTEA.encodeObjectID(id, ObfuscatedObjectIDType.PHOTO);
			case BOARD_TOPIC -> "topic_"+XTEA.encodeObjectID(id, ObfuscatedObjectIDType.BOARD_TOPIC);
		};
	}

	public Server.Feature getRqeuiredServerFeature(){
		return switch(type){
			case PHOTO -> Server.Feature.PHOTO_ALBUMS;
			case BOARD_TOPIC -> null;
		};
	}
}
