///<reference path="./Main.ts"/>

class Popover{
	private wrap:HTMLElement;
	private root:HTMLElement;
	private header:HTMLElement;
	private content:HTMLElement;
	private arrow:HTMLElement;
	private shown:boolean=false;

	public constructor(wrap:HTMLElement){
		this.wrap=wrap;
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
		this.root.classList.remove("belowAnchor");
		this.root.classList.add("aboveAnchor"); // For correct height measurement

		this.root.style.top="0";
		var width=this.root.offsetWidth;
		var height=this.root.offsetHeight;
		var below=false;
		var anchorTop:number, anchorBottom:number;

		if(visualAnchor){
			var rect=visualAnchor.getBoundingClientRect();
			anchorTop=rect.top;
			anchorBottom=rect.bottom;
			x=rect.left+rect.width/2;
		}else{
			// Find the Y bounds of the inline boxes that correspond to mouse X
			var anchorRects=anchor.getClientRects().unfuck();
			anchorTop=anchorRects[anchorRects.length-1].bottom;
			anchorBottom=anchorRects[0].top;
			for(var rect of anchorRects){
				if(rect.left<=x && rect.right>x){
					if(anchorTop>rect.top)
						anchorTop=rect.top;
					if(anchorBottom<rect.bottom)
						anchorBottom=rect.bottom;
				}
			}
			if(anchorTop>anchorBottom){ // Didn't find anything?
				var rect=anchor.getBoundingClientRect();
				anchorTop=rect.top;
				anchorBottom=rect.bottom;
			}
		}
		if(anchorTop<height){
			this.root.classList.remove("aboveAnchor");
			this.root.classList.add("belowAnchor");
			this.root.style.top=anchorBottom+"px";
		}else{
			// aboveAnchor class already added
			this.root.style.top=(anchorTop-height)+"px";
		}
		var arrowWidth=this.arrow.offsetWidth;
		var xOffset=25+arrowWidth/2;
		if(x-xOffset+width>window.innerWidth){
			xOffset+=x-xOffset+width-window.innerWidth+10;
		}
		this.root.style.left=Math.round(x-xOffset)+"px";
		this.arrow.style.left=Math.round(xOffset-arrowWidth/2)+"px";
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
}
