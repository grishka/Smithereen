namespace smithereen.graph{
	export class Legend extends DisplayObject{
		private graph:Graph;

		private items:LegendItem[]=[];
		private itemUnderMouse:LegendItem=null;

		public constructor(graph:Graph){
			super();
			this.graph=graph;
			for(var cat of graph.data){
				var item=new LegendItem(cat);
				this.items.push(item);
				this.children.push(item);
			}
		}

		public draw(ctx:CanvasRenderingContext2D){
			
		}

		public updateLayout(){
			var availWidth=this.graph.width-this.x-Graph.LEGEND_LEFTPADDING;
			var itemWidth=Math.round(availWidth/2);
			var i=0;
			for(var item of this.items){
				item.x=(i%2==0 ? 0 : itemWidth)+Graph.LEGEND_LEFTPADDING;
				item.y=Graph.LEGEND_FONT_SIZE+Graph.LEGEND_ELEM_HEIGHT*Math.floor(i/2);
				item.y=item.y-Graph.LEGEND_CHECK_WIDTH/2+5;
				item.width=itemWidth;
				item.updateLayout(this.graph.ctx);
				i++;
			}
		}

		public setValues(values:number[]){
			for(var i in this.items){
				this.items[i].value=values[i];
			}
		}

		private findItemAtPoint(x:number, y:number):LegendItem{
			var availWidth=this.graph.width-this.x-Graph.LEGEND_LEFTPADDING;
			var itemWidth=Math.round(availWidth/2);
			for(var item of this.items){
				if(x>=item.x && y>=item.y && x<item.x+itemWidth && y<item.y+Graph.LEGEND_ELEM_HEIGHT){
					return item;
				}
			}
			return null;
		}

		public onMouseMove(ev:MouseEvent){
			var x=ev.offsetX-this.x;
			var y=ev.offsetY-this.y;
			var newItem=this.findItemAtPoint(x, y);
			if(newItem==this.itemUnderMouse)
				return;
			if(this.itemUnderMouse!=null)
				this.itemUnderMouse.isOver=false;
			if(newItem!=null)
				newItem.isOver=true;
			this.itemUnderMouse=newItem;
			this.graph.setCursor(newItem ? "pointer" : null);
			this.graph.redraw();
		}

		public onMouseLeave(){
			if(this.itemUnderMouse){
				this.itemUnderMouse.isOver=false;
				this.itemUnderMouse=null;
			}
		}

		public onClick(ev:MouseEvent){
			var item=this.findItemAtPoint(ev.offsetX-this.x, ev.offsetY-this.y);
			if(item){
				item.category.active=!item.category.active;
				this.graph.redraw();
			}
		}
	}

	class LegendItem extends DisplayObject{
		public category:GraphCategory;
		public width:number;
		public value:number=null;
		public isOver:boolean=false;
		private truncatedTitle:string;

		public constructor(category:GraphCategory){
			super();
			this.category=category;
		}

		public draw(ctx:CanvasRenderingContext2D){
			this.drawCheckbox(ctx, this.category.color, this.category.active, this.isOver);
			ctx.fillStyle=Utils.cssColor(this.category.color, 1);
			ctx.fillText(this.truncatedTitle, Graph.LEGEND_CHECK_WIDTH+6, Graph.LEGEND_ELEM_HEIGHT/2+1);
			if(this.value!=null){
				var value=Utils.formatNumber(this.value);
				var valueW=Math.floor(ctx.measureText(value).width);
				var valueX=Math.floor(ctx.measureText(this.truncatedTitle).width)+Graph.LEGEND_VALUE_PADDING+Graph.LEGEND_CHECK_WIDTH+6;
				ctx.fillStyle=Utils.cssColor(this.category.color, 0.2);
				ctx.fillRect(valueX, Graph.LEGEND_ELEM_HEIGHT/2-12, valueW+6, 17);
				ctx.fillStyle=Utils.cssColor(this.category.color, 1);
				ctx.fillText(value, valueX+3, Graph.LEGEND_ELEM_HEIGHT/2+1);
			}
		}

		public updateLayout(ctx:CanvasRenderingContext2D){
			var maxTextWidth=this.width-(Graph.LEGEND_CHECK_WIDTH+6);
			if(ctx.measureText(this.category.name).width<=maxTextWidth){
				this.truncatedTitle=this.category.name;
			}else{
				var title=this.category.name;
				do{
					title=title.substring(0, title.length-1);
				}while(title.length>0 && ctx.measureText(title+"...").width>maxTextWidth);
				this.truncatedTitle=title+"...";
			}
		}

		private drawCheckbox(ctx:CanvasRenderingContext2D, color:number, checked:boolean, over:boolean){
			ctx.lineWidth=1;
			ctx.strokeStyle=Utils.cssColor(color, 0.84);
			ctx.strokeRect(0.5, 0.5, 13, 13);
			if(over){
				ctx.fillStyle=Utils.cssColor(color, 0.25);
				ctx.fillRect(0, 0, 14, 14);
			}
			if(checked){
				ctx.fillStyle=Utils.cssColor(color, 0.2);
				ctx.beginPath();
				ctx.moveTo(3, 9.5);
				ctx.lineTo(3, 7.5);
				ctx.lineTo(6.5, 10);
				ctx.lineTo(13.5, 3);
				ctx.lineTo(15, 2);
				ctx.lineTo(15, 4.5);
				ctx.lineTo(6.5, 13);
				ctx.closePath();
				ctx.fill();
				ctx.fillStyle=Utils.cssColor(color, 1);
				ctx.beginPath();
				ctx.moveTo(3, 7.5);
				ctx.lineTo(4.5, 6);
				ctx.lineTo(6.5, 8);
				ctx.lineTo(13.5, 1);
				ctx.lineTo(15, 1);
				ctx.lineTo(15, 2.5);
				ctx.lineTo(6.5, 11);
				ctx.closePath();
				ctx.fill();
			}
		}
	}
}
