///<reference path="./PostForm.ts"/>

declare var userConfig:any;
declare var langKeys:any;

const ge=document.getElementById.bind(document);
const ce=document.createElement.bind(document);

// Use Cmd instead of Ctrl on Apple devices.
var isApple:boolean=navigator.platform.indexOf("Mac")==0 || navigator.platform=="iPhone" || navigator.platform=="iPad" || navigator.platform=="iPod touch";
var postForms:{[key:string]:PostForm}={};

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

document.body.addEventListener("click", function(ev){
	if((ev.target as HTMLElement).tagName=="A"){
		if(ajaxFollowLink(ev.target as HTMLAnchorElement)){
			ev.preventDefault();
		}
	}
}, false);

document.querySelectorAll(".wallPostForm").forEach(function(el){
	postForms[el.id]=new PostForm(el as HTMLElement);
});

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
