class LayerManager{
	private static instance:LayerManager;
	static getInstance():LayerManager{
		if(!LayerManager.instance){
			LayerManager.instance=new LayerManager();
		}
		return LayerManager.instance;
	}

	private scrim:HTMLDivElement;
	private layerContainer:HTMLDivElement;
	private stack:BaseLayer[]=[];
	private escapeKeyListener=(ev:KeyboardEvent)=>{
		if(ev.keyCode==27){
			this.maybeDismissTopLayer();
		}
	};

	private constructor(){
		this.scrim=ce("div", {id: "layerScrim", onclick: ()=>{
			this.maybeDismissTopLayer();
		}});
		this.scrim.hide();
		document.body.appendChild(this.scrim);

		var container:HTMLDivElement=ce("div", {id: "layerContainer"});
		container.hide();
		this.layerContainer=container;
		document.body.appendChild(container);
	}

	public show(layer:BaseLayer):void{
		var layerContent:HTMLElement=layer.getContent();
		this.layerContainer.appendChild(layerContent);
		if(this.stack.length==0){
			this.scrim.showAnimated();
			this.layerContainer.show();
			layerContent.showAnimated(layer.getCustomAppearAnimation());
			document.body.addEventListener("keydown", this.escapeKeyListener);
			document.body.style.overflowY="hidden";
		}else{
			var prevLayer:BaseLayer=this.stack[this.stack.length-1];
			prevLayer.getContent().hide();
			prevLayer.onHidden();
		}
		this.stack.push(layer);
		layer.onShown();
	}

	public dismiss(layer:BaseLayer):void{
		var i=this.stack.indexOf(layer);
		if(i==-1)
			return;
		var layerContent:HTMLElement=layer.getContent();
		if(isVisible(layerContent)){
			layer.onHidden();
		}
		if(i==this.stack.length-1){
			this.stack.pop();
			if(this.stack.length){
				var newLayer=this.stack[this.stack.length-1];
				newLayer.getContent().show();
				newLayer.onShown();
			}
		}else{
			this.stack.splice(i, 1);
		}
		if(this.stack.length==0){
			document.body.removeEventListener("keydown", this.escapeKeyListener);
			var anim=layer.getCustomDismissAnimation();
			var duration=200;
			if(anim){
				duration=(anim.options as KeyframeAnimationOptions).duration as number;
				layerContent.hideAnimated(anim, ()=>{
					this.layerContainer.removeChild(layerContent);
					this.layerContainer.hide();
					document.body.style.overflowY="";
				});	
			}else{
				this.layerContainer.removeChild(layerContent);
				this.layerContainer.hide();
				document.body.style.overflowY="";
			}
			this.scrim.hideAnimated({keyframes: [{opacity: 1}, {opacity: 0}], options: {duration: duration, easing: "ease"}});
		}else{
			this.layerContainer.removeChild(layerContent);
		}
	}

	private maybeDismissTopLayer():void{
		var topLayer=this.stack[this.stack.length-1];
		if(topLayer.allowDismiss())
			this.dismiss(topLayer);
	}
}

abstract class BaseLayer{
	private content:HTMLElement;

	protected abstract onCreateContentView():HTMLElement;
	public show():void{
		if(!this.content){
			this.content=ce("div", {className: "layerContent"});
			var contentView:HTMLElement=this.onCreateContentView();
			this.content.appendChild(contentView);
		}
		LayerManager.getInstance().show(this);
	}

	public dismiss():void{
		LayerManager.getInstance().dismiss(this);
	}

	public getContent():HTMLElement{
		return this.content;
	}

	public allowDismiss():boolean{
		return true;
	}

	public onShown():void{}
	public onHidden():void{}
	public getCustomDismissAnimation():AnimationDescription{
		return {keyframes: [{opacity: 1}, {opacity: 0}], options: {duration: 200, easing: "ease"}};
	}
	public getCustomAppearAnimation():AnimationDescription{
		return {keyframes: [{opacity: 0}, {opacity: 1}], options: {duration: 200, easing: "ease"}};
	}
}

class Box extends BaseLayer{

	protected title:string;
	protected buttonTitles:string[];
	protected onButtonClick:{(index:number):void;};

	protected titleBar:HTMLDivElement;
	protected buttonBar:HTMLDivElement;
	protected contentWrap:HTMLDivElement;
	protected closeButton:HTMLElement;
	protected closeable:boolean=true;
	protected boxLayer:HTMLElement;
	protected noPrimaryButton:boolean=false;

