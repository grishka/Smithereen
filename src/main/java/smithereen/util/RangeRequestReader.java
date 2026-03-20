package smithereen.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import smithereen.activitypub.ActivityPub;

public class RangeRequestReader implements RandomAccessByteInput{
	@NotNull
	private final URI url;
	private long size = -1;

	public RangeRequestReader(@NotNull URI url){
		this.url=url;
	}

	@Override
	public long getSize(){
		return size;
	}

	public boolean supportsRangeRequests() throws IOException, InterruptedException{
		HttpRequest req=HttpRequest
				.newBuilder(url)
				.HEAD()
				.timeout(Duration.ofSeconds(30))
				.build();
		HttpResponse<Void> resp=ActivityPub.httpClient.send(req, HttpResponse.BodyHandlers.discarding());
		if(resp.statusCode()/100!=2)
			throw new IOException("Response not successful: "+resp.statusCode());
		size = resp.headers().firstValueAsLong("Content-Length").orElse(-1);
		return resp.headers().firstValue("Accept-Ranges").orElse("none").equals("bytes");
	}

	@Override
	public byte[] read(int length, int position) throws IOException, InterruptedException{
		HttpRequest req=HttpRequest
				.newBuilder(url)
				.timeout(Duration.ofSeconds(30))
				.header("Range", "bytes=%d-%d".formatted(position, position+length-1))
				.build();
		HttpResponse<byte[]> resp=ActivityPub.httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
		if(resp.statusCode()!=206){
			throw new IOException("Expected status code '206 Partial Content', got "+resp.statusCode());
		}
		return resp.body();
	}
}
