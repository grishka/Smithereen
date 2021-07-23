function initQSearch(){
	var qsearchWrap=ge("qsearchWrap");
	var qsearchField:HTMLInputElement=ge("qsearchField");
	var qsearchResults=ge("qsearchResults");
	if(!qsearchWrap || !qsearchField)
		return;
	var qsearchHint=ge("qsearchHint");
	var qsearchLoader=ge("qsearchLoader");

	var debounceTimeout:number;
	var currentRequest:XMLHttpRequest;
	var resultsHidden=false;
	var resultsValid=false;
	var loaderVisible=false;
	var hintVisible=true;

	function qsearchInputDebounced(){
		debounceTimeout=undefined;
		var val=qsearchField.value;
		if(val && val.length>1){
			currentRequest=ajaxGet("/system/qsearch?q="+encodeURIComponent(val), (resp:any)=>{
				currentRequest=null;
				resultsHidden=false;
				resultsValid=true;
				qsearchResults.showAnimated();
				qsearchResults.innerHTML=resp;
				if(qsearchLoader && loaderVisible){
					qsearchLoader.hideAnimated();
					loaderVisible=false;
				}
			}, ()=>{
				currentRequest=null;
			}, "text");
		}else{
			if(qsearchLoader && loaderVisible){
				qsearchLoader.hideAnimated();
				loaderVisible=false;
			}
			if(qsearchHint && !hintVisible){
				qsearchHint.showAnimated();
				hintVisible=true;
			}
		}
	}

	if(!mobile){
		qsearchField.addEventListener("focus", (ev:FocusEvent)=>{
			if(resultsHidden && resultsValid){
				qsearchResults.showAnimated();
				resultsHidden=false;
			}
		});
		qsearchField.addEventListener("blur", (ev:FocusEvent)=>{
			if(ev.target===document.activeElement)
				return;
			if(!resultsHidden){
				qsearchResults.hideAnimated();
				resultsHidden=true;
			}
		});
	}
	qsearchField.addEventListener("input", (ev:Event)=>{
		if(debounceTimeout)
			clearTimeout(debounceTimeout);
		debounceTimeout=setTimeout(qsearchInputDebounced, 500);
		if(currentRequest){
			currentRequest.abort();
			currentRequest=null;
		}
		if(!resultsHidden){
			qsearchResults.hideAnimated();
			resultsHidden=true;
			resultsValid=false;
		}
		if(qsearchLoader && !loaderVisible){
			qsearchLoader.showAnimated();
			loaderVisible=true;
		}
		if(qsearchHint && hintVisible){
			qsearchHint.hideAnimated();
			hintVisible=false;
		}
	});
}

function loadRemoteObject(url:string):void{
	LayerManager.getInstance().showBoxLoader();
	ajaxPost("/system/loadRemoteObject", {uri: url}, (resp:any)=>{
		if(resp.success){
			window.location.href=resp.success;
		}else if(resp.error){
			new MessageBox(lang("error"), resp.error as string, lang("ok")).show();
		}
	}, ()=>{
		
	});
}

if(mobile){
	var searchBtn=ge("mainMenuSearchButton");
	if(searchBtn){
		searchBtn.onclick=()=>{
			new MobileSearchLayer().show();
		};
	}
}else{
	initQSearch();
}

class MobileSearchLayer extends BaseLayer{
	private field:HTMLInputElement;

	public onCreateContentView():HTMLElement{
		var loader;
		var el=ce("div", {id: "qsearchWrap"}, [
			ce("div", {id: "qsearchFieldWrap"}, [
				ce("a", {id: "qsearchClose", title: lang("close"), onclick: this.dismiss.bind(this)}),
				ce("div", {id: "qsearchIHateCSS"}, [
					this.field=ce("input", {id: "qsearchField", type: "text", placeholder: lang("search"), autocomplete: "off", autocapitalize: "off"})
				])
			]),
			ce("div", {id: "qsearchResultsWrap"}, [
				ce("div", {id: "qsearchResultsInner"}, [
					loader=ce("div", {id: "qsearchLoader", className: "loader"}),
					ce("div", {id: "qsearchHint", innerText: lang("qsearch_hint")}),
					ce("div", {id: "qsearchResults"})
				])
			])
		]);
		loader.style.display="none";
		return el;
	}

	public onShown(){
		initQSearch();
		this.field.focus();
		ge("boxLoader").style.zIndex="120";
	}

	public onHidden(){
		ge("boxLoader").style.zIndex=null;
	}

	public allowDismiss():boolean{
		return false;
	}
}
