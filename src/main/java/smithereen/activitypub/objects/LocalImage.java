package smithereen.activitypub.objects;

import org.json.JSONObject;

import java.util.ArrayList;

import smithereen.activitypub.ContextCollector;
import smithereen.data.PhotoSize;

public class LocalImage extends Image{
	public ArrayList<PhotoSize> sizes=new ArrayList<>();

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);

		PhotoSize biggest=null;
		int biggestArea=0;
		for(PhotoSize s:sizes){
			if(s.format!=PhotoSize.Format.JPEG)
				continue;
			int area=s.width*s.height;
			if(area>biggestArea){
				biggestArea=area;
				biggest=s;
			}
		}
		if(biggest!=null){
			obj.put("url", biggest.src.toString());
		}

		return obj;
	}
}