	public constructor(title:string, buttonTitles:string[]=[], onButtonClick:{(index:number):void;}=null){
		super();
		this.title=title;
		this.buttonTitles=buttonTitles;
		this.onButtonClick=onButtonClick;

		var contentWrap:HTMLDivElement=ce("div", {className: "boxContent"});
		this.contentWrap=contentWrap;
	}

	protected onCreateContentView():HTMLElement{
		var content:HTMLDivElement=ce("div", {className: "boxLayer"}, [
			this.titleBar=ce("div", {className: "boxTitleBar", innerText: this.title}),
			this.contentWrap,
			this.buttonBar=ce("div", {className: "boxButtonBar"})
		]);
		if(!this.title) this.titleBar.hide();

		if(this.closeable){
			this.closeButton=ce("span", {className: "close", title: lang("close"), onclick: ()=>this.dismiss()});
			this.titleBar.appendChild(this.closeButton);
		}
		this.updateButtonBar();
		this.boxLayer=content;

		return content;
	}

	public setContent(content:HTMLElement):void{
		if(this.contentWrap.hasChildNodes){
			for(var i=0;i<this.contentWrap.children.length;i++)
				this.contentWrap.firstChild.remove();
		}
		this.contentWrap.appendChild(content);
	}

	public getButton(index:number):HTMLElement{
		return this.buttonBar.children[index] as HTMLElement;
	}

	public setButtons(buttonTitles:string[], onButtonClick:{(index:number):void;}){
		this.buttonTitles=buttonTitles;
		this.onButtonClick=onButtonClick;
		this.updateButtonBar();
	}

	public setCloseable(closeable:boolean):void{
		this.closeable=closeable;
	}

	public allowDismiss():boolean{
		return this.closeable;
	}

	public getCustomDismissAnimation(){
		if(mobile){
			var height=this.boxLayer.offsetHeight+32;
			return {
				keyframes: [{transform: "translateY(0)"}, {transform: "translateY(100%)"}],
				options: {duration: 300, easing: "cubic-bezier(0.32, 0, 0.67, 0)"}
			};
		}
		return {
			keyframes: [{opacity: 1, transform: "scale(1)"}, {opacity: 0, transform: "scale(0.95)"}],
			options: {duration: 200, easing: "ease"}
		};
	}

	public getCustomAppearAnimation(){
		if(mobile){
			var height=this.boxLayer.offsetHeight+32;
			console.log("height "+height);
			return {
				keyframes: [{transform: "translateY("+height+"px)"}, {transform: "translateY(0)"}],
				options: {duration: 600, easing: "cubic-bezier(0.22, 1, 0.36, 1)"}
			};
		}
		return {
			keyframes: [{opacity: 0, transform: "scale(0.9)"}, {opacity: 1, transform: "scale(1)"}],
			options: {duration: 300, easing: "ease"}
		};
	}

	private updateButtonBar():void{
		if(this.buttonTitles.length){
			this.buttonBar.show();
			while(this.buttonBar.firstChild)
				this.buttonBar.lastChild.remove();
			for(var i:number=0;i<this.buttonTitles.length;i++){
				var btn:HTMLInputElement=ce("input", {type: "button", value: this.buttonTitles[i]});
				if(i>0 || this.noPrimaryButton){
					btn.className="secondary";
				}
				if(this.onButtonClick){
					btn.onclick=this.onButtonClick.bind(this, i);
				}else{
					btn.onclick=this.dismiss.bind(this);
				}
				this.buttonBar.appendChild(btn);
			}
		}else{
			this.buttonBar.hide();
		}
	}
}

class BoxWithoutContentPadding extends Box{
	public constructor(title:string, buttonTitles:string[]=[], onButtonClick:{(index:number):void;}=null){
		super(title, buttonTitles, onButtonClick);
		this.contentWrap.style.padding="0";
	}
}

class ConfirmBox extends Box{
	public constructor(title:string, msg:string, onConfirmed:{():void}){
		super(title, [lang("yes"), lang("no")], function(idx:number){
			if(idx==0){
				onConfirmed();
			}else{
				this.dismiss();
			}
		});
		var content:HTMLDivElement=ce("div", {innerHTML: msg});
		this.setContent(content);
	}
}

class MessageBox extends Box{
	public constructor(title:string, msg:string, btn:string){
		super(title, [btn]);
		var content:HTMLDivElement=ce("div", {innerHTML: msg});
		this.setContent(content);
	}
}

