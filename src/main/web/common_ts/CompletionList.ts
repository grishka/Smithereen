class CompletionList{
	public completionsWrap:HTMLElement;
	public completionsList:HTMLElement;
	public selectedCompletion:HTMLElement;
	private ignoreNextBlurEvent:boolean=false;
	private bordersOverlay:HTMLElement;
	private edit:HTMLElement;
	private onSelect:{(opt:HTMLElement):void};

	public constructor(textField:HTMLElement, onSelect:{(opt:HTMLElement):void}){
		this.edit=textField;
		this.onSelect=onSelect;

		this.completionsWrap=ce("div", {className: "completionsWW"}, [
			ce("div", {className: "completionsW"}, [
				this.completionsList=ce("div", {className: "completions"}),
				this.bordersOverlay=ce("div", {className: "bordersOverlay"})
			])
		]);
		this.completionsWrap.hide();
		this.completionsList.addEventListener("scroll", this.onCompletionsScroll.bind(this), {passive: true});
		this.edit.addEventListener("blur", (ev)=>{
			if(this.ignoreNextBlurEvent){
				this.ignoreNextBlurEvent=false;
				return;
			}
			this.completionsWrap.hide();
		});
		this.edit.addEventListener("focus", (ev)=>{
			this.ignoreNextBlurEvent=false;
			if(this.completionsList.children.length)
				this.completionsWrap.show();
			else
				this.updateCompletions();
		});
		this.completionsWrap.addEventListener("mousedown", (ev)=>{
			if(ev.target!=this.edit)
				this.ignoreNextBlurEvent=true;
		});
		this.edit.addEventListener("keydown", (ev)=>{
			if((ev.keyCode==40 || ev.keyCode==38 || ev.keyCode==27) && this.completionsWrap.style.display!="none"){
				ev.preventDefault();
			}
			if(ev.keyCode==40){ // down
				if(this.selectedCompletion && this.selectedCompletion.nextElementSibling){
					this.selectCompletion(this.selectedCompletion.nextElementSibling as HTMLElement);
					this.scrollSelectedCompletionIntoView();
				}
			}else if(ev.keyCode==38){ // up
				if(this.selectedCompletion && this.selectedCompletion.previousElementSibling){
					this.selectCompletion(this.selectedCompletion.previousElementSibling as HTMLElement);
					this.scrollSelectedCompletionIntoView();
				}
			}else if(ev.keyCode==27){ // esc
				this.completionsWrap.hide();
			}
		});
	}

	private scrollSelectedCompletionIntoView(){
		if(this.selectedCompletion.offsetTop+this.selectedCompletion.offsetHeight>=this.completionsList.scrollTop+this.completionsList.offsetHeight){
			this.completionsList.scrollTop=this.selectedCompletion.offsetTop+this.selectedCompletion.offsetHeight-this.completionsList.offsetHeight;
		}else if(this.selectedCompletion.offsetTop<this.completionsList.scrollTop){
			this.completionsList.scrollTop=this.selectedCompletion.offsetTop;
		}
		this.updateCompletionListClasses();
	}

	private onCompletionsScroll(ev:Event){
		this.updateCompletionListClasses();
	}

	private updateCompletionListClasses(){
		var isTop=this.selectedCompletion.offsetTop<=this.completionsList.scrollTop && this.selectedCompletion.offsetTop+this.selectedCompletion.offsetHeight>this.completionsList.scrollTop;
		var bottomOffset=1;
		if(!this.selectedCompletion.nextElementSibling)
			bottomOffset=2; // compensates for the lack of bottom border
		var isBottom=this.selectedCompletion.offsetTop+this.selectedCompletion.offsetHeight+bottomOffset>=this.completionsList.scrollTop+this.completionsList.offsetHeight && this.selectedCompletion.offsetTop<this.completionsList.scrollTop+this.completionsList.offsetHeight;
		if(isTop!=this.bordersOverlay.classList.contains("firstSelected")){
			if(isTop)
				this.bordersOverlay.classList.add("firstSelected");
			else
				this.bordersOverlay.classList.remove("firstSelected");
		}
		if(isBottom!=this.bordersOverlay.classList.contains("lastSelected")){
			if(isBottom)
				this.bordersOverlay.classList.add("lastSelected");
			else
				this.bordersOverlay.classList.remove("lastSelected");
		}

		this.bordersOverlay.style.height=this.completionsList.offsetHeight+"px";
		this.bordersOverlay.style.right=(this.completionsList.offsetWidth-this.completionsList.clientWidth)+"px";
	}

	public selectCompletion(el:HTMLElement){
		if(this.selectedCompletion)
			this.selectedCompletion.classList.remove("selected");
		this.selectedCompletion=el;
		el.classList.add("selected");
		this.updateCompletionListClasses();
	}

	private onCompletionHover(ev:MouseEvent){
		if(ev.target!=this.selectedCompletion){
			this.selectCompletion(ev.target as HTMLElement);
		}
	}

	private onCompletionClick(ev:MouseEvent){
		if(ev.target instanceof HTMLElement){
			this.onSelect(ev.target.closest(".completion"));
		}
	}
	
	public updateCompletions(){
		this.selectedCompletion=null;
		var firstOption:HTMLElement;
		for(var opt of this.completionsList.children.unfuck()){
			opt.addEventListener("mouseenter", this.onCompletionHover.bind(this));
			opt.addEventListener("click", this.onCompletionClick.bind(this));
			if(!firstOption){
				firstOption=opt;
			}
		}
		if(!firstOption){
			this.completionsWrap.hide();
		}else{
			this.completionsWrap.show();
			this.selectCompletion(firstOption);
			this.scrollSelectedCompletionIntoView();
		}
	}
}
