namespace smithereen.graph{
	export abstract class DisplayObject{
		public x:number;
		public y:number;
		public children:DisplayObject[]=[];

		public dispatchDraw(ctx:CanvasRenderingContext2D):void{
			ctx.save();
			ctx.translate(this.x, this.y);
			this.draw(ctx);
			for(var child of this.children){
				child.dispatchDraw(ctx);
			}
			ctx.restore();
		}

		public abstract draw(ctx:CanvasRenderingContext2D):void;
		public onMouseDown(ev:MouseEvent):boolean{
			return false;
		}
		public onMouseMove(ev:MouseEvent, global:boolean){}
		public onMouseUp(ev:MouseEvent){}
		public onMouseLeave(){}
	}
}