class FormBox extends Box{
	private form:HTMLFormElement;

	public constructor(title:string, c:string, btn:string, act:string){
		super(title, [btn, lang("cancel")], function(idx:number){
			if(idx==0){
				if(this.form.checkValidity()){
					var btn=this.getButton(0);
					btn.setAttribute("disabled", "");
					this.getButton(1).setAttribute("disabled", "");
					btn.classList.add("loading");
					ajaxSubmitForm(this.form, (resp)=>{
						if(resp){
							this.dismiss();
						}else{
							var btn=this.getButton(0);
							btn.removeAttribute("disabled");
							this.getButton(1).removeAttribute("disabled");
							btn.classList.remove("loading");
						}
					});
				}
			}else{
				this.dismiss();
			}
		});
		var content:HTMLDivElement=ce("div", {}, [
			this.form=ce("form", {innerHTML: c, action: act})
		]);
		this.setContent(content);
	}
}

abstract class FileUploadBox extends Box{

	protected fileField:HTMLInputElement;
	protected dragOverlay:HTMLElement;
	protected acceptMultiple:boolean=false;

	public constructor(title:string, message:string=null){
		super(title, [lang("cancel")], function(idx:number){
			this.dismiss();
		});
		if(!message) message=lang(mobile ? "choose_file_mobile" : "drag_or_choose_file");
		var content:HTMLDivElement=ce("div", {className: "fileUploadBoxContent", innerHTML:
			`<div class="inner">${message}<br/>
				<form>
					<input type="file" id="fileUploadBoxInput" accept="image/*"/>
					<label for="fileUploadBoxInput" class="button">${lang("choose_file")}</label>
				</form>
			</div>`
		});


		if(!mobile){
			content.innerHTML+=`<div class="dropOverlay">${lang("drop_files_here")}</div>`;
			this.dragOverlay=content.qs(".dropOverlay");
			this.dragOverlay.addEventListener("dragenter", function(ev:DragEvent){
				this.dragOverlay.classList.add("over");
			}.bind(this), false);
			this.dragOverlay.addEventListener("dragleave", function(ev:DragEvent){
				this.dragOverlay.classList.remove("over");
			}.bind(this), false);
			content.addEventListener("drop", function(ev:DragEvent){
				ev.preventDefault();
				this.dragOverlay.classList.remove("over");
				this.handleFiles(ev.dataTransfer.files);
			}.bind(this), false);
		}

		this.setContent(content);
		this.fileField=content.qs("input[type=file]");

		this.fileField.addEventListener("change", (ev:Event)=>{
			this.handleFiles(this.fileField.files);
			this.fileField.form.reset();
		});
	}

	protected abstract handleFile(file:File):void;

	protected handleFiles(files:FileList):void{
		for(var i=0;i<files.length;i++){
			var f=files[i];
			if(f.type.indexOf("image/")==0){
				this.handleFile(f);
				if(!this.acceptMultiple)
					return;
			}
		}
	}
}

class ProfilePictureBox extends FileUploadBox{

	private file:File=null;
	private areaSelector:ImageAreaSelector=null;
	private groupID:number=null;

	public constructor(groupID:number=null){
		super(lang("update_profile_picture"));
		if(mobile)
			this.noPrimaryButton=true;
		this.groupID=groupID;
	}

	protected handleFile(file:File):void{
		this.file=file;
		var objURL=URL.createObjectURL(file);

		var img=ce("img");
		img.onload=()=>{
			var ratio:number=img.naturalWidth/img.naturalHeight;
			if(ratio>2.5){
				new MessageBox(lang("error"), lang("picture_too_wide"), lang("ok")).show();
				return;
			}else if(ratio<0.25){
				new MessageBox(lang("error"), lang("picture_too_narrow"), lang("ok")).show();
				return;
			}
			var content=ce("div");
			content.innerText=lang("profile_pic_select_square_version");
			content.align="center";
			var imgWrap=ce("div");
			imgWrap.className="profilePictureBoxImgWrap";
			imgWrap.appendChild(img);
			content.appendChild(ce("br"));
			content.appendChild(imgWrap);
			this.setContent(content);
			if(mobile)
				this.noPrimaryButton=false;
			this.setButtons([lang("save"), lang("cancel")], (idx:number)=>{
				if(idx==1){
					this.dismiss();
					return;
				}
				var area=this.areaSelector.getSelectedArea();
				var contW=imgWrap.clientWidth;
				var contH=imgWrap.clientHeight;
				var x1=area.x/contW;
				var y1=area.y/contH;
				var x2=(area.x+area.w)/contW;
				var y2=(area.y+area.h)/contH;
				this.areaSelector.setEnabled(false);

				this.upload(x1, y1, x2, y2);
			});

			this.areaSelector=new ImageAreaSelector(imgWrap, true);
			var w=imgWrap.clientWidth;
			var h=imgWrap.clientHeight;
			if(w>h){
				this.areaSelector.setSelectedArea(Math.round(w/2-h/2), 0, h, h);
			}else{
				this.areaSelector.setSelectedArea(0, 0, w, w);
			}
		};
		img.onerror=function(){
			new MessageBox(lang("error"), lang("error_loading_picture"), lang("ok")).show();
		};
		img.src=objURL;
	}

