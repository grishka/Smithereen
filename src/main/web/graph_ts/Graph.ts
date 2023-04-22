namespace smithereen.graph{
	export interface GraphDataPoint{
		x:number;
		y:number;
		s?:string;
	}

	export interface GraphCategory{
		name:string;
		color:number;
		langName?:string;
		points:GraphDataPoint[];
		active:boolean;
		filled:boolean;
	}

	export class Graph{
		public static readonly SLIDERSCALE_BORDER_COLOR:number = 0x5F7D9D;
		public static readonly SLIDERSCALE_BORDER_ALPHA:number = 0.6;
		public static readonly SLIDERSCALE_BGCOLOR:number = 0x9FA8AF;
		public static readonly SLIDERSCALE_ALPHA:number = 0.1;
		private static readonly SLIDERSCALE_HEIGHT:number = 40;
		public static readonly SLIDERSCALE_SELECT_COLOR:number = 3564430;

		public static readonly SLIDER_RESIZE_AREA_WIDTH:number = 5;
		public static readonly LEGEND_ALPHA:number = 0.5;
		public static readonly SCALE_MARK_COLOR:number = 6258077;
		public static readonly SLIDER_BORDER_COLOR:number = 6258077;
		public static readonly GRAPH_BGCOLOR:number = 16777215;
		public static readonly GRAPH_SEL_POINT_RADIUS:number = 4;
		private static readonly LEGEND_HEIGHT:number = 20;
		public static readonly SLIDER_QUANT_POINTS:number = 10;
		public static readonly DATE_HEIGHT:number = 20;
		public static readonly GRAPH_DASHED_LINE_LENGTHS:number[] = [5,5];
		public static readonly XSCALE_ALPHA:number = 0.4;
		public static readonly SLIDER_BORDER_ALPHA:number = 0.6;
		public static readonly LEGEND_BASE_HEIGHT:number = 20;
		public static readonly LEGEND_ELEM_HEIGHT:number = 20;
		public static readonly SLIDER_MIN_POINTS:number = 7;
		public static readonly XSCALE_BGCOLOR:number = 13421772;
		public static readonly GRAPH_ZERO_GRID:number = 10465732;
		public static readonly XSCALE_HEIGHT:number = 18;
		public static readonly GRAPH_GRID:number = 15658734;
		public static readonly LEGEND_FONT_SIZE:number = 11;
		public static readonly SLIDER_BGCOLOR:number = 16777215;
		public static readonly LEGEND_FONT:String = "Tahoma";
		public static readonly GRAPH_DOTTED_LINE_LENGTHS:number[] = [2,3];
		public static readonly LEGEND_VALUE_PADDING:number = 20;
		public static readonly MESSAGE_BGCOLOR:number = 15658734;
		public static readonly YSCALE_BGCOLOR:number = 16777215;
		public static readonly LEGEND_LEFTPADDING:number = 10;
		public static readonly LEGEND_CHECK_WIDTH:number = 14;
		public static readonly GRAPH_LONG_DASHED_LINE_LENGTHS:number[] = [12,10];
		public static readonly SLIDER_ALPHA:number = 0.55;
		public static readonly GRAPH_TOP_PADDING:number = 10;
		public static readonly SCALE_TEXT_COLOR:number = 3564430;
		public static readonly LEGEND_BGCOLOR:number = 16777215;
		public static readonly YSCALE_ALPHA:number = 1;
		public static readonly SLIDER_MIN_RANGE:number = 604800;
		public static readonly GRAPH_MIN_DX:number = 15;
		public static readonly SLIDER_MIN_WIDTH:number = 20;
		public static readonly GRAPH_INACTIVE_LINE_ALPHA:number = 1;

		public dpr=window.devicePixelRatio;
		public data:GraphCategory[]=[];
		public width:number;
		public height:number;
		private canvas:HTMLCanvasElement;
		public ctx;
		private displayObjects:DisplayObject[]=[];
		public intScale:boolean=false;
		public slider:Slider;
		public slScale:SliderScale;
		public yScale:YScale;
		public xScale:XScale;
		public legend:Legend;
		private graphArea:GraphArea;
		private dragReceiver:DisplayObject;
		private objectUnderMouse:DisplayObject;
		private globalMoveListener:{(ev:MouseEvent):void}=this.onMouseMoveGlobal.bind(this);
		private globalUpListener:{(ev:MouseEvent):void}=this.onMouseUp.bind(this);
		private legendHeight:number;
		private mouseDownObj:DisplayObject;
		private sliderVisible:boolean;

		public constructor(wrap:HTMLElement){
			var data=JSON.parse(wrap.dataset.graphData);
			for(var c of data){
				var category:GraphCategory={name: c.name, color: c.c, points: [], active: true, filled: c.f};
				for(var p of c.d){
					var point:GraphDataPoint={x: p[0], y: p[1]};
					if(p.length>2)
						point.s=p[2];
					category.points.push(point);
				}
				category.points.sort((a, b)=>{
					return a.x-b.x;
				});
				this.data.push(category);
			}

			this.sliderVisible=this.getPointsCount()>=7;
			this.legendHeight=Graph.LEGEND_BASE_HEIGHT+Math.ceil(this.data.length/2)*Graph.LEGEND_ELEM_HEIGHT;

			this.width=wrap.offsetWidth;
			this.height=385+this.legendHeight;

			this.canvas=ce("canvas", {width: Math.round(this.width*this.dpr), height: Math.round(this.height*this.dpr)});
			this.canvas.style.width=this.width+"px";
			this.canvas.style.height=this.height+"px";
			wrap.appendChild(this.canvas);
			this.ctx=this.canvas.getContext("2d");
			this.ctx.scale(this.dpr, this.dpr);
			this.ctx.font="11px Tahoma";

			this.slScale=new SliderScale(this);
			this.slider=new Slider(this);
			if(this.sliderVisible){
				this.displayObjects.push(this.slScale);
				this.displayObjects.push(this.slider);
			}
			this.displayObjects.push(this.yScale=new YScale(this));
			this.displayObjects.push(this.xScale=new XScale(this));
			this.displayObjects.push(this.graphArea=new GraphArea(this));
			this.displayObjects.push(this.legend=new Legend(this));
			this.updateLayout();
			this.slider.setInitialValues();
			this.redraw();

			this.canvas.addEventListener("mouseleave", this.onMouseLeave.bind(this), false);
			this.canvas.addEventListener("mousemove", this.onMouseMove.bind(this), false);
			this.canvas.addEventListener("mousedown", this.onMouseDown.bind(this), false);
		}

		public getMinDate():number{
			var min=Number.MAX_VALUE;
			for(var cat of this.data){
				if(cat.active && cat.points.length>0 && cat.points[0].x<min){
					min=cat.points[0].x;
				}
			}
			return min;
		}

		public getMaxDate():number{
			var max=Number.MIN_VALUE;
			for(var cat of this.data){
				if(cat.active && cat.points.length>0 && cat.points[cat.points.length-1].x>max){
					max=cat.points[cat.points.length-1].x;
				}
			}
			return max;
		}

		public getMinValue(startDate:number=0, endDate:number=0):number{
			var value=Number.MAX_VALUE;
			if(endDate==0)
				endDate=this.getMaxDate();

			var firstPoint:number=NaN, lastPoint:number=NaN;
			for(var cat of this.data){
				if(cat.active && cat.points.length>0){
					firstPoint=Utils.getFirstPoint(startDate, cat, 1);
				}
				lastPoint=Utils.getLastPoint(endDate, cat, 1);
				if(firstPoint!=-1 && lastPoint!=-1 /*&& cat.points[lastPoint].x<=endDate && cat.points[firstPoint].x>=startDate*/){
					for(var i=firstPoint;i<=lastPoint;i++){
						if(cat.points[i].y<value)
							value=cat.points[i].y;
					}
				}
			}

			return value;
		}

		public getMaxValue(startDate:number=0, endDate:number=0):number{
			var value=0;
			if(endDate==0)
				endDate=this.getMaxDate();

			var firstPoint:number=NaN, lastPoint:number=NaN;
			for(var cat of this.data){
				if(cat.active && cat.points.length>0){
					firstPoint=Utils.getFirstPoint(startDate, cat, 1);
				}
				lastPoint=Utils.getLastPoint(endDate, cat, 1);
				if(firstPoint!=-1 && lastPoint!=-1 /*&& cat.points[lastPoint].x<=endDate && cat.points[firstPoint].x>=startDate*/){
					for(var i=firstPoint;i<=lastPoint;i++){
						if(cat.points[i].y>value)
							value=cat.points[i].y;
					}
				}
			}

			return value;
		}

		public getPointsCount():number{
			var count=0;
			for(var cat of this.data){
				if(cat.active && cat.points.length>count)
					count=cat.points.length;
			}
			return count;
		}

		public getLegendHeight():number{
			return this.legendHeight;
		}

		public getSliderHeight():number{
			return this.sliderVisible ? Graph.SLIDERSCALE_HEIGHT : 0;
		}

		public getDx():number{
			if(this.data.length==0)
				return 0;
			var hasAtLeastTwoPoints=false;
			var i=0;
			for(var cat of this.data){
				if(cat.points.length>1){
					hasAtLeastTwoPoints=true;
					break;
				}
				i++;
			}
			if(!hasAtLeastTwoPoints)
				return 86400;
			var dx=this.data[i].points[1].x-this.data[i].points[0].x;
			for(var cat of this.data){
				for(var j=0;j<cat.points.length-1;j++){
					var diff=cat.points[j+1].x-cat.points[j].x;
					if(diff<dx)
						dx=diff;
				}
			}
			return dx;
		}

		private updateLayout(){
			var maxValue=this.getMaxValue();
			var valueRange=maxValue-this.getMinValue();
			var _step=Math.pow(10, Math.floor(Math.LOG10E*Math.log(valueRange))) || 1;
			var step;
			if(valueRange/_step>4){
				step=_step;
			}else if(valueRange/_step>2){
				step=_step*0.5;
			}else{
				step=_step*0.25;
			}
			var decimalPlaces=0;
			for(var s=step;s!=Math.floor(s);s*=10){
				decimalPlaces++;
			}
			var v=maxValue!=Math.floor(maxValue) ? (maxValue+step) : maxValue;
			v=Math.round(v*Math.pow(10, decimalPlaces))/Math.pow(10, decimalPlaces);
			var metrics=this.ctx.measureText(Utils.formatNumber(v));
			var shift=Math.ceil(metrics.width)+25;
			this.slScale.xShift=shift;
			this.slider.xShift=shift;
			this.yScale.xShift=shift;
			this.xScale.xShift=shift;
			this.graphArea.xShift=shift;

			this.legend.x=shift;
			this.legend.y=this.height-this.legendHeight;
			this.legend.updateLayout();
		}

		public redraw(){
			this.ctx.clearRect(0, 0, this.width, this.height);
			for(var obj of this.displayObjects){
				obj.dispatchDraw(this.ctx);
			}
		}

		public setCursor(cursor:string){
			this.canvas.style.cursor=cursor;
		}

		private onMouseLeave(ev:MouseEvent){
			if(this.objectUnderMouse){
				this.objectUnderMouse.onMouseLeave();
				this.canvas.style.cursor=null;
				this.objectUnderMouse=null;
			}
		}

		private hitTest(ev:MouseEvent):DisplayObject{
			if(ev.offsetX>=this.graphArea.xShift && ev.offsetY>=Graph.DATE_HEIGHT && ev.offsetY<this.height-this.legendHeight-Graph.SLIDERSCALE_HEIGHT-Graph.XSCALE_HEIGHT){
				return this.graphArea;
			}else if(ev.offsetY>=this.legend.y && ev.offsetX>=this.legend.x){
				return this.legend;
			}else if(this.sliderVisible && ev.offsetY>=this.legend.y-Graph.SLIDERSCALE_HEIGHT){
				return this.slider;
			}
			return null;
		}

		private onMouseMove(ev:MouseEvent){
			if(this.dragReceiver)
				return;
			var newObj=this.hitTest(ev);
			if(this.objectUnderMouse!=newObj){
				if(this.objectUnderMouse)
					this.objectUnderMouse.onMouseLeave();
				this.canvas.style.cursor=null;
				this.objectUnderMouse=newObj;
			}
			if(this.objectUnderMouse)
				this.objectUnderMouse.onMouseMove(ev, false);
		}

		private onMouseMoveGlobal(ev:MouseEvent){
			this.dragReceiver.onMouseMove(ev, true);
		}

		private onMouseDown(ev:MouseEvent){
			this.mouseDownObj=this.hitTest(ev);
			window.addEventListener("mouseup", this.globalUpListener, false);
			for(var obj of this.displayObjects){
				if(obj.onMouseDown(ev)){
					this.dragReceiver=obj;
					window.addEventListener("mousemove", this.globalMoveListener, false);
					break;
				}
			}
		}

		private onMouseUp(ev:MouseEvent){
			var obj=this.hitTest(ev);
			if(obj==this.mouseDownObj){
				if(obj==this.legend){
					this.legend.onClick(ev);
				}
			}
			if(this.dragReceiver!=null){
				this.dragReceiver.onMouseUp(ev);
				this.dragReceiver=null;
			}
			this.mouseDownObj=null;
			window.removeEventListener("mousemove", this.globalMoveListener);
			window.removeEventListener("mouseup", this.globalUpListener);
		}
	}
}

function renderGraph(id:string){
	new smithereen.graph.Graph(ge(id));
}

