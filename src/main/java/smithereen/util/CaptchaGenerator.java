package smithereen.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class CaptchaGenerator{
	// Inspired by http://www.captcha.ru/kcaptcha/

	private static final int IMG_WIDTH=130;
	private static final int IMG_HEIGHT=50;
	private static final SecureRandom rand=new SecureRandom();
	private static final String FONT_ALPHABET="0123456789abcdefghijklmnopqrstuvwxyz";
	private static final String GEN_ALPHABET="23456789abcdefghijkmnpqrstuvxyz";
	private static final int V_OFFSET_AMPLITUDE=5;
	private static final Pattern BAD_SUBSTRINGS=Pattern.compile("cp|cb|ck|c6|c9|rn|rm|mm|co|do|cl|db|qp|qb|dp|ww");

	private static final String[] fontNames;
	private static final CaptchaFont[] fonts;

	static{
		try(BufferedReader reader=new BufferedReader(new InputStreamReader(CaptchaGenerator.class.getClassLoader().getResourceAsStream("captcha_fonts/index.txt")))){
			ArrayList<String> lines=new ArrayList<>();
			String line;
			while((line=reader.readLine())!=null){
				lines.add(line);
			}
			fontNames=lines.toArray(new String[0]);
			fonts=new CaptchaFont[fontNames.length];
		}catch(IOException x){
			throw new RuntimeException(x);
		}
	}

	public static Captcha generate(){
		String answer=generateCaptchaString();

		int[] fgColor1={rand.nextInt(30, 70), rand.nextInt(50, 128), rand.nextInt(50)};
		int[] fgColor2={rand.nextInt(30, 70), rand.nextInt(50, 128), rand.nextInt(50)};
		int[][] lineColors={
				{rand.nextInt(30, 70), rand.nextInt(50, 128), rand.nextInt(50)},
				{rand.nextInt(30, 70), rand.nextInt(50, 128), rand.nextInt(50)},
		};
		int[] bgColor={rand.nextInt(230, 240), rand.nextInt(230, 240), rand.nextInt(180, 210)};

		BufferedImage img=new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g=img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

		CaptchaFont font=getRandomFont();
		int fontHeight=font.image.getHeight();
		int xOffset=0;
		for(int i=0;i<answer.length();i++){
			char ch=answer.charAt(i);
			char prevCh=i==0 ? ' ' : answer.charAt(i-1);
			int cIdx=FONT_ALPHABET.indexOf(ch);
			if(cIdx==-1)
				throw new IllegalStateException();

			int cOffset=font.charOffsets[cIdx];
			int cWidth=font.charWidths[cIdx];
			int y=rand.nextInt(-V_OFFSET_AMPLITUDE, V_OFFSET_AMPLITUDE+1)+(IMG_HEIGHT-fontHeight)/2+2;

			int shift=0;
			if(i>0){
				shift=10000;
				for(int sy=7;sy<fontHeight-20;sy++){
					for(int sx=cOffset-1;sx<cOffset+cWidth;sx++){
						int argb=font.image.getRGB(sx, sy);
						int opacity=(argb >> 24) & 0xFF;
						if(opacity>0){
							int left=sx-cOffset+xOffset;
							int py=sy+y;
							if(py>IMG_HEIGHT) break;
							for(int px=Math.min(left, IMG_WIDTH-1);px>left-12 && px>=0;px--){
								int color=img.getRGB(px, py) & 0xFF;
								if(color+(127-opacity/2)<190){
									if(shift>left-px){
										shift=left-px;
									}
									break;
								}
							}
							break;
						}
					}
				}
				if(shift==10000){
					shift=rand.nextInt(4, 7);
				}
				if(ch=='i' || ch=='j' || prevCh=='i' || prevCh=='j' || prevCh=='c' || prevCh=='r')
					shift=Math.min(shift, 2);
			}
			g.drawImage(font.image, xOffset-shift, y, xOffset-shift+cWidth, y+fontHeight-1, cOffset, 1, cOffset+cWidth, fontHeight, null);
			xOffset+=cWidth-shift;
		}
		int center=xOffset/2;

		int[][] linesPixels=new int[][]{null, null};
		for(int i=0;i<2;i++){
			BufferedImage linesImg=new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
			g=linesImg.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			double angle=rand.nextDouble(-Math.PI, Math.PI);
			int x=(int) Math.round(Math.cos(angle)*IMG_WIDTH*2);
			int y=(int) Math.round(Math.sin(angle)*IMG_WIDTH*2);
			int xOff=rand.nextInt(-IMG_WIDTH/3, IMG_WIDTH/3);
			int yOff=rand.nextInt(-IMG_HEIGHT/3, IMG_HEIGHT/3);
			int x1=x+IMG_WIDTH/2+xOff, x2=-x+IMG_WIDTH/2+xOff, y1=y+IMG_HEIGHT/2+yOff, y2=-y+IMG_HEIGHT/2+yOff;

			g.setStroke(new BasicStroke(rand.nextFloat(2, 4)));
			g.setColor(Color.BLACK);
			g.drawLine(x1, y1, x2, y2);
			linesPixels[i]=linesImg.getRGB(0, 0, IMG_WIDTH, IMG_HEIGHT, null, 0, IMG_WIDTH);
		}

		int[] pixels=new int[IMG_WIDTH*IMG_HEIGHT];
		int[] charsPixels=img.getRGB(0, 0, IMG_WIDTH, IMG_HEIGHT, null, 0, IMG_WIDTH);

		float plasmaOffset=rand.nextFloat(100_000f);
		float plasmaScaleFactor=rand.nextFloat(5f, 7f);
		for(int y=0;y<IMG_HEIGHT;y++){
			float yy=y/(float)IMG_HEIGHT/plasmaScaleFactor;
			for(int x=0;x<IMG_WIDTH;x++){
				float xx=x/(float)IMG_WIDTH/plasmaScaleFactor;
				float v=(float)Math.sin(xx*10f+plasmaOffset);
				v+=(float)Math.sin(10f*(xx*Math.sin(plasmaOffset/2f)+yy*Math.cos(plasmaOffset/3f))+plasmaOffset);
				float cx=xx+.5f*(float)Math.sin(plasmaOffset/5f);
				float cy=yy+.5f*(float)Math.cos(plasmaOffset/3f);
				v+=(float)Math.sin(Math.sqrt(100f*(cx*cx+cy*cy)+1)+plasmaOffset);
				v=(float)Math.sin(v*10f*Math.PI)/2f+0.5f;
				int red=Math.round(bgColor[0]+(255-bgColor[0])*v);
				int green=Math.round(bgColor[1]+(255-bgColor[1])*v);
				int blue=Math.round(bgColor[2]+(255-bgColor[2])*v);
				pixels[x+y*IMG_WIDTH]=0xFF000000 | (red << 16) | (green << 8) | blue;
			}
		}

		// periods
		float rand1=rand.nextFloat(0.075f, 0.12f);
		float rand2=rand.nextFloat(0.075f, 0.12f);
		float rand3=rand.nextFloat(0.075f, 0.12f);
		float rand4=rand.nextFloat(0.075f, 0.12f);
		// phases
		float rand5=rand.nextFloat((float)Math.PI);
		float rand6=rand.nextFloat((float)Math.PI);
		float rand7=rand.nextFloat((float)Math.PI);
		float rand8=rand.nextFloat((float)Math.PI);
		// amplitudes
		float rand9=rand.nextFloat(3.0f, 3.82727273f);
		float rand10=rand.nextFloat(3.0f, 4.1f);

		for(int x=0;x<IMG_WIDTH;x++){
			for(int y=0;y<IMG_HEIGHT;y++){
				float sx=x+((float)Math.sin(x*rand1+rand5)+(float)Math.sin(y*rand3+rand6))*rand9;
				float sy=y+((float)Math.sin(x*rand2+rand7)+(float)Math.sin(y*rand4+rand8))*rand10;
				int isx=(int) Math.floor(sx);
				int isy=(int) Math.floor(sy);

				if(isx<0 || isy<0 || isx>=IMG_WIDTH-1 || isy>=IMG_HEIGHT-1){
					continue;
				}

				for(int i=0;i<linesPixels.length;i++){
					int color=linesPixels[i][isx+isy*IMG_WIDTH] & 0xFF;
					int colorX=linesPixels[i][isx+1+isy*IMG_WIDTH] & 0xFF;
					int colorY=linesPixels[i][isx+(isy+1)*IMG_WIDTH] & 0xFF;
					int colorXY=linesPixels[i][isx+1+(isy+1)*IMG_WIDTH] & 0xFF;

					int newRed, newGreen, newBlue;
					if(color==255 && colorX==255 && colorY==255 && colorXY==255){
						continue;
					}else if(color==0 && colorX==0 && colorY==0 && colorXY==0){
						newRed=lineColors[i][0];
						newGreen=lineColors[i][1];
						newBlue=lineColors[i][2];
					}else{
						float frsx=sx%1f;
						float frsy=sy%1f;
						float frsx1=1f-frsx;
						float frsy1=1f-frsy;

						float newColor=color*frsx1*frsy1+colorX*frsx*frsy1+colorY*frsx1*frsy+colorXY*frsx*frsy;
						newColor=Math.min(newColor, 255)/255f;
						float newColor0=1f-newColor;

						newRed=Math.round(newColor0*lineColors[i][0]+newColor*bgColor[0]);
						newGreen=Math.round(newColor0*lineColors[i][1]+newColor*bgColor[1]);
						newBlue=Math.round(newColor0*lineColors[i][2]+newColor*bgColor[2]);
					}

					pixels[x+y*IMG_WIDTH]=0xFF000000 | (newRed << 16) | (newGreen << 8) | newBlue;
				}
			}
		}

		// periods
		rand1=rand.nextFloat(0.075f, 0.12f);
		rand2=rand.nextFloat(0.075f, 0.12f);
		rand3=rand.nextFloat(0.075f, 0.12f);
		rand4=rand.nextFloat(0.075f, 0.12f);
		// phases
		rand5=rand.nextFloat((float)Math.PI);
		rand6=rand.nextFloat((float)Math.PI);
		rand7=rand.nextFloat((float)Math.PI);
		rand8=rand.nextFloat((float)Math.PI);
		// amplitudes
		rand9=rand.nextFloat(3.0f, 3.82727273f);
		rand10=rand.nextFloat(3.0f, 4.1f);

		for(int x=0;x<IMG_WIDTH;x++){
			for(int y=0;y<IMG_HEIGHT;y++){
				float sx=x+((float)Math.sin(x*rand1+rand5)+(float)Math.sin(y*rand3+rand6))*rand9-IMG_WIDTH/2f+center+1f;
				float sy=y+((float)Math.sin(x*rand2+rand7)+(float)Math.sin(y*rand4+rand8))*rand10;
				int isx=(int) Math.floor(sx);
				int isy=(int) Math.floor(sy);

				if(isx<0 || isy<0 || isx>=IMG_WIDTH-1 || isy>=IMG_HEIGHT-1){
					continue;
				}

				int color=charsPixels[isx+isy*IMG_WIDTH] & 0xFF;
				int colorX=charsPixels[isx+1+isy*IMG_WIDTH] & 0xFF;
				int colorY=charsPixels[isx+(isy+1)*IMG_WIDTH] & 0xFF;
				int colorXY=charsPixels[isx+1+(isy+1)*IMG_WIDTH] & 0xFF;

				int newRed, newGreen, newBlue;
				if(color==255 && colorX==255 && colorY==255 && colorXY==255){
					continue;
				}else if(color==0 && colorX==0 && colorY==0 && colorXY==0){
					float k=sx/xOffset;
					newRed=Math.round(fgColor1[0]*(1f-k)+fgColor2[0]*k);
					newGreen=Math.round(fgColor1[1]*(1f-k)+fgColor2[1]*k);
					newBlue=Math.round(fgColor1[2]*(1f-k)+fgColor2[2]*k);
				}else{
					float frsx=sx%1f;
					float frsy=sy%1f;
					float frsx1=1f-frsx;
					float frsy1=1f-frsy;

					float newColor=color*frsx1*frsy1 + colorX*frsx*frsy1 + colorY*frsx1*frsy + colorXY*frsx*frsy;
					newColor=Math.min(newColor, 255)/255f;
					float newColor0=1f-newColor;

					float k=sx/xOffset;
					newRed=Math.round(fgColor1[0]*(1f-k)+fgColor2[0]*k);
					newGreen=Math.round(fgColor1[1]*(1f-k)+fgColor2[1]*k);
					newBlue=Math.round(fgColor1[2]*(1f-k)+fgColor2[2]*k);

					newRed=Math.round(newColor0*newRed+newColor*bgColor[0]);
					newGreen=Math.round(newColor0*newGreen+newColor*bgColor[1]);
					newBlue=Math.round(newColor0*newBlue+newColor*bgColor[2]);
				}

				pixels[x+y*IMG_WIDTH]=0xFF000000 | (newRed << 16) | (newGreen << 8) | newBlue;
			}
		}

		BufferedImage img2=new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		img2.setRGB(0, 0, IMG_WIDTH, IMG_HEIGHT, pixels, 0, IMG_WIDTH);

		return new Captcha(img2, answer);
	}

	private static String generateCaptchaString(){
		char[] chars=new char[rand.nextInt(5, 7)];
		while(true){
			for(int i=0;i<chars.length;i++){
				chars[i]=GEN_ALPHABET.charAt(rand.nextInt(GEN_ALPHABET.length()));
			}
			String s=new String(chars);
			if(!BAD_SUBSTRINGS.matcher(s).find())
				return s;
		}
	}

	private synchronized static CaptchaFont getRandomFont(){
		int idx=rand.nextInt(fontNames.length);
		if(fonts[idx]!=null)
			return fonts[idx];

		try{
			CaptchaFont font=new CaptchaFont(fontNames[idx]);
			fonts[idx]=font;
			return font;
		}catch(IOException x){
			throw new RuntimeException(x);
		}
	}

	public record Captcha(BufferedImage image, String answer){}

	private static class CaptchaFont{
		public final BufferedImage image;
		public final int[] charOffsets=new int[FONT_ALPHABET.length()];
		public final int[] charWidths=new int[FONT_ALPHABET.length()];

		public CaptchaFont(String name) throws IOException{
			try(InputStream in=getClass().getClassLoader().getResourceAsStream("captcha_fonts/"+name)){
				image=ImageIO.read(Objects.requireNonNull(in));
			}
			Raster topLine=image.getData(new Rectangle(0, 0, image.getWidth(), 1));
			int[] alpha=topLine.getSamples(0, 0, image.getWidth(), 1, 3, (int[])null);
			int charIndex=0;
			boolean insideChar=false;
			for(int i=0;i<alpha.length;i++){
				if(!insideChar && alpha[i]>0){
					insideChar=true;
					charOffsets[charIndex]=i;
				}else if(insideChar && alpha[i]==0){
					insideChar=false;
					charWidths[charIndex]=i-charOffsets[charIndex];
					charIndex++;
				}
			}
		}
	}
}
