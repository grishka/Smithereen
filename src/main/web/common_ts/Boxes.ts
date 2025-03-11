class LayerManager{
	private static instance:LayerManager;
	private static mediaInstance:LayerManager;
	private static pageScrollLockCount:number=0;

	static getInstance():LayerManager{
		if(!LayerManager.instance){
			LayerManager.instance=new LayerManager(100, true);
		}
		return LayerManager.instance;
	}

	static getMediaInstance():LayerManager{
		if(!LayerManager.mediaInstance){
			LayerManager.mediaInstance=new LayerManager(90, false);
		}
		return LayerManager.mediaInstance;
	}

	private scrim:HTMLDivElement;
	private layerContainer:HTMLDivElement;
	private stack:BaseLayer[]=[];
	private escapeKeyListener=(ev:KeyboardEvent)=>{
		if(ev.keyCode==27 && !this.hiddenTemporarily){
			var focusedEl=document.activeElement;
			if(!focusedEl || (focusedEl.tagName!="INPUT" && focusedEl.tagName!="TEXTAREA")){
				this.lastEscKeyEvent=ev;
				this.maybeDismissTopLayer();
			}
		}
	};
	private boxLoader:HTMLDivElement;
	private animatingHide:boolean=false;
	private hideAnimCanceled:boolean=false;
	private hiddenTemporarily:boolean=false;
	private lastEscKeyEvent:KeyboardEvent;
	private baseZIndex:number;

	private constructor(baseZIndex:number, isDefaultInstance:boolean){
		this.baseZIndex=baseZIndex;
		this.scrim=ce("div", {className: "layerScrim"});
		this.scrim.style.zIndex=baseZIndex.toString();
		this.scrim.hide();
		document.body.appendChild(this.scrim);

		if(isDefaultInstance){
			this.boxLoader=ce("div", {id: "boxLoader"}, [ce("div")]);
			this.boxLoader.style.zIndex=baseZIndex.toString();
			this.boxLoader.hide();
			document.body.appendChild(this.boxLoader);
		}

		var container:HTMLDivElement=ce("div", {className: "layerContainer"});
		container.style.zIndex=(baseZIndex+1).toString();
		container.hide();
		this.layerContainer=container;
		document.body.appendChild(container);

		window.addEventListener("resize", this.onWindowResize.bind(this));
	}

	public show(layer:BaseLayer):void{
		if(this.animatingHide){
			this.hideAnimCanceled=true;
			this.layerContainer.innerHTML="";
		}
		if(this.hiddenTemporarily){
			this.unhide();
			layer.hideContainerAfterDismiss=true;
		}
		var layerContent:HTMLElement=layer.getContent();
		this.layerContainer.appendChild(layerContent);
		if(this.stack.length==0){
			if(layer.wantsScrim())
				this.scrim.showAnimated();
			this.layerContainer.show();
			layerContent.showAnimated(layer.getCustomAppearAnimation());
			document.body.addEventListener("keydown", this.escapeKeyListener);
			this.lockPageScroll();
		}else{
			var prevLayer:BaseLayer=this.stack[this.stack.length-1];
			prevLayer.getContent().hide();
			prevLayer.onHidden();
		}
		layerContent.addEventListener("click", (ev:MouseEvent)=>{
			if(ev.target==layerContent){
				this.maybeDismissTopLayer();
			}
		});
		if(layer.wantsDarkerScrim()){
			this.scrim.classList.add("darker")
		}else{
			this.scrim.classList.remove("darker")
		}
		this.stack.push(layer);
		layer.onShown();
		if(this.boxLoader)
			this.boxLoader.hideAnimated();
		layer.updateTopOffset();
		if(this==LayerManager.mediaInstance && LayerManager.instance && LayerManager.instance.stack.length){
			LayerManager.instance.hideTemporarily();
		}
	}

	public dismiss(layer:BaseLayer):void{
		var i=this.stack.indexOf(layer);
		if(i==-1)
			return;
		var layerContent:HTMLElement=layer.getContent();
		if(isVisible(layerContent)){
			layer.onHidden();
		}
		for(var callback of layer.dismissCallbacks){
			callback();
		}
		if(i==this.stack.length-1){
			this.stack.pop();
			if(this.stack.length){
				var newLayer=this.stack[this.stack.length-1];
				newLayer.getContent().show();
				newLayer.updateTopOffset();
				newLayer.onShown();
				if(newLayer.wantsDarkerScrim()){
					this.scrim.classList.add("darker")
				}else{
					this.scrim.classList.remove("darker")
				}
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
				this.animatingHide=true;
				layerContent.hideAnimated(anim, ()=>{
					if(this.hideAnimCanceled){
						this.hideAnimCanceled=false;
					}else{
						this.layerContainer.removeChild(layerContent);
						this.layerContainer.hide();
						this.unlockPageScroll();
					}
					this.scrim.classList.remove("darker");
					this.animatingHide=false;
				});
			}else{
				this.layerContainer.removeChild(layerContent);
				this.layerContainer.hide();

				this.unlockPageScroll();
			}
			this.scrim.hideAnimated({keyframes: [{opacity: 1}, {opacity: 0}], options: {duration: duration, easing: "ease"}});
			if(this==LayerManager.mediaInstance && LayerManager.instance && LayerManager.instance.stack.length){
				LayerManager.instance.unhide();

			}
		}else{
			this.layerContainer.removeChild(layerContent);
		}
		if(layer.hideContainerAfterDismiss){
			this.hideTemporarily();
		}
	}

	private hideTemporarily(){
		if(this.hiddenTemporarily)
			return;
		this.layerContainer.hideAnimated();
		this.scrim.hideAnimated();
		this.hiddenTemporarily=true;
	}

	private unhide(){
		if(!this.hiddenTemporarily)
			return;
		this.layerContainer.showAnimated();
		this.scrim.showAnimated();
		this.hiddenTemporarily=false;
	}

	private maybeDismissTopLayer():void{
		if(this==LayerManager.mediaInstance && LayerManager.instance && LayerManager.instance.stack.length && (!LayerManager.instance.hiddenTemporarily || LayerManager.instance.lastEscKeyEvent==this.lastEscKeyEvent))
			return;
		var topLayer=this.stack[this.stack.length-1];
		if(topLayer.allowDismiss())
			topLayer.dismiss();
	}

	public getTopLayer():BaseLayer{
		if(this.stack.length==0)
			return null;
		return this.stack[this.stack.length-1];
	}

	private lockPageScroll(){
		if(LayerManager.pageScrollLockCount++==0){
			var scrollbarW=window.innerWidth-document.body.clientWidth;
			document.body.style.top = `-${window.scrollY}px`;
			document.body.style.position="fixed";
			document.body.style.paddingRight=scrollbarW+"px";
			ge("wrap").classList.add("scrollLocked");
		}
	}

	private unlockPageScroll(){
		if(--LayerManager.pageScrollLockCount==0){
			var scrollY=document.body.style.top;
			document.body.style.position="";
			document.body.style.top="";
			document.body.style.paddingRight="";
			ge("wrap").classList.remove("scrollLocked");
			window.scrollTo(0, parseInt(scrollY || '0') * -1);
		}
	}

	public showBoxLoader(){
		this.updateTopOffset(this.boxLoader);
		this.boxLoader.style.zIndex=(this.stack.length ? (this.baseZIndex+2) : this.baseZIndex).toString();
		this.boxLoader.showAnimated();
	}

	public updateTopOffset(el:HTMLElement){
		if(mobile)
			return;
		var layer=el.children[0] as HTMLElement;
		var height=layer.offsetHeight;
		layer.style.marginTop=Math.round(Math.max(0, (window.innerHeight-height)/3-10))+"px";
	}

	private onWindowResize(ev:Event){
		for(var layer of this.stack){
			layer.onWindowResize();
		}
		this.updateAllTopOffsets();
	}

	public showSnackbar(text:string){
		var snackbar=ce("div", {className: "snackbarWrap"}, [
			ce("div", {className: "snackbar", innerHTML: text})
		]);
		document.body.appendChild(snackbar);
		this.updateTopOffset(snackbar);
		if(this.boxLoader){
			this.boxLoader.hideAnimated();
		}
		setTimeout(()=>{
			snackbar.hideAnimated({keyframes: [{opacity: 1}, {opacity: 0}], options: {duration: 500, easing: "ease"}}, ()=>{
				snackbar.remove();
			});
		}, 2000);
	}

	public updateAllTopOffsets(){
		if(this.boxLoader){
			this.updateTopOffset(this.boxLoader);
		}
		if(this.stack.length){
			this.stack[this.stack.length-1].updateTopOffset();
		}
	}

	public dismissByID(id:string){
		for(var layer of this.stack){
			if(layer.id==id){
				layer.dismiss();
				return true;
			}
		}
		return false;
	}

	public dismissEverything(){
		for(var i=this.stack.length-1;i>=0;i--){
			this.dismiss(this.stack[i]);
		}
	}
}

