var submittingForm:HTMLFormElement=null;
var numberFormatter=window.Intl && Intl.NumberFormat ? new Intl.NumberFormat(userConfig.locale) : null;

function ge<E extends HTMLElement>(id:string):E{
	return document.getElementById(id) as E;
}

function ce<K extends keyof HTMLElementTagNameMap>(tag:K, attrs:Partial<HTMLElementTagNameMap[K]>={}, children:(HTMLElement|string)[]=[]):HTMLElementTagNameMap[K]{
	var el=document.createElement(tag);
	for(var attrName in attrs){
		el[attrName]=attrs[attrName];
	}
	for(var child of children){
		if(child instanceof HTMLElement)
			el.appendChild(child);
		else
			el.appendChild(document.createTextNode(child));
	}
	return el;
};

interface String{
	escapeHTML():string;
}

String.prototype.escapeHTML=function(){
	var el=document.createElement("span");
	el.innerText=this;
	return el.innerHTML;
}

interface Array<T>{
	remove(item:T):void;
}

Array.prototype.remove=function(item){
	var index:number=this.indexOf(item);
	if(index==-1)
		return;
	this.splice(index, 1);
};

interface AnimationDescription{
	keyframes:Keyframe[] | PropertyIndexedKeyframes | null;
	options:number | KeyframeAnimationOptions;
}

interface HTMLElement{
	_popover:Popover;
	customData:{[key:string]: any};

	currentVisibilityAnimation:Animation;

	qs<E extends HTMLElement>(sel:string):E;
	hide():void;
	show():void;
	hideAnimated(animName?:AnimationDescription, onEnd?:{():void}):void;
	showAnimated(animName?:AnimationDescription, onEnd?:{():void}):void;

	anim(keyframes: Keyframe[] | PropertyIndexedKeyframes | null, options?: number | KeyframeAnimationOptions, onEnd?:{():void}):Partial<Animation>;
}

HTMLElement.prototype.qs=function(sel:string){
	return this.querySelector(sel);
};

HTMLElement.prototype.hide=function():void{
	if(this.currentVisibilityAnimation){
		this.currentVisibilityAnimation.cancel();
		this.currentVisibilityAnimation=null;
	}
	this.style.display="none";
};

HTMLElement.prototype.hideAnimated=function(animName:AnimationDescription={keyframes: [{opacity: 1}, {opacity: 0}], options: {duration: 200, easing: "ease"}}, onEnd:{():void}=null):void{
	if(this.currentVisibilityAnimation){
		this.currentVisibilityAnimation.cancel();
		this.currentVisibilityAnimation=null;
	}
	this.currentVisibilityAnimation=this.anim(animName.keyframes, animName.options, ()=>{
		this.hide();
		this.currentVisibilityAnimation=null;
		if(onEnd) onEnd();
	});
};

HTMLElement.prototype.show=function():void{
	if(this.currentVisibilityAnimation){
		this.currentVisibilityAnimation.cancel();
	}
	this.style.display="";
};

HTMLElement.prototype.showAnimated=function(animName:AnimationDescription={keyframes: [{opacity: 0}, {opacity: 1}], options: {duration: 200, easing: "ease"}}, onEnd:{():void}=null):void{
	if(this.currentVisibilityAnimation){
		this.currentVisibilityAnimation.cancel();
	}
	this.show();
	this.currentVisibilityAnimation=this.anim(animName.keyframes, animName.options, ()=>{
		this.currentVisibilityAnimation=null;
		if(onEnd)
			onEnd();
	});
};

// JavaScript is an immensely fucked up language for having some DOM APIs
// return these "arrays that are not quite arrays" for no good reason whatsoever.
interface NodeList{
	unfuck():Node[];
}

NodeList.prototype.unfuck=function(){
	var arr:Node[]=[];
	for(var i=0;i<this.length;i++)
		arr.push(this[i]);
	return arr;
};

interface TouchList{
	unfuck():Touch[];
}

if(window.TouchList!=undefined){
	TouchList.prototype.unfuck=function(){
		var arr:Touch[]=[];
		for(var i=0;i<this.length;i++){
			arr.push(this.item(i));
		}
		return arr;
	}
}

interface HTMLCollection{
	unfuck():HTMLElement[];
}

HTMLCollection.prototype.unfuck=function(){
	var arr:HTMLElement[]=[];
	for(var i=0;i<this.length;i++)
		arr.push(this[i]);
	return arr;
};

interface DOMRectList{
	unfuck():DOMRect[];
}
DOMRectList.prototype.unfuck=function(){
	var arr:DOMRect[]=[];
	for(var i=0;i<this.length;i++)
		arr.push(this[i]);
	return arr;
}

interface HTMLTextAreaElement{
	resizeToFitContent():void;
}

var compatAnimStyle:HTMLStyleElement;

function cssRuleForCamelCase(s:string):string{
	return s.replace( /([A-Z])/g, "-$1" );
}

function removeCssRuleByName(sheet:CSSStyleSheet, name:string){
	for(var i=0;i<sheet.rules.length;i++){
		if((sheet.rules[i] as any).name==name){
			sheet.removeRule(i);
			return;
		}
	}
}