	private upload(x1:number, y1:number, x2:number, y2:number):void{
		var btn=this.getButton(0);
		btn.setAttribute("disabled", "");
		this.getButton(1).setAttribute("disabled", "");
		btn.classList.add("loading");
		setGlobalLoading(true);

		ajaxUpload("/settings/updateProfilePicture?x1="+x1+"&y1="+y1+"&x2="+x2+"&y2="+y2+(this.groupID ? ("&group="+this.groupID) : ""), "pic", this.file, (resp:any)=>{
			this.dismiss();
			setGlobalLoading(false);
			return false;
		});
	}
}

class MobileOptionsBox extends Box{
	public constructor(options:any[]){
		super(null, [lang("cancel")]);
		this.noPrimaryButton=true;
		var list:HTMLElement;
		var content:HTMLDivElement=ce("div", {}, [list=ce("ul", {className: "actionList"})]);
		this.contentWrap.classList.add("optionsBoxContent");
		options.forEach((opt)=>{
			var attrs:Partial<HTMLAnchorElement>={innerText: opt.label};
			if(opt.type=="link"){
				attrs.href=opt.href;
				if(opt.target){
					attrs.target=opt.target;
					attrs.rel="noopener";
				}
			}
			attrs.onclick=()=>{
				if(opt.type=="confirm"){
					ajaxConfirm(opt.title, opt.msg, opt.url);
				}
				this.dismiss();
			};
			list.appendChild(ce("li", {}, [
				ce("a", attrs)
			]));
		});
		this.setContent(content);
	}
}

interface PhotoInfo{
	webp:string;
	jpeg:string;
	width:number;
	height:number;
}

class PhotoViewerLayer extends BaseLayer{
	
	private photos:PhotoInfo[];
	private index:number;
	private contentWrap:HTMLDivElement;
	private photoImage:HTMLImageElement;
	private photoPicture:HTMLPictureElement;
	private photoSourceWebp:HTMLSourceElement;

	private arrowsKeyListener=(ev:KeyboardEvent)=>{
		if(ev.keyCode==37){
			this.showPreviousPhoto();
		}else if(ev.keyCode==39){
			this.showNextPhoto();
		}
	};

	public constructor(photos:PhotoInfo[], index:number){
		super();

		this.photos=photos;
		this.contentWrap=ce("div", {className: "photoViewer"}, [
			ce("a", {className: "photoViewerNavButton buttonPrev", onclick: this.showPreviousPhoto.bind(this)}),
			ce("div", {className: "photoWrap"}, [
				this.photoPicture=ce("picture", {}, [
					this.photoSourceWebp=ce("source", {type: "image/webp"}),
					this.photoImage=ce("img")
				])
			]),
			ce("a", {className: "photoViewerNavButton buttonNext", onclick: this.showNextPhoto.bind(this)})
		]);
		this.setCurrentPhotoIndex(index);
	}

	public setCurrentPhotoIndex(i:number){
		this.index=i;
		var ph=this.photos[this.index];
		this.photoImage.width=ph.width;
		this.photoImage.height=ph.height;
		this.photoSourceWebp.srcset=ph.webp;
		this.photoImage.src=ph.jpeg;
	}

	public showNextPhoto(){
		this.setCurrentPhotoIndex((this.index+1)%this.photos.length);
	}

	public showPreviousPhoto(){
		this.setCurrentPhotoIndex(this.index==0 ? this.photos.length-1 : this.index-1);
	}

	protected onCreateContentView():HTMLElement{
		return this.contentWrap;
	}

	public onShown(){
		document.body.addEventListener("keydown", this.arrowsKeyListener);
	}

	public onHidden(){
		document.body.removeEventListener("keydown", this.arrowsKeyListener);
	}
}
