namespace smithereen.graph{
	export class XScale extends DisplayObject{
		private graph:Graph;
		public xShift:number=0;

		public constructor(graph:Graph){
			super();
			this.graph=graph;
		}

		public draw(ctx:CanvasRenderingContext2D){
			Utils.drawRect(ctx, this.xShift, this.graph.height-this.graph.getSliderHeight()-Graph.XSCALE_HEIGHT-this.graph.getLegendHeight(), this.graph.width-this.xShift+1, Graph.XSCALE_HEIGHT+1, Graph.XSCALE_BGCOLOR, Graph.XSCALE_ALPHA);
			ctx.save();
			ctx.rect(this.xShift, this.graph.height-this.graph.getSliderHeight()-Graph.XSCALE_HEIGHT-this.graph.getLegendHeight(), this.graph.width-this.xShift+1, Graph.XSCALE_HEIGHT+1);
			ctx.clip();

			var sliderLeft=this.graph.slider.xLeft;
			var sliderRight=this.graph.slider.xRight;
			var minDate=this.graph.getMinDate();
			var maxDate=this.graph.getMaxDate();
			var minDisplayedDate:number;
			var maxDisplayedDate:number;

			if(minDate!=Number.MAX_VALUE){
				minDisplayedDate=minDate+sliderLeft/(this.graph.slider.width-1)*(maxDate-minDate);
				maxDisplayedDate=minDate+sliderRight/(this.graph.slider.width-1)*(maxDate-minDate);
				minDisplayedDate=Math.floor(minDisplayedDate*1000)/1000;
				maxDisplayedDate=Math.floor(maxDisplayedDate*1000)/1000;
				if(maxDate-minDate<Graph.SLIDER_MIN_RANGE && this.graph.getPointsCount()<7){
					while(maxDisplayedDate-minDisplayedDate<Graph.SLIDER_MIN_RANGE){
						maxDisplayedDate+=43200;
						minDisplayedDate-=43200;
					}
				}
			}else{
				minDisplayedDate=maxDisplayedDate=0;
			}
			var lines=Utils.getDateRange(minDisplayedDate, maxDisplayedDate, this.graph.width-this.xShift-2);
			var height=Graph.XSCALE_HEIGHT;
			var top=this.graph.height-this.graph.getSliderHeight()-Graph.XSCALE_HEIGHT-this.graph.getLegendHeight();
			ctx.lineWidth=1;
			ctx.fillStyle=Utils.cssColor(Graph.SCALE_TEXT_COLOR, 1);
			for(var line of lines){
				var x=Math.round(line.x)+0.5;
				if(!line.v){
					if(line.x>0){
						var lineHeight=line.v===null ? 2 : height;
						ctx.beginPath();
						ctx.moveTo(x+this.xShift, top);
						ctx.lineTo(x+this.xShift, top+lineHeight);
						ctx.strokeStyle=Utils.cssColor(Graph.SCALE_MARK_COLOR, 0.4);
						ctx.stroke();
					}
				}else{
					var metrics=ctx.measureText(line.v);
					ctx.fillText(line.v, x-0.5+this.xShift-Math.floor(metrics.width/2), top+5+9);
					if(line.p){
						ctx.beginPath();
						ctx.moveTo(x+this.xShift, top);
						ctx.lineTo(x+this.xShift, top+3);
						ctx.strokeStyle=Utils.cssColor(Graph.SCALE_MARK_COLOR, 0.8);
						ctx.stroke();
					}
				}
			}
			ctx.restore();
		}
	}
}