HTMLElement.prototype.anim=function(keyframes, options, onFinish):Partial<Animation>{
	if(this.animate!==undefined){
		var a=this.animate(keyframes, options);
		if(onFinish)
			a.onfinish=onFinish;
		return a;
	}else if(this.style.animationName!==undefined || this.style.webkitAnimationName!==undefined){
		var needsWebkitPrefix=this.style.animationName===undefined;
		if(!compatAnimStyle){
			compatAnimStyle=ce("style");
			document.body.appendChild(compatAnimStyle);
		}
		var ruleName="";
		for(var i=0;i<40;i++){
			ruleName+=String.fromCharCode(0x61+Math.floor(Math.random()*26));
		}
		var rule=(needsWebkitPrefix ? "@-webkit-" : "@")+"keyframes "+ruleName+"{";
		rule+="0%{";
		var _keyframes:any=keyframes as any;
		for(var k in _keyframes[0]){
			rule+=cssRuleForCamelCase(k)+": "+_keyframes[0][k]+";";
		}
		rule+="} 100%{";
		for(var k in _keyframes[1]){
			rule+=cssRuleForCamelCase(k)+": "+_keyframes[1][k]+";";
		}
		rule+="}}";
		var sheet:CSSStyleSheet=compatAnimStyle.sheet as CSSStyleSheet;
		sheet.insertRule(rule, sheet.rules.length);
		var duration:number=(options instanceof Number) ? (options as number) : ((options as KeyframeAnimationOptions).duration as number);
		var easing=(options instanceof Number) ? "" : ((options as KeyframeAnimationOptions).easing);
		if(!needsWebkitPrefix){
			this.style.animation=ruleName+" "+(duration/1000)+"s "+easing;
			var fn=()=>{
				this.style.animation="";
				removeCssRuleByName(sheet, ruleName);
				if(onFinish) onFinish();
				this.removeEventListener("animationend", fn);
			};
			this.addEventListener("animationend", fn);
		}else{
			this.style.webkitAnimation=ruleName+" "+(duration/1000)+"s "+easing;
			var fn=()=>{
				this.style.webkitAnimation="";
				removeCssRuleByName(sheet, ruleName);
				if(onFinish) onFinish();
				this.removeEventListener("webkitanimationend", fn);
			};
			this.addEventListener("webkitanimationend", fn);
		}
		return {cancel: ()=>{
			if(!needsWebkitPrefix)
				this.style.animation="";
			else
				this.style.webkitAnimation="";
		}};
	}
	if(onFinish)
		onFinish();
	return null;
};

function ajaxPost(uri:string, params:any, onDone:Function, onError:Function, responseType:XMLHttpRequestResponseType="json"):XMLHttpRequest{
	var xhr:XMLHttpRequest=new XMLHttpRequest();
	xhr.open("POST", uri);
	xhr.onload=function(){
		if(Math.floor(xhr.status/100)==2){
			try{
				var parsedResponse=responseType=="json" ? JSON.parse(xhr.response) : xhr.response;
				onDone(parsedResponse);
			}catch(e){
				onError(null);
			}
		}else{
			onError(xhr.response || xhr.statusText);
		}
	};
	xhr.onerror=function(ev:Event){
		console.log(ev);
		onError();
	};
	xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	var formData:string[]=[];
	for(var key in params){
		var val=params[key];
		if(val instanceof Array){
			var arr=val as any[];
			for(var e of arr){
				formData.push(key+"="+encodeURIComponent(e));
			}
		}else{
			formData.push(key+"="+encodeURIComponent(params[key]));
		}
	}
	formData.push("_ajax=1");
	xhr.send(formData.join("&"));
	return xhr;
}

function ajaxGet(uri:string, onDone:{(r:any):void}, onError:{(msg:string):void}, responseType:XMLHttpRequestResponseType="json"):XMLHttpRequest{
	if(!onError){
		onError=(msg)=>{
			new MessageBox(lang("error"), msg || lang("network_error"), lang("ok")).show();
		};
	}
	var xhr:XMLHttpRequest=new XMLHttpRequest();
	xhr.open("GET", addParamsToURL(uri, {_ajax: "1"}));
	xhr.onload=function(){
		if(Math.floor(xhr.status/100)==2){
			try{
				var parsedResponse=responseType=="json" ? JSON.parse(xhr.response) : xhr.response;
				onDone(parsedResponse);
			}catch(e){
				console.error(e);
				onError(null);
			}
		}else{
			if(xhr.response && responseType=="json" && xhr.getResponseHeader("content-type")=="application/json"){
				try{
					onDone(JSON.parse(xhr.response));
				}catch(e){
					console.error(e);
					onError(null);
				}
			}else{
				onError(xhr.response || xhr.statusText);
			}
		}
	};
	xhr.onerror=function(ev:Event){
		console.log(ev);
		onError(xhr.statusText);
	};
	xhr.send();
	return xhr;
}

function ajaxUpload(uri:string, fieldName:string, file:File, onDone:{(resp:any):boolean}=null, onError:Function=null, onProgress:{(progress:number):void}=null):void{
	var formData=new FormData();
	formData.append(fieldName, file);
	var xhr=new XMLHttpRequest();
	xhr.open("POST", addParamsToURL(uri, {_ajax: "1", csrf: userConfig.csrf}));
	xhr.onload=function(){
		var resp=xhr.response;
		if(Math.floor(xhr.status/100)!=2){
			if(onError)
				onError(xhr.response || xhr.statusText);
			return;
		}
		if(onDone){
			if(onDone(resp))
				return;
		}
		if(resp instanceof Array){
			for(var i=0;i<resp.length;i++){
				applyServerCommand(resp[i]);
			}
		}
	}.bind(this);
	xhr.onerror=function(ev:ProgressEvent){
		console.log(ev);
		if(onError) onError();
	};
	xhr.upload.onprogress=function(ev:ProgressEvent){
		// pbarInner.style.transform="scaleX("+(ev.loaded/ev.total)+")";
		if(onProgress)
			onProgress(ev.loaded/ev.total);
	};
	xhr.responseType="json";
	xhr.send(formData);
}


function isVisible(el:HTMLElement):boolean{
	return el.style.display!="none";
}

function lang(key:string, args:{[key:string]:(string|number)}={}):string{
	if(!langKeys[key])
		return key.replace(/_/g, " ");
	var v=langKeys[key];
	if(typeof v==="function")
		return (v as Function).apply(this, [args]);
	return v as string;
}

