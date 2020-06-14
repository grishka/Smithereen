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
			this.root=ce("div");
			this.root.className="popover aboveAnchor";
			hide(this.root);
			wrap.appendChild(this.root);

			this.header=ce("div");
			this.header.className="popoverHeader";
			this.root.appendChild(this.header);

			this.content=ce("div");
			this.content.className="popoverContent";
			this.root.appendChild(this.content);

			this.arrow=ce("div");
			this.arrow.className="popoverArrow";
			this.root.appendChild(this.arrow);
		}
	}

	public show(x:number=-1, y:number=-1){
		this.shown=true;
		show(this.root);
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
		hideAnimated(this.root);
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
}
