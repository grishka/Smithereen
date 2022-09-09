///<reference path="./Helpers.ts"/>

class ProgressBar{
	private progress:HTMLElement;
	private progressInner:HTMLElement;
	private root:HTMLElement;
	private curProgress:number=-1;

	public constructor(el:HTMLElement){
		this.root=el;
		el.appendChild(ce("div", {className: "inner"}, [
			this.progress=ce("div", {className: "progress"}, [
				this.progressInner=ce("div", {className: "progressInner"})
			])
		]));
		this.setProgress(0);
	}

	public setProgress(p:number){
		p=Math.max(0, Math.min(p, 1));
		if(p==this.curProgress)
			return;
		if(this.curProgress==0)
			this.root.classList.remove("empty");
		else if(p==0)
			this.root.classList.add("empty");
		var w=Math.round(this.progress.offsetWidth*(1-p));
		this.progress.style.transform="translateX(-"+w+"px)";
		this.progressInner.style.transform="translateX("+w+"px)";
		this.curProgress=p;
	}
}