abstract class BaseLayer{
	private content:HTMLElement;
	public dismissCallbacks:{():void}[]=[];
	public id:string;
	public hideContainerAfterDismiss:boolean=false;

	protected abstract onCreateContentView():HTMLElement;
	public show():void{
		if(!this.content){
			this.content=ce("div", {className: "layerContent"});
			var contentView:HTMLElement=this.onCreateContentView();
			this.content.appendChild(contentView);
		}
		this.getLayerManager().show(this);
	}

	public dismiss():void{
		this.getLayerManager().dismiss(this);
	}

	public getContent():HTMLElement{
		return this.content;
	}

	public allowDismiss():boolean{
		return true;
	}

	public updateTopOffset(){
		this.getLayerManager().updateTopOffset(this.content);
	}

	public onShown():void{}
	public onHidden():void{}
	public onWindowResize():void{}
	public wantsDarkerScrim():boolean{
		return false;
	}
	public wantsScrim():boolean{
		return true;
	}
	public getCustomDismissAnimation():AnimationDescription{
		return {keyframes: [{opacity: 1}, {opacity: 0}], options: {duration: 200, easing: "ease"}};
	}
	public getCustomAppearAnimation():AnimationDescription{
		return {keyframes: [{opacity: 0}, {opacity: 1}], options: {duration: 200, easing: "ease"}};
	}
	public getLayerManager():LayerManager{
		return LayerManager.getInstance();
	}
}

