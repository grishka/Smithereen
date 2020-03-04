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

	private constructor(){
		this.scrim=document.createElement("div");
		this.scrim.id="layerScrim";
		hide(this.scrim);
		document.body.appendChild(this.scrim);

		var container:HTMLDivElement=document.createElement("div");
		container.id="layerContainer";
		hide(container);
		this.layerContainer=container;
		document.body.appendChild(container);
	}

	public show(layer:BaseLayer):void{
		if(this.stack.length==0){
			show(this.scrim);
			show(this.layerContainer);
		}else{
			var prevLayer:BaseLayer=this.stack[this.stack.length-1];
			hide(prevLayer.getContent());
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
		this.layerContainer.removeChild(layerContent);
		if(i==this.stack.length-1){
			this.stack.pop();
			if(this.stack.length){
				var newLayer=this.stack[this.stack.length-1];
				show(newLayer.getContent());
				newLayer.onShown();
			}
		}else{
			this.stack.splice(i, 1);
		}
		if(this.stack.length==0){
			hide(this.scrim);
			hide(this.layerContainer);
		}
	}
}

abstract class BaseLayer{
	private content:HTMLElement;

	protected abstract onCreateContentView():HTMLElement;
	public show():void{
		if(!this.content){
			this.content=document.createElement("div");
			this.content.className="layerContent";
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

	public onShown():void{}
	public onHidden():void{}
}

class Box extends BaseLayer{

	protected title:string;
	protected buttonTitles:string[];
	protected onButtonClick:{(index:number):void;};

	protected titleBar:HTMLDivElement;
	protected buttonBar:HTMLDivElement;
	protected contentWrap:HTMLDivElement;

	public constructor(title:string, buttonTitles:string[]=[], onButtonClick:{(index:number):void;}=null){
		super();
		this.title=title;
		this.buttonTitles=buttonTitles;
		this.onButtonClick=onButtonClick;

		var contentWrap:HTMLDivElement=document.createElement("div");
		contentWrap.className="boxContent";
		this.contentWrap=contentWrap;
	}

	protected onCreateContentView():HTMLElement{
		var content:HTMLDivElement=document.createElement("div");
		content.className="boxLayer";

		var titleBar:HTMLDivElement=document.createElement("div");
		titleBar.innerText=this.title;
		titleBar.className="boxTitleBar";
		this.titleBar=titleBar;
		content.appendChild(titleBar);

		content.appendChild(this.contentWrap);

		if(this.buttonTitles.length){
			var buttonBar:HTMLDivElement=document.createElement("div");
			buttonBar.className="boxButtonBar";
			for(var i:number=0;i<this.buttonTitles.length;i++){
				var btn:HTMLInputElement=document.createElement("input");
				btn.type="button";
				btn.value=this.buttonTitles[i];
				if(i>0){
					btn.className="secondary";
				}
				if(this.onButtonClick){
					btn.onclick=this.onButtonClick.bind(this, i);
				}else{
					btn.onclick=this.dismiss.bind(this);
				}
				buttonBar.appendChild(btn);
			}
			content.appendChild(buttonBar);
			this.buttonBar=buttonBar;
		}

		return content;
	}

	public setContent(content:HTMLElement):void{
		this.contentWrap.appendChild(content);
	}

	public getButton(index:number):HTMLElement{
		return this.buttonBar.children[index] as HTMLElement;
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
		var content:HTMLDivElement=document.createElement("div");
		content.innerHTML=msg;
		this.setContent(content);
	}
}

class MessageBox extends Box{
	public constructor(title:string, msg:string, btn:string){
		super(title, [btn]);
		var content:HTMLDivElement=document.createElement("div");
		content.innerHTML=msg;
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
		var content:HTMLDivElement=document.createElement("div");
		this.form=document.createElement("form");
		this.form.innerHTML=c;
		this.form.action=act;
		content.appendChild(this.form);
		this.setContent(content);
	}
}
