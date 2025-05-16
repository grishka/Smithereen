const GRAFFITI_WIDTH=586;
const GRAFFITI_HEIGHT=293;
const GRAFFITI_DENSITY=2;
const GRAFFITI_BITMAP_FLUSH_PERIOD=100;
const GRAFFITI_MAX_UNDO_BUFFER=100;
const SVG_XMLNS="http://www.w3.org/2000/svg";

class GraffitiEditorBox extends Box{
	private form:PostForm;
	private drawingAreaWrap:HTMLDivElement;
	private drawingArea:DrawingArea;
	private thicknessSlider:HTMLInputElement;
	private opacitySlider:HTMLInputElement;
	private colorPreview:HTMLDivElement;
	private sample:SVGPathElement;
	private colorPickerWrap:HTMLDivElement;
	private colorPicker:HTMLDivElement;
	private sending:boolean;

	public constructor(title:string, form:PostForm){
		super(title, [lang("attach"), lang("cancel")], (idx)=>{
			if(idx==0){
				this.send();
			}else{
				this.dismiss();
			}
		});
		this.form=form;
		this.contentWrap.style.padding="0";
		var colorChooserBtn, thicknessLabel, sampleWrap;
		this.setContent(
			ce("div", {className: "graffitiEditorInner"}, [
				ce("div", {className: "realSummaryWrap grayText"}, [
					ce("div", {className: "summaryLinks"}, [
						ce("a", {onclick: this.clear.bind(this)}, [lang("graffiti_clear")]),
						" | ",
						ce("a", {onclick: this.undo.bind(this)}, [lang("graffiti_undo")])
					])
				]),
				ce("div", {className: "columnLayout gray"}, [
					this.drawingAreaWrap=ce("div", {className: "graffitiDrawAreaWrap"})
				]),
				ce("div", {className: "realBottomSummaryWrap graffitiControls"}, [
					sampleWrap=ce("div", {className: "sampleWrap"}),
					ce("label", {}, [lang("graffiti_color")+":"]),
					colorChooserBtn=ce("a", {className: "colorBtn", onclick: this.showColorPicker.bind(this)}),
					thicknessLabel=ce("label", {htmlFor: "graffitiThickness"}, [lang("graffiti_thickness")+":"]),
					this.thicknessSlider=ce("input", {type: "range", id: "graffitiThickness", step: "any", min: "1", max: "64", value: "13", oninput: this.onThicknessChanged.bind(this)}),
					ce("label", {htmlFor: "graffitiOpacity"}, [lang("graffiti_opacity")+":"]),
					this.opacitySlider=ce("input", {type: "range", id: "graffitiOpacity", step: "any", min: "0", max: "1", value: "0.8", oninput: this.onOpacityChanged.bind(this)})
				]),
				this.colorPickerWrap=ce("div", {className: "colorPickerWrap", onclick: this.hideColorPicker.bind(this)}, [
					this.colorPicker=ce("div", {className: "colorPicker"})
				])
			])
		);

		this.colorPickerWrap.style.display="none";
		thicknessLabel.style.flexGrow="1";
		colorChooserBtn.innerHTML=`<svg xmlns="${SVG_XMLNS}" width="17" height="7" viewBox="0 0 17 7"><path d="M1 5.5h15l-7.5-5Z"/></svg>`;
		this.colorPreview=ce("div", {className: "colorPreview"});
		colorChooserBtn.appendChild(this.colorPreview);
		var ssvg=document.createElementNS(SVG_XMLNS, "svg");
		ssvg.setAttribute("width", "68");
		ssvg.setAttribute("height", "68");
		ssvg.setAttribute("viewBox", "0 0 68 68");
		ssvg.setAttribute("fill", "none");
		ssvg.setAttribute("stroke-linecap", "round");
		ssvg.setAttribute("stroke-linejoin", "round");
		this.sample=document.createElementNS(SVG_XMLNS, "path");
		this.sample.setAttribute("d", "M34 34v0.5");
		ssvg.appendChild(this.sample);
		sampleWrap.appendChild(ssvg);

		this.drawingArea=new DrawingArea(this.drawingAreaWrap);
		this.colorPreview.style.backgroundColor=this.drawingArea.currentColor;
		this.updateSample();

		function intToHex(i:number){
			return i<16 ? ("0"+i.toString(16)) : i.toString(16);
		}

		var allColors:string[]=[];
		for(var r=0;r<6;r++){
			for(var g=0;g<6;g++){
				for(var b=0;b<6;b++){
					allColors.push("#"+intToHex(r/5*255)+intToHex(g/5*255)+intToHex(b/5*255));
				}
			}
		}
		for(var j=0;j<12;j++){
			for(var i=0;i<18;i++){
				var r=Math.floor(i/6)+3*Math.floor(j/6);
				var g=i%6;
				var b=j%6;
				var n=r*36+g*6+b;
				var color=allColors[n];
				var el=ce("div", {onclick: this.setColor.bind(this, color)});
				el.style.backgroundColor=color;
				this.colorPicker.appendChild(el);
			}
		}
	}

