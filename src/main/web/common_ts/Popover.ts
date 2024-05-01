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
				ce("div", {className: "popoverContentW"}, [
					this.header=ce("div", {className: "popoverHeader"}),
					this.content=ce("div", {className: "popoverContent"}),
				]),
				this.arrow=ce("div", {className: "popoverArrow"})
			]);
			this.root.hide();
			wrap.appendChild(this.root);
			this.header.hide();
		}
	}

	public show(x:number=-1, y:number=-1, visualAnchor:HTMLElement=null){
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
		if(visualAnchor){
			this.updateArrowPosition(visualAnchor);
		}
	}

	public hide(){
		this.shown=false;
		this.root.hideAnimated();
	}

	public setTitle(title:string){
		this.header.innerHTML=title;
		this.header.show();
		this.root.classList.add("hasHeader");
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

	public updateArrowPosition(visualAnchor:HTMLElement){
		var visualRect=visualAnchor.getBoundingClientRect();
		this.arrow.style.left="0";
		var arrowRect=this.arrow.getBoundingClientRect();
		this.arrow.style.left=Math.round(visualRect.left-arrowRect.left+visualRect.width/2-arrowRect.width/2)+"px";
	}
}