function langFileSize(size:number):string{
	var key, amount;
	if(size<1024){
		key="file_size_bytes";
		amount=size;
	}else if(size<1024*1024){
		key="file_size_kilobytes";
		amount=size/1024.0;
	}else if(size<1024*1024*1024){
		key="file_size_megabytes";
		amount=size/(1024.0*1024.0);
	}else if(size<1024*1024*1024*1024){
		key="file_size_gigabytes";
		amount=size/(1024.0*1024.0*1024.0);
	}else{
		key="file_size_terabytes";
		amount=size/(1024.0*1024.0*1024.0*1024.0);
	}
	return lang(key, {amount: Intl.NumberFormat(userConfig.locale, {maximumFractionDigits: 2}).format(amount)});
}

var langPluralRules:{[key:string]:(quantity:number)=>string}={
	single: function(quantity:number){
		return "other";
	},
	english: function(quantity:number){
		return quantity==1 ? "one" : "other";
	},
	slavic: function(quantity:number){
		if(Math.floor(quantity/10)%10==1)
			return "other";
		var units=quantity%10;
		if(units==1)
			return "one";
		if(units>1 && units<5)
			return "few";
		return "other";
	}
};

function choosePluralForm(n:number, args:any, values:{[key:string]:Function}):string{
	if(values[n.toString()])
		return values[n.toString()](args);
	var k=langPluralRules[userConfig.langPluralRulesName](n);
	return (values[k] || values["other"])(args);
}

function chooseLangOption(v:string, args:any, values:{[key:string]:Function}):string{
	return (values[v] || values["other"])(args);
}

function formatNumber(n:number):string{
	if(numberFormatter)
		return numberFormatter.format(n);
	return n.toString();
}

function setGlobalLoading(loading:boolean):void{
	document.body.style.cursor=loading ? "progress" : "";
}

function ajaxConfirm(titleKey:string, msgKey:string, url:string, params:any={}, useLang:boolean=true):boolean{
	var box:ConfirmBox;
	box=new ConfirmBox(useLang ? lang(titleKey) : titleKey, useLang ? lang(msgKey) : msgKey, function(){
		var btn=box.getButton(0);
		btn.setAttribute("disabled", "");
		box.getButton(1).setAttribute("disabled", "");
		btn.classList.add("loading");
		setGlobalLoading(true);
		params.csrf=userConfig.csrf;
		ajaxPost(url, params, function(resp:any){
			setGlobalLoading(false);
			box.dismiss();
			if(resp instanceof Array){
				for(var i=0;i<resp.length;i++){
					applyServerCommand(resp[i]);
				}
			}
		}, function(msg:string){
			setGlobalLoading(false);
			box.dismiss();
			new MessageBox(lang("error"), msg || lang("network_error"), lang("ok")).show();
		});
	});
	box.show();
	return false;
}

function ajaxSubmitForm(form:HTMLFormElement, onDone:{(resp?:any):void}=null, submitter:HTMLElement=null, extra:any={}):boolean{
	if(submittingForm)
		return false;
	if(!form.checkValidity()){
		setGlobalLoading(false);
		return false;
	}
	if(submitter && submitter.dataset.confirmMessage && !extra.confirmed){
		new ConfirmBox(lang(submitter.dataset.confirmTitle), lang(submitter.dataset.confirmMessage), ()=>ajaxSubmitForm(form, onDone, submitter, {confirmed: true})).show();
		return;
	}
	submittingForm=form;
	if(!submitter && form.dataset.submitterId)
		submitter=ge(form.dataset.submitterId);
	if(submitter)
		submitter.classList.add("loading");
	setGlobalLoading(true);
	var data:any={};
	var elems=form.elements;
	for(var i=0;i<elems.length;i++){
		var el=elems[i] as any;
		if(!el.name)
			continue;
		if(((el.tagName=="INPUT" && el.type=="submit") || el.tagName=="BUTTON") && el!=submitter)
			continue;
		if((el.type!="radio" && el.type!="checkbox") || ((el.type=="radio" || el.type=="checkbox") && el.checked)){
			if(data[el.name]){
				var existing=data[el.name];
				if(existing instanceof Array)
					(existing as any[]).push(el.value);
				else
					data[el.name]=[existing, el.value];
			}else{
				data[el.name]=el.value;
			}
		}
	}
	data.csrf=userConfig.csrf;
	if(location.search){
		var params=new URLSearchParams(location.search);
		if(params.has("lang")){
			data.lang=params.get("lang");
		}
	}
	ajaxPost(form.action, data, function(resp:any){
		if(extra.onResponseReceived){
			extra.onResponseReceived(resp);
		}
		submittingForm=null;
		if(submitter)
			submitter.classList.remove("loading");
		setGlobalLoading(false);
		var dismiss=true;
		if(resp instanceof Array){
			for(var i=0;i<resp.length;i++){
				if(resp[i].a=="kb"){
					dismiss=false;
				}else{
					applyServerCommand(resp[i]);
				}
			}
		}
		if(onDone) onDone(dismiss);
	}, function(msg:string){
		submittingForm=null;
		if(submitter)
			submitter.classList.remove("loading");
		setGlobalLoading(false);
		new MessageBox(lang("error"), msg || lang("network_error"), lang("ok")).show();
		if(onDone) onDone(false);
	});
	return false;
}

function ajaxFollowLink(link:HTMLAnchorElement):boolean{
	if(link.dataset.ajax!=undefined){
		var elToHide:HTMLElement;
		var elToShow:HTMLElement;
		if(link.dataset.ajaxHide!=undefined){
			elToHide=ge(link.dataset.ajaxHide);
			if(elToHide)
				elToHide.hide();
		}
		if(link.dataset.ajaxShow!=undefined){
			elToShow=ge(link.dataset.ajaxShow);
			if(elToShow)
				elToShow.show();
		}
		link.classList.add("ajaxLoading");
		var done=()=>{
			if(elToHide)
				elToHide.show();
			if(elToShow)
				elToShow.hide();
			link.classList.remove("ajaxLoading");
		};
		ajaxGetAndApplyActions(link.href, done, done);
		return true;
	}
	if(link.dataset.ajaxBox!=undefined){
		LayerManager.getInstance().showBoxLoader();
		ajaxGetAndApplyActions(link.href);
		return true;
	}
	if(link.dataset.confirmAction){
		ajaxConfirm(link.dataset.confirmTitle, link.dataset.confirmMessage, link.dataset.confirmAction);
		return true;
	}
	return false;
}

