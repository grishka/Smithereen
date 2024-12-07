class ImageAreaSelector{

	private parentEl:HTMLElement;
	private container:HTMLElement;
	private selected:HTMLElement;

	private markerTL:HTMLElement;
	private markerTR:HTMLElement;
	private markerBL:HTMLElement;
	private markerBR:HTMLElement;
	private edgeTop:HTMLElement;
	private edgeBottom:HTMLElement;
	private edgeLeft:HTMLElement;
	private edgeRight:HTMLElement;

	private scrimTop:HTMLElement;
	private scrimBottom:HTMLElement;
	private scrimLeft:HTMLElement;
	private scrimRight:HTMLElement;

	private curX:number;
	private curY:number;
	private curW:number;
	private curH:number;

	private curTarget:HTMLElement=null;
	private downX:number;
	private downY:number;
	private downSelectedX:number;
	private downSelectedY:number;
	private downSelectedW:number;
	private downSelectedH:number;
	private trackedTouchID:number;
	private minAspect:number;
	private maxAspect:number;

	private mouseUpListener:any;
	private mouseMoveListener:any;
	private touchUpListener:any;
	private touchMoveListener:any;

	private square:boolean;
	private enabled:boolean=true;
	public onUpdate:{():void};
	public onStartDrag:{():void};
	public onEndDrag:{():void};

	public constructor(parentEl:HTMLElement, square:boolean=false){
		this.parentEl=parentEl;
		this.container=ce("div");
		this.container.className="imageAreaSelector";
		this.parentEl.appendChild(this.container);

		this.scrimTop=this.makeDiv("scrim");
		this.scrimTop.style.top="0";
		this.container.appendChild(this.scrimTop);
		this.scrimBottom=this.makeDiv("scrim");
		this.scrimBottom.style.bottom="0";
		this.container.appendChild(this.scrimBottom);
		this.scrimLeft=this.makeDiv("scrim");
		this.scrimLeft.style.left=this.scrimLeft.style.top=this.scrimLeft.style.bottom="0";
		this.container.appendChild(this.scrimLeft);
		this.scrimRight=this.makeDiv("scrim");
		this.scrimRight.style.right=this.scrimRight.style.top=this.scrimRight.style.bottom="0";
		this.container.appendChild(this.scrimRight);

		this.selected=ce("div");
		this.selected.className="selected";
		this.container.appendChild(this.selected);
		this.container.addEventListener("mousedown", this.onMouseDown.bind(this), false);
		this.container.addEventListener("touchstart", this.onTouchDown.bind(this), false);
		this.container.addEventListener("dragstart", function(ev:Event){ev.preventDefault();}, false);

		var markerCont=ce("div");
		markerCont.className="markers";
		this.selected.appendChild(markerCont);

		markerCont.appendChild(this.markerTL=this.makeDiv("marker tl"));
		markerCont.appendChild(this.markerTR=this.makeDiv("marker tr"));
		markerCont.appendChild(this.markerBL=this.makeDiv("marker bl"));
		markerCont.appendChild(this.markerBR=this.makeDiv("marker br"));

		if(!square){
			markerCont.appendChild(this.edgeTop=this.makeDiv("edge top"));
			markerCont.appendChild(this.edgeBottom=this.makeDiv("edge bottom"));
			markerCont.appendChild(this.edgeLeft=this.makeDiv("edge left"));
			markerCont.appendChild(this.edgeRight=this.makeDiv("edge right"));
			markerCont.appendChild(this.makeDiv("marker top"));
			markerCont.appendChild(this.makeDiv("marker bottom"));
			markerCont.appendChild(this.makeDiv("marker left"));
			markerCont.appendChild(this.makeDiv("marker right"));
		}
		this.square=square;
	}

	private makeDiv(cls:string):HTMLElement{
		var el=ce("div");
		el.className=cls;
		return el;
	}

	public setSelectedArea(x:number, y:number, w:number, h:number){
		this.curX=x;
		this.curY=y;
		this.curW=w;
		this.curH=h;
		this.updateStyles();
	}

	public getSelectedArea():{x:number, y:number, w:number, h:number}{
		return {x: this.curX, y: this.curY, w: this.curW, h: this.curH};
	}

	public setEnabled(enabled:boolean):void{
		this.enabled=enabled;
	}

	public setAspectRatioLimits(min:number, max:number){
		this.minAspect=min;
		this.maxAspect=max;
	}

	private updateStyles():void{
		var contW=Math.round(this.container.clientWidth);
		var contH=Math.round(this.container.clientHeight);

		// Round to avoid rendering artifacts
		var x=Math.round(this.curX);
		var y=Math.round(this.curY);
		var w=Math.round(this.curW);
		var h=Math.round(this.curH);

		this.selected.style.left=x+"px";
		this.selected.style.top=y+"px";
		this.selected.style.width=w+"px";
		this.selected.style.height=h+"px";

		this.scrimTop.style.left=x+"px";
		this.scrimTop.style.width=w+"px";
		this.scrimTop.style.height=y+"px";
		this.scrimBottom.style.left=x+"px";
		this.scrimBottom.style.width=w+"px";
		this.scrimBottom.style.height=(contH-y-h)+"px";
		this.scrimLeft.style.width=x+"px";
		this.scrimRight.style.width=(contW-x-w)+"px";
	}

	private onTouchDown(ev:TouchEvent):void{
		ev.preventDefault();
		if(this.trackedTouchID)
			return;
		var touch=ev.touches[0];
		this.trackedTouchID=touch.identifier;
		this.onPointerDown(Math.round(touch.clientX), Math.round(touch.clientY), touch.target as HTMLElement);
		window.addEventListener("touchend", this.touchUpListener=this.onTouchUp.bind(this), false);
		window.addEventListener("touchcancel", this.touchUpListener, false);
		window.addEventListener("touchmove", this.touchMoveListener=this.onTouchMove.bind(this), false);
	}

	private onTouchMove(ev:TouchEvent):void{
		// ev.preventDefault();
		for(var i=0;i<ev.touches.length;i++){
			var touch=ev.touches[i];
			if(touch.identifier==this.trackedTouchID){
				this.onPointerMove(Math.round(touch.clientX), Math.round(touch.clientY), ev);
				break;
			}
		}
	}

	private onTouchUp(ev:TouchEvent):void{
		ev.preventDefault();
		for(var i=0;i<ev.changedTouches.length;i++){
			var touch=ev.changedTouches[i];
			if(touch.identifier==this.trackedTouchID){
				this.onPointerUp();
				this.trackedTouchID=null;
				window.removeEventListener("touchend", this.touchUpListener);
				window.removeEventListener("touchcancel", this.touchUpListener);
				window.removeEventListener("touchmove", this.touchMoveListener);
				break;
			}
		}
	}

	private onMouseDown(ev:MouseEvent):void{
		this.onPointerDown(ev.clientX, ev.clientY, ev.target as HTMLElement);
		window.addEventListener("mouseup", this.mouseUpListener=this.onMouseUp.bind(this), false);
		window.addEventListener("mousemove", this.mouseMoveListener=this.onMouseMove.bind(this), false);
	}

	private onMouseUp(ev:MouseEvent):void{
		this.onPointerUp();
		window.removeEventListener("mouseup", this.mouseUpListener);
		window.removeEventListener("mousemove", this.mouseMoveListener);
	}

	private onMouseMove(ev:MouseEvent):void{
		this.onPointerMove(ev.clientX, ev.clientY, ev);
	}

	private onPointerDown(x:number, y:number, target:HTMLElement):void{
		if(!this.enabled) return;
		this.curTarget=target;
		this.downX=x;
		this.downY=y;
		this.downSelectedX=this.curX;
		this.downSelectedY=this.curY;
		this.downSelectedW=this.curW;
		this.downSelectedH=this.curH;
		this.container.classList.add("moving");
		if(this.onStartDrag)
			this.onStartDrag();
	}

	private onPointerUp():void{
		this.curTarget=null;
		this.container.classList.remove("moving");
		if(this.onEndDrag)
			this.onEndDrag();
	}

	private enforceAspectRatio(){
		if(this.square){
			this.curW=this.curH=Math.min(this.curH, this.curW);
		}else{
			var aspect=this.curW/this.curH;
			if(this.maxAspect && aspect>this.maxAspect){
				this.curH=this.curW/this.maxAspect;
			}else if(this.minAspect && aspect<this.minAspect){
				this.curW=this.curH*this.minAspect;
			}
		}
	}

	private onPointerMove(x:number, y:number, ev:Event):void{
		if(!this.curTarget)
			return;
		ev.stopPropagation();
		var dX=x-this.downX;
		var dY=y-this.downY;
		var contW=this.container.clientWidth;
		var contH=this.container.clientHeight;
		if(this.curTarget==this.selected){
			this.curX=Math.max(0, Math.min(this.downSelectedX+dX, contW-this.curW));
			this.curY=Math.max(0, Math.min(this.downSelectedY+dY, contH-this.curH));
			this.updateStyles();
		}else if(this.curTarget==this.edgeRight){
			this.curW=Math.max(30, Math.min(this.downSelectedW+dX, contW-this.curX));
			this.enforceAspectRatio();
			this.updateStyles();
		}else if(this.curTarget==this.edgeBottom){
			this.curH=Math.max(30, Math.min(this.downSelectedH+dY, contH-this.curY));
			this.enforceAspectRatio();
			this.updateStyles();
		}else if(this.curTarget==this.markerBR){
			this.curW=Math.max(30, Math.min(this.downSelectedW+dX, contW-this.curX));
			this.curH=Math.max(30, Math.min(this.downSelectedH+dY, contH-this.curY));
			this.enforceAspectRatio();
			this.updateStyles();
		}else if(this.curTarget==this.edgeTop){
			var prevH=this.curH;
			this.curH=Math.max(30, Math.min(this.downSelectedH-dY, this.curY+this.curH));
			this.enforceAspectRatio();
			this.curY+=prevH-this.curH;
			this.updateStyles();
		}else if(this.curTarget==this.edgeLeft){
			var prevW=this.curW;
			this.curW=Math.max(30, Math.min(this.downSelectedW-dX, this.curX+this.curW));
			this.enforceAspectRatio();
			this.curX+=prevW-this.curW;
			this.updateStyles();
		}else if(this.curTarget==this.markerTL){
			var prevW=this.curW;
			this.curW=Math.max(30, Math.min(this.downSelectedW-dX, this.curX+this.curW));
			var prevH=this.curH;
			this.curH=Math.max(30, Math.min(this.downSelectedH-dY, this.curY+this.curH));
			this.enforceAspectRatio();
			this.curX+=prevW-this.curW;
			this.curY+=prevH-this.curH;
			this.updateStyles();
		}else if(this.curTarget==this.markerTR){
			this.curW=Math.max(30, Math.min(this.downSelectedW+dX, contW-this.curX));
			var prevH=this.curH;
			this.curH=Math.max(30, Math.min(this.downSelectedH-dY, this.curY+this.curH));
			this.enforceAspectRatio();
			this.curY+=prevH-this.curH;
			this.updateStyles();
		}else if(this.curTarget==this.markerBL){
			this.curH=Math.max(30, Math.min(this.downSelectedH+dY, contH-this.curY));
			var prevW=this.curW;
			this.curW=Math.max(30, Math.min(this.downSelectedW-dX, this.curX+this.curW));
			this.enforceAspectRatio();
			this.curX+=prevW-this.curW;
			this.updateStyles();
		}
		if(this.onUpdate)
			this.onUpdate();
	}
}
