namespace smithereen.graph{
	export class GraphArea extends DisplayObject{
		private graph:Graph;
		public xShift:number=0;
		private yShift:number;
		private w:number;
		private h:number;
		private points:{x:number, y:number, i:number}[][]=[];
		private minDx:number=0;
		private selectedPoints:number[]=[];
		private topDateText:string;
		private lastMouseX:number;

		private downX:number;
		private dragging:boolean=false;
		private downLeft:number;
		private downRange:number;

		public constructor(graph:Graph){
			super();
			this.graph=graph;
			this.yShift=Graph.DATE_HEIGHT;
			for(var i in graph.data){
				this.selectedPoints.push(-1);
			}
		}

		public draw(ctx:CanvasRenderingContext2D){
			if(this.minDx==0){
				this.minDx=this.graph.getDx();
			}

			// Border
			this.h=this.graph.height-this.graph.getSliderHeight()-Graph.DATE_HEIGHT-Graph.XSCALE_HEIGHT-this.graph.getLegendHeight();
			this.w=this.graph.width-this.xShift;
			Utils.drawRect(ctx, this.xShift, this.yShift, this.w, this.h, 0, 0, 1, Graph.SCALE_MARK_COLOR, 0.6);

			// Grid
			ctx.beginPath();
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
			var xLines=Utils.getDateRange(minDisplayedDate, maxDisplayedDate, this.graph.width-this.xShift-2);
			for(let line of xLines){
				if(line.p>1 && Math.floor(line.x)>0 && Math.ceil(line.x)<this.w){
					var x=Math.round(line.x)+0.5;
					ctx.moveTo(x+this.xShift, this.yShift+1);
					ctx.lineTo(x+this.xShift, this.yShift+this.h-1);
				}
			}
			var yLines=Utils.prepareYGrid(this.graph, this.h-1-Graph.GRAPH_TOP_PADDING);
			for(let line of yLines){
				if(line.y<this.h-2-Graph.GRAPH_TOP_PADDING){
					var y=Math.round(line.y)+0.5;
					ctx.moveTo(this.xShift+1, y+this.yShift+Graph.GRAPH_TOP_PADDING);
					ctx.lineTo(this.xShift+this.w-1, y+this.yShift+Graph.GRAPH_TOP_PADDING);
				}
			}

			ctx.strokeStyle=Utils.cssColor(Graph.GRAPH_GRID, 1);
			ctx.stroke();

			// Graph itself
			ctx.save();
			ctx.translate(this.xShift+1, this.yShift+1);
			ctx.beginPath();
			ctx.rect(0, 0, this.w, this.h);
			ctx.clip();
			ctx.lineCap="round";

			var minValue=this.graph.slScale.minValue<0 ? this.graph.slScale.minValue : 0;
			var maxValue=this.graph.getMaxValue(minDisplayedDate, maxDisplayedDate);
			var valueRange=maxValue-minValue;
			var _step=Math.pow(10, Math.floor(Math.LOG10E*Math.log(valueRange))) || 1;
			var step;
			if(valueRange/_step>4){
				step=_step;
			}else if(valueRange/_step>2){
				step=_step*0.5;
			}else{
				step=_step*0.25;
			}
			if(this.graph.intScale && Math.round(step)!=step){
				step=_step;
			}

			var roundedMinValue=Math.floor(minValue/step)*step;
			var roundedMaxValue=Math.ceil(maxValue/step)*step;
			var dx=this.minDx/(maxDisplayedDate-minDisplayedDate)*(this.w-2) || Graph.GRAPH_MIN_DX;

			this.points=[];

			var j=0;
			for(var cat of this.graph.data){
				this.points.push([]);
				if(cat.points.length>0 && cat.active){
					var firstPoint=Utils.getFirstPoint(minDisplayedDate, cat, 2);
					var lastPoint=Utils.getLastPoint(maxDisplayedDate, cat, 2);
					if(firstPoint!=-1 && lastPoint!=-1){
						var lastIsDashed=false;
						for(var i=firstPoint;i<=lastPoint;i++){
							var pt=cat.points[i];
							var x:number;
							var y:number;
							if(maxDisplayedDate==minDisplayedDate){
								x=(this.w-2)/2;
							}else{
								x=(pt.x-minDisplayedDate)/(maxDisplayedDate-minDisplayedDate)*(this.w-2);
							}
							if(minValue==maxValue){
								if(minValue==0){
									y=this.h-3;
								}else{
									y=(this.h-3+Graph.GRAPH_TOP_PADDING)/2;
								}
							}else{
								y=Math.max(0, (roundedMaxValue-pt.y)*(this.h-3-Graph.GRAPH_TOP_PADDING)/(roundedMaxValue-roundedMinValue))+Graph.GRAPH_TOP_PADDING;
							}
							this.points[j].push({x: x, y: y, i: i});
							if(i==lastPoint && pt.s=="-")
								lastIsDashed=true;
						}
						if(dx<Graph.GRAPH_MIN_DX || this.points[j].length<=2){
							this.drawLineThroughPoints(ctx, this.points[j], cat.color, 2, lastIsDashed);
						}else{
							this.drawCurveThroughPoints(ctx, this.points[j], 1, cat.color, 2, lastIsDashed);
						}
						if(cat.filled && this.points[j].length>1){
							ctx.lineTo(this.points[j][this.points[j].length-1].x, this.h-2);
							ctx.lineTo(this.points[j][0].x, this.h-2);
							ctx.closePath();
							ctx.fillStyle=Utils.cssColor(cat.color, 0.2);
							ctx.fill();
						}
						if(dx>=Graph.GRAPH_MIN_DX){
							this.drawPoints(ctx, this.points[j], cat.color);
						}
					}
				}
				j++;
			}

			var selectedDate;
			var selectedDateXDiff=Number.MAX_VALUE;
			for(i=this.graph.data.length-1;i>=0;i--){
				var sel=this.selectedPoints[i];
				if(sel!=-1 && sel<this.points[i].length && this.graph.data[i].active){
					var xDiff=Math.abs(this.lastMouseX-this.points[i][sel].x);
					if(xDiff<selectedDateXDiff){
						selectedDate=this.graph.data[i].points[this.points[i][sel].i].x;
						selectedDateXDiff=xDiff;
					}
					ctx.strokeStyle=ctx.fillStyle=Utils.cssColor(this.graph.data[i].color, 1);
					ctx.lineWidth=2;
					ctx.beginPath();
					ctx.arc(this.points[i][sel].x, this.points[i][sel].y, 2, 0, Math.PI*2);
					ctx.stroke();
					ctx.fill();

					var hint=Utils.formatNumber(this.graph.data[i].points[this.points[i][sel].i].y);
					var hintWidth=Math.ceil(ctx.measureText(hint).width)+6;
					var hintX=Math.round(this.points[i][sel].x-hintWidth/2);
					var hintY=Math.round(this.points[i][sel].y-18);

					if(hintY<5)
						hintY+=25;
					if(hintY>this.h-15)
						hintY=this.h-15;
					if(hintX<3)
						hintX=3;
					if(hintX+hintWidth>this.w-5)
						hintX=this.w-hintWidth-5;

					ctx.fillStyle="#FFFFFF";
					ctx.fillRect(hintX, hintY, hintWidth, 15);
					ctx.fillStyle=ctx.strokeStyle=Utils.cssColor(this.graph.data[i].color, 1);
					ctx.lineWidth=1;
					ctx.strokeRect(hintX+0.5, hintY+0.5, hintWidth-1, 14);
					ctx.fillText(hint, hintX+3, hintY+11);
				}
			}

			ctx.restore();
			if(selectedDate){
				this.topDateText=Utils.formatDate("dayMonthYear", new Date(selectedDate*1000));
			}else{
				var min=Utils.formatDate("dayMonthYear", new Date(minDisplayedDate*1000));
				var max=Utils.formatDate("dayMonthYear", new Date(maxDisplayedDate*1000));
				if(min!=max){
					this.topDateText=min+" — "+max;
				}else{
					this.topDateText=min;
				}
			}
			ctx.fillStyle=Utils.cssColor(Graph.SCALE_TEXT_COLOR, 1);
			ctx.fillText(this.topDateText, this.xShift+10, 15);
		}

