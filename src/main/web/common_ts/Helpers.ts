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
	popover:Popover;
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
	this.style.display="none";
};

HTMLElement.prototype.hideAnimated=function(animName:AnimationDescription={keyframes: [{opacity: 1}, {opacity: 0}], options: {duration: 200, easing: "ease"}}, onEnd:{():void}=null):void{
	if(this.currentVisibilityAnimation){
		this.currentVisibilityAnimation.cancel();
	}
	this.currentVisibilityAnimation=this.anim(animName.keyframes, animName.options, ()=>{
		this.hide();
		this.currentVisibilityAnimation=null;
		if(onEnd) onEnd();
	});
};

HTMLElement.prototype.show=function():void{
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
		if(Math.floor(xhr.status/100)==2)
			onDone(xhr.response);
		else
			onError(xhr.statusText);
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
	xhr.responseType=responseType;
	xhr.send(formData.join("&"));
	return xhr;
}

function ajaxGet(uri:string, onDone:Function, onError:Function, responseType:XMLHttpRequestResponseType="json"):XMLHttpRequest{
	var xhr:XMLHttpRequest=new XMLHttpRequest();
	if(uri.indexOf("?")!=-1)
		uri+="&_ajax=1";
	else
		uri+="?_ajax=1";
	xhr.open("GET", uri);
	xhr.onload=function(){
		if(Math.floor(xhr.status/100)==2)
			onDone(xhr.response);
		else
			onError(xhr.statusText);
	};
	xhr.onerror=function(ev:Event){
		console.log(ev);
		onError(xhr.statusText);
	};
	xhr.responseType=responseType;
	xhr.send();
	return xhr;
}

