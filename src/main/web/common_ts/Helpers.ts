var submittingForm:HTMLFormElement=null;

interface String{
	format(...args:(string|number)[]):string;
	escapeHTML():string;
}

String.prototype.format=function(...args:(string|number)[]){
	var currentIndex=0;
	return this.replace(/%(?:(\d+)\$)?([ds%])/gm, function(match:string, g1:string, g2:string){
		if(g2=="%")
			return "%";
		var index=g1 ? (parseInt(g1)-1) : currentIndex;
		currentIndex++;
		switch(g2){
			case "d":
				return Number(args[index]);
			case "s":
				return args[index].toString().escapeHTML();
		}
	});
};

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

function ajaxPost(uri:string, params:any, onDone:Function, onError:Function):void{
	var xhr:XMLHttpRequest=new XMLHttpRequest();
	xhr.open("POST", uri);
	xhr.onload=function(){
		onDone(xhr.response);
	};
	xhr.onerror=function(ev:Event){
		console.log(ev);
		onError();
	};
	xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	var formData:string[]=[];
	for(var key in params){
		formData.push(key+"="+encodeURIComponent(params[key]));
	}
	formData.push("_ajax=1");
	xhr.responseType="json";
	xhr.send(formData.join("&"));
}

function ajaxGet(uri:string, onDone:Function, onError:Function):void{
	var xhr:XMLHttpRequest=new XMLHttpRequest();
	if(uri.indexOf("?")!=-1)
		uri+="&_ajax=1";
	else
		uri+="?_ajax=1";
	xhr.open("GET", uri);
	xhr.onload=function(){
		onDone(xhr.response);
	};
	xhr.onerror=function(ev:Event){
		console.log(ev);
		onError();
	};
	xhr.responseType="json";
	xhr.send();
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

function hide(el:HTMLElement):void{
	el.style.display="none";
}

function show(el:HTMLElement):void{
	el.style.display="";
}

function isVisible(el:HTMLElement):boolean{
	return el.style.display!="none";
}

function lang(key:(string|Array<string>)):string{
	if(!(key instanceof Array))
		return langKeys[key as string] ? langKeys[key as string] : key;
	var _key=key[0];
	if(!langKeys[_key])
		return key.toString().escapeHTML();
	return langKeys[_key].format(key.slice(1));
}

function setGlobalLoading(loading:boolean):void{
	document.body.style.cursor=loading ? "progress" : "";
}

function ajaxConfirm(titleKey:string, msgKey:(string|Array<string>), url:string, params:any={}):boolean{
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
		}, function(){
			setGlobalLoading(false);
			box.dismiss();
			new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
		});
	});
	box.show();
	return false;
}

function ajaxSubmitForm(form:HTMLFormElement, onDone:{():void}=null):boolean{
	if(submittingForm)
		return false;
	submittingForm=form;
	var submitBtn=form.querySelector("input[type=submit]");
	if(submitBtn)
		submitBtn.classList.add("loading");
	setGlobalLoading(true);
	var data:any={};
	var elems=form.elements;
	for(var i=0;i<elems.length;i++){
		var el=elems[i] as any;
		if(!el.name)
			continue;
		if(el.type!="radio" || (el.type=="radio" && el.checked))
			data[el.name]=el.value;
	}
	data.csrf=userConfig.csrf;
	ajaxPost(form.action, data, function(resp:any){
		submittingForm=null;
		if(submitBtn)
			submitBtn.classList.remove("loading");
		setGlobalLoading(false);
		if(resp instanceof Array){
			for(var i=0;i<resp.length;i++){
				applyServerCommand(resp[i]);
			}
		}
		if(onDone) onDone();
	}, function(){
		submittingForm=null;
		if(submitBtn)
			submitBtn.classList.remove("loading");
		setGlobalLoading(false);
		new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
		if(onDone) onDone();
	});
	return false;
}

function ajaxFollowLink(link:HTMLAnchorElement):boolean{
	if(link.getAttribute("data-ajax")){
		setGlobalLoading(true);
		ajaxGet(link.href, function(resp:any){
				setGlobalLoading(false);
				if(resp instanceof Array){
					for(var i=0;i<resp.length;i++){
						applyServerCommand(resp[i]);
					}
				}
			}, function(){
				setGlobalLoading(false);
				new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
			});
		return true;
	}
	return false;
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
		case "msgBox":
			new MessageBox(cmd.t, cmd.m, cmd.b).show();
			break;
		case "formBox":
			new FormBox(cmd.t, cmd.m, cmd.b, cmd.fa).show();
			break;
		case "show":
		{
			var ids:string[]=cmd.ids;
			for(var i=0;i<ids.length;i++){
				var el=document.getElementById(ids[i]);
				if(el){
					show(el);
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
					hide(el);
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
		case "refresh":
			location.reload();
			break;
	}
}

function showPostReplyForm(id:number):boolean{
	var form=document.getElementById("wallPostForm_reply");
	var replies=document.getElementById("postReplies"+id);
	replies.insertAdjacentElement("afterbegin", form);

	postForms["wallPostForm_reply"].setupForReplyTo(id);

	return false;
}

function highlightComment(id:number):boolean{
	var existing=document.querySelectorAll(".highlight");
	for(var i=0;i<existing.length;i++) existing[i].classList.remove("highlight");
	ge("post"+id).classList.add("highlight");
	window.location.hash="#comment"+id;
	return false;
}
