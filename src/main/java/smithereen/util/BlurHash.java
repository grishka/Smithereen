package smithereen.util;

import java.io.IOException;
import java.util.Arrays;

import smithereen.libvips.VipsImage;
import smithereen.libvips.VipsRegion;

// adapted from https://github.com/hsch/blurhash-java
public class BlurHash{
	private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~";

	private static void applyBasisFunction(byte[] pixels, int width, int height, double normalisation, int i, int j, double[][] factors, int index){
		double r=0, g=0, b=0;
		for(int x=0; x<width; x++){
			for(int y=0; y<height; y++){
				double basis=normalisation*Math.cos((Math.PI*i*x)/width)*Math.cos((Math.PI*j*y)/height);
				int pixelR=(int) pixels[(y*width+x)*3] & 0xFF;
				int pixelG=(int) pixels[(y*width+x)*3+1] & 0xFF;
				int pixelB=(int) pixels[(y*width+x)*3+2] & 0xFF;
				r+=basis*sRGBToLinear(pixelR);
				g+=basis*sRGBToLinear(pixelG);
				b+=basis*sRGBToLinear(pixelB);
			}
		}
		double scale=1.0/(width*height);
		factors[index][0]=r*scale;
		factors[index][1]=g*scale;
		factors[index][2]=b*scale;
	}

	private static long encodeDC(double[] value){
		long r=linearTosRGB(value[0]);
		long g=linearTosRGB(value[1]);
		long b=linearTosRGB(value[2]);
		return (r<<16)+(g<<8)+b;
	}

	private static long encodeAC(double[] value, double maximumValue){
		double quantR=Math.floor(Math.max(0, Math.min(18, Math.floor(signPow(value[0]/maximumValue, 0.5)*9+9.5))));
		double quantG=Math.floor(Math.max(0, Math.min(18, Math.floor(signPow(value[1]/maximumValue, 0.5)*9+9.5))));
		double quantB=Math.floor(Math.max(0, Math.min(18, Math.floor(signPow(value[2]/maximumValue, 0.5)*9+9.5))));
		return Math.round(quantR*19*19+quantG*19+quantB);
	}

	private static double sRGBToLinear(long value){
		double v=value/255.0;
		if(v<=0.04045){
			return v/12.92;
		}else{
			return Math.pow((v+0.055)/1.055, 2.4);
		}
	}

	private static long linearTosRGB(double value){
		double v=Math.max(0, Math.min(1, value));
		if(v<=0.0031308){
			return (long) (v*12.92*255+0.5);
		}else{
			return (long) ((1.055*Math.pow(v, 1/2.4)-0.055)*255+0.5);
		}
	}

	private static double signPow(double val, double exp){
		return Math.copySign(Math.pow(Math.abs(val), exp), val);
	}

	private static double max(double[][] values, int from, int endExclusive) {
		double result = Double.NEGATIVE_INFINITY;
		for (int i = from; i < endExclusive; i++) {
			for (int j = 0; j < values[i].length; j++) {
				double value = values[i][j];
				if (value > result) {
					result = value;
				}
			}
		}
		return result;
	}

	private static void base83Encode(long value, int length, char[] buffer, int offset) {
		int exp = 1;
		for (int i = 1; i <= length; i++, exp *= 83) {
			int digit = (int)(value / exp % 83);
			buffer[offset + length - i] = ALPHABET.charAt(digit);
		}
	}

	private static long base83Decode(String str){
		long value=0;
		for(int i=0;i<str.length();i++){
			char c=str.charAt(i);
			value=value*83+ALPHABET.indexOf(c);
		}
		return value;
	}

	public static String encode(VipsImage img, int componentX, int componentY) throws IOException{
		if (componentX < 1 || componentX > 9 || componentY < 1 || componentY > 9)
			throw new IllegalArgumentException("Blur hash must have between 1 and 9 components");

		int w=img.getWidth();
		int h=img.getHeight();
		VipsImage resized=null;
		VipsRegion rgn=null;
		try{
			resized=img.resize(32.0/w, 32.0/h);
			if(resized.getBands()!=3)
				throw new IllegalArgumentException("Image must have 3 channels");
			if(resized.getFormat()!=VipsImage.BandFormat.UCHAR){
				VipsImage converted=resized.castUChar();
				resized.release();
				resized=converted;
			}
			if(resized.hasColorProfile()){
				VipsImage converted=resized.iccTransform("srgb");
				resized.release();
				resized=converted;
			}

			rgn=new VipsRegion(resized);
			byte[] pixels=rgn.fetch(0, 0, resized.getWidth(), resized.getHeight());

			double[][] factors=new double[componentX*componentY][3];
			for(int j=0; j<componentY; j++){
				for(int i=0; i<componentX; i++){
					double normalisation=i==0 && j==0 ? 1 : 2;
					applyBasisFunction(pixels, resized.getWidth(), resized.getHeight(), normalisation, i, j, factors, j*componentX+i);
				}
			}

			char[] hash=new char[1+1+4+2*(factors.length-1)]; // size flag + max AC + DC + 2 * AC components

			long sizeFlag=componentX-1+(componentY-1)*9;
			base83Encode(sizeFlag, 1, hash, 0);

			double maximumValue;
			if(factors.length>1){
				double actualMaximumValue=max(factors, 1, factors.length);
				double quantisedMaximumValue=Math.floor(Math.max(0, Math.min(82, Math.floor(actualMaximumValue*166-0.5))));
				maximumValue=(quantisedMaximumValue+1)/166;
				base83Encode(Math.round(quantisedMaximumValue), 1, hash, 1);
			}else{
				maximumValue=1;
				base83Encode(0, 1, hash, 1);
			}

			double[] dc=factors[0];
			base83Encode(encodeDC(dc), 4, hash, 2);

			for(int i=1; i<factors.length; i++){
				base83Encode(encodeAC(factors[i], maximumValue), 2, hash, 6+2*(i-1));
			}
			return new String(hash);
		}finally{
			if(rgn!=null)
				rgn.release();
			if(resized!=null)
				resized.release();
		}
	}

	public static int decodeToSingleColor(String hash){
		if(hash.length()<6)
			return 0;

		int value=(int)base83Decode(hash.substring(2, 6));
		return value & 0xFFFFFF;
	}
}