		private drawCurveThroughPoints(ctx:CanvasRenderingContext2D, points:{x:number, y:number, i:number}[], alpha:number, color:number, thickness:number, lastIsDashed:boolean){
			var controlPoints:{x1:number,y1:number,x2:number,y2:number}[]=[];
			var lastPointIndex=points.length-1;
			for(var i=1;i<lastPointIndex;i++){
				var prevX=points[i-1].x;
				var prevY=points[i-1].y;
				var curX=points[i].x;
				var curY=points[i].y;
				var nextX=points[i+1].x;
				var nextY=points[i+1].y;
				var diffPrevCurX=prevX-curX;
				var diffPrevCurY=prevY-curY;
				var diffNextCurX=nextX-curX;
				var diffNextCurY=nextY-curY;
				var thereIsFlatSegment=diffPrevCurY*diffNextCurY>0;
				var prevCurDistanceSquared=Math.max(0.000001, diffPrevCurX*diffPrevCurX+diffPrevCurY*diffPrevCurY);
				var nextCurDistanceSquared=Math.max(0.000001, diffNextCurX*diffNextCurX+diffNextCurY*diffNextCurY);
				var prevNextDistanceSquared=Math.max(0.000001, (prevX-nextX)*(prevX-nextX)+(prevY-nextY)*(prevY-nextY));
				var someAngleInRadians=Math.acos(1 - prevNextDistanceSquared / (prevCurDistanceSquared + nextCurDistanceSquared)) || 1;
				var someDiffScaleFactor = 0;
				if(prevCurDistanceSquared > nextCurDistanceSquared){
					someDiffScaleFactor = (3 + 6 * nextCurDistanceSquared / prevCurDistanceSquared - nextCurDistanceSquared * nextCurDistanceSquared / prevCurDistanceSquared / prevCurDistanceSquared) / 8;
					diffPrevCurX *= someDiffScaleFactor;
					diffPrevCurY *= someDiffScaleFactor;
				}else if(nextCurDistanceSquared > prevCurDistanceSquared){
					someDiffScaleFactor = (3 + 6 * prevCurDistanceSquared / nextCurDistanceSquared - prevCurDistanceSquared * prevCurDistanceSquared / nextCurDistanceSquared / nextCurDistanceSquared) / 8;
					diffNextCurX *= someDiffScaleFactor;
					diffNextCurY *= someDiffScaleFactor;
				}

				var _loc29_ = diffPrevCurX * diffPrevCurX / prevCurDistanceSquared;
				var _loc30_ = diffNextCurX * diffNextCurX / nextCurDistanceSquared;
				var _loc31_ = Math.pow((_loc29_ + _loc30_) / 2,0.2);
				var diffX = diffNextCurX - diffPrevCurX;
				var diffY = diffNextCurY - diffPrevCurY;
				diffPrevCurX += curX;
				diffPrevCurY += curY;
				diffNextCurX += curX;
				diffNextCurY += curY;
				var distancePrevNext = Math.sqrt(diffX * diffX + diffY * diffY);
				var prevCurDistanceHalved = Math.sqrt(prevCurDistanceSquared) * 0.5;
				var nextCurDistanceHalved = Math.sqrt(nextCurDistanceSquared) * 0.5;
				var _loc37_ = someAngleInRadians / Math.PI;
				var controlPointsScaleFactor = 1 - 0.75 + 0.75 * _loc37_;
				if(thereIsFlatSegment){
					controlPointsScaleFactor *= _loc31_;
					diffY /= 4;
					diffX /= 1.5;
				}
				if(!distancePrevNext){
					distancePrevNext = diffX = 1;
					diffY = 0;
				}
				var controlX1 = curX - diffX / distancePrevNext * prevCurDistanceHalved * controlPointsScaleFactor;
				var controlY1 = curY - diffY / distancePrevNext * prevCurDistanceHalved * controlPointsScaleFactor;
				var controlX2 = curX + diffX / distancePrevNext * nextCurDistanceHalved * controlPointsScaleFactor;
				var controlY2 = curY + diffY / distancePrevNext * nextCurDistanceHalved * controlPointsScaleFactor;
				controlX1 = Math.max(points[0].x,Math.min(points[lastPointIndex - 1].x,controlX1));
				controlX2 = Math.max(points[1].x,Math.min(points[lastPointIndex].x,controlX2));
				if(Math.abs((points[i].y) - thickness) < 0.000001){
					controlPoints[i]={x1: controlX1, y1: thickness, x2: controlX2, y2: thickness};
				}else if(Math.abs((points[i].y) - Graph.GRAPH_TOP_PADDING) < 0.000001){
					controlPoints[i]={x1: controlX1, y1: Graph.GRAPH_TOP_PADDING, x2: controlX2, y2: Graph.GRAPH_TOP_PADDING};
				}else{
					controlPoints[i]={x1: controlX1, y1: controlY1, x2: controlX2, y2: controlY2};
				}
			}
			ctx.beginPath();
			ctx.moveTo(points[0].x, points[0].y);
			ctx.quadraticCurveTo(controlPoints[1].x1, controlPoints[1].y1, points[1].x, points[1].y);
			for(i=1;i<lastPointIndex-1;i++){
				if(points[i].y==points[i+1].y && (Math.abs(points[i].y-thickness)<0.000001 || Math.abs(points[i].y-Graph.GRAPH_TOP_PADDING)<0.000001)){
					ctx.lineTo(points[i+1].x, points[i+1].y);
				}else{
					ctx.bezierCurveTo(Math.min(points[i+1].x, controlPoints[i].x2), controlPoints[i].y2, Math.max(points[i].x, controlPoints[i+1].x1), controlPoints[i+1].y1, points[i+1].x, points[i+1].y);
				}
			}

			ctx.strokeStyle=Utils.cssColor(color, alpha);
			ctx.lineWidth=thickness;
			ctx.stroke();

			var lastSegment=new Path2D();
			lastSegment.moveTo(points[i].x, points[i].y);
			if(points[i].y==points[i].y+1 && (Math.abs(points[i].y-thickness)<0.000001 || Math.abs(points[i].y-Graph.GRAPH_TOP_PADDING)<0.000001)){
				lastSegment.lineTo(points[i+1].x, points[i+1].y);
				ctx.lineTo(points[i+1].x, points[i+1].y);
			}else{
				lastSegment.quadraticCurveTo(controlPoints[i].x2, controlPoints[i].y2, points[i+1].x, points[i+1].y);
				ctx.quadraticCurveTo(controlPoints[i].x2, controlPoints[i].y2, points[i+1].x, points[i+1].y);
			}
			if(lastIsDashed)
				ctx.setLineDash(Graph.GRAPH_DASHED_LINE_LENGTHS);
			ctx.stroke(lastSegment);
			ctx.setLineDash([]);
		}

