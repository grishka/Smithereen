interface UserSelectorOption{
	id:string;
	title:string;
	subtitle?:string;
}

class UserSelector{
	private input:HTMLInputElement;
	private hiddenField:HTMLInputElement;
	private defaultOptions:UserSelectorOption[];
	private hasSelection:boolean;
	private list:CompletionList;
	private debounceTimeout:number;
	private searchXHR:XMLHttpRequest;

	public constructor(input:HTMLInputElement, hiddenField:HTMLInputElement, defaultOptions:UserSelectorOption[]){
		this.input=input;
		this.hiddenField=hiddenField;
		this.defaultOptions=defaultOptions;

		for(var opt of defaultOptions){
			if(hiddenField.value==opt.id){
				input.value=ce("span", {innerHTML: opt.title}).innerText;
				this.hasSelection=true;
				break;
			}
		}

		input.addEventListener("input", this.onFieldInput.bind(this), false);
		input.addEventListener("focus", this.onFieldFocus.bind(this), false);
		input.addEventListener("keydown", this.onFieldKeyDown.bind(this), false);

		this.list=new CompletionList(input, this.onCompletionSelect.bind(this));
		input.parentElement.appendChild(ce("div", {className: "completionsContainer"}, [this.list.completionsWrap]));
		this.setCompletions(defaultOptions, true);
		this.list.completionsWrap.hide();
	}

	private onFieldInput(ev:Event){
		if(this.hasSelection){
			this.hasSelection=false;
		}
		if(this.debounceTimeout){
			clearTimeout(this.debounceTimeout);
			this.debounceTimeout=0;
		}
		if(this.searchXHR){
			this.searchXHR.abort();
			this.searchXHR=null;
		}
		if(this.input.value.length==0){
			this.setCompletions(this.defaultOptions, false);
		}else{
			this.debounceTimeout=setTimeout(()=>{
				this.debounceTimeout=0;
				this.searchXHR=ajaxGet("/system/simpleUserCompletions?q="+encodeURIComponent(this.input.value), (r)=>{
					this.searchXHR=null;
					this.setCompletions(r as UserSelectorOption[], false);
				}, (err)=>{
					this.searchXHR=null;
				}, "json");
			}, 300);
		}
	}

	private onFieldFocus(ev:FocusEvent){
		if(this.hasSelection){
			this.input.selectionStart=0;
			this.input.selectionEnd=this.input.value.length;
		}
	}

	private onFieldKeyDown(ev:KeyboardEvent){
		if(ev.keyCode==13){ // enter
			ev.preventDefault();
			if(this.list.selectedCompletion){
				this.onCompletionSelect(this.list.selectedCompletion);
			}
		}
	}

	private onCompletionSelect(completion:HTMLElement){
		this.hiddenField.value=completion.customData.option.id;
		this.input.value=completion.innerText;
		this.list.completionsWrap.hide();
		this.hasSelection=true;
		this.input.blur();
	}

	private setCompletions(completions:UserSelectorOption[], selectCurrent:boolean){
		this.list.completionsList.innerHTML="";
		var selected:HTMLElement;
		for(var compl of completions){
			var el=ce("div", {className: "completion", innerHTML: compl.title});
			el.customData={option: compl};
			this.list.completionsList.appendChild(el);
			if(selectCurrent && compl.id==this.hiddenField.value){
				selected=el;
			}
		}
		this.list.updateCompletions();
		if(selected)
			this.list.selectCompletion(selected);
	}
}
