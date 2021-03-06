///<reference path="./Main.ts"/>

class Popover{
	
	private root:HTMLElement;
	private header:HTMLElement;
	private content:HTMLElement;
	private arrow:HTMLElement;
	private shown:boolean=false;

	public constructor(wrap:HTMLElement){
		this.root=wrap.querySelector(".popover");
		if(!this.root){
			this.root=ce("div", {className: "popover aboveAnchor"}, [
				this.header=ce("div", {className: "popoverHeader"}),
				this.content=ce("div", {className: "popoverContent"}),
				this.arrow=ce("div", {className: "popoverArrow"})
			]);
			this.root.hide();
			wrap.appendChild(this.root);
		}
	}

	public show(x:number=-1, y:number=-1){
		this.shown=true;
		this.root.show();
		var anchor=this.root.parentElement;
		var anchorRect=anchor.getBoundingClientRect();
		this.root.classList.remove("belowAnchor", "aboveAnchor");
		if(this.root.offsetHeight>anchorRect.top){
			this.root.classList.add("belowAnchor");
			this.root.style.top="";
		}else{
			this.root.classList.add("aboveAnchor");
			this.root.style.top="-"+(this.root.offsetHeight)+"px";
		}
	}

	public hide(){
		this.shown=false;
		this.root.hideAnimated();
	}

	public setTitle(title:string){
		this.header.innerHTML=title;
	}
	
	public setContent(content:string){
		this.content.innerHTML=content;
	}

	public getTitle():string{
		return this.header.innerHTML;
	}

	public isShown():boolean{
		return this.shown;
	}

	public setOnClick(onClick:{():void}):void{
		this.header.style.cursor="pointer";
		this.header.onclick=onClick;
	}
}
