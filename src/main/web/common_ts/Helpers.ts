var submittingForm:HTMLFormElement=null;

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

function hide(el:HTMLElement):void{
	el.style.display="none";
}

function show(el:HTMLElement):void{
	el.style.display="";
}

function isVisible(el:HTMLElement):boolean{
	return el.style.display!="none";
}

function lang(key:string):string{
	return langKeys[key] ? langKeys[key] : key;
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
		}, function(){
			setGlobalLoading(false);
			box.dismiss();
			new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
		});
	});
	box.show();
	return false;
}

function ajaxSubmitForm(form:HTMLFormElement):boolean{
	if(submittingForm)
		return false;
	submittingForm=form;
	var submitBtn=form.querySelector("input[type=submit]");
	submitBtn.classList.add("loading");
	setGlobalLoading(true);
	var data:any={};
	var elems=form.elements;
	for(var i=0;i<elems.length;i++){
		var el=elems[i] as any;
		if(!el.name)
			continue;
		data[el.name]=el.value;
	}
	ajaxPost(form.action, data, function(resp:any){
		submittingForm=null;
		submitBtn.classList.remove("loading");
		setGlobalLoading(false);
		if(resp instanceof Array){
			for(var i=0;i<resp.length;i++){
				applyServerCommand(resp[i]);
			}
		}
	}, function(){
		submittingForm=null;
		submitBtn.classList.remove("loading");
		setGlobalLoading(false);
		new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
	});
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
	}
}

function showPostReplyForm(id:number):boolean{
	var form=document.getElementById("wallPostForm");
	var replies=document.getElementById("postReplies"+id);
	replies.insertAdjacentElement("afterbegin", form);
	var hidden:HTMLInputElement=document.getElementById("postFormReplyTo") as HTMLInputElement;
	hidden.value=id+"";
	var field:HTMLTextAreaElement=document.getElementById("postFormText") as HTMLTextAreaElement;
	var name:string=document.getElementById("post"+id).getAttribute("data-reply-name");
	if(name){
		if(field.value.length==0 || (field.hasAttribute("data-reply-name") && field.value==field.getAttribute("data-reply-name"))){
			field.value=name+", ";
			field.setAttribute("data-reply-name", name+", ");
		}
	}
	field.focus();
	return false;
}