	public onShown(){
		(this.getContent().querySelector(".boxLayer") as HTMLElement).style.width="608px";
		document.body.classList.add("disableSelection");
	}

	public onHidden(){
		document.body.classList.remove("disableSelection");
	}

	public allowDismiss(){
		return false;
	}

	public dismiss(){
		if(!this.drawingArea.isDirty()){
			super.dismiss();
			return;
		}
		var confirmBox=new ConfirmBox(lang("confirm_title"), lang("graffiti_close_confirm"), ()=>{
			super.dismiss();
			confirmBox.dismiss();
		});
		confirmBox.show();
	}

	private onThicknessChanged(ev:InputEvent){
		this.drawingArea.currentThickness=this.thicknessSlider.valueAsNumber;
		this.updateSample();
	}

	private onOpacityChanged(ev:InputEvent){
		this.drawingArea.currentOpacity=this.opacitySlider.valueAsNumber;
		this.updateSample();
	}

	private clear(){
		var confirmBox=new ConfirmBox(lang("confirm_title"), lang("graffiti_clear_confirm"), ()=>{
			this.drawingArea.clear();
			confirmBox.dismiss();
		});
		confirmBox.show();
	}

	private undo(){
		this.drawingArea.undo();
	}

	private showColorPicker(){
		this.colorPickerWrap.show();
	}

	private hideColorPicker(){
		this.colorPickerWrap.hide();
	}

	private setColor(color:string){
		this.drawingArea.currentColor=color;
		this.updateSample();
		this.colorPreview.style.backgroundColor=color;
	}

	private updateSample(){
		this.sample.setAttribute("stroke-width", this.drawingArea.currentThickness+"");
		this.sample.setAttribute("stroke-opacity", this.drawingArea.currentOpacity+"");
		this.sample.setAttribute("stroke", this.drawingArea.currentColor);
	}

	private send(){
		if(this.sending)
			return;
		this.sending=true;
		this.showButtonLoading(0, true);
		this.drawingArea.getBlob(blob=>{
			this.form.uploadFile(blob, "graffiti.png", {graffiti: 1}, ()=>{
				super.dismiss();
			}, ()=>{
				this.sending=false;
				this.showButtonLoading(0, false);
			});
		});
	}
}

interface Window {
	GraffitiEditorBox:typeof GraffitiEditorBox;
}

window.GraffitiEditorBox = GraffitiEditorBox;

class DrawingArea{
	private wrap:HTMLDivElement;
	private svg:SVGSVGElement;
	private canvas:HTMLCanvasElement;
	private mouseUpListener=this.onMouseUp.bind(this);
	private mouseMoveListener=this.onMouseMove.bind(this);

	private xs:number[];
	private ys:number[];
	private currentPath:SVGPathElement;
	private paths:SVGPathElement[]=[];
	private ctx:CanvasRenderingContext2D;
	private didRasterize=false;