function ajaxGetAndApplyActions(url:string, onDone:{():void}=null, onError:{():void}=null, onBeforeDone:{():void}=null):XMLHttpRequest{
	setGlobalLoading(true);
	if(location.search){
		var params=new URLSearchParams(location.search);
		if(params.has("lang")){
			url=addParamsToURL(url, {lang: params.get("lang")});
		}
	}
	return ajaxGet(url, function(resp:any){
		if(onBeforeDone) onBeforeDone();
		setGlobalLoading(false);
		if(resp instanceof Array){
			for(var i=0;i<resp.length;i++){
				applyServerCommand(resp[i]);
			}
		}
		if(onDone) onDone();
	}, function(msg:string){
		setGlobalLoading(false);
		new MessageBox(lang("error"), msg || lang("network_error"), lang("ok")).show();
		if(onError) onError();
	});
}

function applyServerCommand(cmd:any){
	switch(cmd.a){
		case "remove":
		{
			var ids:string[]=cmd.ids;
			for(var i=0;i<ids.length;i++){
				var el=document.getElementById(ids[i]);
				if(el){
					el.parentNode.removeChild(el);
				}
			}
		}
		break;
		case "setContent":
		{
			var id:string=cmd.id;
			var content:string=cmd.c;
			var el=document.getElementById(id);
			if(el){
				el.innerHTML=content;
			}
		}
		break;
		case "setAttr":
		{
			var id:string=cmd.id;
			var value:string=cmd.v;
			var name:string=cmd.n;
			var el=document.getElementById(id);
			if(el){
				el.setAttribute(name, value);
			}
		}
		break;
		case "msgBox":
			new MessageBox(cmd.t, cmd.m, cmd.b).show();
			break;
		case "formBox":
			new FormBox(cmd.t, cmd.m, cmd.b, cmd.fa).show();
			break;
		case "confirmBox":
			ajaxConfirm(cmd.t, cmd.m, cmd.fa, {}, false);
			break;
		case "box":
		{
			var box=cmd.s ? new ScrollableBox(cmd.t, [lang("close")]) : new BoxWithoutContentPadding(cmd.t);
			var cont=ce("div");
			if(cmd.i){
				cont.id=cmd.i;
			}
			cont.innerHTML=cmd.c;
			cont.customData={box: box};
			box.setContent(cont);
			box.show();
			if(cmd.aux){
				box.addButtonBarAuxHTML(cmd.aux);
			}
			if(cmd.w){
				(box.getContent().querySelector(".boxLayer") as HTMLElement).style.width=cmd.w+"px";
				(box.getContent().querySelector(".boxLayer") as HTMLElement).style.minWidth=cmd.w+"px";
			}
		}
		break;
		case "show":
		{
			var ids:string[]=cmd.ids;
			for(var i=0;i<ids.length;i++){
				var el=document.getElementById(ids[i]);
				if(el){
					el.show();
				}
			}
		}
		break;
		case "hide":
		{
			var ids:string[]=cmd.ids;
			for(var i=0;i<ids.length;i++){
				var el=document.getElementById(ids[i]);
				if(el){
					el.hide();
				}
			}
		}
		break;
		case "insert":
		{
			var el=document.getElementById(cmd.id);
			if(!el) return;
			var mode:InsertPosition=({"bb": "beforeBegin", "ab": "afterBegin", "be": "beforeEnd", "ae": "afterEnd"} as any)[cmd.m as string] as InsertPosition;
			el.insertAdjacentHTML(mode, cmd.c);
		}
		break;
		case "setValue":
		{
			var el=document.getElementById(cmd.id);
			if(!el) return;
			(el as any).value=cmd.v;
		}
		break;
		case "addClass":
		{
			var el=document.getElementById(cmd.id);
			if(!el) return;
			el.classList.add(cmd.cl);
		}
		break;
		case "remClass":
		{
			var el=document.getElementById(cmd.id);
			if(!el) return;
			el.classList.remove(cmd.cl);
		}
		break;
		case "refresh":
			location.reload();
			break;
		case "location":
			location.href=cmd.l;
			break;
		case "run":
			eval(cmd.s);
			break;
		case "snackbar":
			LayerManager.getInstance().showSnackbar(cmd.t);
			break;
		case "setURL":
			history.replaceState(null, "", cmd.url);
			break;
		case "dismissBox":
		{
			var boxEl=ge(cmd.id);
			if(boxEl && boxEl.customData && boxEl.customData.box){
				((boxEl.customData.box) as Box).dismiss();
			}
			break;
		}
		case "layer":
			new SimpleLayer(cmd.h, cmd.c).show();
			break;
	}
}

function showPostReplyForm(id:number, formID:string="wallPostForm_reply", moveForm:boolean=true, containerPostID:number=0):boolean{
	var form=ge(formID);
	form.show();
	if(moveForm){
		var replies=ge("postReplies"+(containerPostID || id));
		replies.insertAdjacentElement("afterbegin", form);
	}
	form.customData.postFormObj.setupForReplyTo(id);
	return false;
}

function showPostCommentForm(id:number):boolean{
	var form=ge("wallPostForm_commentPost"+id);
	var link=ge("postCommentLinkWrap"+id);
	link.hide();
	form.show();
	form.customData.postFormObj.focus();
	return false;
}

function highlightComment(id:number):boolean{
	var existing=document.querySelectorAll(".highlight");
	for(var i=0;i<existing.length;i++) existing[i].classList.remove("highlight");
	ge("post"+id).classList.add("highlight");
	window.location.hash="#comment"+id;
	return false;
}

