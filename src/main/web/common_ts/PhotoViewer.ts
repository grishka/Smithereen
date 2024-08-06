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
}

interface PhotoViewerPhotoInteractions{
	likes:number;
	isLiked:number;
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
	var url=el.dataset.pvUrl || "/photos/ajaxViewerInfo";
	if(mobile){
		new MobilePhotoViewer(info, url).show();
	}else{
		new DesktopPhotoViewer(info, url).show();
	}
	return false;
}