	public currentThickness:number=13;
	public currentOpacity:number=0.8;
	public currentColor:string="#336699";

	public constructor(wrap:HTMLDivElement){
		this.wrap=wrap;

		this.canvas=ce("canvas", {width: GRAFFITI_WIDTH*GRAFFITI_DENSITY, height: GRAFFITI_HEIGHT*GRAFFITI_DENSITY});
		this.wrap.appendChild(this.canvas);

		this.svg=document.createElementNS(SVG_XMLNS, "svg");
		this.svg.setAttribute("viewBox", `0 0 ${GRAFFITI_WIDTH} ${GRAFFITI_HEIGHT}`);
		this.svg.setAttribute("width", GRAFFITI_WIDTH+"");
		this.svg.setAttribute("height", GRAFFITI_HEIGHT+"");
		this.svg.setAttribute("fill", "none");
		this.svg.setAttribute("stroke-linecap", "round");
		this.svg.setAttribute("stroke-linejoin", "round");
		this.wrap.appendChild(this.svg);

		this.wrap.addEventListener("mousedown", this.onMouseDown.bind(this));
		this.ctx=this.canvas.getContext("2d");
		this.ctx.lineCap="round";
		this.ctx.lineJoin="round";
		this.ctx.fillStyle="#FFF";
		this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
		this.ctx.scale(GRAFFITI_DENSITY, GRAFFITI_DENSITY);
	}

	public undo(){
		if(this.paths.length){
			this.removePathAnimated(this.paths.pop());
		}
	}

	public clear(){
		this.canvas.style.animation="fadeOut 0.5s cubic-bezier(0.16, 1, 0.3, 1) "+((this.paths.length+1)*0.02)+"s";
		var listener=()=>{
			this.canvas.removeEventListener("animationend", listener);
			this.ctx.globalAlpha=1;
			this.ctx.fillRect(0, 0, GRAFFITI_WIDTH, GRAFFITI_HEIGHT);
		};
		this.canvas.addEventListener("animationend", listener);
		var i=0;
		var path;
		while(path=this.paths.pop()){
			i++;
			this.removePathAnimated(path, i*0.02);
		}
		this.didRasterize=false;
	}

	private removePathAnimated(path:SVGPathElement, delay:number=0){
		path.style.animation="fadeOut 0.5s cubic-bezier(0.16, 1, 0.3, 1) "+delay+"s";
		path.addEventListener("animationend", ev=>path.remove());
	}

	private onMouseDown(ev:MouseEvent){
		window.addEventListener("mousemove", this.mouseMoveListener);
		window.addEventListener("mouseup", this.mouseUpListener);
		this.xs=[];
		this.ys=[];
		this.currentPath=document.createElementNS(SVG_XMLNS, "path");
		this.svg.appendChild(this.currentPath);
		this.currentPath.setAttribute("stroke", this.currentColor);
		this.currentPath.setAttribute("stroke-width", this.currentThickness.toString());
		this.currentPath.setAttribute("stroke-opacity", this.currentOpacity.toString());
		this.addEvent(ev);
	}

	private onMouseUp(ev:MouseEvent){
		window.removeEventListener("mousemove", this.mouseMoveListener);
		window.removeEventListener("mouseup", this.mouseUpListener);
		this.addEvent(ev, true);
		this.paths.push(this.currentPath);
		this.currentPath=null;
		if(this.paths.length%GRAFFITI_BITMAP_FLUSH_PERIOD==0){
			this.flushBitmap();
		}
	}

	private onMouseMove(ev:MouseEvent){
		this.addEvent(ev);
	}

	private addEvent(ev:MouseEvent, adjust:boolean=false){
		var rect=this.svg.getBoundingClientRect();
		var x=ev.clientX-rect.left;
		var y=ev.clientY-rect.top;
		if(adjust){
			// Firefox draws nothing if all points have the same coordinates
			var allSame=true;
			for(var i=0;i<this.xs.length;i++){
				if(this.xs[i]!=x || this.ys[i]!=y){
					allSame=false;
					break;
				}
			}
			if(allSame){
				x+=0.01;
			}
		}
		this.xs.push(x);
		this.ys.push(y);

		this.redraw();
	}

