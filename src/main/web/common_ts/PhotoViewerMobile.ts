class MobilePhotoViewer extends BaseMediaViewerLayer{
	private contentWrap:HTMLElement;
	private pager:HTMLElement;
	private uiOverlay:HTMLElement;
	private titleLoader:HTMLElement;
	private title:HTMLElement;
	private subtitle:HTMLElement;
	private bottomBar:HTMLElement;
	private description:HTMLElement;
	private interactionBar:HTMLElement;
	private likeBtn:HTMLAnchorElement;
	private likeCount:HTMLElement;
	private commentBtn:HTMLAnchorElement;
	private commentCount:HTMLElement;
	private tagBtn:HTMLAnchorElement;
	private tagCount:HTMLElement;

	private currentIndex:number;
	public listID:string;
	private total:number;
	private listURL:string;
	private photos:PhotoViewerPhoto[]=[];
	private photosLoadedOffset:number=null;
	private loading:boolean=false;
	private titleStr:string;
	private indexWhenPagesWereLastUpdated=-1;
	private hasNext=false;
	private hasPrev=false;
	private hasUnloadedAdjacentPhotos=false;
	private uiVisible=true;
	private tagsVisible=false;

	public constructor(info:PhotoViewerInlineData, listURL:string, fromPopState:boolean){
		super(fromPopState);
		this.currentIndex=info.index;
		this.listID=info.list;
		this.listURL=listURL;

		var initialPage, closeBtn, optionsBtn;
		this.contentWrap=ce("div", {className: "photoViewer"}, [
			this.pager=ce("div", {className: "pager"}, [
				initialPage=ce("div", {className: "photoW"})
			]),
			ce("div", {className: "pvUIW"}, [
				this.uiOverlay=ce("div", {className: "pvUI"}, [
					ce("div", {className: "pvTitleBar"}, [
						closeBtn=ce("a", {className: "close", title: lang("close")}),
						ce("div", {className: "titleInner"}, [
							this.titleLoader=ce("div", {className: "loader white"}),
							this.title=ce("div", {className: "title ellipsize"}),
							this.subtitle=ce("div", {className: "subtitle ellipsize"})
						]),
						optionsBtn=ce("a", {className: "options", title: lang("more_actions")})
					]),
					this.bottomBar=ce("div", {className: "pvBottom"}, [
						ce("div", {className: "description"}, [
							this.description=ce("div")
						]),
						this.interactionBar=ce("div", {className: "pvActions"}, [
							this.likeBtn=ce("a", {className: "action like", onclick: ()=>{return likeOnClick(this.likeBtn)}}, [
								ce("span", {className: "wideOnly"}, [lang("like")]),
								ce("span", {className: "icon"}),
								this.likeCount=ce("span", {className: "counter"})
							]),
							this.commentBtn=ce("a", {className: "action comment"}, [
								ce("span", {className: "wideOnly"}, [lang("add_comment")]),
								ce("span", {className: "icon"}),
								this.commentCount=ce("span", {className: "counter"})
							]),
							this.tagBtn=ce("a", {className: "action tag", href: "javascript:void(0)", onclick: ()=>{return this.toggleTags()}}, [
								ce("span", {className: "wideOnly"}, [lang("photo_tags")]),
								ce("span", {className: "icon"}),
								this.tagCount=ce("span", {className: "counter"})
							])
						])
					])
				])
			])
		]);
		closeBtn.addEventListener("click", (ev)=>this.dismiss(), false);
		optionsBtn.addEventListener("click", (ev)=>this.showOptions(), false);
		this.title.hide();
		this.subtitle.hide();
		this.pager.addEventListener("scroll", this.onPagerScroll.bind(this), false);
		this.pager.addEventListener("click", (ev)=>this.toggleUI(), false);

		this.loadPhotoIntoPage(initialPage, info.urls, info.index);
		this.loadPhotoList(Math.floor(this.currentIndex/10)*10);
	}

	public onCreateContentView():HTMLElement{
		return this.contentWrap;
	}

	public wantsScrim(){
		return false;
	}

	public onWindowResize(){
		super.onWindowResize();
		this.updateImageSizes();
	}

	public onShown(){
		super.onShown();
		this.updateImageSizes();
	}

	private updateImageSizes(){
		for(var el of this.pager.children.unfuck()){
			el.customData.zp.updateSize();
		}
	}

	private loadPhotoIntoPage(el:HTMLElement, urls:PhotoViewerSizedImageURLs[], index:number){
		var pictureEl, sourceWebp, sourceJpeg, imgEl, photoInner;
		var lastURL=urls[urls.length-1];
		var overlayTag;
		photoInner=ce("div", {className: "photo"}, [
			ce("div", {className: "photoInner"}, [
				pictureEl=ce("picture", {}, [
					sourceWebp=ce("source", {type: "image/webp", src: lastURL.webp}),
					sourceJpeg=ce("source", {type: "image/jpeg", src: lastURL.jpeg}),
					imgEl=ce("img", {src: lastURL.jpeg})
				]),
				ce("div", {className: "overlayW"}, [
					overlayTag=ce("div", {className: "overlayTag"})
				])
			])
		]);
		overlayTag.hide();
		el.appendChild(photoInner);
		el.customData={
			zp: new ZoomPanController(el, photoInner, lastURL.width, lastURL.height),
			index: index
		};
	}

	private loadPhotoList(offset:number){
		if(this.loading)
			throw new Error("already loading");
		this.loading=true;
		ajaxGet(this.listURL+"?list="+this.listID+"&offset="+offset, (_r)=>{
			var r=_r as PhotoViewerInfoAjaxResponse;
			this.total=r.total;
			this.titleStr=r.title;
			this.updateTitle();
			if(this.photosLoadedOffset==null){
				this.photosLoadedOffset=offset;
			}else{
				this.photosLoadedOffset=Math.min(this.photosLoadedOffset, offset);
			}
			for(var i=0;i<r.photos.length;i++){
				this.photos[i+offset]=r.photos[i];
			}
			this.updateBottomPart();
			this.loading=false;
			this.maybeLoadMorePhotos();
			this.maintainIllusionOfInfiniteScroll(false);
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
		}
	}

	private updateTitle(){
		this.titleLoader.hide();
		this.title.show();
		if(this.titleStr){
			this.subtitle.show();
			this.title.innerText=this.titleStr;
			this.subtitle.innerText=lang("object_X_of_Y", {current: this.currentIndex+1, total: this.total});
		}else{
			this.title.innerText=lang("object_X_of_Y", {current: this.currentIndex+1, total: this.total});
		}
	}

	private updateBottomPart(){
		var ph=this.photos[this.currentIndex];
		var url=ph.historyURL || `#${this.listID}/${this.currentIndex}`;
		this.updateHistory({layer: "PhotoViewer", pvInline: this.getCurrentInlineData(), pvListURL: this.listURL}, url);
		this.tagsVisible=false;
		this.tagBtn.classList.remove("active");

		if((!ph.html || !ph.html.length) && !ph.interactions){
			this.bottomBar.hide();
		}else{
			this.bottomBar.show();
			this.description.innerHTML=ph.html;
			if(ph.interactions){
				this.interactionBar.show();
				this.likeBtn.href=`/photos/${ph.id}/${ph.interactions.isLiked ? 'un' : ''}like?csrf=${userConfig.csrf}`;
				this.likeBtn.id="likeButtonPhoto"+ph.id;
				this.likeBtn.dataset.objType="photo";
				this.likeBtn.dataset.objId=ph.id;
				if(ph.interactions.isLiked)
					this.likeBtn.classList.add("liked");
				else
					this.likeBtn.classList.remove("liked");

				this.likeCount.id="likeCounterPhoto"+ph.id;
				if(ph.interactions.likes){
					this.likeCount.show();
					this.likeCount.innerText=formatNumber(ph.interactions.likes);
				}else{
					this.likeCount.hide();
				}

				this.commentBtn.href="/photos/"+ph.id;
				if(ph.interactions.comments){
					this.commentCount.show();
					this.commentCount.innerText=formatNumber(ph.interactions.comments);
				}else{
					this.commentCount.hide();
				}

				var tagsEl=this.bottomBar.qs(".pvBottomTags");
				if(tagsEl){
					this.tagBtn.show();
					this.tagCount.innerText=formatNumber(tagsEl.children.length);

					var currentHighlightedTag:HTMLElement;
					var observer=new IntersectionObserver((entries, observer)=>{
						var overlayTag:HTMLElement;
						for(var page of this.pager.children.unfuck()){
							if(page.customData.index==this.currentIndex){
								overlayTag=page.qs(".overlayTag");
								break;
							}
						}
						for(var entry of entries){
							var target=entry.target as HTMLElement;
							if(target==currentHighlightedTag && entry.intersectionRatio<=0.75){
								currentHighlightedTag=null;
								overlayTag.hideAnimated();
							}else if(target!=currentHighlightedTag && entry.intersectionRatio>=0.75){
								currentHighlightedTag=target;
								var rect=target.dataset.rect.split(",");
								var lastURL=ph.urls[ph.urls.length-1];
								var x=parseFloat(rect[0])*lastURL.width;
								var y=parseFloat(rect[1])*lastURL.height;
								var w=parseFloat(rect[2])*lastURL.width-x;
								var h=parseFloat(rect[3])*lastURL.height-y;
								overlayTag.style.left=x+"px";
								overlayTag.style.top=y+"px";
								overlayTag.style.setProperty("--tag-width", Math.round(w)+"px");
								overlayTag.style.setProperty("--tag-height", Math.round(h)+"px");
								overlayTag.showAnimated();
							}
						}
					}, {
						root: tagsEl,
						rootMargin: "0px 24px",
						threshold: [0.25, 0.75]
					});
					for(var tag of tagsEl.children.unfuck()){
						observer.observe(tag);
					}
				}else{
					this.tagBtn.hide();
				}
			}else{
				this.interactionBar.hide();
			}
		}
	}

	private getCurrentInlineData():PhotoViewerInlineData{
		return {
			list: this.listID,
			index: this.currentIndex,
			urls: this.photos[this.currentIndex].urls
		};
	}

	private maintainIllusionOfInfiniteScroll(fromScroll:boolean=true){
		if(this.currentIndex==this.indexWhenPagesWereLastUpdated && !this.hasUnloadedAdjacentPhotos)
			return;
		this.indexWhenPagesWereLastUpdated=this.currentIndex;
		this.pager.innerHTML="";
		var current=ce("div", {className: "photoW"});
		this.hasPrev=false;
		this.loadPhotoIntoPage(current, this.photos[this.currentIndex].urls, this.currentIndex);
		this.hasUnloadedAdjacentPhotos=false;
		if(this.currentIndex>0){
			if(this.photos[this.currentIndex-1]){
				this.hasPrev=true;
				var prev=ce("div", {className: "photoW"});
				this.loadPhotoIntoPage(prev, this.photos[this.currentIndex-1].urls, this.currentIndex-1);
				this.pager.appendChild(prev);
			}else{
				this.hasUnloadedAdjacentPhotos=true;
			}
		}
		this.pager.appendChild(current);
		this.hasNext=false;
		if(this.currentIndex<this.total-1){
			if(this.photos[this.currentIndex+1]){
				this.hasNext=true;
				var next=ce("div", {className: "photoW"});
				this.loadPhotoIntoPage(next, this.photos[this.currentIndex+1].urls, this.currentIndex+1);
				this.pager.appendChild(next);
			}else{
				this.hasUnloadedAdjacentPhotos=true;
			}
		}
		this.updateImageSizes();
		if(fromScroll){
			this.pager.style.scrollSnapType="none";
		}
		this.pager.scrollLeft=this.hasPrev ? window.innerWidth : 0;
		if(fromScroll){
			setTimeout(()=>{
				this.pager.scrollLeft=this.hasPrev ? window.innerWidth : 0;
				this.pager.style.scrollSnapType="";
			}, 200);
		}
	}

	private onPagerScroll(ev:Event){
		var pageIndex=Math.max(0, Math.min(this.pager.children.length-1, this.pager.scrollLeft/window.innerWidth));
		if(this.hasPrev && pageIndex==0){
			this.setCurrentPhoto(this.currentIndex-1);
		}else if(this.hasNext && pageIndex==this.pager.children.length-1){
			this.setCurrentPhoto(this.currentIndex+1);
		}
	}

	private setCurrentPhoto(index:number){
		if(this.currentIndex==index)
			return;
		this.currentIndex=index;
		this.maintainIllusionOfInfiniteScroll();
		this.updateTitle();
		this.updateBottomPart();
		this.maybeLoadMorePhotos();
	}

	public toggleUI(){
		this.uiVisible=!this.uiVisible;
		if(this.uiVisible)
			this.uiOverlay.showAnimated();
		else
			this.uiOverlay.hideAnimated();
	}

	public toggleTags(){
		this.tagsVisible=!this.tagsVisible;
		var tagsEl=this.bottomBar.qs(".pvBottomTags");
		var descrEl=this.bottomBar.qs(".pvBottomDescription");
		if(this.tagsVisible){
			this.tagBtn.classList.add("active");
			tagsEl.show();
			descrEl.hide();
		}else{
			this.tagBtn.classList.remove("active");
			tagsEl.hide();
			descrEl.show();
		}
	}

	private showOptions(){
		var ph=this.photos[this.currentIndex];
		var options:any[]=[];
		if(ph.actions.indexOf("SAVE_TO_ALBUM")!=-1){
			options.push({type: "link", href: `/photos/${ph.id}/saveToAlbum?csrf=${userConfig.csrf}`, ajax: "box", label: lang("photo_save_to_album")});
		}else if(ph.saveURL){
			options.push({type: "link", href: ph.saveURL, ajax: "box", label: lang("photo_save_to_album")});
		}
		if(ph.actions.indexOf("SET_AS_COVER")!=-1){
			options.push({type: "link", href: `/photos/${ph.id}/setAsAlbumCover?csrf=${userConfig.csrf}`, ajax: "box", label: lang("set_photo_as_album_cover")});
		}
		if(ph.actions.indexOf("DELETE")!=-1){
			options.push({type: "confirm", label: lang("delete"), title: lang("delete_photo"), msg: lang("delete_photo_confirm"), url: `/photos/${ph.id}/delete`});
		}
		if(ph.apURL){
			options.push({type: "link", href: ph.apURL, target: "_blank", label: lang("open_on_server_X", {domain: new URL(ph.apURL).host})});
		}
		if(ph.actions.indexOf("REPORT")!=-1){
			options.push({type: "link", href: "/system/reportForm?type=photo&id="+ph.id, ajax: "box", label: lang("report")});
		}
		options.push({type: "link", href: ph.originalURL, target: "_blank", label: lang("photo_open_original")});
		new MobileOptionsBox(options).show();
	}
}

class ZoomPanController{
	private container:HTMLElement;
	private img:HTMLElement;
	private scrollHolder:HTMLElement;
	private imgW:number;
	private imgH:number;
	private imgX:number;
	private imgY:number;
	private minScale:number;
	private maxScale:number;
	private currentScale:number;

	private capturingGesture=false;
	private gestureInitialDistance:number;
	private gestureCenterX:number;
	private gestureCenterY:number;
	private gestureScale:number;
	private transformMatrix:Matrix=new Matrix();
	private animating:boolean;

	public constructor(container:HTMLElement, img:HTMLElement, imgW:number, imgH:number){
		this.container=container;
		this.img=img;
		this.imgW=imgW;
		this.imgH=imgH;

		container.addEventListener("touchstart", this.onTouchStart.bind(this), false);
		container.addEventListener("touchmove", this.onTouchMove.bind(this), false);
		container.addEventListener("touchend", this.onTouchEnd.bind(this), false);

		this.scrollHolder=ce("div");
		this.scrollHolder.style.position="absolute";
		this.scrollHolder.style.top="0";
		this.scrollHolder.style.left="0";
	}

	public updateSize(){
		var winW=window.innerWidth;
		var winH=window.innerHeight;
		var scale=Math.min(winW/this.imgW, winH/this.imgH);
		var scaledW=Math.floor(this.imgW*scale);
		var scaledH=Math.floor(this.imgH*scale);
		this.img.style.width=this.imgW+"px";
		this.img.style.height=this.imgH+"px";
		this.img.style.left=this.img.style.top="0";
		this.img.style.transformOrigin="0 0";
		this.minScale=this.currentScale=scale;
		this.maxScale=Math.max(3, winH/this.imgH);
		this.transformMatrix.reset();
		this.transformMatrix.postTranslate(this.imgX=Math.round(winW/2-scaledW/2), this.imgY=Math.round(winH/2-scaledH/2));
		this.transformMatrix.postScale(scale);
		this.img.style.transform=this.transformMatrix.getCSSTransform();
		this.updateScaleProperty();

	}

	private onTouchStart(ev:TouchEvent){
		if(this.animating){
			ev.preventDefault();
		}
		if(ev.touches.length==2){
			this.capturingGesture=true;
			ev.preventDefault();
			var dx=ev.touches[0].clientX-ev.touches[1].clientX;
			var dy=ev.touches[0].clientY-ev.touches[1].clientY;
			this.gestureInitialDistance=Math.sqrt(dx*dx+dy*dy);
			var center=[(ev.touches[0].clientX+ev.touches[1].clientX)/2, (ev.touches[0].clientY+ev.touches[1].clientY)/2];
			var unmapped=this.transformMatrix.unmapPoint(center[0]+this.container.scrollLeft, center[1]+this.container.scrollTop);
			this.gestureCenterX=unmapped[0];
			this.gestureCenterY=unmapped[1];

			if(this.container.scrollWidth>0 || this.container.scrollHeight>0){
				var scrollW=this.container.scrollWidth+this.container.offsetWidth;
				var scrollH=this.container.scrollHeight+this.container.offsetHeight;
				this.scrollHolder.style.width=scrollW+"px";
				this.scrollHolder.style.height=scrollH+"px";
				this.container.insertAdjacentElement("afterbegin", this.scrollHolder);
			}
		}
	}

	private onTouchMove(ev:TouchEvent){
		if(this.animating){
			ev.preventDefault();
		}
		if(this.capturingGesture){
			ev.preventDefault();
			if(ev.touches.length==2){
				var dx=ev.touches[0].clientX-ev.touches[1].clientX;
				var dy=ev.touches[0].clientY-ev.touches[1].clientY;
				var distance=Math.sqrt(dx*dx+dy*dy);
				var scale=distance/this.gestureInitialDistance;
				this.gestureInitialDistance=distance;
				this.transformMatrix.postScaleAround(scale, this.gestureCenterX, this.gestureCenterY);
				this.img.style.transform=this.transformMatrix.getCSSTransform();
				this.gestureScale=scale;
				this.updateScaleProperty();
			}
		}
	}

	private onTouchEnd(ev:TouchEvent){
		if(this.animating){
			ev.preventDefault();
		}
		if(this.capturingGesture){
			ev.preventDefault();
			if(ev.touches.length==0){
				this.capturingGesture=false;
				this.applyTransformToPosition(true);
			}
		}
	}

	private applyTransformToPosition(applyLimits:boolean){
		var pt=this.transformMatrix.mapPoint(0, 0);
		if(pt[0]<0){
			this.transformMatrix.values[2]=0;
		}
		if(pt[1]<0){
			this.transformMatrix.values[5]=0;
		}
		// let the element grow before setting the scroll offsets, otherwise they won't stick
		this.img.style.transform=this.transformMatrix.getCSSTransform();
		var scrollW=this.container.scrollWidth+this.container.offsetWidth;
		var scrollH=this.container.scrollHeight+this.container.offsetHeight;
		if(pt[0]<0){
			this.container.scrollLeft+=-pt[0];
		}
		if(pt[1]<0){
			this.container.scrollTop+=-pt[1];
		}
		if(applyLimits){
			var scale=this.transformMatrix.values[0];
			var targetScale=Math.max(this.minScale, Math.min(this.maxScale, scale));
			var winW=window.innerWidth;
			var winH=window.innerHeight;
			var scaledW=Math.floor(this.imgW*targetScale);
			var scaledH=Math.floor(this.imgH*targetScale);

			var minTransX, maxTransX, minTransY, maxTransY;
			var transX=this.transformMatrix.values[2]-this.container.scrollLeft;
			var transY=this.transformMatrix.values[5]-this.container.scrollTop;
			if(scaledW<winW){
				minTransX=maxTransX=Math.round(winW/2-scaledW/2);
			}else{
				minTransX=winW-scaledW;
				maxTransX=0;
			}
			if(scaledH<winH){
				minTransY=maxTransY=Math.round(winH/2-scaledH/2);
			}else{
				minTransY=winH-scaledH;
				maxTransY=0;
			}

			if(scale<this.minScale || scale>this.maxScale || transX<minTransX || transX>maxTransX || transY<minTransY || transY>maxTransY){
				var offX=this.container.scrollLeft, offY=this.container.scrollTop;
				this.transformMatrix.values[2]-=offX;
				this.transformMatrix.values[5]-=offY;
				this.container.scrollLeft=this.container.scrollTop=0;
				this.img.style.transform=this.transformMatrix.getCSSTransform();

				this.transformMatrix.postScaleAround(targetScale/scale, this.gestureCenterX, this.gestureCenterY);
				transX=this.transformMatrix.values[2];
				transY=this.transformMatrix.values[5];
				this.transformMatrix.values[2]=Math.max(minTransX, Math.min(maxTransX, transX));
				this.transformMatrix.values[5]=Math.max(minTransY, Math.min(maxTransY, transY));

				this.animating=true;
				this.img.anim([{transform: this.transformMatrix.getCSSTransform()}], {duration: 200, easing: "ease"}, ()=>{
					this.applyTransformToPosition(false);
					this.animating=false;
					this.updateScaleProperty();
				});
				var animCallback:{(time:DOMHighResTimeStamp):void}=(time:DOMHighResTimeStamp)=>{
					var scale=this.img.offsetWidth/this.imgW;
					this.updateScaleProperty();
					if(this.animating)
						window.requestAnimationFrame(animCallback);
				};
				window.requestAnimationFrame(animCallback);
			}
			this.scrollHolder.remove();
		}
	}

	private updateScaleProperty(){
		var scale=this.img.getBoundingClientRect().width/this.imgW;
		this.img.style.setProperty("--zoom-scale", scale.toString());
	}
}

class Matrix{
	public values:number[];

	public constructor(values:number[]=[1, 0, 0,
										0, 1, 0,
										0, 0, 1]){
		this.values=values;
	}

	public static multiply(a:number[], b:number[]):number[]{
		return [
			a[0]*b[0] + a[1]*b[3] + a[2]*b[6], a[0]*b[1] + a[1]*b[4] + a[2]*b[7], a[0]*b[2] + a[1]*b[5] + a[2]*b[8],
			a[3]*b[0] + a[4]*b[3] + a[5]*b[6], a[3]*b[1] + a[4]*b[4] + a[5]*b[7], a[3]*b[2] + a[4]*b[5] + a[5]*b[8],
			a[6]*b[0] + a[7]*b[3] + a[8]*b[6], a[6]*b[1] + a[7]*b[4] + a[8]*b[7], a[6]*b[2] + a[7]*b[5] + a[8]*b[8]
		];
	}

	public postTranslate(x:number, y:number){
		this.values=Matrix.multiply(this.values,
			[1, 0, x,
			 0, 1, y,
			 0, 0, 1]
		);
	}

	public preTranslate(x:number, y:number){
		this.values=Matrix.multiply(
			[1, 0, x,
			 0, 1, y,
			 0, 0, 1], this.values
		);
	}

	public postScale(s:number){
		this.postScaleXY(s, s);
	}

	public postScaleXY(sx:number, sy:number){
		this.values=Matrix.multiply(this.values,
			[sx, 0, 0,
			 0, sy, 0,
			 0, 0, 1]
		);
	}

	public postScaleAround(s:number, cx:number, cy:number){
		this.postTranslate(cx, cy);
		this.postScale(s);
		this.postTranslate(-cx, -cy);
	}

	public postMultiply(other:Matrix){
		this.values=Matrix.multiply(this.values, other.values);
	}

	public reset(){
		this.values=[
			1, 0, 0,
			0, 1, 0,
			0, 0, 1
		];
	}

	public mapPoint(x:number, y:number):number[]{
		return [
			x*this.values[0] + y*this.values[1] + this.values[2],
			x*this.values[3] + y*this.values[4] + this.values[5]
		];
	}

	public getInverse():Matrix{
		// https://nigeltao.github.io/blog/2021/inverting-3x2-affine-transformation-matrix.html
		var fd=(this.values[0]*this.values[4])-(this.values[1]*this.values[3]);
		return new Matrix([
			this.values[4]/fd, -this.values[1]/fd, ((this.values[1]*this.values[5])-(this.values[4]*this.values[2]))/fd,
			-this.values[3]/fd, this.values[0]/fd, ((this.values[3]*this.values[2])-(this.values[0]*this.values[5]))/fd,
			0, 0, 1
		]);
	}

	public unmapPoint(x:number, y:number):number[]{
		return this.getInverse().mapPoint(x, y);
	}

	public getCSSTransform():string{
		return `matrix(${this.values[0]}, ${this.values[3]}, ${this.values[1]}, ${this.values[4]}, ${this.values[2]}, ${this.values[5]})`;
	}
}

