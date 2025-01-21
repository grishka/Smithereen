interface ImageRect{
	x1:number;
	y1:number;
	x2:number;
	y2:number;
}

interface AvatarCropRects{
	profile:ImageRect;
	thumb:ImageRect;
}

class ProfilePictureLayer extends FileUploadLayer{
	private file:File=null;
	private areaSelector:ImageAreaSelector=null;
	private groupID:number=null;
	private fullSizeImage:HTMLImageElement;
	private crop:number[];
	private rotation:number=0;
	private fullSizeSvg:SVGSVGElement;
	private fullSizeSvgImage:SVGImageElement;

	private existingPhotoID:string;
	private existingPhotoURL:string;
	private existingPhotoCrop:AvatarCropRects;

	public constructor(groupID:number=null, existingPhotoID:string=null, existingPhotoURL:string=null, existingPhotoCrop:AvatarCropRects=null){
		super(lang("update_avatar_title"), lang(groupID ? "update_avatar_intro_group" : "update_avatar_intro")+"<br/>"+lang("update_avatar_formats"));
		this.groupID=groupID;
		this.existingPhotoID=existingPhotoID;
		this.existingPhotoURL=existingPhotoURL;
		this.existingPhotoCrop=existingPhotoCrop;
		if(!this.existingPhotoID){
			this.contentWrap.qs(".inner").append(lang("update_avatar_footer"));
		}else{
			this.crop=[existingPhotoCrop.profile.x1, existingPhotoCrop.profile.y1, existingPhotoCrop.profile.x2, existingPhotoCrop.profile.y2];
		}
	}

	public show(){
		if(this.existingPhotoID){
			LayerManager.getInstance().showBoxLoader();
			var img=ce("img");
			this.fullSizeImage=img;
			img.onload=()=>{
				super.show();
				this.showCropForProfile();
			};
			img.onerror=()=>{
				new MessageBox(lang("error"), lang("error_loading_picture"), lang("close")).show();
			};
			img.src=this.existingPhotoURL;
		}else{
			super.show();
		}
	}

	protected handleFile(file:File):void{
		this.file=file;
		var objURL=URL.createObjectURL(file);

		var img=ce("img");
		this.fullSizeImage=img;
		img.onload=()=>{
			this.showCropForProfile();
		};
		img.onerror=()=>{
			new MessageBox(lang("error"), lang("error_loading_picture"), lang("close")).show();
		};
		img.src=objURL;
	}

	private showIntro(){
		this.resetContent();
		this.titleEl.innerText=lang("update_avatar_title");
		this.contentWrap.qs(".inner").append(lang("update_avatar_footer"));
		this.updateTopOffset();
		this.crop=null;
		this.rotation=0;
	}

