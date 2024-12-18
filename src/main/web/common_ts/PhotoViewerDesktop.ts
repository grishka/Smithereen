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

	private tagsWrap:HTMLElement;
	private tagTopBar:HTMLElement;
	private tagFriendListPopup:HTMLElement;
	private tagFriendListSubmit:HTMLElement;
	private tagHighlight:HTMLElement;
	private tagHighlightSvg:SVGSVGElement;
	private tagHighlightBorder:HTMLElement;
	private tagHighlightSvgRect:SVGRectElement;

	private currentIndex:number;
	public listID:string;
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
	public bottomPartUpdateCallback:{(el:HTMLElement):void};
	private tagging:boolean=false;
	private tagAreaSelector:ImageAreaSelector;

	public constructor(info:PhotoViewerInlineData, listURL:string, fromPopState:boolean){
		super(fromPopState);
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
				this.imgLoader=ce("div", {className: "pvLoader"}),
				this.tagsWrap=ce("div", {className: "tagsW"}, [
					this.tagHighlight=ce("div", {className: "tagHighlight"}, [
						this.tagHighlightBorder=ce("div", {className: "tagHighlightBorder"})
					])
				])
			]),
			this.bottomPart=ce("div", {className: "infoAndComments"})
		]);
		this.updateImage();
		this.historyEntryAdded=fromPopState;
		this.loadPhotoList(Math.floor(this.currentIndex/10)*10);
		this.tagsWrap.hide();

		this.tagHighlightSvg=document.createElementNS("http://www.w3.org/2000/svg", "svg");
		this.tagHighlightSvg.innerHTML=`<defs>
			<mask id="tagMask">
				<rect x="0" y="0" width="100%" height="100%" fill="white"/>
				<rect x="50" y="50" width="100" height="100" fill="black" id="maskRect"/>
			</mask>
			</defs>
			<rect x="0" y="0" width="100%" height="100%" fill="black" fill-opacity="0.75" mask="url(#tagMask)"/>`;
		this.tagHighlight.insertAdjacentElement("afterbegin", this.tagHighlightSvg);
		this.tagHighlightSvgRect=this.tagHighlightSvg.getElementById("maskRect") as SVGRectElement;
		this.tagHighlight.hide();
	}

	public onCreateContentView():HTMLElement{
		var cont=this.getContent();
		cont.appendChild(this.layerCloseThing=ce("div", {className: "pvClose"}, [
			ce("div", {className: "icon"})
		]));
		cont.appendChild(this.layerBackThing=ce("div", {className: "pvBack"}, [
			ce("div", {className: "icon"})
		]));
		this.layerCloseThing.addEventListener("click", (ev)=>{
			if(this.tagging)
				this.doneTagging();
			else
				this.dismiss();
		}, false);
		this.layerBackThing.addEventListener("click", (ev)=>this.showPrev(), false);
		this.layerBackThing.hide();
		var wheelListener=(ev:WheelEvent)=>{
			cont.scrollTop+=ev.deltaY;
			cont.scrollLeft+=ev.deltaX;
		};
		this.layerCloseThing.addEventListener("wheel", wheelListener, false);
		this.layerBackThing.addEventListener("wheel", wheelListener, false);
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

		var tagOverlayW=Math.min(w, phW);
		var tagOverlayH=Math.min(h, phH);
		this.tagsWrap.style.width=tagOverlayW+"px";
		this.tagsWrap.style.left=(this.contentWrap.offsetWidth-tagOverlayW)/2+"px";
		this.tagsWrap.style.height=tagOverlayH+"px";
		this.tagsWrap.style.top=(h-tagOverlayH)/2+"px";
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
		var isNew;
		if(ph.bottomPartEl){
			isNew=false;
			for(var child of this.bottomPart.children.unfuck()){
				child.remove();
			}
			this.bottomPart.appendChild(ph.bottomPartEl);
		}else{
			isNew=true;
			this.bottomPart.innerHTML=ph.html;
			ph.bottomPartEl=this.bottomPart.children[0] as HTMLElement;
		}
		var url=ph.historyURL || `#${this.listID}/${this.currentIndex}`;
		this.updateHistory({layer: "PhotoViewer", pvInline: this.getCurrentInlineData(), pvListURL: this.listURL}, url);
		if(isNew){
			updatePostForms(this.bottomPart);

			var editW=this.bottomPart.qs(".editableDescriptionW");
			if(editW){
				var descr=editW.qs(".description");
				descr.addEventListener("mouseenter", (ev)=>{
					showTooltip(editW, lang("photo_edit_description"));
				});
				descr.addEventListener("mouseleave", (ev)=>{
					hideTooltip(editW);
				});
				for(var el of editW.querySelectorAll(".description, .descriptionPlaceholder").unfuck()){
					el.addEventListener("click", (ev)=>{
						if((ev.target as HTMLElement).tagName=="A")
							return;
						var loader=editW.qs(".overlayLoader");
						loader.show();
						ajaxGetAndApplyActions(`/photos/${ph.id}/ajaxEditDescription`, ()=>{
							loader.hide();
						}, ()=>{
							loader.hide();
						});
					});
				}
			}

			var rotateW=this.bottomPart.qs(".pvRotate");
			if(rotateW){
				var buttons=rotateW.qs(".pvRotateButtons");
				var loader=rotateW.qs(".inlineLoader");
				buttons.addEventListener("click", (ev)=>{
					var target=ev.target;
					if(!(target instanceof HTMLAnchorElement))
						return;
					ev.preventDefault();
					buttons.hide();
					loader.show();
					ajaxGet(target.href, (r)=>{
						buttons.show();
						loader.hide();
						var urls=r as PhotoViewerSizedImageURLs[];
						this.photos[this.currentIndex].urls=urls;
						this.currentURLs=urls;
						this.updateSize();
						this.updateImageURLs();
					}, (err)=>{
						new MessageBox(lang("error"), err || lang("network_error"), lang("close")).show();
						buttons.show();
						loader.hide();
					}, "json");
				});
			}

			var addTagBtn=this.bottomPart.qs(".pvAddTag");
			if(addTagBtn){
				addTagBtn.addEventListener("click", (ev)=>{
					this.startTagging();
				});
			}

			var tagsCont=this.bottomPart.qs(".pvTagsCont");
			if(tagsCont){
				tagsCont.addEventListener("mouseover", (ev)=>{
					var target=ev.target as HTMLElement;
					if(target.dataset.rect)
						this.highlightTag(target.dataset.rect);
				});
				tagsCont.addEventListener("mouseout", (ev)=>{
					var target=ev.target as HTMLElement;
					if(target.dataset.rect)
						this.hideTagHighlight();
				});
			}
		}

		// Clean up bottom part DOM trees. Keep current, 10 photos forward and 10 photos backward.
		if(this.total>21){
			var startIndex=this.currentIndex-10;
			var endIndex=this.currentIndex+10;
			if(endIndex>this.total){
				endIndex-=this.total;
			}
			if(startIndex<0){
				startIndex=this.total+startIndex;
			}
			if(startIndex>endIndex){
				for(var i=endIndex;i<startIndex;i++){
					if(this.photos[i] && this.photos[i].bottomPartEl){
						this.photos[i].bottomPartEl=null;
					}
				}
			}else{
				for(var i=0;i<startIndex;i++){
					if(this.photos[i] && this.photos[i].bottomPartEl){
						this.photos[i].bottomPartEl=null;
					}
				}
				for(var i=endIndex;i<this.total;i++){
					if(this.photos[i] && this.photos[i].bottomPartEl){
						this.photos[i].bottomPartEl=null;
					}
				}
			}
		}

		if(this.bottomPartUpdateCallback)
			this.bottomPartUpdateCallback(ph.bottomPartEl);
	}

	private getCurrentInlineData():PhotoViewerInlineData{
		return {
			list: this.listID,
			index: this.currentIndex,
			urls: this.photos[this.currentIndex].urls
		};
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
		if(this.tagging)
			return;
		this.setCurrentPhoto(this.currentIndex<this.total-1 ? this.currentIndex+1 : 0);
	}

	public showPrev(){
		if(this.tagging)
			return;
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

	public showLocalDescriptionEditor(description:string, onDone:{(newDescription:string):void}){
		var textarea:HTMLTextAreaElement;
		var editEl=ce("div", {className: "pvEditDescription marginAfter"}, [
			textarea=ce("textarea", {placeholder: lang("photo_description"), value: description}),
			ce("div", {className: "buttons"}, [
				ce("input", {type: "button", value: lang("save"), onclick: (ev)=>onDone(textarea.value)})
			])
		]);
		var editW=ge("pvEditDescriptionW_");
		editW.hide();
		editW.insertAdjacentElement("afterend", editEl);
		textarea.addEventListener("keydown", (ev:KeyboardEvent)=>{
			if(ev.keyCode==13 && (isApple ? ev.metaKey : ev.ctrlKey)){
				onDone(textarea.value);
			}
		});
	}

	private startTagging(){
		if(this.tagging)
			return;
		this.tagging=true;
		this.imgWrap.insertAdjacentElement("beforebegin", this.tagTopBar=ce("div", {className: "settingsMessage pvTaggingTopBar"}, [
			lang("photo_tagging_info"),
			ce("a", {href: "javascript:void(0)", className: "flR", innerHTML: lang("photo_tagging_done"), onclick: ()=>this.doneTagging()})
		]));
		this.bottomPart.qs(".actionList").hide();
		this.tagsWrap.show();

		if(!this.tagAreaSelector){
			this.tagAreaSelector=new ImageAreaSelector(this.tagsWrap);
			var listLoader;
			var list:HTMLElement;
			var input:HTMLInputElement;
			this.tagFriendListPopup=ce("div", {className: "pvTagFriendListPopup"}, [
				ce("div", {className: "boxTitleBar"}, [
					ce("div", {className: "title", innerHTML: lang("photo_tag_select_friend")}),
					ce("a", {className: "close", href: "javascript:void(0)", onclick: this.resetCurrentTag.bind(this)})
				]),
				ce("div", {className: "searchBar"}, [
					input=ce("input", {type: "text", placeholder: lang("photo_tag_name_search")})
				]),
				list=ce("div", {className: "list"}, [
					listLoader=ce("div", {className: "loader"})
				]),
				ce("div", {className: "boxButtonBar"}, [
					this.tagFriendListSubmit=ce("button", {onclick: ()=>{
						if(!input.value)
							return;
						this.addTag(null, input.value);
					}}, [lang("photo_add_tag_submit")]),
					ce("button", {className: "secondary", onclick: this.resetCurrentTag.bind(this)}, [lang("cancel")])
				])
			]);
			listLoader.style.marginTop="15px";
			this.imgWrap.appendChild(this.tagFriendListPopup);
			this.tagAreaSelector.onStartDrag=()=>{
				this.tagFriendListPopup.hideAnimated();
			};
			this.tagAreaSelector.onEndDrag=()=>{
				var area=this.tagAreaSelector.getSelectedArea();
				this.tagFriendListPopup.style.left=(this.tagsWrap.offsetLeft+area.x+area.w)+"px";
				this.tagFriendListPopup.style.top=(this.tagsWrap.offsetTop+area.y)+"px";
				this.tagFriendListPopup.showAnimated();
			};
			list.addEventListener("click", (ev)=>{
				var target=ev.target as HTMLElement;
				if(target.qs(".unsupported"))
					return;
				this.addTag(target.dataset.userId, null);
			});
			ajaxGet("/photos/friendListForTagging", (r)=>{
				list.innerHTML=r;
			}, (err)=>new MessageBox(lang("error"), err, lang("close")).show(), "text");
		}
		this.tagAreaSelector.reset();
		this.tagFriendListPopup.hide();
		this.layerBackThing.hide();
	}

	private doneTagging(){
		if(!this.tagging)
			return;
		this.tagging=false;
		this.tagTopBar.remove();
		this.tagTopBar=null;
		this.bottomPart.qs(".actionList").show();
		this.tagsWrap.hide();
		if(this.photos.length>1)
			this.layerBackThing.show();
	}

	private resetCurrentTag(){
		this.tagAreaSelector.reset();
		this.tagFriendListPopup.hideAnimated();
	}

	private addTag(userID:string, name:string){
		var area=this.tagAreaSelector.getSelectedArea();
		var w=this.tagsWrap.offsetWidth;
		var h=this.tagsWrap.offsetHeight;
		var rect=[area.x/w, area.y/h, (area.x+area.w)/w, (area.y+area.h)/h];
		this.tagFriendListSubmit.classList.add("loading");
		ajaxGetAndApplyActions(`/photos/${this.photos[this.currentIndex].id}/addTag?csrf=${userConfig.csrf}&rect=${rect.join(',')}&`+(userID ? `user=${userID}` : `name=${encodeURIComponent(name)}`), ()=>{
			this.tagFriendListSubmit.classList.remove("loading");
			this.resetCurrentTag();
		}, ()=>{
			this.tagFriendListSubmit.classList.remove("loading");
		});
	}

	private highlightTag(_rect:string){
		this.tagsWrap.show();
		this.tagHighlight.showAnimated();
		var w=this.tagsWrap.offsetWidth;
		var h=this.tagsWrap.offsetHeight;
		var rect=_rect.split(",");
		var x=Math.round(parseFloat(rect[0])*w);
		var y=Math.round(parseFloat(rect[1])*h);
		var tw=Math.round(parseFloat(rect[2])*w)-x;
		var th=Math.round(parseFloat(rect[3])*h)-y;

		this.tagHighlightSvgRect.setAttribute("x", x.toString());
		this.tagHighlightSvgRect.setAttribute("y", y.toString());
		this.tagHighlightSvgRect.setAttribute("width", tw.toString());
		this.tagHighlightSvgRect.setAttribute("height", th.toString());

		this.tagHighlightBorder.style.left=x+"px";
		this.tagHighlightBorder.style.top=y+"px";
		this.tagHighlightBorder.style.width=(tw+2)+"px";
		this.tagHighlightBorder.style.height=(th+2)+"px";
	}

	private hideTagHighlight(){
		this.tagHighlight.hideAnimated({keyframes: [{opacity: 1}, {opacity: 0}], options: {duration: 200, easing: "ease"}}, ()=>{
			if(!this.tagging)
				this.tagsWrap.hide();
		});
	}
}
