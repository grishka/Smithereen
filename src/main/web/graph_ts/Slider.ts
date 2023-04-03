namespace smithereen.graph{
	enum SliderInteractionMode{
		None,
		SelectingNewArea,
		Dragging,
		ResizingLeft,
		ResizingRight,
	}

	export class Slider extends DisplayObject{
		public xLeft:number;
		public xRight:number;
		public xShift:number=0;

		public interactionMode:SliderInteractionMode=SliderInteractionMode.None;

		public selectStart:number=0;
		public selectEnd:number=0;
		public width:number;

		private downX:number;
		private downY:number;
		private downLeft:number;
		private downRight:number;
		private currentCursor:string;

		private graph:Graph;

		public constructor(graph:Graph){
			super();
			this.graph=graph;
		}

		public setInitialValues(){
			this.width=this.graph.width-this.xShift;
			this.xRight=this.graph.width-this.xShift-1;

			this.xLeft=this.initialLeft();
			if(this.xRight-this.xLeft<Graph.SLIDER_MIN_WIDTH)
				this.xLeft=this.xRight-Graph.SLIDER_MIN_WIDTH;
		}

		private initialLeft():number{
			var minDate=this.graph.getMinDate();
			var maxDate=this.graph.getMaxDate();
			var maxXFound=false;
			var maxX=0;
			var minX=Number.MAX_VALUE;
			for(var cat of this.graph.data){
				if(cat.points.length==0)
					continue;
				var x=0;
				var numPoints=0;
				for(var i=cat.points.length-1;i>=0;i--){
					if(++numPoints>Graph.SLIDER_QUANT_POINTS)
						break;
					x=cat.points[i].x;
				}
				if(numPoints==Graph.SLIDER_QUANT_POINTS+1){
					maxXFound=true;
					if(x>maxX)
						maxX=x;
				}
				if(numPoints<=Graph.SLIDER_QUANT_POINTS && x<minX){
					minX=x;
				}
			}
			if(!maxXFound)
				maxX=minX;
			var r=Math.floor((maxX-minDate)*(this.width-1)/(maxDate-minDate));
			return isFinite(r) ? r : 0;
		}

		private setLeftRight(){
			this.xLeft=this.xLeft*(this.graph.width-this.xShift)/this.width;
			this.xRight=this.xRight*(this.graph.width-this.xShift)/this.width;
			this.width=this.graph.width-this.xShift;
		}

		public draw(ctx:CanvasRenderingContext2D){
			this.setLeftRight();
			var left=Math.round(this.xLeft);
			var right=Math.round(this.xRight);
			var x=left;
			Utils.drawRect(ctx, this.xShift, this.graph.height-this.graph.getSliderHeight()-this.graph.getLegendHeight(), x+1, this.graph.getSliderHeight()+1, Graph.SLIDER_BGCOLOR, Graph.SLIDER_ALPHA);
			x=right;
			Utils.drawRect(ctx, x+this.xShift, this.graph.height-this.graph.getSliderHeight()-this.graph.getLegendHeight(), this.graph.width-x+1, this.graph.getSliderHeight()+1, Graph.SLIDER_BGCOLOR, Graph.SLIDER_ALPHA);
			Utils.drawRect(ctx, left+this.xShift, this.graph.height-this.graph.getSliderHeight()-this.graph.getLegendHeight(), right-left+1, this.graph.getSliderHeight(), 16777215, 0.001, 1, Graph.SLIDER_BORDER_COLOR, Graph.SLIDER_BORDER_ALPHA);
			if(this.interactionMode==SliderInteractionMode.SelectingNewArea){
				Utils.drawRect(ctx, Math.min(this.selectStart, this.selectEnd)+this.xShift, this.graph.height-this.graph.getSliderHeight()-this.graph.getLegendHeight(), Math.max(this.selectStart, this.selectEnd)-Math.min(this.selectStart, this.selectEnd)+1, this.graph.getSliderHeight(), Graph.SLIDERSCALE_SELECT_COLOR, 0.1, 1, Graph.SLIDERSCALE_SELECT_COLOR, Graph.SLIDER_BORDER_ALPHA);
			}
		}

		public onMouseDown(ev:MouseEvent):boolean{
			var x=ev.offsetX-this.xShift;
			var y=ev.offsetY-(this.graph.height-this.graph.getSliderHeight()-this.graph.getLegendHeight());
			if(y>=0 && y<this.graph.getSliderHeight()){
				this.downX=ev.pageX;
				this.downY=ev.pageY;
				this.downLeft=this.xLeft;
				this.downRight=this.xRight;
				if(x>=this.xLeft-Graph.SLIDER_RESIZE_AREA_WIDTH && x<=this.xLeft+Graph.SLIDER_RESIZE_AREA_WIDTH){
					this.interactionMode=SliderInteractionMode.ResizingLeft;
				}else if(x>=this.xRight-Graph.SLIDER_RESIZE_AREA_WIDTH && x<=this.xRight+Graph.SLIDER_RESIZE_AREA_WIDTH){
					this.interactionMode=SliderInteractionMode.ResizingRight;
				}else if(x>=this.xLeft && x<=this.xRight){
					this.interactionMode=SliderInteractionMode.Dragging;
					this.graph.setCursor(this.currentCursor="grabbing");
				}else if(x>=0){
					this.selectStart=x;
					this.interactionMode=SliderInteractionMode.SelectingNewArea;
				}
				return true;
			}
			return false;
		}

		public onMouseMove(ev:MouseEvent, global:boolean){
			if(global){
				if(this.interactionMode==SliderInteractionMode.Dragging){
					var xDiff=this.downRight-this.downLeft;
					this.xLeft=Math.min(this.graph.width-this.xShift-xDiff-1, Math.max(0, this.downLeft-(this.downX-ev.pageX)));
					this.xRight=this.xLeft+xDiff;
				}else if(this.interactionMode==SliderInteractionMode.ResizingLeft){
					this.xLeft=Math.min(this.xRight-Graph.SLIDER_MIN_WIDTH, Math.max(0, this.downLeft-(this.downX-ev.pageX)));
				}else if(this.interactionMode==SliderInteractionMode.ResizingRight){
					this.xRight=Math.min(this.graph.width-this.xShift-1, Math.max(this.xLeft+Graph.SLIDER_MIN_WIDTH, this.downRight-(this.downX-ev.pageX)));
				}else if(this.interactionMode==SliderInteractionMode.SelectingNewArea){
					this.selectEnd=Math.min(this.graph.width-this.xShift-1, Math.max(0, this.selectStart-(this.downX-ev.pageX)));
				}
				this.graph.redraw();
			}else{
				var x=ev.offsetX-this.xShift;
				var y=ev.offsetY-(this.graph.height-this.graph.getSliderHeight()-this.graph.getLegendHeight());
				var cursor:string=null;
				if(x>=this.xLeft-Graph.SLIDER_RESIZE_AREA_WIDTH && x<=this.xLeft+Graph.SLIDER_RESIZE_AREA_WIDTH){
					cursor="ew-resize";
				}else if(x>=this.xRight-Graph.SLIDER_RESIZE_AREA_WIDTH && x<=this.xRight+Graph.SLIDER_RESIZE_AREA_WIDTH){
					cursor="ew-resize";
				}else if(x>=this.xLeft && x<=this.xRight){
					cursor="grab";
				}else if(x>=0){
					cursor="pointer";
				}
				if(this.currentCursor!=cursor){
					this.graph.setCursor(this.currentCursor=cursor);
				}
			}
		}

		public onMouseUp(ev:MouseEvent){
			if(this.interactionMode==SliderInteractionMode.SelectingNewArea){
				var maxWidth=this.graph.width-this.xShift-1;
				if(this.selectEnd==0){
					var xDiff=this.xRight-this.xLeft;
					this.xLeft=Math.min(maxWidth-xDiff, Math.max(0, Math.round(this.selectStart-xDiff/2)));
					this.xRight=this.xLeft+xDiff;
				}else{
					this.xLeft=Math.min(this.selectStart, this.selectEnd);
					this.xRight=Math.max(this.selectStart, this.selectEnd);
				}
				if(this.xRight-this.xLeft<Graph.SLIDER_MIN_WIDTH){
					if(this.xLeft+Graph.SLIDER_MIN_WIDTH<maxWidth)
						this.xRight=this.xLeft+Graph.SLIDER_MIN_WIDTH;
					else
						this.xLeft=this.xRight-Graph.SLIDER_MIN_WIDTH;
				}
				this.selectStart=this.selectEnd=0;
			}else if(this.interactionMode==SliderInteractionMode.Dragging){
				this.graph.setCursor(this.currentCursor="grab");
			}
			this.interactionMode=SliderInteractionMode.None;
			this.graph.redraw();
		}

		public onMouseLeave(){
			this.currentCursor=null;
		}
	}
}
