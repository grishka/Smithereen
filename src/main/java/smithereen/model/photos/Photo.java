package smithereen.model.photos;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Utils;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.SizedImage;
import smithereen.storage.DatabaseUtils;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public class Photo{
	public long id;
	public int ownerID;
	public int authorID;
	public long albumID;
	public long localFileID;
	public URI remoteSrc;
	public String description;
	public Instant createdAt;
	public PhotoMetadata metadata;
	public URI apID;

	public SizedImage image;

	public static Photo fromResultSet(ResultSet res) throws SQLException{
		Photo p=new Photo();
		p.id=res.getLong("id");
		p.ownerID=res.getInt("owner_id");
		p.authorID=res.getInt("author_id");
		p.albumID=res.getLong("album_id");
		p.localFileID=res.getLong("local_file_id");
		String remoteSrc=res.getString("remote_src");
		if(StringUtils.isNotEmpty(remoteSrc))
			p.remoteSrc=URI.create(remoteSrc);
		p.description=res.getString("description");
		p.createdAt=DatabaseUtils.getInstant(res, "created_at");
		String meta=res.getString("metadata");
		if(meta!=null)
			p.metadata=Utils.gson.fromJson(meta, PhotoMetadata.class);
		String apID=res.getString("ap_id");
		if(apID!=null)
			p.apID=URI.create(apID);
		return p;
	}

	public String getURL(){
		return "/photos/"+Utils.encodeLong(XTEA.obfuscateObjectID(id, ObfuscatedObjectIDType.PHOTO));
	}

	public String getIdString(){
		return Utils.encodeLong(XTEA.obfuscateObjectID(id, ObfuscatedObjectIDType.PHOTO));
	}
}
