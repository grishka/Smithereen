declare var userConfig:any;
declare var langKeys:any;

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