	private canSmooth(x1:number, y1:number, x2:number, y2:number, x3:number, y3:number):boolean{
		var a=Math.abs(x1-x2)+Math.abs(y1-y2)+Math.abs(x2-x3)+Math.abs(y2-y3);
		var b=Math.abs(x1-x3)+Math.abs(y1-y3);
		return a>10 && b>a*0.8;
	}

	private drawBezier(x1:number, y1:number, x2:number, y2:number):string{
		return `Q${x1} ${y1},${(x1+x2)*0.5} ${(y1+y2)*0.5}`;
	}

	private drawTriangle(x1:number, y1:number, x2:number, y2:number):string{
		return `L${x1} ${y1}L${(x1+x2)*0.5} ${(y1+y2)*0.5}`;
	}

	private redraw(){
		var pathData="";
		if(this.xs.length<2){
			pathData=`M${this.xs[0]} ${this.ys[0]}L${this.xs[0]+0.51} ${this.ys[0]}`;
			this.currentPath.setAttribute("d", pathData);
			return;
		}
		pathData=`M${this.xs[0]} ${this.ys[0]}L${(this.xs[0]+this.xs[1])*0.5} ${(this.ys[0]+this.ys[1])*0.5}`;
		for(var i=1;i<this.xs.length-1;i++){
			if(this.canSmooth(this.xs[i-1], this.ys[i-1], this.xs[i], this.ys[i], this.xs[i+1], this.ys[i+1])){
				pathData+=this.drawBezier(this.xs[i], this.ys[i], this.xs[i+1], this.ys[i+1]);
			}else{
				pathData+=this.drawTriangle(this.xs[i], this.ys[i], this.xs[i+1], this.ys[i+1]);
			}
		}
		pathData+=`L${this.xs[this.xs.length-1]} ${this.ys[this.ys.length-1]}`;
		this.currentPath.setAttribute("d", pathData);
	}

	private flushBitmap(){
		while(this.paths.length>GRAFFITI_MAX_UNDO_BUFFER){
			var path=this.paths.shift();
			path.remove();
			this.renderPathIntoCanvasContext(this.ctx, path);
			this.didRasterize=true;
		}
	}

	private renderPathIntoCanvasContext(ctx:CanvasRenderingContext2D, path:SVGPathElement){
		ctx.strokeStyle=path.getAttribute("stroke");
		ctx.lineWidth=parseFloat(path.getAttribute("stroke-width"));
		ctx.globalAlpha=parseFloat(path.getAttribute("stroke-opacity"));
		ctx.stroke(new Path2D(path.getAttribute("d")));
	}

	public getBlob(callback:{(b:Blob):void}){
		var canvas=ce("canvas", {width: GRAFFITI_WIDTH*GRAFFITI_DENSITY, height: GRAFFITI_HEIGHT*GRAFFITI_DENSITY});
		var ctx=canvas.getContext("2d");
		ctx.putImageData(this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height), 0, 0);
		ctx.lineCap="round";
		ctx.lineJoin="round";
		ctx.scale(GRAFFITI_DENSITY, GRAFFITI_DENSITY);
		for(var path of this.paths){
			this.renderPathIntoCanvasContext(ctx, path);
		}

		if(canvas.toBlob!=undefined){
			canvas.toBlob(callback, "image/png");
			return;
		}

		var data=atob(canvas.toDataURL("image/png").split(",")[1]);
		var bytes=new Array(data.length);
		for(var i=0;i<data.length;i++){
			bytes[i]=data.charCodeAt(i);
		}
		callback(new Blob([new Uint8Array(bytes)], {type: "image/png"}));
	}

	public isDirty():boolean{
		return this.didRasterize || this.paths.length>0;
	}
}
