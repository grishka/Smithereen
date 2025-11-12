function renderBlurhashes(root:HTMLElement=null){
	function decode83(str:string){
		const alphabet="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~";
		var value=0;
		for(var c of str){
			value=value*83+alphabet.indexOf(c);
		}
		return value;
	}

	function srgbToLinear(v:number){
		v/=255;
		return v<=0.04045 ? (v/12.95) : Math.pow((v+0.055)/1.055, 2.4);
	}

	function linearToSrgb(v:number){
		v=Math.max(0, Math.min(1, v));
		return v<=0.0031308 ? Math.trunc(v*12.92*255+0.5) : Math.trunc((1.055*Math.pow(v, 1/2.4)-0.055)*255+0.5);
	}

	function signPow(v:number, p:number){
		return (v<0 ? -1 : 1)*Math.pow(Math.abs(v), p);
	}

	for(var _el of (root || document).querySelectorAll("img[data-blurhash]").unfuck()){
		var el=_el as HTMLImageElement;
		var hash=el.dataset.blurhash;
		if(el.complete){
			delete el.dataset.blurhash;
			continue;
		}
		var listener=(ev:Event)=>{
			var img=ev.target as HTMLImageElement;
			img.style.backgroundImage="";
			img.style.backgroundSize="";
			img.style.backgroundColor="";
			img.removeEventListener("load", listener);
		};
		el.addEventListener("load", listener);
		
		var numComponents=decode83(hash[0]);
		var numY=Math.floor(numComponents/9)+1;
		var numX=(numComponents%9)+1;

		var maxValue=(decode83(hash[1])+1)/166;

		var colors=new Array(numX*numY);
		var dc=decode83(hash.substring(2, 6));
		colors[0]=[srgbToLinear(dc >> 16), srgbToLinear((dc >> 8) & 0xff), srgbToLinear(dc & 0xff)];
		for(var i=1;i<colors.length;i++){
			var v=decode83(hash.substring(4+i*2, 6+i*2));
			colors[i]=[
				signPow((Math.floor(v/(19*19))-9)/9, 2)*maxValue,
				signPow((Math.floor(v/19)%19-9)/9, 2)*maxValue,
				signPow((v%19-9)/9, 2)*maxValue
			];
		}

		const width=10;
		const height=10;
		const stride=width*3+(width*3%4%3); // each line of pixel data must be padded to 4 bytes

		var pixelData=new Uint8Array(stride*height);
		var offsetIntoPixelData=0;
		for(var y=width-1;y>=0;y--){ // BMP stores lines bottom to top for some reason
			for(var x=0;x<width;x++){
				var r=0, g=0, b=0;
				for(var j=0;j<numY;j++){
					var basisY=Math.cos((Math.PI*y*j)/width);
					for(var i=0;i<numX;i++){
						var basis=Math.cos((Math.PI*x*i)/height)*basisY;
						var color=colors[i+j*numX];
						r+=color[0]*basis;
						g+=color[1]*basis;
						b+=color[2]*basis;
					}
				}
				pixelData[offsetIntoPixelData+x*3]=linearToSrgb(b);
				pixelData[offsetIntoPixelData+x*3+1]=linearToSrgb(g);
				pixelData[offsetIntoPixelData+x*3+2]=linearToSrgb(r);
			}
			offsetIntoPixelData+=stride;
		}
		var bg="url('data:image/bmp;base64,Qk2iAAAAAAAAADYAAAAoAAAACgAAAAoAAAABABgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"+btoa(String.fromCodePoint(...pixelData))+"')";
		el.style.backgroundImage=bg;
		el.style.backgroundSize="100% 100%";
		el.style.backgroundColor="";
		delete el.dataset.blurhash;
	}
}
