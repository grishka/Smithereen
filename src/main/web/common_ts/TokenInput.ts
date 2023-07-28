interface TokenInputToken{
	id:string;
	title:string;
	subtitle?:string;
}

class TokenInput{
	private root:HTMLElement;
	private edit:HTMLInputElement;
	private placeholder:string;
	private tokens:HTMLElement[]=[];
	private completionCallback:{(q:string):TokenInputToken[]};
	private completionsWrap:HTMLElement;
	private completionsList:HTMLElement;
	private selectedCompletion:HTMLElement;
	private tokenIDs:string[]=[];
	private ignoreNextBlurEvent:boolean=false;
	private bordersOverlay:HTMLElement;

	public constructor(el:HTMLElement, placeholder:string, completionCallback:{(q:string):TokenInputToken[]}){
		this.root=el;
		this.completionCallback=completionCallback;
		el.classList.add("tokenInput");
		this.edit=ce("input", {type: "text"});
		el.appendChild(this.edit);
		el.addEventListener("keydown", (ev)=>{
			if(ev.keyCode==13){ // enter
				if(this.selectedCompletion){
					var compl=this.selectedCompletion.customData.token;
					this.addToken(compl.id, compl.title);
				}
			}else if(ev.keyCode==8){ // backspace
				if(this.tokens.length){
					if(this.edit.selectionEnd==this.edit.selectionStart && this.edit.selectionEnd==0){
						var lastToken=this.tokens[this.tokens.length-1];
						if(lastToken.classList.contains("selected")){
							this.removeToken(lastToken.dataset.id);
						}else{
							lastToken.classList.add("selected");
						}
					}
				}
			}else if(ev.keyCode==40){ // down
				if(this.selectedCompletion && this.selectedCompletion.nextElementSibling){
					this.selectCompletion(this.selectedCompletion.nextElementSibling as HTMLElement);
					ev.preventDefault();
					this.scrollSelectedCompletionIntoView();
				}
			}else if(ev.keyCode==38){ // up
				if(this.selectedCompletion && this.selectedCompletion.previousElementSibling){
					this.selectCompletion(this.selectedCompletion.previousElementSibling as HTMLElement);
					ev.preventDefault();
					this.scrollSelectedCompletionIntoView();
				}
			}
		});
		this.placeholder=placeholder;
		this.edit.placeholder=placeholder;
		this.edit.addEventListener("input", this.onTextChanded.bind(this));
		
		this.completionsWrap=ce("div", {className: "completionsWW"}, [
			ce("div", {className: "completionsW"}, [
				this.completionsList=ce("div", {className: "completions"}),
				this.bordersOverlay=ce("div", {className: "bordersOverlay"})
			])
		]);
		el.appendChild(this.completionsWrap);
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
		el.addEventListener("mousedown", (ev)=>{
			if(ev.target!=this.edit)
				this.ignoreNextBlurEvent=true;
		});
	}

	public addToken(id:string, title:string, needUpdateCompletions:boolean=true){
		id=id.toString();
		this.edit.value="";
		var token;
		this.edit.insertAdjacentElement("beforebegin", token=ce("div", {innerText: title, className: "token"}, [
			ce("a", {href: "javascript:void(0)", className: "remove", title: lang("delete"), onclick: (ev)=>{
				this.removeToken(id);
			}})
		]));
		token.dataset.id=id;
		this.edit.placeholder="";
		if(this.tokens.length){
			var lastToken=this.tokens[this.tokens.length-1];
			lastToken.classList.remove("selected");
		}
		this.tokens.push(token);
		this.tokenIDs.push(id);
		if(needUpdateCompletions)
			this.updateCompletions();
	}

	public removeToken(id:string){
		for(var token of this.tokens){
			if(token.dataset.id==id){
				this.tokens.remove(token);
				token.remove();
				break;
			}
		}
		if(this.tokens.length==0){
			this.edit.placeholder=this.placeholder;
		}
		this.tokenIDs.remove(id);
		this.updateCompletions();
		this.edit.focus();
	}

	public getTokenIDs():string[]{
		return this.tokenIDs;
	}

	private onTextChanded(ev:Event){
		this.updateCompletions();
	}

	private onCompletionHover(ev:MouseEvent){
		if(ev.target!=this.selectedCompletion){
			this.selectCompletion(ev.target as HTMLElement);
		}
	}

	private onCompletionClick(compl:TokenInputToken, ev:MouseEvent){
		this.addToken(compl.id, compl.title);
		this.edit.focus();
	}

	private selectCompletion(el:HTMLElement){
		if(this.selectedCompletion)
			this.selectedCompletion.classList.remove("selected");
		this.selectedCompletion=el;
		el.classList.add("selected");
		this.updateCompletionListClasses();
	}

	private updateCompletions(){
		this.selectedCompletion=null;
		for(var el of this.completionsList.querySelectorAll(".completion").unfuck()){
			this.completionsList.removeChild(el);
		}
		var firstOption:HTMLElement;
		for(var compl of this.completionCallback(this.edit.value)){
			if(this.tokenIDs.indexOf(compl.id)!=-1)
				continue;
			var opt=ce("div", {className: "completion"}, [compl.title]);
			opt.customData={token: compl};
			opt.addEventListener("mouseenter", this.onCompletionHover.bind(this));
			opt.addEventListener("click", this.onCompletionClick.bind(this, compl));
			this.completionsList.appendChild(opt);
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

	private scrollSelectedCompletionIntoView(){
		if(this.selectedCompletion.offsetTop+this.selectedCompletion.offsetHeight>=this.completionsList.scrollTop+this.completionsList.offsetHeight){
			this.selectedCompletion.scrollIntoView(false);
		}else if(this.selectedCompletion.offsetTop<this.completionsList.scrollTop){
			this.selectedCompletion.scrollIntoView();
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
}