function likeOnClick(btn:HTMLAnchorElement):boolean{
	if(btn.hasAttribute("in_progress"))
		return false;
	if(!userConfig.uid)
		return false;
	var objType=btn.getAttribute("data-obj-type");
	var objID=btn.getAttribute("data-obj-id");
	var liked=btn.classList.contains("liked");
	var counter=ge("likeCounter"+objType.substring(0,1).toUpperCase()+objType.substring(1)+objID);
	var count=parseInt(counter.innerText);
	var ownAva=document.querySelector(".likeAvatars"+objID+".likeAvatars .currentUserLikeAva") as HTMLElement;
	if(btn.customData && btn.customData.popoverTimeout){
		clearTimeout(btn.customData.popoverTimeout);
		delete btn.customData.popoverTimeout;
	}
	if(!liked){
		counter.innerText=(count+1).toString();
		btn.classList.add("liked");
		if(count==0){
			counter.show();
			btn.classList.remove("revealOnHover");
		}
		if(btn._popover){
			if(!btn._popover.isShown())
				btn._popover.show(-1, -1, btn.qs("span.icon"));
			var title=btn._popover.getTitle();
			btn._popover.setTitle(btn.customData.altPopoverTitle);
			btn.customData.altPopoverTitle=title;
		}
		if(ownAva) ownAva.show();
	}else{
		counter.innerText=(count-1).toString();
		btn.classList.remove("liked");
		if(count==1){
			counter.hide();
			if(btn._popover){
				btn._popover.hide();
			}
			if(btn.classList.contains("commentLike")){
				btn.classList.add("revealOnHover");
			}
		}
		if(btn._popover){
			var title=btn._popover.getTitle();
			btn._popover.setTitle(btn.customData.altPopoverTitle);
			btn.customData.altPopoverTitle=title;
		}
		if(ownAva) ownAva.hide();
	}
	btn.setAttribute("in_progress", "");
	ajaxGet(btn.href, function(resp:any){
			btn.removeAttribute("in_progress");
			if(resp instanceof Array){
				for(var i=0;i<resp.length;i++){
					applyServerCommand(resp[i]);
				}
			}
		}, function(){
			btn.removeAttribute("in_progress");
			new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
			if(liked){
				counter.innerText=(count+1).toString();
				btn.classList.add("liked");
				if(count==0) counter.show();
			}else{
				counter.innerText=(count-1).toString();
				btn.classList.remove("liked");
				if(count==1) counter.hide();
			}
		});
	return false;
}

function likeOnMouseChange(wrap:HTMLElement, entered:boolean):void{
	var btn=wrap.querySelector(".popoverButton") as HTMLElement;

	var ev:MouseEvent=event as MouseEvent;
	var popover=btn._popover;
	if(entered){
		if(!btn.customData) btn.customData={};
		btn.customData.popoverTimeout=setTimeout(()=>{
			delete btn.customData.popoverTimeout;
			ajaxGet(btn.getAttribute("data-popover-url"), (resp:any)=>{
				if(!resp.content){
					return;
				}
				var popoverPlace=wrap.querySelector(".popoverPlace") as HTMLElement;
				if(!popover){
					popover=new Popover(popoverPlace);
					popover.setOnClick(()=>{
						popover.hide();
						LayerManager.getInstance().showBoxLoader();
						ajaxGetAndApplyActions(resp.fullURL);
					});
					btn._popover=popover;
				}
				popover.setTitle(resp.title);
				popover.setContent(resp.content);
				btn.customData.altPopoverTitle=resp.altTitle;
				if(resp.show)
					popover.show(ev.clientX, ev.clientY, btn.qs("span.icon"));
				for(var i=0;i<resp.actions.length;i++){
					applyServerCommand(resp.actions[i]);
				}
			}, ()=>{
				if(popover)
					popover.show(ev.clientX, ev.clientY, btn.qs("span.icon"));
			});
		}, 500);
	}else{
		// Some versions of Firefox can fire mouseLeave without a corresponding mouseEnter on page refresh
		if(btn.customData && btn.customData.popoverTimeout){
			clearTimeout(btn.customData.popoverTimeout);
			delete btn.customData.popoverTimeout;
		}else if(popover){
			popover.hide();
		}
	}
}

function showOptions(el:HTMLElement){
	new MobileOptionsBox(JSON.parse(el.getAttribute("data-options"))).show();
	return false;
}

function autoSizeTextArea(el:HTMLTextAreaElement){
	var updateHeight=function(){
		var st=window.getComputedStyle(el);
		var borderWidth=parseInt(st.borderBottomWidth)+parseInt(st.borderTopWidth);
		var minHeight=parseInt(st.minHeight);
		el.style.height=minHeight+"px";
		el.style.height=(el.scrollHeight+borderWidth)+"px";
	};
	el.resizeToFitContent=updateHeight;
	el.addEventListener("input", (ev:InputEvent)=>{
		updateHeight();
	}, false);
	updateHeight();
}

function addSendOnCtrlEnter(el:(HTMLTextAreaElement|HTMLInputElement)){
	el.addEventListener("keydown", (ev:KeyboardEvent)=>{
		if(ev.keyCode==13 && (isApple ? ev.metaKey : ev.ctrlKey)){
			(el.form.querySelector("input[type=submit]") as HTMLElement).click();
		}
	});
}

function loadOlderComments(id:number){
	var btn=ge("loadPrevBtn"+id);
	var loader=ge("prevLoader"+id);
	btn.hide();
	loader.show();
	var firstID=parseInt(btn.dataset.firstId);
	var heightBefore=document.body.offsetHeight;
	ajaxGetAndApplyActions("/posts/"+id+"/ajaxCommentPreview?firstID="+firstID, ()=>{
		document.documentElement.scrollTop+=document.body.offsetHeight-heightBefore;
	}, ()=>{
		btn.show();
		loader.hide();
	});
	return false;
}

