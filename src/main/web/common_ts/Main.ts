///<reference path="./PostForm.ts"/>

declare var userConfig:any;
declare var langKeys:{[key:string]:string|string[]};
declare var mobile:boolean;

// Use Cmd instead of Ctrl on Apple devices.
var isApple:boolean=navigator.platform.indexOf("Mac")==0 || navigator.platform=="iPhone" || navigator.platform=="iPad" || navigator.platform=="iPod touch";
var postForms:{[key:string]:PostForm}={};

window.onerror=function(message, source, lineno, colno, error){
	alert("JS error:\n\n"+message);
};

var timeZone:String;
if(window["Intl"]){
	timeZone=Intl.DateTimeFormat().resolvedOptions().timeZone;
}else{
	var offset:number=new Date().getTimezoneOffset();
	timeZone="GMT"+(offset>0 ? "+" : "")+Math.floor(offset/60)+(offset%60!=0 ? (":"+(offset%60)) : "");
}
if(!userConfig || !userConfig["timeZone"] || timeZone!=userConfig.timeZone){
	ajaxPost("/settings/setTimezone", {tz: timeZone}, function(resp:any){}, function(){});
}
var supportsFormSubmitter=window.SubmitEvent!==undefined;

document.body.addEventListener("click", function(ev){
	var el:HTMLElement=ev.target as HTMLElement;
	if(((el.tagName=="INPUT" && (el as HTMLInputElement).type=="submit") || el.tagName=="BUTTON") && !supportsFormSubmitter){
		var form:HTMLFormElement=(el as any).form;
		if(!form.customData)
			form.customData={};
		form.customData.lastSubmitter=el;
	}
	do{
		if(el.tagName=="A"){
			if(ajaxFollowLink(el as HTMLAnchorElement)){
				ev.preventDefault();
			}
			return;
		}
		el=el.parentElement;
	}while(el && el.tagName!="BODY");
}, false);

function updatePostForms(){
	document.querySelectorAll(".wallPostForm").forEach(function(el){
		if(!postForms[el.id])
			postForms[el.id]=new PostForm(el as HTMLElement);
	});
}
updatePostForms();

var dragTimeout=-1;
var dragEventCount=0;
document.body.addEventListener("dragenter", function(ev:DragEvent){
	if(ev.dataTransfer.types.indexOf("Files")!=-1)
		document.body.classList.add("fileIsBeingDragged");
	ev.preventDefault();
	dragEventCount++;
	if(dragTimeout!=-1){
		clearTimeout(dragTimeout);
		dragTimeout=-1;
	}
}, false);
document.body.addEventListener("dragover", function(ev:DragEvent){
	ev.preventDefault();
}, false);
document.body.addEventListener("dragleave", function(ev:DragEvent){
	dragEventCount--;
	if(dragEventCount==0 && dragTimeout==-1){
		dragTimeout=setTimeout(function(){
			dragTimeout=-1;
			document.body.classList.remove("fileIsBeingDragged");
			dragEventCount=0;
		}, 100);
	}
}, false);
document.body.addEventListener("drop", function(ev:DragEvent){
	if(dragTimeout!=-1)
		clearTimeout(dragTimeout);
	dragTimeout=-1;
	dragEventCount=0;
	document.body.classList.remove("fileIsBeingDragged");
}, false);
document.body.addEventListener("submit", function(ev:SubmitEvent){
	var form:HTMLFormElement=ev.target as HTMLFormElement;
	if(form.dataset.ajax==undefined)
		return;
	ev.preventDefault();
	if(supportsFormSubmitter){
		ajaxSubmitForm(form, null, ev.submitter);
	}else{
		ajaxSubmitForm(form, null, form.customData.lastSubmitter);
	}
}, false);

var elevator=ge("elevator");
if(elevator){
	var currentElevatorAlpha=0.0;
	var elevatorBackY=0;
	var elevatorVisible=document.documentElement.scrollTop>200;
	if(!elevatorVisible)
		elevator.hide();
	document.addEventListener("scroll", function(ev:Event){
		var sy=document.documentElement.scrollTop;
		var alpha;
		if(elevatorBackY>0 && sy>200){
			elevatorBackY=0;
			elevator.classList.remove("goBackDown");
		}
		if(sy>=200 && sy<400){
			alpha=(sy-200)/200;
		}else if(sy>=400){
			alpha=1;
		}else if(sy<200 && elevatorBackY>0){
			alpha=1-sy/200;
		}else{
			alpha=0;
			if(elevatorVisible){
				elevatorVisible=false;
				elevator.hide();
			}
		}
		if(alpha>0 && !elevatorVisible){
			elevatorVisible=true;
			elevator.show();
		}
		if(alpha!=currentElevatorAlpha){
			currentElevatorAlpha=alpha;
			elevator.style.opacity=alpha+"";
		}
	}, {passive: true});
	elevator.onclick=function(){
		if(elevatorBackY>0){
			document.documentElement.scrollTop=elevatorBackY;
			elevatorBackY=0;
			elevator.classList.remove("goBackDown");
			document.body.qs(".wrap").anim([{transform: "translateY(20px)"}, {transform: "translateY(0)"}], {duration: 200, easing: "cubic-bezier(0.22, 1, 0.36, 1)"});
		}else{
			elevator.classList.add("goBackDown");
			elevatorBackY=document.documentElement.scrollTop;
			document.documentElement.scrollTop=0;
			document.body.qs(".wrap").anim([{transform: "translateY(-20px)"}, {transform: "translateY(0)"}], {duration: 200, easing: "cubic-bezier(0.22, 1, 0.36, 1)"});
		}
		return false;
	};
}
