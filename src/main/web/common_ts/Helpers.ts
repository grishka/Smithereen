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
	xhr.send(formData.join("&"));
}