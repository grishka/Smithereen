class DesktopPhotoViewer extends BaseMediaViewerLayer{
	private contentWrap:HTMLElement;
	private titleEl:HTMLElement;
	private imgWrap:HTMLElement;
	private sourceWebp:HTMLSourceElement
	private sourceJpeg:HTMLSourceElement;
	private imgEl:HTMLImageElement;
	private imgLoader:HTMLElement;
	private bottomPart:HTMLElement;
	private pictureEl:HTMLElement;
	private layerBackThing:HTMLElement;
	private layerCloseThing:HTMLElement;

	private currentIndex:number;
	private listID:string;
	private total:number;
	private imgW:number;
	private imgH:number;
	private listURL:string;
	private currentURLs:PhotoViewerSizedImageURLs[];
	private photos:PhotoViewerPhoto[]=[];
	private photosLoadedOffset:number=null;
	private loading:boolean=false;
	private keyDownListener=this.onKeyDown.bind(this);
	private wasShown:boolean=false;

	public constructor(info:PhotoViewerInlineData, listURL:string){
		super();
		this.currentIndex=info.index;
		this.currentURLs=info.urls;
		this.listID=info.list;
		this.listURL=listURL;
		this.contentWrap=ce("div", {className: ("simpleLayer pvLayer photoViewer")}, [
			ce("div", {className: "layerTitle"}, [
				this.titleEl=ce("div", {className: "title"}, [
					ce("span", {className: "inlineLoader"})
				]),
				ce("a", {className: "close", innerText: lang("close"), onclick: ()=>this.dismiss()})
			]),
			this.imgWrap=ce("div", {className: "imgW"}, [
				this.imgLoader=ce("div", {className: "pvLoader"})
			]),
			this.bottomPart=ce("div", {className: "infoAndComments"})
		]);
		this.updateImage();
		this.loadPhotoList(Math.floor(this.currentIndex/10)*10);
	}

	public onCreateContentView():HTMLElement{
		var cont=this.getContent();
		cont.appendChild(this.layerCloseThing=ce("div", {className: "pvClose"}, [
			ce("div", {className: "icon"})
		]));
		cont.appendChild(this.layerBackThing=ce("div", {className: "pvBack"}, [
			ce("div", {className: "icon"})
		]));
		this.layerCloseThing.addEventListener("click", (ev)=>this.dismiss(), false);
		this.layerBackThing.addEventListener("click", (ev)=>this.showPrev(), false);
		this.layerBackThing.hide();
		return this.contentWrap;
	}

	public getCustomDismissAnimation(){
		return {
			keyframes: [{transform: "translateY(0)", opacity: 1}, {transform: "translateY(15px)", opacity: 0}],
			options: {duration: 150, easing: "cubic-bezier(0.32, 0, 0.67, 0)"}
		};
	}

	public getCustomAppearAnimation(){
		return {
			keyframes: [{transform: "translateY(25px)", opacity: 0}, {transform: "translateY(0)", opacity: 1}],
			options: {duration: 250, easing: "cubic-bezier(0.22, 1, 0.36, 1)"}
		};
	}

	public onWindowResize(){
		super.onWindowResize();
		this.updateSize();
	}

	public wantsDarkerScrim(){
		return true;
	}

	public updateTopOffset(){
		// no-op
	}
	
	public onShown(){
		super.onShown();
		window.addEventListener("keydown", this.keyDownListener, false);
		if(!this.wasShown){
			this.wasShown=true;
			this.updateSize();
			this.updateImageURLs();
		}
	}

	public onHidden(){
		super.onHidden();
		window.removeEventListener("keydown", this.keyDownListener);
	}

	private updateTitle(){
		this.titleEl.innerText=lang("photo_X_of_Y", {current: this.currentIndex+1, total: this.total});
	}

	private updateSize(){
		var viewportW=window.innerWidth;
		var viewportH=window.innerHeight;
		var biggest=this.currentURLs[this.currentURLs.length-1];
		var phW=biggest.width;
		var phH=biggest.height;

		var w=viewportW-2-120-34-50;
		var h=viewportH-31-28-72;
		if(w>1280){
			w=1280;
		}else if(w>807 && w>907){
			w=807;
		}else if(w<604){
			w=604;
		}
		if(h<453){
			h=453;
		}
		
		var c=phW>w ? (w/phW) : 1;
		if(phH*c>h){
			c=h/phH;
		}
		w=Math.max(604, Math.floor(phW*c));
		h=Math.max(453, Math.floor(phH*c));

		this.imgEl.style.width=w+"px";
		this.imgEl.style.height=h+"px";
		this.imgW=w;
		this.imgH=h;

		this.layerBackThing.style.width=Math.round(viewportW/2-this.contentWrap.offsetWidth/2)+"px";
	}

	private updateImage(){
		this.imgLoader.show();
		if(this.pictureEl){
			this.pictureEl.remove();
		}
		this.pictureEl=ce("picture", {}, [
			this.sourceWebp=ce("source", {type: "image/webp"}),
			this.sourceJpeg=ce("source", {type: "image/jpeg"}),
			this.imgEl=ce("img")
		]);
		this.imgEl.addEventListener("load", (ev)=>{
			this.imgLoader.hide();
		});
		this.imgEl.addEventListener("click", (ev)=>{
			if(this.total==1)
				this.dismiss();
			else
				this.showNext();
		});
		this.imgWrap.appendChild(this.pictureEl);
	}

	private updateImageURLs(){
		var size=this.currentURLs[0], x2size=this.currentURLs[0];
		for(var i=0;i<this.currentURLs.length;i++){
			size=this.currentURLs[i];
			x2size=this.currentURLs[Math.min(i+1, this.currentURLs.length-1)];
			if(size.width>=this.imgW && size.height>=this.imgH)
				break;
		}
		this.imgEl.src=size.jpeg;
		this.sourceWebp.srcset=`${size.webp}, ${x2size.webp} 2x`;
		this.sourceJpeg.srcset=`${size.jpeg}, ${x2size.jpeg} 2x`;
	}

	private updateBottomPart(){
		var ph=this.photos[this.currentIndex];
		this.bottomPart.innerHTML=ph.html;
	}

	private loadPhotoList(offset:number){
		if(this.loading)
			throw new Error("already loading");
		this.loading=true;
		ajaxGet(this.listURL+"?list="+this.listID+"&offset="+offset, (_r)=>{
			var r=_r as PhotoViewerInfoAjaxResponse;
			this.total=r.total;
			this.updateTitle();
			if(this.photosLoadedOffset==null){
				this.photosLoadedOffset=offset;
				if(this.total>1){
					this.layerBackThing.show();
				}
			}else{
				this.photosLoadedOffset=Math.min(this.photosLoadedOffset, offset);
			}
			for(var i=0;i<r.photos.length;i++){
				this.photos[i+offset]=r.photos[i];
			}
			this.updateBottomPart();
			this.loading=false;
			this.maybeLoadMorePhotos();
		}, (msg)=>{
			new MessageBox(lang("error"), msg, lang("close")).show();
			this.loading=false;
		}, "json");
	}

	private maybeLoadMorePhotos(){
		if(this.currentIndex<this.total-1 && !this.photos[this.currentIndex+1]){
			this.loadPhotoList(this.currentIndex+1);
		}else if(this.currentIndex>0 && !this.photos[this.currentIndex-1]){
			this.loadPhotoList(Math.max(0, this.currentIndex-10));
		}else if(this.currentIndex==this.total-1 && !this.photos[0]){
			this.loadPhotoList(0);
		}else if(this.currentIndex==0 && !this.photos[this.total-1]){
			this.loadPhotoList(Math.floor((this.total-1)/10)*10);
		}
	}

	public setCurrentPhoto(index:number){
		if(index==this.currentIndex)
			return;
		if(!this.photos[index])
			return;
		if(this.loading)
			return;
		this.currentIndex=index;
		this.currentURLs=this.photos[index].urls;
		this.updateImage();
		this.updateSize();
		this.updateImageURLs();
		this.updateTitle();
		this.updateBottomPart();
		this.maybeLoadMorePhotos();
	}

	public showNext(){
		this.setCurrentPhoto(this.currentIndex<this.total-1 ? this.currentIndex+1 : 0);
	}

	public showPrev(){
		this.setCurrentPhoto(this.currentIndex>0 ? this.currentIndex-1 : this.total-1);
	}

	private onKeyDown(ev:KeyboardEvent){
		if(document.activeElement && (document.activeElement.tagName=="INPUT" || document.activeElement.tagName=="TEXTAREA"))
			return;
		if(ev.keyCode==37){ // left arrow
			this.showPrev();
		}else if(ev.keyCode==39){ // right arrow
			this.showNext();
		}
	}
}
