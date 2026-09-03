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
	topHTML:string;
	actions:string[];
	urls:PhotoViewerSizedImageURLs[];
	interactions:PhotoViewerPhotoInteractions;
	originalURL:string;
	historyURL:string;
	apURL:string;
	humanReadableDomain:string;
	saveURL:string;

	bottomPartEl:HTMLElement; // cached DOM tree of comments and stuff, set at runtime
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

function doOpenPhotoViewer(info:PhotoViewerInlineData, listURL:string="/photos/ajaxViewerInfo", fromPopState:boolean=false):any{
	var topLayer=LayerManager.getMediaInstance().getTopLayer();
	if(mobile){
		if(topLayer instanceof MobilePhotoViewer){
			if(topLayer.listID==info.list)
				return;
		}
		let viewer=new MobilePhotoViewer(info, listURL, fromPopState);
		viewer.show();
		return viewer;
	}else{
		if(topLayer instanceof DesktopPhotoViewer){
			if(topLayer.listID==info.list)
				return;
		}
		let viewer=new DesktopPhotoViewer(info, listURL, fromPopState);
		viewer.show();
		return viewer;
	}
}

// After a photo is deleted from the middle of the list, the indices of photos after the deleted one
// become invalid. This can result in an error when after deleting a photo from the middle of the list
// the user tries to open the last photo in the list.
//
// For this reason, all the indices of photos after the deleted one must be adjusted.
function updatePhotoIndicesAfterDeletion(deletedIndex:number){
	for(const photo of document.querySelectorAll<HTMLAnchorElement>("a.photo")){
		const data=JSON.parse(photo.dataset.pv) as PhotoViewerInlineData;
		if(data.index<=deletedIndex) continue;
		data.index=deletedIndex++;
		photo.dataset.pv=JSON.stringify(data);
	}
}

