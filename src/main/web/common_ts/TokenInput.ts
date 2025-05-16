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
	private tokenIDs:string[]=[];
	private valueField:HTMLInputElement;
	private completionList:CompletionList;

	public constructor(el:HTMLElement, placeholder:string, completionCallback:{(q:string):TokenInputToken[]}, valueField:HTMLInputElement=null){
		this.root=el;
		this.completionCallback=completionCallback;
		el.classList.add("tokenInput");
		this.edit=ce("input", {type: "text"});
		el.appendChild(this.edit);
		this.completionList=new CompletionList(this.edit, (el)=>{
			this.onCompletionClick(el.customData.token);
		});
		el.addEventListener("keydown", (ev)=>{
			if(ev.keyCode==13){ // enter
				if(this.completionList.selectedCompletion){
					var compl=this.completionList.selectedCompletion.customData.token;
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
			}
		});
		this.placeholder=placeholder;
		this.edit.placeholder=placeholder;
		this.edit.addEventListener("input", this.onTextChanded.bind(this));
		this.edit.addEventListener("focus", ev=>{
			if(!this.edit.value.length)
				this.updateCompletions();
		});
		
		el.appendChild(this.completionList.completionsWrap);
		this.valueField=valueField;
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
		this.updateValueField();
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
		this.updateValueField();
	}

	public getTokenIDs():string[]{
		return this.tokenIDs;
	}

	private onTextChanded(ev:Event){
		this.updateCompletions();
	}

	private onCompletionClick(compl:TokenInputToken){
		this.addToken(compl.id, compl.title);
		this.edit.focus();
	}

	private updateCompletions(){
		this.completionList.completionsList.innerHTML="";
		for(var compl of this.completionCallback(this.edit.value)){
			if(this.tokenIDs.indexOf(compl.id)!=-1)
				continue;
			var opt=ce("div", {className: "completion"}, [compl.title]);
			opt.customData={token: compl};
			this.completionList.completionsList.appendChild(opt);
		}
		this.completionList.updateCompletions();
	}

	private updateValueField(){
		if(!this.valueField)
			return;
		this.valueField.value=this.getTokenIDs().join(",");
	}

	public static filterTokens(q:string, fullList:TokenInputToken[]):TokenInputToken[]{
		if(q.trim()==""){
			return fullList;
		}
		var re=new RegExp("\\b"+quoteRegExp(q), "i");
		var res:TokenInputToken[]=[];
		for(var f of fullList){
			if(re.test(f.title)){
				res.push(f);
			}
		}
		return res;
	}
}