function ajaxUpload(uri:string, fieldName:string, file:File, onDone:{(resp:any):boolean}=null, onError:Function=null, onProgress:{(progress:number):void}=null):void{
	var formData=new FormData();
	formData.append(fieldName, file);
	var xhr=new XMLHttpRequest();
	if(uri.indexOf("?")!=-1)
		uri+="&";
	else
		uri+="?";
	uri+="_ajax=1&csrf="+userConfig.csrf;
	xhr.open("POST", uri);
	xhr.onload=function(){
		var resp=xhr.response;
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
		return key.replace("_", " ");
	var v=langKeys[key];
	if(typeof v==="function")
		return (v as Function).apply(this, [args]);
	return v as string;
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

function ajaxConfirm(titleKey:string, msgKey:string, url:string, params:any={}):boolean{
	var box:ConfirmBox;
	box=new ConfirmBox(lang(titleKey), lang(msgKey), function(){
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

function ajaxSubmitForm(form:HTMLFormElement, onDone:{(resp?:any):void}=null, submitter:HTMLElement=null):boolean{
	if(submittingForm)
		return false;
	if(!form.checkValidity()){
		setGlobalLoading(false);
		return false;
	}
	submittingForm=form;
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
	ajaxPost(form.action, data, function(resp:any){
		submittingForm=null;
		if(submitter)
			submitter.classList.remove("loading");
		setGlobalLoading(false);
		if(resp instanceof Array){
			for(var i=0;i<resp.length;i++){
				applyServerCommand(resp[i]);
			}
		}
		if(onDone) onDone(true);
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
		if(link.dataset.ajaxHide!=undefined)
			ge(link.dataset.ajaxHide).hide();
		if(link.dataset.ajaxShow!=undefined)
			ge(link.dataset.ajaxShow).show();
		link.classList.add("ajaxLoading");
		ajaxGetAndApplyActions(link.href);
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

function ajaxGetAndApplyActions(url:string, onDone:{():void}=null, onError:{():void}=null):void{
	setGlobalLoading(true);
	ajaxGet(url, function(resp:any){
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
		case "box":
		{
			var box=cmd.s ? new ScrollableBox(cmd.t, [lang("close")]) : new BoxWithoutContentPadding(cmd.t);
			var cont=ce("div");
			if(cmd.i){
				cont.id=cmd.i;
			}
			cont.innerHTML=cmd.c;
			box.setContent(cont);
			box.show();
			if(cmd.w){
				(box.getContent().querySelector(".boxLayer") as HTMLElement).style.width=cmd.w+"px";
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
	}
}

function showPostReplyForm(id:number, formID:string="wallPostForm_reply", moveForm:boolean=true):boolean{
	var form=ge(formID);
	form.show();
	if(moveForm){
		var replies=ge("postReplies"+id);
		replies.insertAdjacentElement("afterbegin", form);
	}
	postForms[formID].setupForReplyTo(id);
	return false;
}

function showPostCommentForm(id:number):boolean{
	var form=ge("wallPostForm_commentPost"+id);
	var link=ge("postCommentLinkWrap"+id);
	link.hide();
	form.show();
	postForms[form.id].focus();
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
		if(count==0) counter.show();
		if(btn.popover){
			if(!btn.popover.isShown())
				btn.popover.show();
			var title=btn.popover.getTitle();
			btn.popover.setTitle(btn.customData.altPopoverTitle);
			btn.customData.altPopoverTitle=title;
		}
		if(ownAva) ownAva.show();
	}else{
		counter.innerText=(count-1).toString();
		btn.classList.remove("liked");
		if(count==1){
			counter.hide();
			if(btn.popover){
				btn.popover.hide();
			}
		}
		if(btn.popover){
			var title=btn.popover.getTitle();
			btn.popover.setTitle(btn.customData.altPopoverTitle);
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
	var objID=btn.getAttribute("data-obj-id");
	var objType=btn.getAttribute("data-obj-type");

	var ev:MouseEvent=event as MouseEvent;
	var popover=btn.popover;
	if(entered){
		if(!btn.customData) btn.customData={};
		btn.customData.popoverTimeout=setTimeout(()=>{
			delete btn.customData.popoverTimeout;
			ajaxGet(btn.getAttribute("data-popover-url"), (resp:any)=>{
				if(!popover){
					popover=new Popover(wrap.querySelector(".popoverPlace"));
					popover.setOnClick(()=>{
						popover.hide();
						LayerManager.getInstance().showBoxLoader();
						ajaxGetAndApplyActions(resp.fullURL);
					});
					btn.popover=popover;
				}
				popover.setTitle(resp.title);
				popover.setContent(resp.content);
				btn.customData.altPopoverTitle=resp.altTitle;
				if(resp.show)
					popover.show(ev.offsetX, ev.offsetY);
				for(var i=0;i<resp.actions.length;i++){
					applyServerCommand(resp.actions[i]);
				}
			}, ()=>{
				if(popover)
					popover.show(ev.offsetX, ev.offsetY);
			});
		}, 500);
	}else{
		if(btn.customData.popoverTimeout){
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

function openPhotoViewer(el:HTMLElement){
	var parent=el.parentNode.parentNode;
	var photoList:PhotoInfo[]=[];
	var index=0;
	var j=0;
	for(var i=0;i<parent.children.length;i++){
		var link=parent.children[i].querySelector("a.photo");
		if(!link) continue;
		var size=link.getAttribute("data-size").split(" ");
		photoList.push({webp: link.getAttribute("data-full-webp"), jpeg: link.getAttribute("data-full-jpeg"), width: parseInt(size[0]), height: parseInt(size[1])});
		if(link==el){
			index=j;
		}
		j++;
	}
	new PhotoViewerLayer(photoList, index).show();
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

function loadOlderComments(id:number){
	var btn=ge("loadPrevBtn"+id);
	var loader=ge("prevLoader"+id);
	btn.hide();
	loader.show();
	var firstID=parseInt(btn.dataset.firstId);
	ajaxGetAndApplyActions("/posts/"+id+"/ajaxCommentPreview?firstID="+firstID, null, ()=>{
		btn.show();
		loader.hide();
	});
	return false;
}

function loadCommentBranch(id:number){
	var btn=ge("loadRepliesLink"+id);
	var loader=ge("repliesLoader"+id);
	btn.hide();
	loader.show();
	ajaxGetAndApplyActions("/posts/"+id+"/ajaxCommentBranch", null, ()=>{
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
	delete postForms[fid];
	ge("postEditingLabel"+id).remove();
}

function cancelEditingPost(id:number){
	doneEditingPost(id);
	ge("postInner"+id).show();
}