abstract class BaseMediaViewerLayer extends BaseLayer{
	protected historyEntryAdded:boolean;
	private popStateListener=this.onPopState.bind(this);
	private addListenerTimeout:number;

	public constructor(historyEntryAdded:boolean){
		super();
		this.historyEntryAdded=historyEntryAdded;
	}

	public getLayerManager(){
		return LayerManager.getMediaInstance();
	}

	protected updateHistory(state:any, url:string){
		if(this.historyEntryAdded){
			window.history.replaceState(state, "", url);
		}else{
			window.history.pushState(state, "", url);
			this.historyEntryAdded=true;
		}
	}

	public onShown(){
		super.onShown();
		this.addListenerTimeout=setTimeout(()=>{
			window.addEventListener("popstate", this.popStateListener, false);
			this.addListenerTimeout=null;
		}, 300);
	}

	public onHidden(){
		super.onHidden();
		if(this.addListenerTimeout){
			clearTimeout(this.addListenerTimeout);
			this.addListenerTimeout=null;
		}
		window.removeEventListener("popstate", this.popStateListener);
	}

	public dismiss(){
		super.dismiss();
		if(this.historyEntryAdded){
			ignoreNextPopState=true;
			window.history.back();
		}
	}

	private onPopState(ev:PopStateEvent){
		this.historyEntryAdded=false;
		this.dismiss();
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
	protected onDismissListener:{():void;};
	protected buttonBarLoader:HTMLDivElement;
	protected buttons:HTMLElement[];

	public constructor(title:string, buttonTitles:string[]=[], onButtonClick:{(index:number):void;}=null){
		super();
		this.buttons=[];
		this.title=title;
		this.buttonTitles=buttonTitles;
		this.onButtonClick=onButtonClick;

		var contentWrap:HTMLDivElement=ce("div", {className: "boxContent"});
		this.contentWrap=contentWrap;
	}

	protected onCreateContentView():HTMLElement{
		var content:HTMLDivElement=ce("div", {className: "boxLayer"}, [
			ce("div", {className: "boxLayerInner"}, [
				this.titleBar=ce("div", {className: "boxTitleBar"}, [
					ce("span", {className: "title ellipsize", innerText: this.title})
				]),
				this.getRawContentWrap(),
				this.buttonBar=ce("div", {className: "boxButtonBar"})
			])
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

	protected getRawContentWrap():HTMLElement{
		return this.contentWrap;
	}

	public setContent(content:HTMLElement):void{
		if(this.contentWrap.hasChildNodes){
			for(var i=0;i<this.contentWrap.children.length;i++)
				this.contentWrap.firstChild.remove();
		}
		this.contentWrap.appendChild(content);
		if(this.boxLayer){
			LayerManager.getInstance().updateTopOffset(this.getContent());
		}
	}

	public getButton(index:number):HTMLElement{
		return this.buttons[index];
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
		for(var button of this.buttons){
			button.remove();
		}
		this.buttons=[];
		if(this.buttonTitles.length){
			this.buttonBar.show();
			for(var i:number=0;i<this.buttonTitles.length;i++){
				var btn:HTMLButtonElement=ce("button", {type: "button"});
				btn.innerText=this.buttonTitles[i];
				if(i>0 || this.noPrimaryButton){
					btn.className="secondary";
				}
				if(this.onButtonClick){
					btn.onclick=this.onButtonClick.bind(this, i);
				}else{
					btn.onclick=this.dismiss.bind(this);
				}
				this.buttonBar.appendChild(btn);
				this.buttons.push(btn);
			}
		}else{
			this.buttonBar.hide();
		}
	}

	public dismiss(){
		super.dismiss();
		if(this.onDismissListener)
			this.onDismissListener();
	}

	public setOnDismissListener(listener:{():void}){
		this.onDismissListener=listener;
	}

	public showButtonLoading(index:number, loading:boolean){
		if(mobile){
			var cl=this.getButton(index).classList;
			if(loading)
				cl.add("loading");
			else
				cl.remove("loading");
		}else{
			if(loading){
				if(!this.buttonBarLoader){
					this.buttonBarLoader=ce("div", {className: "buttonBarAux"}, [ce("div", {className: "loader flL"})]);
					if(this.buttonBar.firstChild)
						this.buttonBar.insertBefore(this.buttonBarLoader, this.buttonBar.firstChild);
					else
						this.buttonBar.appendChild(this.buttonBarLoader);
				}else{
					this.buttonBarLoader.show();
				}
			}else if(this.buttonBarLoader){
				this.buttonBarLoader.hide();
			}
		}
	}

	public addButtonBarAuxHTML(html:string){
		var aux=ce("div", {className: "buttonBarAux", innerHTML: html});
		this.buttonBar.insertBefore(aux, this.buttonBar.firstChild);
	}
}

class BoxWithoutContentPadding extends Box{
	public constructor(title:string, buttonTitles:string[]=[], onButtonClick:{(index:number):void;}=null){
		super(title, buttonTitles, onButtonClick);
		this.contentWrap.style.padding="0";
	}
}

class BaseScrollableBox extends BoxWithoutContentPadding{
	private scrollAtTop:boolean=true;
	private scrollAtBottom:boolean=false;
	private contentWrapWrap:HTMLDivElement;
	private scrollableEl:HTMLElement;
	public constructor(title:string, buttonTitles:string[]=[], onButtonClick:{(index:number):void;}=null){
		super(title, buttonTitles, onButtonClick);
	}

	protected wrapScrollableElement(e:HTMLElement):HTMLElement{
		this.scrollableEl=e;
		e.addEventListener("scroll", this.onContentScroll.bind(this), {passive: true});
		var shadowTop;

		this.contentWrapWrap=ce("div", {className: "scrollableShadowWrap scrollAtTop"}, [
			shadowTop=ce("div", {className: "shadowTop"}),
			ce("div", {className: "shadowBottom"})
		]);
		// e.insertBefore(this.contentWrapWrap, e);
		this.contentWrapWrap.insertBefore(e, shadowTop);
		return this.contentWrapWrap;
	}

	protected onContentScroll(e:Event){
		var atTop=this.scrollableEl.scrollTop==0;
		var atBottom=this.scrollableEl.scrollTop>=this.scrollableEl.scrollHeight-this.scrollableEl.offsetHeight;
		if(this.scrollAtTop!=atTop){
			this.scrollAtTop=atTop;
			if(atTop)
				this.contentWrapWrap.classList.add("scrollAtTop");
			else
				this.contentWrapWrap.classList.remove("scrollAtTop");
		}
		if(this.scrollAtBottom!=atBottom){
			this.scrollAtBottom=atBottom;
			if(atBottom)
				this.contentWrapWrap.classList.add("scrollAtBottom");
			else
				this.contentWrapWrap.classList.remove("scrollAtBottom");
		}
	}

	public onShown(){
		super.onShown();
		this.onContentScroll(null);
	}
}

class ScrollableBox extends BaseScrollableBox{
	public constructor(title:string, buttonTitles:string[]=[], onButtonClick:{(index:number):void;}=null){
		super(title, buttonTitles, onButtonClick);
	}

	protected onCreateContentView():HTMLElement{
		var cont=super.onCreateContentView();
		cont.classList.add("scrollable");
		return cont;
	}

	protected getRawContentWrap():HTMLElement{
		return this.wrapScrollableElement(this.contentWrap);
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
				if(this.form.reportValidity()){
					var btn=this.getButton(0);
					btn.setAttribute("disabled", "");
					this.getButton(1).setAttribute("disabled", "");
					this.showButtonLoading(0, true);
					ajaxSubmitForm(this.form, (resp)=>{
						if(resp){
							this.dismiss();
						}else{
							var btn=this.getButton(0);
							btn.removeAttribute("disabled");
							this.getButton(1).removeAttribute("disabled");
							this.showButtonLoading(0, false);
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

	protected onCreateContentView():HTMLElement{
		var cont=super.onCreateContentView();
		var textareas=cont.querySelectorAll("textarea").unfuck();
		for(var ta of textareas){
			autoSizeTextArea(ta as HTMLTextAreaElement);
		}
		return cont;
	}
}

class SimpleLayer extends BaseLayer{
	protected contentWrap:HTMLElement;

	public constructor(innerHTML:string="", addClasses:string=""){
		super();
		this.contentWrap=ce("div", {className: ("simpleLayer "+addClasses).trim(), innerHTML: innerHTML});
	}

	public onCreateContentView():HTMLElement{
		return this.contentWrap;
	}
}

abstract class FileUploadLayer extends BaseLayer{
	protected contentWrap:HTMLElement;
	protected fileField:HTMLInputElement;
	protected dragOverlay:HTMLElement;
	protected acceptMultiple:boolean=false;
	protected layerContent:HTMLElement;
	protected titleEl:HTMLElement;
	protected message:string;

	public constructor(title:string, message:string=null){
		super();
		this.contentWrap=ce("div", {className: "layerWithTitle simpleLayer"}, [
			ce("div", {className: "layerTitle"}, [
				this.titleEl=ce("div", {className: "title ellipsize", innerText: title}),
				ce("a", {className: "close", href: "#", onclick: ()=>{this.dismiss(); return false;}, innerText: lang("close")})
			])
		]);
		if(!message) message=lang(mobile ? "choose_file_mobile" : "drag_or_choose_file");
		this.message=message;
		this.resetContent();
	}

	public onCreateContentView():HTMLElement{
		return this.contentWrap;
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

	public setContent(content:HTMLElement){
		if(this.layerContent)
			this.layerContent.remove();
		this.layerContent=content;
		this.contentWrap.appendChild(content);
	}

	public resetContent(){
		var content:HTMLDivElement=ce("div", {className: "fileUploadBoxContent", innerHTML:
			`<div class="inner">${this.message}<br/>
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

		this.contentWrap.appendChild(content);
		this.fileField=content.qs("input[type=file]");

		this.fileField.addEventListener("change", (ev:Event)=>{
			this.handleFiles(this.fileField.files);
			this.fileField.form.reset();
		});
		this.setContent(content);
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
			}else{
				attrs.href="javascript:void(0)";
			}
			if(opt.id)
				attrs.id=opt.id;
			var link:HTMLAnchorElement;
			list.appendChild(ce("li", {}, [
				link=ce("a", attrs)
			]));
			link.addEventListener("click", (ev:Event)=>{
				if(opt.type=="confirm"){
					ajaxConfirm(opt.title, opt.msg, opt.url);
				}else if(opt.ajax){
					if(opt.ajax=="box"){
						LayerManager.getInstance().showBoxLoader();
					}
					ajaxGetAndApplyActions(link.href);
					ev.preventDefault();
				}else if(opt.onclick){
					opt.onclick();
				}
				this.dismiss();
			}, false);
		});
		this.setContent(content);
	}
}

