namespace smithereen.graph{
	export class SliderScale extends DisplayObject{
		private graph:Graph;
		public xShift:number=0;
		private width:number;
		private minDate:number;
		private maxDate:number;
		public minValue:number;
		private maxValue:number;

		public constructor(graph:Graph){
			super();
			this.graph=graph;

		}

		public draw(ctx:CanvasRenderingContext2D){
			this.maxDate=this.graph.getMaxDate();
			this.minDate=this.graph.getMinDate();
			this.minValue=this.graph.getMinValue();
			this.maxValue=this.graph.getMaxValue();

			// drawSliderScaleArea
			this.width=this.graph.width-this.xShift;
			Utils.drawRect(ctx, this.xShift, this.graph.height-this.graph.getSliderHeight()-this.graph.getLegendHeight(), this.width, this.graph.getSliderHeight(), Graph.SLIDERSCALE_BGCOLOR, Graph.SLIDERSCALE_ALPHA, 1, Graph.SLIDERSCALE_BORDER_COLOR, Graph.SLIDERSCALE_BORDER_ALPHA);

			// drawMask?
			
			// drawPreviewLines
			var negativeMin=this.minValue<0 ? this.minValue : 0;
			for(var cat of this.graph.data){
				if(!cat.active || cat.points.length==0)
					continue;
				ctx.beginPath();
				
				var x=(cat.points[0].x-this.minDate)/(this.maxDate-this.minDate)*(this.width-2)+1+this.xShift;
				var y:number;
				if(this.minValue==this.maxValue){
					if(this.minValue==0){
						y=this.graph.height-this.graph.getLegendHeight()-2;
					}else{
						y=this.graph.height-this.graph.getLegendHeight()-2-0.5*(this.graph.getSliderHeight()-3);
					}
				}else{
					y=this.graph.height-this.graph.getLegendHeight()-2-(this.graph.getSliderHeight()-3)*(cat.points[0].y-negativeMin)/(this.maxValue-negativeMin);
				}
				ctx.moveTo(x, y);
				for(var pt of cat.points){
					x=(pt.x-this.minDate)/(this.maxDate-this.minDate)*(this.width-2)+1+this.xShift;
					if(this.minValue==this.maxValue){
						if(this.minValue==0){
							y=this.graph.height-this.graph.getLegendHeight()-2;
						}else{
							y=this.graph.height-this.graph.getLegendHeight()-2-0.5*(this.graph.getSliderHeight()-3);
						}
					}else{
						y=this.graph.height-this.graph.getLegendHeight()-2-(this.graph.getSliderHeight()-3)*(pt.y-negativeMin)/(this.maxValue-negativeMin);
					}
					ctx.lineTo(x, y);
				}
				ctx.strokeStyle=Utils.cssColor(cat.color, 1);
				ctx.stroke();
				if(cat.filled){
					ctx.lineTo(this.xShift+this.width, this.graph.height-this.graph.getSliderHeight()-this.graph.getLegendHeight()+this.graph.getSliderHeight());
					ctx.lineTo(this.xShift, this.graph.height-this.graph.getSliderHeight()-this.graph.getLegendHeight()+this.graph.getSliderHeight());
					ctx.closePath();
					ctx.fillStyle=Utils.cssColor(cat.color, 0.2);
					ctx.fill();
				}
			}
		}
	}
}