	private showCropForProfile(){
		var iw=this.fullSizeImage.naturalWidth;
		var ih=this.fullSizeImage.naturalHeight;
		var imgWrap=ce("div", {className: "avatarCropImgWrap"}, []);
		var imgSvg=document.createElementNS("http://www.w3.org/2000/svg", "svg");
		imgSvg.setAttribute("width", iw+"");
		imgSvg.setAttribute("height", ih+"");
		imgSvg.setAttribute("viewBox", `0 0 ${iw} ${ih}`);
		var imgSvgImage=document.createElementNS("http://www.w3.org/2000/svg", "image");
		imgSvgImage.setAttribute("href", this.fullSizeImage.src);
		imgSvgImage.setAttribute("width", iw+"");
		imgSvgImage.setAttribute("height", ih+"");
		imgSvg.appendChild(imgSvgImage);
		imgWrap.appendChild(imgSvg);
		this.fullSizeSvg=imgSvg;
		this.fullSizeSvgImage=imgSvgImage;
		var backBtn;
		var content=ce("div", {className: "avatarCropLayerContent"}, [
			lang(this.groupID ? "update_avatar_crop_explanation1_group" : "update_avatar_crop_explanation1"), ce("br"), lang("update_avatar_crop_explanation2"),
			ce("div", {align: "center"}, [imgWrap]),
			ce("div", {className: "layerButtons"}, [
				ce("button", {onclick: ()=>{
					var area=this.areaSelector.getSelectedArea();
					var contW=imgWrap.clientWidth;
					var contH=imgWrap.clientHeight;
					this.crop=[area.x/contW, area.y/contH, (area.x+area.w)/contW, (area.y+area.h)/contH];
					this.showCropForThumb();
				}}, [lang("save_and_continue")]),
				backBtn=ce("button", {className: "tertiary", onclick: ()=>this.showIntro()}, [lang("go_back")])
			])
		]);
		if(this.existingPhotoID)
			backBtn.hide();
		this.titleEl.innerText=lang(this.groupID ? "update_avatar_crop_title_group" : "update_avatar_crop_title");
		this.setContent(content);
		this.updateRotation(imgSvg, imgSvgImage);

		this.areaSelector=new ImageAreaSelector(imgWrap, false);
		this.areaSelector.setAspectRatioLimits(0.25, 2.5);
		var w=imgWrap.clientWidth;
		var h=imgWrap.clientHeight;
		if(this.crop){
			this.areaSelector.setSelectedArea(w*this.crop[0], h*this.crop[1], w*(this.crop[2]-this.crop[0]), h*(this.crop[3]-this.crop[1]));
		}else{
			this.areaSelector.setSelectedArea(20, 20, w-40, h-40);
		}
		this.updateTopOffset();
		imgWrap.appendChild(ce("div", {className: "rotateButtons"}, [
			ce("a", {href: "#", className: "rotateCW", onclick: ()=>{
				if(this.rotation==270)
					this.rotation=0;
				else
					this.rotation+=90;
				this.updateRotation(this.fullSizeSvg, this.fullSizeSvgImage);
				var w=imgWrap.clientWidth;
				var h=imgWrap.clientHeight;
				this.areaSelector.setSelectedArea(20, 20, w-40, h-40);
				this.updateTopOffset();
			}}),
			ce("a", {href: "#", className: "rotateCCW", onclick: ()=>{
				if(this.rotation==0)
					this.rotation=270;
				else
					this.rotation-=90;
				this.updateRotation(this.fullSizeSvg, this.fullSizeSvgImage);
				var w=imgWrap.clientWidth;
				var h=imgWrap.clientHeight;
				this.areaSelector.setSelectedArea(20, 20, w-40, h-40);
				this.updateTopOffset();
			}})
		]));
	}

	private updateRotation(svg:SVGSVGElement, svgImage:SVGImageElement){
		var iw=this.fullSizeImage.naturalWidth;
		var ih=this.fullSizeImage.naturalHeight;
		var svgW:string;
		var svgH:string;
		var svgTransform:string;
		switch(this.rotation){
			case 0:
				svgW=iw.toString();
				svgH=ih.toString();
				svgTransform="";
				break;
			case 90:
				svgW=ih.toString();
				svgH=iw.toString();
				svgTransform=`translate(${ih} 0) rotate(90)`;
				break;
			case 180:
				svgW=iw.toString();
				svgH=ih.toString();
				svgTransform=`rotate(180 ${iw/2} ${ih/2})`;
				break;
			case 270:
				svgW=ih.toString();
				svgH=iw.toString();
				svgTransform=`translate(0 ${iw}) rotate(270)`;
				break;
		}
		if(svg){
			svg.setAttribute("width", svgW);
			svg.setAttribute("height", svgH);
			svg.setAttribute("viewBox", `0 0 ${svgW} ${svgH}`);
		}
		svgImage.setAttribute("transform", svgTransform);
	}

