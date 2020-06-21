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
		if(this.stack.length==0){
			this.scrim.showAnimated();
			this.layerContainer.show();
			document.body.addEventListener("keydown", this.escapeKeyListener);
			document.body.style.overflowY="hidden";
		}else{
			var prevLayer:BaseLayer=this.stack[this.stack.length-1];
			prevLayer.getContent().hide();
			prevLayer.onHidden();
		}
		this.stack.push(layer);
		var layerContent:HTMLElement=layer.getContent();
		this.layerContainer.appendChild(layerContent);
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
			this.scrim.hideAnimated();
				document.body.removeEventListener("keydown", this.escapeKeyListener);
			var anim=layer.getCustomDismissAnimation();
			if(anim){
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
	public getCustomDismissAnimation():string{return null;}
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

		if(this.closeable){
			this.closeButton=ce("span", {className: "close", title: lang("close"), onclick: ()=>this.dismiss()});
			this.titleBar.appendChild(this.closeButton);
		}
		this.updateButtonBar();

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

	public getCustomDismissAnimation():string{
		return "boxDisappear 0.2s";
	}

	private updateButtonBar():void{
		if(this.buttonTitles.length){
			this.buttonBar.show();
			while(this.buttonBar.firstChild)
				this.buttonBar.lastChild.remove();
			for(var i:number=0;i<this.buttonTitles.length;i++){
				var btn:HTMLInputElement=ce("input", {type: "button", value: this.buttonTitles[i]});
				if(i>0){
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
				var btn=this.getButton(0);
				btn.setAttribute("disabled", "");
				this.getButton(1).setAttribute("disabled", "");
				btn.classList.add("loading");
				setGlobalLoading(true);
				ajaxSubmitForm(this.form, this.dismiss.bind(this));
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
		if(!message) message=lang("drag_or_choose_file");
		var content:HTMLDivElement=ce("div", {className: "fileUploadBoxContent", innerHTML:
			`<div class="inner">${message}<br/>
				<form>
					<input type="file" id="fileUploadBoxInput" accept="image/*"/>
					<label for="fileUploadBoxInput" class="button">${lang("choose_file")}</label>
				</form>
			</div>
			<div class="dropOverlay">${lang("drop_files_here")}</div>`
		});

		this.setContent(content);
		this.fileField=content.qs("input[type=file]");
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
		this.fileField.addEventListener("change", function(ev:Event){
			this.handleFiles(this.fileField.files);
			this.fileField.form.reset();
		}.bind(this), false);
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

	public constructor(){
		super(lang("update_profile_picture"));
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

		ajaxUpload("/settings/updateProfilePicture?x1="+x1+"&y1="+y1+"&x2="+x2+"&y2="+y2, "pic", this.file, (resp:any)=>{
			this.dismiss();
			setGlobalLoading(false);
			return false;
		});
	}
}
