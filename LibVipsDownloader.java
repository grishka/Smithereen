import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class LibVipsDownloader{
	private static final String LIBVIPS_VERSION="8.10.6";

	private static String unpad(String s){
		int i=s.indexOf(0);
		return i>=0 ? s.substring(0, i) : s;
	}

	private static boolean readFully(InputStream in, byte[] buf) throws IOException{
		int read;
		int totalRead=0;
		while((read=in.read(buf, totalRead, buf.length-totalRead))>0 && totalRead<buf.length){
			totalRead+=read;
		}
		return totalRead==buf.length;
	}

	public static void main(String[] args) throws Exception{
		String osProp=System.getProperty("os.name");
		String archProp=System.getProperty("os.arch");
		String os, arch;
		List<String> filesToExtract, targetNames;
		if(osProp.equals("Mac OS X")){
			os="darwin";
			filesToExtract=List.of("lib/libvips-cpp.42.dylib");
			targetNames=List.of("libvips-cpp.dylib");
		}else if(osProp.startsWith("Linux")){
			os="linux";
			filesToExtract=List.of("lib/libvips-cpp.so.42");
			targetNames=List.of("libvips-cpp.so");
		}else if(osProp.startsWith("Windows")){
			os="win32";
			filesToExtract=List.of("lib/libglib-2.0-0.dll", "lib/libgobject-2.0-0.dll", "lib/libvips-42.dll");
			targetNames=List.of("glib.dll", "gobject.dll", "vips-cpp.dll");
		}else{
			throw new IllegalStateException("There's no prebuilt libvips for your operating system ("+osProp+").");
		}

		arch=switch(archProp){
			case "x86" -> "ia32";
			case "amd64", "x86_64" -> "x64";
			case "aarch64" -> "arm64v8";
			case "arm" -> "armv7";
			default -> throw new IllegalStateException("There's no prebuilt libvips for your CPU architecture ("+archProp+").");
		};

		URL url=new URL("https://github.com/lovell/sharp-libvips/releases/download/v"+LIBVIPS_VERSION+"/libvips-"+LIBVIPS_VERSION+"-"+os+"-"+arch+".tar.gz");
		System.out.println("Downloading from: "+url);
		HttpURLConnection conn=(HttpURLConnection) url.openConnection();
		conn.connect();
		if(conn.getResponseCode()!=200){
			conn.disconnect();
			throw new IllegalStateException("There's no prebuilt libvips for this combination of OS and CPU architecture.");
		}
		int remainingFiles=filesToExtract.size();
		// https://en.wikipedia.org/wiki/Tar_(computing)#File_format
		try(GZIPInputStream in=new GZIPInputStream(conn.getInputStream())){
			byte[] buf=new byte[512];
			while(readFully(in, buf)){
				String name=unpad(new String(buf, 0, 100));
				if(name.isEmpty())
					break;
				int size=Integer.parseInt(new String(buf, 124, 11), 8);
				if(size>0){
					int blockCount=size/512+(size%512>0 ? 1 : 0);
					int sizeWithPadding=blockCount*512;
					int idx;
					if((idx=filesToExtract.indexOf(name))!=-1){
						String targetName=targetNames.get(idx);
						try(FileOutputStream out=new FileOutputStream(targetName)){
							System.out.print("Extracting '"+name+"' as '"+targetName+"'... ");
							for(int i=0;i<blockCount;i++){
								if(!readFully(in, buf))
									throw new IOException("Archive EOF before file EOF");
								out.write(buf, 0, Math.min(512, size));
								size-=512;
							}
							System.out.println("Done.");
							remainingFiles--;
							if(remainingFiles==0)
								break;
						}
					}else{
						while(sizeWithPadding>0){
							sizeWithPadding-=in.skip(sizeWithPadding);
						}
					}
				}
			}
		}finally{
			conn.disconnect();
		}
	}
}