	private showCropForThumb(){
		if(this.rotation==90 || this.rotation==270){
			var iw=this.fullSizeImage.naturalHeight;
			var ih=this.fullSizeImage.naturalWidth;
		}else{
			var iw=this.fullSizeImage.naturalWidth;
			var ih=this.fullSizeImage.naturalHeight;
		}
		var croppedWInImage=iw*(this.crop[2]-this.crop[0]);
		var croppedHInImage=ih*(this.crop[3]-this.crop[1]);
		var croppedH=croppedHInImage/croppedWInImage*200;
		var croppedW=200;

		var imgWrap=ce("div", {className: "avatarCropImgWrap"});
		var imgSvg=document.createElementNS("http://www.w3.org/2000/svg", "svg");
		imgSvg.setAttribute("width", croppedW+"");
		imgSvg.setAttribute("height", croppedH+"");
		imgSvg.setAttribute("viewBox", `${this.crop[0]*iw} ${this.crop[1]*ih} ${croppedWInImage} ${croppedHInImage}`);
		var imgSvgImage=document.createElementNS("http://www.w3.org/2000/svg", "image");
		imgSvgImage.setAttribute("href", this.fullSizeImage.src);
		imgSvgImage.setAttribute("width", this.fullSizeImage.naturalWidth+"");
		imgSvgImage.setAttribute("height", this.fullSizeImage.naturalHeight+"");
		this.updateRotation(null, imgSvgImage);
		imgSvg.appendChild(imgSvgImage);
		imgWrap.appendChild(imgSvg);

		this.titleEl.innerText=lang("update_avatar_thumb_title");
		var thumbSelectW;
		var content=ce("div", {className: "avatarCropLayerContent"}, [
			lang("update_avatar_thumb_explanation1"), ce("br"), lang(this.groupID ? "update_avatar_thumb_explanation2_group" : "update_avatar_thumb_explanation2"),
			ce("div", {align: "center"}, [
				thumbSelectW=ce("div", {className: "avatarThumbSelectW"}, [imgWrap])
			]),
			ce("div", {className: "layerButtons"}, [
				ce("button", {onclick: (ev)=>{
					var btn=ev.target as HTMLElement;
					btn.classList.add("loading");
					btn.setAttribute("disabled", "");
					var area=this.areaSelector.getSelectedArea();
					var w=imgWrap.clientWidth;
					var h=imgWrap.clientHeight;
					this.upload(this.crop, [area.x/w, area.y/h, (area.x+area.w)/w, (area.y+area.h)/h]);
				}}, [lang("save")]),
				ce("button", {className: "tertiary", onclick: ()=>this.showCropForProfile()}, [lang("go_back")])
			])
		]);

		var mediumPreviewSvg:SVGSVGElement=imgSvg.cloneNode(true) as SVGSVGElement;
		var smallPreviewSvg:SVGSVGElement=imgSvg.cloneNode(true) as SVGSVGElement;

		mediumPreviewSvg.setAttribute("width", "100");
		mediumPreviewSvg.setAttribute("height", "100");
		smallPreviewSvg.setAttribute("width", "50");
		smallPreviewSvg.setAttribute("height", "50");

		thumbSelectW.appendChild(mediumPreviewSvg);
		thumbSelectW.appendChild(smallPreviewSvg);
		this.setContent(content);

		this.areaSelector=new ImageAreaSelector(imgWrap, true);
		var w=imgWrap.clientWidth;
		var h=imgWrap.clientHeight;
		if(this.existingPhotoCrop){
			var rect=this.existingPhotoCrop.thumb;
			var size=Math.min((rect.x2-rect.x1)*w, (rect.y2-rect.y1)*h);
			this.areaSelector.setSelectedArea(rect.x1*w, rect.y1*h, size, size);
		}else{
			if(w>h){
				this.areaSelector.setSelectedArea(w/2-h/2, 0, h, h);
			}else{
				this.areaSelector.setSelectedArea(0, 0, w, w);
			}
		}
		this.areaSelector.onUpdate=()=>{
			var x=this.crop[0]*iw;
			var y=this.crop[1]*ih;
			var w=imgWrap.clientWidth;
			var h=imgWrap.clientHeight;
			var thumbArea=this.areaSelector.getSelectedArea();

			var viewBox=`${x+thumbArea.x/w*croppedWInImage} ${y+thumbArea.y/h*croppedHInImage} ${croppedWInImage*(thumbArea.w/w)} ${croppedHInImage*(thumbArea.h/h)}`;
			mediumPreviewSvg.setAttribute("viewBox", viewBox);
			smallPreviewSvg.setAttribute("viewBox", viewBox);
		};
		this.areaSelector.onUpdate();

		this.updateTopOffset();
	}

	private upload(profileCrop:number[], squareCrop:number[]):void{
		setGlobalLoading(true);
		if(this.existingPhotoID){
			ajaxPostAndApplyActions("/photos/"+this.existingPhotoID+"/updateAvatarCrop", {crop: profileCrop.join(",")+","+squareCrop.join(","), rotation: this.rotation});
		}else{
			ajaxUpload("/settings/updateProfilePicture?rotation="+this.rotation+"&crop="+profileCrop.join(",")+","+squareCrop.join(",")+(this.groupID ? ("&group="+this.groupID) : ""), "file", this.file, (resp:any)=>{
				this.dismiss();
				setGlobalLoading(false);
				return false;
			});
		}
	}
}

