package smithereen.util;

import org.jetbrains.annotations.NotNull;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smithereen.Utils;

public record InetAddressRange(InetAddress address, int prefixLength){
	private static final Pattern IP_WITH_SUBNET_PATTERN=Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|(?:[\\da-f]{0,4}:){1,7}[\\da-f]{0,4})(?:/(\\d{1,3}))?$", Pattern.CASE_INSENSITIVE);

	public byte[] getRawPrefix(boolean wholeBytesOnly){
		byte[] serialized=Utils.serializeInetAddress(address);
		if(isSingleAddress())
			return serialized;
		int prefixLengthInBytes=prefixLength >> 3;
		if(!wholeBytesOnly && prefixLength%8!=0){
			prefixLengthInBytes++;
		}
		if(address instanceof Inet4Address){
			prefixLengthInBytes+=12;
		}
		byte[] prefix=new byte[prefixLengthInBytes];
		System.arraycopy(serialized, 0, prefix, 0, prefix.length);
		return prefix;
	}

	public InetAddress getMinAddress(){
		if(isSingleAddress())
			return address;
		byte[] addr=address.getAddress();
		byte[] mask=getSubnetMask();
		for(int i=0;i<addr.length;i++){
			addr[i]&=mask[i];
		}
		try{
			return InetAddress.getByAddress(addr);
		}catch(UnknownHostException x){
			throw new RuntimeException(x);
		}
	}

	public InetAddress getMaxAddress(){
		if(isSingleAddress())
			return address;
		byte[] addr=address.getAddress();
		byte[] mask=getSubnetMask();
		for(int i=0;i<addr.length;i++){
			addr[i]|=(byte) ~mask[i];
		}
		try{
			return InetAddress.getByAddress(addr);
		}catch(UnknownHostException x){
			throw new RuntimeException(x);
		}
	}

	public byte[] getSubnetMask(){
		byte[] mask;
		if(address instanceof Inet4Address){
			mask=new byte[4];
		}else if(address instanceof Inet6Address){
			mask=new byte[16];
		}else{
			throw new IllegalStateException();
		}
		int prefixLengthInBytes=prefixLength >> 3;
		for(int i=0;i<prefixLengthInBytes;i++){
			mask[i]=-1;
		}
		if(prefixLength%8>0){
			mask[prefixLengthInBytes]=(byte)(0xFF << (8-prefixLength%8));
		}
		return mask;
	}

	public boolean isSingleAddress(){
		return (address instanceof Inet4Address && prefixLength==32) || (address instanceof Inet6Address && prefixLength==128);
	}

	public boolean contains(InetAddress addr){
		if((addr instanceof Inet4Address && address instanceof Inet6Address) || (addr instanceof Inet6Address && address instanceof Inet4Address))
			return false;
		if(isSingleAddress())
			return addr.equals(address);
		byte[] mask=getSubnetMask();
		byte[] raw=addr.getAddress();
		byte[] rawMasked=new byte[mask.length];
		for(int i=0;i<mask.length;i++){
			rawMasked[i]=(byte) (raw[i] & mask[i]);
		}
		try{
			return InetAddress.getByAddress(rawMasked).equals(getMinAddress());
		}catch(UnknownHostException x){
			throw new RuntimeException(x);
		}
	}

	@Override
	public String toString(){
		String addrStr=address.getHostAddress();
		if((address instanceof Inet4Address && prefixLength<32) || (address instanceof Inet6Address && prefixLength<128))
			addrStr+="/"+prefixLength;
		return addrStr;
	}

	public static InetAddressRange parse(@NotNull String subnet){
		Matcher matcher=IP_WITH_SUBNET_PATTERN.matcher(subnet);
		if(!matcher.find())
			return null;
		String ip=matcher.group(1);
		String prefixLen=matcher.group(2);
		try{
			InetAddress addr=InetAddress.getByName(ip);
			int prefix=128;
			if(prefixLen!=null){
				prefix=Utils.safeParseInt(prefixLen);
			}
			prefix=switch(addr){
				case Inet4Address ipv4 -> Math.min(prefix, 32);
				case Inet6Address ipv6 -> Math.min(prefix, 128);
				default -> throw new IllegalStateException("Unexpected value: " + addr);
			};
			return new InetAddressRange(addr, prefix);
		}catch(UnknownHostException x){
			return null;
		}
	}
}