function loadCommentBranch(el:HTMLElement, id:number, topLevelRepostID:number){
	var btn=ge("loadRepliesLink"+id);
	var loader=ge("repliesLoader"+id);
	var offset=parseInt(el.dataset.offset);
	btn.hide();
	loader.show();
	ajaxGetAndApplyActions("/posts/"+id+"/ajaxCommentBranch?offset="+(offset || 0)+(topLevelRepostID ? `&topLevel=${topLevelRepostID}` : ""), null, ()=>{
		btn.show();
		loader.hide();
	});
	return false;
}

function onPollInputChange(el:HTMLInputElement){
	var form=el.form;
	if(submittingForm==form)
		return;
	if(el.type=="radio"){
		el.labels[0].appendChild(ce("span", {className: "inlineLoader"}));
		ajaxSubmitForm(form);
	}else{
		var cboxes=el.form.querySelectorAll("input[type=checkbox]").unfuck();
		var anyChecked=false;
		for(var cbox of cboxes){
			if((cbox as HTMLInputElement).checked){
				anyChecked=true;
				break;
			}
		}
		(el.form.qs("input[type=submit]") as HTMLInputElement).disabled=!anyChecked;
	}
}

function doneEditingPost(id:number){
	var fid="wallPostForm_edit"+id;
	ge(fid).remove();
	ge("postEditingLabel"+id).remove();
}

function cancelEditingPost(id:number){
	doneEditingPost(id);
	ge("postInner"+id).show();
	var actions=ge("postFloatingActions"+id);
	if(actions)
		actions.show();
	var inReply=ge("inReplyTo"+id);
	if(inReply)
		inReply.show();
}

function copyText(text:string, doneMsg:string){
	if(!navigator.clipboard){
		var ta=ce("textarea", {value: text});
		ta.style.position="fixed";
		ta.style.left=ta.style.top=ta.style.width=ta.style.height="0";
		document.body.appendChild(ta);
		ta.focus();
		ta.select();
		try{
			document.execCommand("copy");
			new MessageBox("", doneMsg, lang("close")).show();
		}catch(err){
			new MessageBox(lang("error"), err.toString(), lang("close")).show();
		}
		document.body.removeChild(ta);
		return;
	}

	navigator.clipboard.writeText(text).then(()=>{
		LayerManager.getInstance().showSnackbar(doneMsg);
	}, (err)=>{
		LayerManager.getInstance().showSnackbar(lang("error")+"\n"+err.toString());
	});
}

function showGraffitiBox(el:HTMLAnchorElement):boolean{
	class GraffitiBox extends Box{
		public constructor(el:HTMLAnchorElement){
			super(el.dataset.boxTitle, [lang("close")]);
			var imgEl;
			this.setContent(ce("div", {className: ""}, [
				imgEl=ce("img", {width: 586, height: 293, src: el.href})
			]));
			if(!mobile){
				this.contentWrap.style.padding="10px";
			}else{
				imgEl.style.width="100%";
				imgEl.style.height="auto";
			}
		}

		public show(){
			super.show();
			if(!mobile){
				(this.getContent().querySelector(".boxLayer") as HTMLElement).style.width="606px";
			}
		}
	}
	new GraffitiBox(el).show();
	return false;
}

function initAjaxSearch(fieldID:string){
	var input=ge(fieldID) as HTMLInputElement;
	var inputWrap=input.parentElement;
	var currentXHR:XMLHttpRequest=null;
	var debounceTimeout:number=null;
	var focusedEl:Element;
	var focusedElSelStart:number, focusedElSelEnd:number;
	var extraFieldIDs:string[];
	if(input.dataset.extraFields){
		extraFieldIDs=input.dataset.extraFields.split(",");
	}else{
		extraFieldIDs=[];
	}

	const onInputListener=(ev:Event)=>{
		if(ev.target!=input && (!extraFieldIDs || !(ev.target instanceof HTMLElement) || extraFieldIDs.indexOf(ev.target.id)==-1))
			return;
		if(debounceTimeout){
			clearTimeout(debounceTimeout);
		}
		if(currentXHR){
			currentXHR.abort();
			currentXHR=null;
		}
		debounceTimeout=setTimeout(()=>{
			debounceTimeout=null;
			performSearch(input.value);
		}, 300);
	};
	const ajaxDone=()=>{
		currentXHR=null;
		if(focusedEl && focusedEl.id){
			var newEl=ge(focusedEl.id);
			if(newEl && newEl!=focusedEl && newEl instanceof HTMLInputElement){
				newEl.focus();
				newEl.selectionStart=focusedElSelStart;
				newEl.selectionEnd=focusedElSelEnd;
			}
		}
		focusedEl=null;
	};
	const ajaxBeforeDone=()=>{
		focusedEl=document.activeElement;
		if(focusedEl && focusedEl instanceof HTMLInputElement){
			focusedElSelStart=focusedEl.selectionStart;
			focusedElSelEnd=focusedEl.selectionEnd;
		}
	};

	const performSearch=(q:string)=>{
		var baseURL=input.dataset.baseUrl;
		var qstr:string;
		if(baseURL.indexOf('?')!=-1){
			var parts=baseURL.split('?', 2);
			qstr=parts[1];
			baseURL=parts[0];
		}else{
			qstr="";
		}
		var params=new URLSearchParams(qstr);
		params.set("q", q);
		var extraFields=input.dataset.extraFields;
		if(extraFields){
			for(var fid of extraFields.split(',')){
				var field=ge(fid) as HTMLInputElement;
				if(field){
					if(field.value.length)
						params.set(field.name, field.value);
					else
						params.delete(field.name);
				}
			}
		}
		var url=baseURL+"?"+params.toString();
		currentXHR=ajaxGetAndApplyActions(url, ajaxDone, ajaxDone, ajaxBeforeDone);
	};
	input.addEventListener("input", onInputListener);
	ge("ajaxUpdatable").addEventListener("input", onInputListener);
}

