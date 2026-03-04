package smithereen.id3tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import smithereen.activitypub.objects.Audio;
import smithereen.util.RandomAccessByteInput;

/**
 * This class has been adapted from the <a href="https://github.com/43081j/id3">id3 TypeScript library</a>.
 * We only used parts of the implementation that are relevant for Smithereen, and also supported unsynchronization,
 * which wasn't supported in the original library.
 *
 * <p>
 * Here is the copy of the library's license:
 *
 * <pre>
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 43081j
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * </pre>
 * </p>
 */
public class ID3v2TagParser{
	@NotNull
	RandomAccessByteInput reader;

	public ID3v2TagParser(@NotNull RandomAccessByteInput reader){
		this.reader=reader;
	}

	public void parseInto(@NotNull Audio target) throws IOException, InterruptedException{
		long size=reader.getSize();
		if(size<=0){
			throw new IOException("Invalid size of audio file");
		}

		// Opportunistically request the first 10 KiB of the MP3 file.
		// Hopefully, all the necessary frames will be present in those first 10 KiB.
		// If not, we won't bother.
		ByteBuffer requestedBytes=ByteBuffer.wrap(reader.read(10240, 0));

		// Be sure that the buffer is at least the size of an id3v2 header.
		if(requestedBytes.remaining()<14){
			return;
		}
		ByteBuffer v2prefix=requestedBytes.slice(0, 14);
		int majorVersion=v2prefix.get(3);

		if(!getStringUtf8(v2prefix, 3).equals("ID3") || majorVersion<2 || majorVersion>4){
			return;
		}

		int headerSize=10;
		int tagSize=0;
		int tagFlags=v2prefix.get(5);

		boolean unsync=(tagFlags & 0x80)!=0;

		if(v2prefix.remaining()<headerSize){
			return;
		}
		// Calculate the tag size to be read
		tagSize+=getSynchSafeInt(v2prefix, 6);

		// Increment the header size to offset by if an extended header exists
		if((tagFlags & 0x40)!=0){
			if(v2prefix.remaining()<15){
				return;
			}
			headerSize+=getSynchSafeInt(v2prefix, 11);
		}

		if(headerSize>=requestedBytes.remaining()){
			// That's some enormous header. Don't bother.
			return;
		}

		ByteBuffer v2Tag=requestedBytes.slice(headerSize, Math.min(tagSize, requestedBytes.remaining()-headerSize));
		int pos=0;

		while(pos<v2Tag.remaining()){
			for(int i=0;i<3;++i){
				if(pos+i>=v2Tag.remaining()){
					return;
				}
				byte frameBit=v2Tag.get(pos+i);
				// frameBit must be A-Z or 0-9
				if((frameBit<0x41 || frameBit>0x5a) && (frameBit<0x30 || frameBit>0x39)){
					// This is not a frame, abort
					return;
				}
			}

			// < v2.3, frame ID is 3 chars, size is 3 bytes making a total size of 6 bytes.
			// >= v2.3, frame ID is 4 chars, size is 4 bytes, flags are 2 bytes, total 10 bytes.
			int frameHeaderSize;
			int payloadSize;
			if(majorVersion<3){
				frameHeaderSize=6;
				if(pos+frameHeaderSize>=v2Tag.remaining()){
					return;
				}
				payloadSize=getUInt24(v2Tag, pos+3);
			}else if(majorVersion==3){
				frameHeaderSize=10;
				if(pos+frameHeaderSize>=v2Tag.remaining()){
					return;
				}
				payloadSize=v2Tag.getInt(pos+4);
			}else{
				frameHeaderSize=10;
				if(pos+frameHeaderSize>=v2Tag.remaining()){
					return;
				}
				payloadSize=getSynchSafeInt(v2Tag, pos+4);
			}
			int frameSize=frameHeaderSize+payloadSize;
			if(pos+frameSize<0 || frameSize<=0 || pos+frameSize>=v2Tag.remaining()){
				return;
			}
			ByteBuffer slice=v2Tag.slice(pos, frameSize);
			parseFrameInto(target, slice, unsync, majorVersion<3);

			pos+=frameSize;
		}
	}

