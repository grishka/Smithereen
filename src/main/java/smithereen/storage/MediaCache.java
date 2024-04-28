package smithereen.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.libvips.VipsImage;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;

public class MediaCache{
	private static final Logger LOG=LoggerFactory.getLogger(MediaCache.class);
	private static MediaCache instance=new MediaCache();

	private LruCache<byte[], Item> metaCache=new LruCache<>(500);
	private final ScheduledExecutorService asyncUpdater;
	private long cacheSize=-1;
	private final Object cacheSizeLock=new Object();
	private Set<byte[]> pendingLastAccessUpdates=new HashSet<>();
	private ScheduledFuture<?> pendingLastAccessUpdateAction;
	private final Object lastAccessQueueLock=new Object();

	private static final int TYPE_PHOTO=0;

	public static MediaCache getInstance(){
		return instance;
	}

	private MediaCache(){
		asyncUpdater=Executors.newSingleThreadScheduledExecutor();
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
		Item item;
		synchronized(this){
			item=metaCache.get(key);
		}
		if(item!=null){
			updateLastAccess(key);
			return item;
		}
		Item result=new SQLQueryBuilder()
				.selectFrom("media_cache")
				.where("url_hash=?", (Object) key)
				.executeAndGetSingleObject(this::itemFromResultSet);
		if(result!=null){
			synchronized(this){
				metaCache.put(key, result);
			}
			updateLastAccess(key);
		}
		return result;
	}

	private void updateLastAccess(byte[] key){
		synchronized(lastAccessQueueLock){
			if(pendingLastAccessUpdates.isEmpty()){
				pendingLastAccessUpdateAction=asyncUpdater.schedule(new LastAccessUpdater(), 10, TimeUnit.SECONDS);
			}
			pendingLastAccessUpdates.add(key);
		}
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

		HttpRequest req=HttpRequest.newBuilder(uri).build();
		Item result=null;
		File tmp=File.createTempFile(keyHex, null);
		try{
			HttpResponse<Path> resp=ActivityPub.httpClient.send(req, responseInfo->{
				if(responseInfo.headers().firstValueAsLong("content-length").orElse(Long.MAX_VALUE)>Config.mediaCacheFileSizeLimit)
					return new HttpResponse.BodySubscriber<>(){
						@Override
						public CompletionStage<Path> getBody(){
							return CompletableFuture.failedStage(new IOException("File too large"));
						}

						@Override
						public void onSubscribe(Flow.Subscription subscription){}

						@Override
						public void onNext(List<ByteBuffer> item){}

						@Override
						public void onError(Throwable throwable){}

						@Override
						public void onComplete(){}
					};
				return HttpResponse.BodySubscribers.ofFile(tmp.toPath());
			});
			if(resp.statusCode()/100!=2){
				return null;
			}

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
					if(img.hasAlpha()){
						VipsImage flat=img.flatten(255, 255, 255);
						img.release();
						img=flat;
					}
					int[] size={0, 0};
					File destination=new File(Config.mediaCachePath, keyHex+".webp");
					photo.totalSize=MediaStorageUtils.writeResizedWebpImage(img, 2560, 0, lossless ? MediaStorageUtils.QUALITY_LOSSLESS : 93, destination, size);
					photo.width=size[0];
					photo.height=size[1];
					photo.key=keyHex;
				}catch(IOException x){
					throw new IOException(x);
				}finally{
					if(img!=null)
						img.release();
				}
			}
		}catch(InterruptedException ignored){
		}finally{
			if(tmp.exists())
				tmp.delete();
		}
		metaCache.put(key, result);

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
				synchronized(lastAccessQueueLock){
					// Perform pending access time updates right now, if any, to accidentally deleting recently accessed files
					if(pendingLastAccessUpdateAction!=null){
						pendingLastAccessUpdateAction.cancel(false);
						pendingLastAccessUpdateAction=null;
						asyncUpdater.submit(new LastAccessUpdater());
					}
					asyncUpdater.submit(new OldFileDeleter());
				}
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

	private class LastAccessUpdater implements Runnable{

		@Override
		public void run(){
			Set<byte[]> keysToUpdate;
			synchronized(lastAccessQueueLock){
				pendingLastAccessUpdateAction=null;
				keysToUpdate=pendingLastAccessUpdates;
				pendingLastAccessUpdates=new HashSet<>();
			}
			try{
				new SQLQueryBuilder()
						.update("media_cache")
						.valueExpr("last_access", "CURRENT_TIMESTAMP()")
						.whereIn("url_hash", keysToUpdate)
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
						LOG.debug("Deleting from media cache: {}", deletedKeys);
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