function quoteRegExp(str:string):string{
	return (str+'').replace(/[.?*+^$[\]\\(){}|-]/g, "\\$&");
}

function makeAvatar(urls:string[], baseSize:string, customSize:number=0):HTMLElement{
	var el;
	var size=customSize || {s: 50, m: 100, l: 200, xl: 400}[baseSize];
	if(!urls || !urls.length){
		el=ce("span", {className: "ava avaPlaceholder size"+baseSize.toUpperCase()});
	}else{
		el=ce("span", {className: "ava avaHasImage size"+baseSize.toUpperCase()}, [
			ce("picture", {}, [
				ce("source", {srcset: urls[1]+", "+urls[3]+" 2x", type: "image/webp"}),
				ce("source", {srcset: urls[0]+", "+urls[2]+" 2x", type: "image/jpeg"}),
				ce("img", {src: urls[0], className: "avaImage", width: size, height: size})
			])
		]);
	}
	if(customSize){
		el.style.width=el.style.height=customSize+"px";
	}
	return el;
}

function showMailFormBox(el:HTMLAnchorElement){
	LayerManager.getInstance().showBoxLoader();
	ajaxGet(el.href, (r)=>{
		var cont=ce("div", {innerHTML: r.toString()});
		var form=cont.qs("form") as HTMLFormElement;
		form.dataset.submitterId="mailMessageFormSubmit";
		var postForm:PostForm;
		var box=new Box(lang("mail_tab_compose"), [lang("send"), lang("cancel")], (idx)=>{
			if(idx==0){
				var onDone=(success:boolean)=>{
					if(success){
						box.dismiss();
					}else{
						var btn=this.getButton(0);
						btn.removeAttribute("disabled");
						box.getButton(1).removeAttribute("disabled");
						box.showButtonLoading(0, false);
					}
				};
				if(postForm.send(onDone)){
					var btn=box.getButton(0);
					btn.setAttribute("disabled", "");
					box.getButton(1).setAttribute("disabled", "");
					box.showButtonLoading(0, true);
				}
			}else{
				box.dismiss();
			}
		});
		box.setContent(cont);
		box.show();
		var button=box.getButton(0);
		button.id="mailMessageFormSubmit";
		postForm=new PostForm(ge("wallPostForm_mailMessage"));
		postForm.onSendDone=(success)=>{
			if(success)
				box.dismiss();
		};
		postForm.focus();
	}, (msg)=>{
		new MessageBox(lang("error"), msg, lang("close")).show();
	}, "text");
}

function showTooltip(el:HTMLElement, text:string){
	if(!el.customData)
		el.customData={};
	var ttEl:HTMLElement=el.customData.tooltip;
	if(!ttEl){
		el.customData.tooltip=ttEl=ce("div", {className: "tooltipOuter"}, [
			ce("div", {className: "tooltip"}, [
				ce("div", {className: "tooltipInner"}, [text])
			])
		]);
		el.insertAdjacentElement("afterbegin", ttEl);
	}
	ttEl.showAnimated();
}

function hideTooltip(el:HTMLElement){
	var ttEl:HTMLElement=el.customData && el.customData.tooltip;
	if(ttEl){
		ttEl.hideAnimated();
	}
}

function expandAllCommentCWs(){
	for(var cbox of document.querySelectorAll(".commentCWCheckbox").unfuck()){
		if(cbox instanceof HTMLInputElement && !cbox.checked){
			cbox.checked=true;
		}
	}
}

function addParamsToURL(url:string, params:{[key:string]:string}):string{
	var paramsParts=[];
	for(var key in params){
		var part=encodeURIComponent(key);
		if(params[key])
			part+='='+encodeURIComponent(params[key]);
		paramsParts.push(part);
	}
	var fragment="";
	if(url.indexOf('#')!=-1){
		var parts=url.split("#", 2);
		url=parts[0];
		fragment='#'+parts[1];
	}
	return url+(url.indexOf('?')==-1 ? '?' : '&')+paramsParts.join('&')+fragment;
}

function initTabbedBox(tabBar:HTMLElement, content:HTMLElement){
	var tabs=tabBar.querySelectorAll("a").unfuck() as HTMLAnchorElement[];
	var panes=content.children.unfuck();
	var activeTab=tabBar.qs("a.selected") as HTMLAnchorElement;
	var loader=tabBar.qs(".loader");
	var listener=(ev:MouseEvent)=>{
		ev.preventDefault();
		if(ev.target==activeTab || !(ev.target instanceof HTMLAnchorElement))
			return;
		var newTab=ev.target;
		var index=tabs.indexOf(newTab);
		if(index==-1)
			return;
		var newPane=panes[index];
		var oldPane=panes[tabs.indexOf(activeTab)];
		if(!newPane.customData.loaded){
			loader.show();
			setGlobalLoading(true);
			ajaxGet(addParamsToURL(newTab.href, {fromTab: null}), (r)=>{
				newPane.innerHTML=r;
				newPane.customData.loaded=true;
				initDynamicControls(newPane);
				loader.hide();
				newPane.show();
				oldPane.hide();
				activeTab.classList.remove("selected");
				newTab.classList.add("selected");
				activeTab=newTab;
				setGlobalLoading(false);
				LayerManager.getInstance().updateAllTopOffsets();
			}, (err)=>{
				LayerManager.getInstance().showSnackbar(err);
				loader.hide();
				setGlobalLoading(false);
			}, "text");
		}else{
			activeTab.classList.remove("selected");
			activeTab=newTab;
			activeTab.classList.add("selected");
			newPane.show();
			oldPane.hide();
			LayerManager.getInstance().updateAllTopOffsets();
			if(newPane.customData.onShown)
				newPane.customData.onShown();
		}
	};
	for(var tab of tabs){
		tab.addEventListener("click", listener);
	}
	for(var pane of panes){
		if(!pane.customData){
			pane.customData={};
		}
		pane.customData.loaded=pane.innerHTML.trim().length>0;
	}
}

