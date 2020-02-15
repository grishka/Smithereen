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
		});
	});
	box.show();
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
					console.log("removing:", el);
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
	}
}
