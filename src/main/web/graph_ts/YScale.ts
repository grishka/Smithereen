namespace smithereen.graph{
	export class YScale extends DisplayObject{
		private graph:Graph;
		public xShift:number=0;

		public constructor(graph:Graph){
			super();
			this.graph=graph;
		}

		public draw(ctx:CanvasRenderingContext2D){
			var h=this.graph.height-this.graph.getSliderHeight()-this.graph.getLegendHeight()-Graph.DATE_HEIGHT+1;

			var yGrid=Utils.prepareYGrid(this.graph, h-Graph.XSCALE_HEIGHT-2-Graph.GRAPH_TOP_PADDING);
			var top=Graph.DATE_HEIGHT;
			ctx.beginPath();
			ctx.fillStyle=Utils.cssColor(Graph.SCALE_TEXT_COLOR, 1);
			for(var line of yGrid){
				var text=Utils.formatNumber(line.v);
				var textMetrics=ctx.measureText(text);
				ctx.fillText(text, this.xShift-1-Math.round(textMetrics.width)-8, line.y+top+Graph.GRAPH_TOP_PADDING+4);
				ctx.moveTo(this.xShift-3, Math.floor(line.y+top+Graph.GRAPH_TOP_PADDING)+0.5);
				ctx.lineTo(this.xShift, Math.floor(line.y+top+Graph.GRAPH_TOP_PADDING)+0.5);
			}
			ctx.lineWidth=1;
			ctx.strokeStyle=Utils.cssColor(Graph.SCALE_MARK_COLOR, 1);
			ctx.stroke();
		}
	}
}