		private drawLineThroughPoints(ctx:CanvasRenderingContext2D, points:{x:number, y:number, i:number}[], color:number, thickness:number, lastIsDashed:boolean){
			if(points.length==0){
				return;
			}
			if(points.length==1){
				ctx.strokeStyle=ctx.fillStyle=Utils.cssColor(color, 1);
				ctx.lineWidth=thickness;
				ctx.beginPath();
				ctx.arc(points[0].x, points[0].y, 1.5, 0, Math.PI*2);
				ctx.stroke();
				ctx.fill();
				return;
			}
			ctx.beginPath();
			ctx.strokeStyle=Utils.cssColor(color, 1);
			ctx.lineWidth=thickness;
			ctx.moveTo(points[0].x, points[0].y);
			for(var i=1;i<points.length-1;i++){
				ctx.lineTo(points[i].x, points[i].y);
			}
			ctx.stroke();
			var lastSegment=new Path2D();
			lastSegment.moveTo(points[i-1].x, points[i-1].y);
			lastSegment.lineTo(points[i].x, points[i].y);
			ctx.lineTo(points[i].x, points[i].y);
			if(lastIsDashed)
				ctx.setLineDash(Graph.GRAPH_DASHED_LINE_LENGTHS);
			ctx.stroke(lastSegment);
			ctx.setLineDash([]);
		}

