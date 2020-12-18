package smithereen.storage;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import smithereen.Config;
import smithereen.DisallowLocalhostInterceptor;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.LocalImage;
import smithereen.libvips.VImage;

public class MediaCache{

	private static MediaCache instance=new MediaCache();

	private LruCache<String, Item> metaCache=new LruCache<>(500);
	private MessageDigest md5;
	private ExecutorService asyncUpdater;
	private OkHttpClient httpClient;
	private long cacheSize=-1;
	private final Object cacheSizeLock=new Object();

	private static final int TYPE_PHOTO=0;

	public static MediaCache getInstance(){
		return instance;
	}

	private MediaCache(){
		try{
			md5=MessageDigest.getInstance("MD5");
		}catch(NoSuchAlgorithmException ignore){}
		asyncUpdater=Executors.newFixedThreadPool(1);
		httpClient=new OkHttpClient.Builder()
				.addNetworkInterceptor(new DisallowLocalhostInterceptor())
				.build();
		try{
			updateTotalSize();
		}catch(SQLException x){
			x.printStackTrace();
		}
	}

	private void updateTotalSize() throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		try(ResultSet res=conn.createStatement().executeQuery("SELECT sum(`size`) FROM `media_cache`")){
			res.first();
			synchronized(cacheSizeLock){
				cacheSize=res.getLong(1);
			}
		}
	}

	public Item get(URI uri) throws SQLException{
		byte[] key=keyForURI(uri);
		String keyHex=Utils.byteArrayToHexString(key);
		Item item=metaCache.get(keyHex);
		if(item!=null){
			asyncUpdater.submit(new LastAccessUpdater(key));
			return item;
		}
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT * FROM `media_cache` WHERE `url_hash`=?");
		stmt.setBytes(1, key);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				Item result=itemFromResultSet(res);
				metaCache.put(keyHex, result);
				asyncUpdater.submit(new LastAccessUpdater(key));
				return result;
			}
		}
		return null;
	}

	private Item itemFromResultSet(ResultSet res) throws SQLException{
		long size=res.getInt("size");
		byte[] info=res.getBytes("info");
		int type=res.getInt("type");
		String keyHex=Utils.byteArrayToHexString(res.getBytes("url_hash"));
		Item result;
		switch(type){
			case TYPE_PHOTO:
				result=new PhotoItem();
				break;
			default:
				return null;
		}
		try{
			result.deserialize(new DataInputStream(new ByteArrayInputStream(info)), keyHex);
		}catch(IOException x){
			return null;
		}
		result.totalSize=size;
		return result;
	}

	public Item downloadAndPut(URI uri, String mime, ItemType type) throws IOException, SQLException{
		byte[] key=keyForURI(uri);
		String keyHex=Utils.byteArrayToHexString(key);

		Request req=new Request.Builder()
				.url(uri.toString())
				.build();
		Call call=httpClient.newCall(req);
		Response resp=call.execute();
		if(!resp.isSuccessful()){
			resp.body().close();
			return null;
		}
		Item result=null;
		try(ResponseBody body=resp.body()){
			System.out.println(uri+" length: "+body.contentLength());
			if(body.contentLength()>Config.mediaCacheFileSizeLimit){
				throw new IOException("File too large");
			}
			File tmp=File.createTempFile(keyHex, null);
			InputStream in=body.byteStream();
			FileOutputStream out=new FileOutputStream(tmp);
			int read;
			byte[] buf=new byte[10240];
			while((read=in.read(buf))>0){
				out.write(buf, 0, read);
			}
			out.close();

			if(!Config.mediaCachePath.exists()){
				Config.mediaCachePath.mkdirs();
			}

			if(mime.startsWith("image/")){
				PhotoItem photo=new PhotoItem();
				result=photo;
				VImage img=null;
				try{
					img=new VImage(tmp.getAbsolutePath());
					//System.out.println(img.getWidth()+"x"+img.getHeight());
					if(img.hasAlpha()){
						VImage flat=img.flatten(255, 255, 255);
						img.release();
						img=flat;
					}
					int[] size={0,0};
					photo.totalSize=MediaStorageUtils.writeResizedWebpImage(img, 2560, 0, 90, keyHex, Config.mediaCachePath, size);
					photo.width=size[0];
					photo.height=size[1];
					photo.key=keyHex;
//					photo.totalSize=MediaStorageUtils.writeResizedImages(img, dimensions, heights, sizes, jpegQuality, webpQuality, keyHex, Config.mediaCachePath, Config.mediaCacheURLPath, photo.sizes);
				}catch(IOException x){
					throw new IOException(x);
				}finally{
					if(img!=null)
						img.release();
				}
			}
			tmp.delete();
		}
		//System.out.println("Total size: "+result.totalSize);
		metaCache.put(keyHex, result);

		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("INSERT INTO `media_cache` (`url_hash`, `size`, `info`, `type`) VALUES (?, ?, ?, ?)");
		stmt.setBytes(1, key);
		stmt.setInt(2, (int) result.totalSize);
		ByteArrayOutputStream buf=new ByteArrayOutputStream();
		result.serialize(new DataOutputStream(buf));
		stmt.setBytes(3, buf.toByteArray());
		stmt.setInt(4, result.getType());
		stmt.execute();

		if(cacheSize==-1)
			updateTotalSize();
		synchronized(cacheSizeLock){
			cacheSize+=result.totalSize;
			if(cacheSize>Config.mediaCacheMaxSize){
				asyncUpdater.submit(new OldFileDeleter());
			}
		}

		return result;
	}

	private byte[] keyForURI(URI uri){
		return md5.digest(uri.toString().getBytes(StandardCharsets.UTF_8));
	}

	public static void putDraftAttachment(@NotNull LocalImage img, int ownerID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("INSERT INTO `draft_attachments` (`id`, `owner_account_id`, `info`) VALUES (?, ?, ?)");
		stmt.setBytes(1, Utils.hexStringToByteArray(img.localID));
		stmt.setInt(2, ownerID);
		stmt.setString(3, MediaStorageUtils.serializeAttachment(img).toString());
		stmt.execute();
	}

	public static boolean deleteDraftAttachment(@NotNull String id, int ownerID) throws Exception{
		Connection conn=DatabaseConnectionManager.getConnection();

		PreparedStatement stmt=conn.prepareStatement("SELECT `info` FROM `draft_attachments` WHERE `id`=? AND `owner_account_id`=?");
		stmt.setBytes(1, Utils.hexStringToByteArray(id));
		stmt.setInt(2, ownerID);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				ActivityPubObject obj=ActivityPubObject.parse(new JSONObject(res.getString(1)), ParserContext.LOCAL);
				if(obj instanceof Document)
					MediaStorageUtils.deleteAttachmentFiles((Document)obj);
			}else{
				return false;
			}
		}

		stmt=conn.prepareStatement("DELETE FROM `draft_attachments` WHERE `id`=? AND `owner_account_id`=?");
		stmt.setBytes(1, Utils.hexStringToByteArray(id));
		stmt.setInt(2, ownerID);
		return stmt.executeUpdate()==1;
	}

	public static ActivityPubObject getAndDeleteDraftAttachment(@NotNull String id, int ownerID) throws Exception{
		Connection conn=DatabaseConnectionManager.getConnection();
		ActivityPubObject result;

		PreparedStatement stmt=conn.prepareStatement("SELECT `info` FROM `draft_attachments` WHERE `id`=? AND `owner_account_id`=?");
		stmt.setBytes(1, Utils.hexStringToByteArray(id));
		stmt.setInt(2, ownerID);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				result=ActivityPubObject.parse(new JSONObject(res.getString(1)), ParserContext.LOCAL);
			}else{
				return null;
			}
		}

		stmt=conn.prepareStatement("DELETE FROM `draft_attachments` WHERE `id`=? AND `owner_account_id`=?");
		stmt.setBytes(1, Utils.hexStringToByteArray(id));
		stmt.setInt(2, ownerID);
		stmt.execute();

		return result;
	}

	public enum ItemType{
		PHOTO,
		AVATAR
	}

	public static abstract class Item{
		public long totalSize=0;

		public abstract int getType();
		public abstract void serialize(DataOutputStream out) throws IOException;
		public abstract void deserialize(DataInputStream in, String key) throws IOException;
		protected abstract void deleteFiles();
	}

	public static class PhotoItem extends Item{
		public String src;
		public int width, height;
		public String key;

		@Override
		public int getType(){
			return TYPE_PHOTO;
		}

		@Override
		public void serialize(DataOutputStream out) throws IOException{
			out.writeInt(width);
			out.writeInt(height);
		}

		@Override
		public void deserialize(DataInputStream in, String key) throws IOException{
			this.key=key;
			src=Config.mediaCacheURLPath+key+".webp";
			width=in.readInt();
			height=in.readInt();
		}

		@Override
		protected void deleteFiles(){
			File file=new File(Config.mediaCachePath, key+".webp");
			if(!file.delete()){
				System.out.println("Failed to delete "+file.getAbsolutePath());
			}
		}
	}

	private static class LastAccessUpdater implements Runnable{
		private byte[] key;

		public LastAccessUpdater(byte[] key){
			this.key=key;
		}

		@Override
		public void run(){
			try{
				Connection conn=DatabaseConnectionManager.getConnection();
				PreparedStatement stmt=conn.prepareStatement("UPDATE `media_cache` SET `last_access`=CURRENT_TIMESTAMP() WHERE `url_hash`=?");
				stmt.setBytes(1, key);
				stmt.execute();
			}catch(SQLException x){
				x.printStackTrace();
			}
		}
	}

	private class OldFileDeleter implements Runnable{

		@Override
		public void run(){
			try{
				Connection conn=DatabaseConnectionManager.getConnection();
				long sizeNeeded;
				ArrayList<String> deletedKeys=new ArrayList<>();
				synchronized(cacheSizeLock){
					sizeNeeded=cacheSize-Config.mediaCacheMaxSize;
				}
				while(sizeNeeded>0){
					try(ResultSet res=conn.createStatement().executeQuery("SELECT * FROM `media_cache` ORDER BY `last_access` ASC LIMIT 100")){
						if(res.first()){
							do{
								Item item=itemFromResultSet(res);
								item.deleteFiles();
								sizeNeeded-=item.totalSize;
								synchronized(cacheSizeLock){
									cacheSize-=item.totalSize;
								}
								deletedKeys.add("0x"+Utils.byteArrayToHexString(res.getBytes(1)));
								if(sizeNeeded<=0)
									break;
							}while(res.next());
						}
					}
					System.out.println("Deleting from media cache: "+deletedKeys);
					if(!deletedKeys.isEmpty()){
						conn.createStatement().execute("DELETE FROM `media_cache` WHERE `url_hash` IN ("+String.join(",", deletedKeys)+")");
					}
				}
			}catch(SQLException x){
				x.printStackTrace();
			}
		}
	}
}
