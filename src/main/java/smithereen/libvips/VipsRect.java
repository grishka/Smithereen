package smithereen.libvips;

import com.sun.jna.Structure;

@Structure.FieldOrder({"left", "top", "width", "height"})
public class VipsRect extends Structure{
	public int left;
	public int top;
	public int width;
	public int height;

	public VipsRect(){

	}

	public VipsRect(int left, int top, int width, int height){
		this.left=left;
		this.top=top;
		this.width=width;
		this.height=height;
	}

	public VipsRect(int width, int height){
		this.width=width;
		this.height=height;
	}
}
