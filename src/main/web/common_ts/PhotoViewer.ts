// Embedded in <a>'s that open the photo viewer
interface PhotoViewerInlineData{
	index:number;
	list:string;
	urls:PhotoViewerSizedImageURLs[];
}

// /photos/ajaxViewerInfo returns an array of these
interface PhotoViewerPhoto{
	id:string;
	authorURL:string;
	authorName:string;
	albumID:string;
	albumTitle:string;
	html:string; // bottom part of the layer on desktop, description on mobile
	actions:string[];
	urls:PhotoViewerSizedImageURLs[];
	interactions:PhotoViewerPhotoInteractions;
	originalURL:string;
	historyURL:string;
	apURL:string;
}

interface PhotoViewerPhotoInteractions{
	likes:number;
	isLiked:boolean;
	comments:number;
}

interface PhotoViewerInfoAjaxResponse{
	total:number;
	title:string;
	photos:PhotoViewerPhoto[];
}

interface PhotoViewerSizedImageURLs{
	type:string;
	width:number;
	height:number;
	webp:string;
	jpeg:string;
}

function openPhotoViewer(el:HTMLElement):boolean{
	var info:PhotoViewerInlineData=JSON.parse(el.dataset.pv);
	doOpenPhotoViewer(info, el.dataset.pvUrl);
	return false;
}

function doOpenPhotoViewer(info:PhotoViewerInlineData, listURL:string="/photos/ajaxViewerInfo", fromPopState:boolean=false){
	var topLayer=LayerManager.getMediaInstance().getTopLayer();
	if(mobile){
		if(topLayer instanceof MobilePhotoViewer){
			if(topLayer.listID==info.list)
				return;
		}
		new MobilePhotoViewer(info, listURL, fromPopState).show();
	}else{
		if(topLayer instanceof DesktopPhotoViewer){
			if(topLayer.listID==info.list)
				return;
		}
		new DesktopPhotoViewer(info, listURL, fromPopState).show();
	}
}

