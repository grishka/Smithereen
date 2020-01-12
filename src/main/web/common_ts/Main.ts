var userConfig:any=(window as any).userConfig;

var timeZone:String;
if(Intl){
	timeZone=Intl.DateTimeFormat().resolvedOptions().timeZone;
}else{
	var offset:number=new Date().getTimezoneOffset();
	timeZone="GMT"+(offset>0 ? "+" : "-")+Math.floor(offset/60)+(offset%60!=0 ? (":"+(offset%60)) : "");
}
if(timeZone!=userConfig.timeZone){
	ajaxPost("/settings/setTimezone", {tz: timeZone}, function(resp:any){}, function(){});
}