		private drawPoints(ctx:CanvasRenderingContext2D, points:{x:number, y:number}[], color:number){
			ctx.strokeStyle=ctx.fillStyle="#FFFFFF";
			ctx.lineWidth=2;
			for(var pt of points){
				ctx.beginPath();
				ctx.arc(pt.x, pt.y, 2, 0, Math.PI*2);
				ctx.fill();
				ctx.stroke();
			}
			ctx.strokeStyle=ctx.fillStyle=Utils.cssColor(color, 1);
			for(var pt of points){
				ctx.beginPath();
				ctx.arc(pt.x, pt.y, 1, 0, Math.PI*2);
				ctx.fill();
				ctx.stroke();
			}
		}

		private getNearest(points:{x:number, y:number, i:number}[], x:number, start:number, end:number):number{
			if(start==end){
				if(x<points[start].x)
					return start;
				if(!points[start+1] || (points[start] && Math.abs(x-points[start].x)<=Math.abs(x-points[start+1].x)))
					return start;
				return start+1;
			}
			if(x<points[Math.ceil((start+end)/2)].x)
				return this.getNearest(points, x, start, Math.ceil((start+end)/2)-1);
			return this.getNearest(points, x, Math.ceil((start+end)/2), end);
		}

		public onMouseMove(ev:MouseEvent){
			if(this.dragging){
				var diff=this.downX-ev.pageX;
				this.graph.slider.xLeft=Math.max(0, Math.min(this.downLeft+diff/this.w*this.downRange, this.graph.slider.width-this.downRange));
				this.graph.slider.xRight=this.graph.slider.xLeft+this.downRange;
				this.graph.redraw();
				return;
			}
			var x=ev.offsetX-this.xShift;
			var needRedraw=false;
			var values:number[]=[];
			for(var i in this.graph.data){
				if(this.graph.data[i].active && this.graph.data[i].points.length>0){
					var nearest=this.getNearest(this.points[i], x, 0, this.points[i].length-1);
					if(!this.points[i][nearest] || this.points[i][nearest].x<0)
						nearest++;
					if(!this.points[i][nearest] || this.points[i][nearest].x>=this.w)
						nearest--;

					var prev=this.selectedPoints[i];
					if(!this.points[i][nearest] || this.points[i][nearest].x<0 || this.points[i][nearest].x>=this.w)
						this.selectedPoints[i]=-1;
					else
						this.selectedPoints[i]=nearest;

					if(prev!=this.selectedPoints[i])
						needRedraw=true;

					if(this.selectedPoints[i]!=-1)
						values.push(this.graph.data[i].points[this.points[i][this.selectedPoints[i]].i].y);
					else
						values.push(null);
				}else{
					values.push(null);
				}
			}
			this.lastMouseX=x;
			this.graph.legend.setValues(values);
			if(needRedraw)
				this.graph.redraw();
		}

		public onMouseLeave(){
			for(var i in this.selectedPoints){
				this.selectedPoints[i]=-1;
			}
			this.graph.legend.setValues([]);
			this.graph.redraw();
		}

		public onMouseDown(ev:MouseEvent):boolean{
			var x=ev.offsetX-this.xShift;
			var y=ev.offsetY-this.yShift;

			if(x>=0 && y>=0 && x<this.w && y<this.h){
				this.graph.setCursor("grabbing");
				this.downX=ev.pageX;
				this.dragging=true;
				this.downLeft=this.graph.slider.xLeft;
				this.downRange=this.graph.slider.xRight-this.downLeft;
				for(var i in this.selectedPoints){
					this.selectedPoints[i]=-1;
				}
				return true;
			}
			
			return false;
		}

		public onMouseUp(ev:MouseEvent){
			this.graph.setCursor("");
			this.dragging=false;
		}
	}
}
