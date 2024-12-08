package smithereen.model.photos;

public record AvatarCropRects(ImageRect profile, ImageRect thumb){
	public static AvatarCropRects fromString(String _crop){
		AvatarCropRects cropRects=null;
		if(_crop!=null){
			String[] parts=_crop.split(",");
			if(parts.length==8){
				float[] crop=new float[8];
				try{
					for(int i=0;i<8;i++){
						float coord=Float.parseFloat(parts[i]);
						if(coord<0 || coord>1){
							crop=null;
							break;
						}
						crop[i]=coord;
					}
				}catch(NumberFormatException x){
					crop=null;
				}
				if(crop!=null && crop[0]<crop[2] && crop[1]<crop[3] && crop[4]<crop[6] && crop[5]<crop[7]){
					cropRects=new AvatarCropRects(
							new ImageRect(crop[0], crop[1], crop[2], crop[3]),
							new ImageRect(crop[4], crop[5], crop[6], crop[7])
					);
				}
			}
		}
		return cropRects;
	}
}