function initExpandingProfileColumn(wide:HTMLElement, narrow:HTMLElement, container:HTMLElement){
	var observer=new IntersectionObserver((entries, observer)=>{
		for(var entry of entries){
			if(entry.isIntersecting){
				container.classList.remove("expanded");
				wide.style.marginTop="";
			}else{
				// Find the topmost visible post
				var topmostPost:HTMLElement;
				var topmostPostTop:number;
				for(var post of wide.querySelectorAll(".wallRow").unfuck()){
					if(post instanceof HTMLElement){
						var rect=post.getBoundingClientRect();
						if(rect.top>=0){
							topmostPost=post;
							topmostPostTop=rect.top;
							break;
						}
					}
				}
				container.classList.add("expanded");
				if(topmostPost){
					var rect=topmostPost.getBoundingClientRect();
					var offset=topmostPostTop-rect.top;
					wide.style.marginTop=Math.round(offset)+"px";
				}
			}
		}
	});
	observer.observe(narrow);
}

function initEmbedPreview(postID:number){
	var tab=ge("postEmbedTab"+postID);
	tab.customData.onShown=()=>{
		delete tab.customData.onShown;
		actuallyInitEmbedPreview(postID);
	};
}

function actuallyInitEmbedPreview(postID:number){
	var iframe=ge("embedPreview"+postID) as HTMLIFrameElement;
	var messageListener=(ev:MessageEvent)=>{
		if(ev.source!=iframe.contentWindow)
			return;
		if(ev.data.act=="setHeight"){
			iframe.height=ev.data.height;
			LayerManager.getInstance().updateAllTopOffsets();
		}
	};
	window.addEventListener("message", messageListener);
	LayerManager.getInstance().getTopLayer().dismissCallbacks.push(()=>{
		window.removeEventListener("message", messageListener);
	});
	iframe.src="/posts/"+postID+"/embed";
}

function showAjaxHoverCard(link:HTMLElement, ev:MouseEvent, ajaxURL:string){
	var popover=link._popover;
	var container=link.closest(".hoverCardContainer") as HTMLElement;
	var setupHider=()=>{
		container.addEventListener("mouseleave", function(ev){
			link.customData.popoverHideTimeout=setTimeout(()=>{
				delete link.customData.popoverHideTimeout;
				popover.hide();
			}, 100);
			container.removeEventListener("mouseleave", arguments.callee as any);
		}, false);
	};
	if(link.customData && link.customData.popoverHideTimeout){
		clearTimeout(link.customData.popoverHideTimeout);
		delete link.customData.popoverHideTimeout;
		setupHider();
		return;
	}
	if(popover){
		if(!popover.isShown()){
			popover.show(ev.clientX, ev.clientY);
			setupHider();
		}
		return;
	}
	if(!link.customData) link.customData={};

	// Cancel things if the mouse moves outside of the container before the timeout elapses
	var timeoutCanceler=(ev:MouseEvent)=>{
		if(link.customData.popoverTimeout){
			clearTimeout(link.customData.popoverTimeout);
			delete link.customData.popoverTimeout;
		}
		container.removeEventListener("mouseleave", timeoutCanceler);
	};
	container.addEventListener("mouseleave", timeoutCanceler, false);
	// Track mouse movement for the duration of timeout/ajax to show the popover in a more expected place
	var moveTracker=(moveEv:MouseEvent)=>{
		ev=moveEv;
	};
	container.addEventListener("mousemove", moveTracker, false);

	link.customData.popoverTimeout=setTimeout(()=>{
		container.removeEventListener("mouseleave", timeoutCanceler);
		delete link.customData.popoverTimeout;

		// If the mouse moves outside of the container during the ajax request,
		// still make the request and initialize the popover, but don't show it
		var canceled=false;
		var canceler=(ev:MouseEvent)=>{
			canceled=true;
			container.removeEventListener("mouseleave", canceler);
		};
		container.addEventListener("mouseleave", canceler, false);

		ajaxGet(ajaxURL, (resp:any)=>{
			container.removeEventListener("mouseleave", canceler);
			container.removeEventListener("mousemove", moveTracker);
			if(!resp){
				return;
			}
			if(!popover){
				popover=new Popover(container);
				link._popover=popover;
			}
			popover.setContent(resp);
			if(!canceled){
				popover.show(ev.clientX, ev.clientY);
				setupHider();
			}
		}, ()=>{
			container.removeEventListener("mouseleave", canceler);
			container.removeEventListener("mousemove", moveTracker);
			if(popover && !canceled){
				popover.show(ev.clientX, ev.clientY);
				setupHider();
			}
		}, "text");
	}, 300);
}

function showMentionHoverCard(link:HTMLElement, ev:MouseEvent){
	var userID=link.dataset.userId;
	showAjaxHoverCard(link, ev, "/users/"+userID+"/hoverCard");
}

function showParentCommentHoverCard(link:HTMLElement, ev:MouseEvent){
	var commentID=link.dataset.parentId;
	showAjaxHoverCard(link, ev, "/posts/"+commentID+"/hoverCard");
}

function closeTopmostLayer(){
	var topLayer=LayerManager.getInstance().getTopLayer();
	if(topLayer)
		LayerManager.getInstance().dismiss(topLayer);
}

function saveRemoteInteractionDomain(){
	var input=ge("remoteInteractionDomain");
	if(input instanceof HTMLInputElement){
		try{
			window.localStorage.setItem("remoteInteractionDomain", input.value);
		}catch(e){
			console.log(e);
		}
	}
}

function restoreRemoteInteractionDomain(){
	var input=ge("remoteInteractionDomain");
	if(input instanceof HTMLInputElement){
		try{
			var savedDomain=window.localStorage.getItem("remoteInteractionDomain");
			if(savedDomain){
				input.value=savedDomain;
			}
		}catch(e){
			console.log(e);
		}
		if(!mobile)
			input.focus();
	}
}

