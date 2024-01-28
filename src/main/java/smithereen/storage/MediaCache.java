package smithereen.storage;

import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.util.DisallowLocalhostInterceptor;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Document;
import smithereen.activitypub.objects.LocalImage;
import smithereen.libvips.VipsImage;
import smithereen.util.UserAgentInterceptor;

public class MediaCache{
	private static final Logger LOG=LoggerFactory.getLogger(MediaCache.class);
	private static MediaCache instance=new MediaCache();

	private LruCache<String, Item> metaCache=new LruCache<>(500);
	private ExecutorService asyncUpdater;
	private OkHttpClient httpClient;
	private long cacheSize=-1;
	private final Object cacheSizeLock=new Object();

	private static final int TYPE_PHOTO=0;

	public static MediaCache getInstance(){
		return instance;
	}

	private MediaCache(){
		asyncUpdater=Executors.newFixedThreadPool(1);
		httpClient=new OkHttpClient.Builder()
				.addNetworkInterceptor(new DisallowLocalhostInterceptor())
				.addNetworkInterceptor(new UserAgentInterceptor())
				.build();
		try{
			updateTotalSize();
		}catch(SQLException x){
			LOG.warn("Exception while updating total size", x);
		}
	}

	private void updateTotalSize() throws SQLException{
		synchronized(cacheSizeLock){
			cacheSize=new SQLQueryBuilder()
					.selectFrom("media_cache")
					.selectExpr("sum(`size`)")
					.executeAndGetInt();
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
		Item result=new SQLQueryBuilder()
				.selectFrom("media_cache")
				.where("url_hash=?", (Object) key)
				.executeAndGetSingleObject(this::itemFromResultSet);
		if(result!=null){
			metaCache.put(keyHex, result);
			asyncUpdater.submit(new LastAccessUpdater(key));
		}
		return result;
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

	public Item downloadAndPut(URI uri, String mime, ItemType type, boolean lossless, int enforcedWidth, int enforcedHeight) throws IOException, SQLException{
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
				VipsImage img=null;
				try{
					img=new VipsImage(tmp.getAbsolutePath());
					if((enforcedWidth!=0 && img.getWidth()!=enforcedWidth) || (enforcedHeight!=0 && img.getHeight()!=enforcedHeight))
						throw new IllegalArgumentException("Invalid image size");
					//System.out.println(img.getWidth()+"x"+img.getHeight());
					if(img.hasAlpha()){
						VipsImage flat=img.flatten(255, 255, 255);
						img.release();
						img=flat;
					}
					int[] size={0,0};
					File destination=new File(Config.mediaCachePath, keyHex+".webp");
					photo.totalSize=MediaStorageUtils.writeResizedWebpImage(img, 2560, 0, lossless ? MediaStorageUtils.QUALITY_LOSSLESS : 93, destination, size);
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

		ByteArrayOutputStream buf=new ByteArrayOutputStream();
		result.serialize(new DataOutputStream(buf));
		new SQLQueryBuilder()
				.insertInto("media_cache")
				.value("url_hash", key)
				.value("size", result.totalSize)
				.value("info", buf.toByteArray())
				.value("type", result.getType())
				.executeNoResult();

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
		try{
			return MessageDigest.getInstance("MD5").digest(uri.toString().getBytes(StandardCharsets.UTF_8));
		}catch(NoSuchAlgorithmException x){
			throw new RuntimeException(x);
		}
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
			width=in.readInt();
			height=in.readInt();
		}

		@Override
		protected void deleteFiles(){
			File file=new File(Config.mediaCachePath, key+".webp");
			if(!file.delete()){
				LOG.warn("Failed to delete {}", file.getAbsolutePath());
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
				new SQLQueryBuilder()
						.update("media_cache")
						.valueExpr("last_access", "CURRENT_TIMESTAMP()")
						.where("url_hash=?", (Object) key)
						.executeNoResult();
			}catch(SQLException x){
				LOG.warn("Exception while updating last access time", x);
			}
		}
	}

	private class OldFileDeleter implements Runnable{

		@Override
		public void run(){
			try{
				try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
					long sizeNeeded;
					ArrayList<String> deletedKeys=new ArrayList<>();
					synchronized(cacheSizeLock){
						sizeNeeded=cacheSize-Config.mediaCacheMaxSize;
					}
					while(sizeNeeded>0){
						try(ResultSet res=conn.createStatement().executeQuery("SELECT * FROM `media_cache` ORDER BY `last_access` ASC LIMIT 100")){
							while(res.next()){
								Item item=itemFromResultSet(res);
								item.deleteFiles();
								sizeNeeded-=item.totalSize;
								synchronized(cacheSizeLock){
									 cacheSize-=item.totalSize;
								}
								deletedKeys.add("0x"+Utils.byteArrayToHexString(res.getBytes(1)));
								if(sizeNeeded<=0)
									 break;
							}
						}
						LOG.info("Deleting from media cache: {}", deletedKeys);
						if(!deletedKeys.isEmpty()){
							conn.createStatement().execute("DELETE FROM `media_cache` WHERE `url_hash` IN ("+String.join(",", deletedKeys)+")");
						}
					}
				}
			}catch(SQLException x){
				LOG.warn("Exception while deleting from media cache", x);
			}
		}
	}
}
