(function(){
	var embeds=document.querySelectorAll(".smithereenPostEmbed");
	var ourFrames=[];
	var ourHost=new URL(document.currentScript.src).host;
	window.addEventListener("message", function(ev){
		var sourceFrame;
		for(var i=0;i<ourFrames.length;i++){
			if(ourFrames[i].contentWindow==ev.source){
				sourceFrame=ourFrames[i];
				break;
			}
		}
		if(!sourceFrame){
			return;
		}
		if(ev.data.act=="setHeight"){
			sourceFrame.height=ev.data.height;
		}
	});
	for(var i=0;i<embeds.length;i++){
		var embed=embeds[i];
		if(!embed.dataset || embed.dataset.domain!=ourHost){
			continue;
		}
		populateEmbed(embed);
	}

	function populateEmbed(embed){
		var id=embed.dataset.postId;
		var xhr=new XMLHttpRequest();
		xhr.open("GET", "https://"+ourHost+"/posts/"+id+"/embedURL");
		xhr.onload=function(){
			if(xhr.response){
				var iframe=document.createElement("iframe");
				ourFrames.push(iframe);
				iframe.style.width="100%";
				iframe.style.maxWidth="500px";
				iframe.style.border="none";
				iframe.style.borderRadius="3px";
				embed.replaceWith(iframe);
				iframe.src=xhr.response;
			}
		};
		xhr.send();
	}
})();