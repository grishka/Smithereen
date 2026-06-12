package smithereen.storage.migrations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import smithereen.Config;
import smithereen.Utils;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.media.ImageMetadata;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.media.MediaFileType;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.XTEA;

class SchemaMigration39 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		migrateMediaFiles(conn);
	}

	private static void migrateMediaFiles(DatabaseConnection conn) throws SQLException{
		LOG.info("Started migrating user avatars");
		try(ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("users")
				.columns("id", "avatar")
				.where("domain='' AND avatar IS NOT NULL")
				.execute()){
			while(res.next()){
				int id=res.getInt(1);
				JsonObject avaObj=JsonParser.parseString(res.getString(2)).getAsJsonObject();
				if(!avaObj.has("_lid"))
					continue;
				long newID=migrateOneAvatar(conn, avaObj, id);
				if(newID==0)
					continue;
				new SQLQueryBuilder(conn)
						.insertInto("media_file_refs")
						.value("file_id", newID)
						.value("object_id", id)
						.value("object_type", MediaFileReferenceType.USER_AVATAR)
						.value("owner_user_id", id)
						.executeNoResult();
				new SQLQueryBuilder(conn)
						.update("users")
						.value("avatar", new JsonObjectBuilder().add("type", "_LocalImage").add("_fileID", newID).build().toString())
						.where("id=?", id)
						.executeNoResult();
			}
		}
		LOG.info("Started migrating group avatars");
		try(ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("groups")
				.columns("id", "avatar")
				.where("domain='' AND avatar IS NOT NULL")
				.execute()){
			while(res.next()){
				int id=res.getInt(1);
				JsonObject avaObj=JsonParser.parseString(res.getString(2)).getAsJsonObject();
				if(!avaObj.has("_lid"))
					continue;
				long newID=migrateOneAvatar(conn, avaObj, -id);
				if(newID==0)
					continue;
				new SQLQueryBuilder(conn)
						.insertInto("media_file_refs")
						.value("file_id", newID)
						.value("object_id", id)
						.value("object_type", MediaFileReferenceType.GROUP_AVATAR)
						.value("owner_group_id", id)
						.executeNoResult();
				new SQLQueryBuilder(conn)
						.update("groups")
						.value("avatar", new JsonObjectBuilder().add("type", "_LocalImage").add("_fileID", newID).build().toString())
						.where("id=?", id)
						.executeNoResult();
			}
		}
		LOG.info("Started migrating wall attachments");
		try(ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("wall_posts")
				.columns("id", "owner_user_id", "owner_group_id", "attachments")
				.where("ap_id IS NULL AND attachments IS NOT NULL")
				.execute()){
			while(res.next()){
				int id=res.getInt(1);
				int ownerID=res.getInt(2);
				if(res.wasNull())
					ownerID=-res.getInt(3);
				JsonElement _attachments=JsonParser.parseString(res.getString(4));
				List<JsonObject> attachments;
				if(_attachments instanceof JsonObject jo){
					attachments=List.of(jo);
				}else if(_attachments instanceof JsonArray ja){
					attachments=new ArrayList<>(ja.size());
					for(JsonElement el:ja)
						attachments.add(el.getAsJsonObject());
				}else{
					throw new IllegalStateException();
				}
				if(!attachments.getFirst().has("_lid"))
					continue;
				long[] attachmentIDs=migrateMediaAttachments(conn, attachments, ownerID);
				JsonArray newAttachments=new JsonArray();
				for(long attID:attachmentIDs){
					newAttachments.add(new JsonObjectBuilder()
							.add("type", "_LocalImage")
							.add("_fileID", attID)
							.build());
					if(attID==0)
						continue;
					new SQLQueryBuilder(conn)
							.insertInto("media_file_refs")
							.value("file_id", attID)
							.value("object_id", id)
							.value("object_type", MediaFileReferenceType.WALL_ATTACHMENT)
							.value(ownerID>0 ? "owner_user_id" : "owner_group_id", Math.abs(ownerID))
							.executeNoResult();
				}
				new SQLQueryBuilder(conn)
						.update("wall_posts")
						.value("attachments", (newAttachments.size()==1 ? newAttachments.get(0) : newAttachments).toString())
						.where("id=?", id)
						.executeNoResult();
			}
		}
		LOG.info("Started migrating mail attachments");
		try(ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("mail_messages")
				.columns("id", "owner_id", "attachments")
				.where("ap_id IS NULL AND attachments IS NOT NULL")
				.execute()){
			while(res.next()){
				long id=res.getInt(1);
				int ownerID=res.getInt(2);
				JsonElement _attachments=JsonParser.parseString(res.getString(3));
				List<JsonObject> attachments;
				if(_attachments instanceof JsonObject jo){
					attachments=List.of(jo);
				}else if(_attachments instanceof JsonArray ja){
					attachments=new ArrayList<>(ja.size());
					for(JsonElement el:ja)
						attachments.add(el.getAsJsonObject());
				}else{
					throw new IllegalStateException();
				}
				if(!attachments.getFirst().has("_lid"))
					continue;
				long[] attachmentIDs=migrateMediaAttachments(conn, attachments, ownerID);
				JsonArray newAttachments=new JsonArray();
				for(long attID:attachmentIDs){
					newAttachments.add(new JsonObjectBuilder()
							.add("type", "_LocalImage")
							.add("_fileID", attID)
							.build());
					if(attID==0)
						continue;
					new SQLQueryBuilder(conn)
							.insertInto("media_file_refs")
							.value("file_id", attID)
							.value("object_id", id)
							.value("object_type", MediaFileReferenceType.MAIL_ATTACHMENT)
							.value("owner_user_id", ownerID)
							.executeNoResult();
				}
				new SQLQueryBuilder(conn)
						.update("mail_messages")
						.value("attachments", (newAttachments.size()==1 ? newAttachments.get(0) : newAttachments).toString())
						.where("id=?", id)
						.executeNoResult();
			}
		}
		LOG.info("Media file migration done");
	}

	private static long migrateOneAvatar(DatabaseConnection conn, JsonObject avaObj, int id) throws SQLException{
		String fileID=avaObj.get("_lid").getAsString();
		String dirName=avaObj.has("_p") ? avaObj.get("_p").getAsString() : "avatars";

		File actualFile=new File(Config.uploadPath, dirName+"/"+fileID+".webp");
		if(!actualFile.exists()){
			LOG.debug("Skipping file {} because it does not exist on disk", actualFile.getAbsolutePath());
			return 0;
		}

		int width=avaObj.getAsJsonArray("_sz").get(0).getAsInt();
		int height=avaObj.getAsJsonArray("_sz").get(1).getAsInt();
		JsonArray _cropRegion=avaObj.getAsJsonArray("cropRegion");
		float[] cropRegion=new float[4];
		for(int i=0;i<4;i++)
			cropRegion[i]=_cropRegion.get(i).getAsFloat();
		ImageMetadata meta=new ImageMetadata(width, height, null, cropRegion);
		byte[] randomID=Utils.randomBytes(18);
		long newID=new SQLQueryBuilder(conn)
				.insertInto("media_files")
				.value("random_id", randomID)
				.value("size", actualFile.length())
				.value("type", MediaFileType.IMAGE_AVATAR)
				.value("metadata", Utils.gson.toJson(meta))
				.value("original_owner_id", id)
				.executeAndGetIDLong();
		int oid=Math.abs(id);
		File newFileDir=new File(Config.uploadPath, String.format(Locale.US, "%02d/%02d/%02d", oid%100, oid/100%100, oid/100_00%100));
		if(!newFileDir.exists() && !newFileDir.mkdirs())
			throw new RuntimeException("mkdirs failed");
		File newFile=new File(newFileDir, Base64.getUrlEncoder().withoutPadding().encodeToString(randomID)+"_"
				+Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.packLong(XTEA.obfuscateObjectID(newID, ObfuscatedObjectIDType.MEDIA_FILE)))+".webp");
		try{
			LOG.debug("Copying: {} -> {}", actualFile.getAbsolutePath(), newFile.getAbsolutePath());
			Files.copy(actualFile.toPath(), newFile.toPath());
		}catch(IOException x){
			throw new RuntimeException("failed to copy file", x);
		}
		return newID;
	}

	private static long[] migrateMediaAttachments(DatabaseConnection conn, List<JsonObject> attachments, int ownerID) throws SQLException{
		long[] ids=new long[attachments.size()];
		int i=0;
		for(JsonObject obj:attachments){
			String fileID=obj.get("_lid").getAsString();
			String dirName=obj.has("_p") ? obj.get("_p").getAsString() : "post_media";

			File actualFile=new File(Config.uploadPath, dirName+"/"+fileID+".webp");
			if(!actualFile.exists()){
				LOG.debug("Skipping file {} because it does not exist on disk", actualFile.getAbsolutePath());
				i++;
				continue;
			}
			int width=obj.getAsJsonArray("_sz").get(0).getAsInt();
			int height=obj.getAsJsonArray("_sz").get(1).getAsInt();
			String blurhash=obj.has("blurhash") ? obj.get("blurhash").getAsString() : null;
			ImageMetadata meta=new ImageMetadata(width, height, blurhash, null);
			byte[] randomID=Utils.randomBytes(18);
			boolean isGraffiti=obj.has("graffiti") && obj.get("graffiti").getAsBoolean();
			long newID=new SQLQueryBuilder(conn)
					.insertInto("media_files")
					.value("random_id", randomID)
					.value("size", actualFile.length())
					.value("type", isGraffiti ? MediaFileType.IMAGE_GRAFFITI : MediaFileType.IMAGE_PHOTO)
					.value("metadata", Utils.gson.toJson(meta))
					.value("original_owner_id", ownerID)
					.executeAndGetIDLong();
			int oid=Math.abs(ownerID);
			File newFileDir=new File(Config.uploadPath, String.format(Locale.US, "%02d/%02d/%02d", oid%100, oid/100%100, oid/100_00%100));
			if(!newFileDir.exists() && !newFileDir.mkdirs())
				throw new RuntimeException("mkdirs failed");
			File newFile=new File(newFileDir, Base64.getUrlEncoder().withoutPadding().encodeToString(randomID)+"_"
					+Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.packLong(XTEA.obfuscateObjectID(newID, ObfuscatedObjectIDType.MEDIA_FILE)))+".webp");
			try{
				LOG.debug("Copying: {} -> {}", actualFile.getAbsolutePath(), newFile.getAbsolutePath());
				Files.copy(actualFile.toPath(), newFile.toPath());
			}catch(IOException x){
				throw new RuntimeException("failed to copy file", x);
			}
			ids[i]=newID;
			i++;
		}

		return ids;
	}
}