	private static void parseFrameInto(Audio target, ByteBuffer buf, boolean unsync, boolean legacy){
		// Read the header
		String frameID=getStringUtf8(buf, legacy ? 3 : 4);

		if(!legacy){
			if(buf.remaining()<10){
				return;
			}
			short flags=buf.getShort(8);

			unsync=unsync || (flags & 0x02)!=0;

			boolean encrypted=(flags & 0x04)!=0;
			boolean compressed=(flags & 0x08)!=0;
			if(compressed || encrypted){
				// No support for encrypted or compressed frames.
				return;
			}
		}

		switch(frameID){
			case "TLEN", "TLE" -> {
				if(target.duration!=0) break;
				String duration=parseFrameValue(buf, unsync, legacy);
				if(duration==null) break;
				try{
					target.duration=Long.parseLong(duration);
				}catch(NumberFormatException ignored){
				}
			}
			case "TIT2", "TT2" -> {
				if(target.title==null){
					target.title=parseFrameValue(buf, unsync, legacy);
				}
			}
			case "TPE1", "TP1" -> {
				if(target.artist==null){
					target.artist=parseFrameValue(buf, unsync, legacy);
				}
			}
			default -> {
			}
		}
	}

	private static int getUInt24(@NotNull ByteBuffer buf, int offset){
		return (Byte.toUnsignedInt(buf.get(offset)) << 16) | (Byte.toUnsignedInt(buf.get(offset+1)) << 8) | (Byte.toUnsignedInt(buf.get(offset+2)));
	}

	private static @NotNull String getStringUtf8(@NotNull ByteBuffer buf, int length){
		ByteBuffer sliced=buf.slice(0, Math.min(length, buf.remaining()));
		return StandardCharsets.UTF_8.decode(sliced).toString();
	}

	private static @Nullable String parseFrameValue(@NotNull ByteBuffer buf, boolean unsync, boolean legacy){
		int payloadStart=legacy ? 6 : 10;
		if(buf.remaining()<=payloadStart){
			return null;
		}
		if(unsync){
			// https://github.com/id3/ID3v2.4/blob/516075e38ff648a6390e48aff490abed987d3199/id3v2.40-structure.txt#L564
			ByteBuffer processed=ByteBuffer.allocate(buf.remaining()-payloadStart);
			int processedPos=0;
			byte prev=0;
			for(int i=payloadStart;i<buf.remaining();i++){
				byte cur=buf.get(i);
				// Skip a zero byte if it follows a 0xFF byte.
				if(!(prev==(byte) 0xFF && cur==0x00)){
					processed.put(processedPos++, cur);
				}
				prev=cur;
			}
			buf=processed;
			payloadStart=0;
		}

		if(payloadStart>=buf.remaining()){
			return null;
		}

		byte encoding=buf.get(payloadStart++);

		// https://github.com/id3/ID3v2.4/blob/516075e38ff648a6390e48aff490abed987d3199/id3v2.40-structure.txt#L360-L369
		Charset charset;
		int length;
		switch(encoding){
			case 0 -> {
				length=utf8StringLength(buf, payloadStart);
				charset=StandardCharsets.ISO_8859_1;
			}
			case 1 -> {
				length=utf16StringLength(buf, payloadStart);
				charset=StandardCharsets.UTF_16;
			}
			case 2 -> {
				length=utf16StringLength(buf, payloadStart);
				charset=StandardCharsets.UTF_16BE;
			}
			case 3 -> {
				length=utf8StringLength(buf, payloadStart);
				charset=StandardCharsets.UTF_8;
			}
			default -> {
				return null;
			}
		}
		return charset.decode(buf.slice(payloadStart, length)).toString();
	}

	private static int utf8StringLength(@NotNull ByteBuffer buf, int offset){
		for(int i=offset;i<buf.remaining();++i){
			if(buf.get(i)==0){
				return i-offset;
			}
		}
		return buf.remaining()-offset;
	}

	private static int utf16StringLength(@NotNull ByteBuffer buf, int offset){
		for(int i=offset;i<buf.remaining()-1;i+=2){
			if(buf.getShort(i)==0){
				return i-offset;
			}
		}
		return buf.remaining()-offset;
	}

	private static int getSynchSafeInt(@NotNull ByteBuffer buf, int offset){
		// https://github.com/id3/ID3v2.4/blob/516075e38ff648a6390e48aff490abed987d3199/id3v2.40-structure.txt#L610
		int num=buf.getInt(offset);
		int out=0;
		int mask=0b01111111_00000000_00000000_00000000;
		while(mask!=0){
			out >>>= 1;
			out|=num & mask;
			mask >>= 8;
		}
		return out;
	}
}
