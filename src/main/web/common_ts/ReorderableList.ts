class ReorderableList{

	private items:HTMLElement[];
	private root:HTMLElement;

	private offsetX:number;
	private offsetY:number;
	private draggedEl:HTMLElement;
	private draggedWrap:HTMLElement;

	private moveListener:any;
	private upListener:any;
	private idx:number;
	private initialIdx:number;

	private reorderUrl:string;
	private currentTouchID:number=undefined;

	public constructor(root:HTMLElement, reorderUrl:string){
		this.items=root.querySelectorAll(".reorderableItemWrap").unfuck() as HTMLElement[];
		this.items.forEach((el)=>{
			el.addEventListener("mousedown", this.onMouseDown.bind(this), false);
			var grip=el.qs(".draggyGrippyThing");
			if(grip){
				grip.addEventListener("touchstart", this.onTouchStart.bind(this), false);
			}
		});
		this.root=root;
		this.reorderUrl=reorderUrl;
	}

	private onMouseDown(ev:MouseEvent){
		var target=ev.target as HTMLElement;
		if(target.tagName=="A")
			return;
		if(this.items.length<2)
			return;
		this.startDragging(ev.pageX, ev.pageY, target);
		document.addEventListener("mousemove", this.moveListener=this.onMouseMove.bind(this), false);
		document.addEventListener("mouseup", this.upListener=this.onMouseUp.bind(this), false);
	}

	private onTouchStart(ev:TouchEvent){
		var target=ev.target as HTMLElement;
		if(this.items.length<2)
			return;
		if(this.currentTouchID)
			return;
		this.currentTouchID=ev.touches[0].identifier;
		this.startDragging(0, ev.touches[0].pageY, target);
		ev.preventDefault();
		document.addEventListener("touchmove", this.moveListener=this.onTouchMove.bind(this), false);
		document.addEventListener("touchend", this.upListener=this.onTouchEnd.bind(this), false);
		document.addEventListener("touchcancel", this.upListener, false);
	}

	private onMouseMove(ev:MouseEvent){
		this.drag(ev.pageX, ev.pageY);
	}

	private onTouchMove(ev:TouchEvent){
		if(ev.touches.length==1){
			this.drag(0, ev.touches[0].pageY);	
		}else{
			ev.touches.unfuck().forEach((touch)=>{
				if(touch.identifier==this.currentTouchID){
					this.drag(0, touch.pageY);	
				}
			});
		}
	}

	private onMouseUp(ev:MouseEvent){
		document.removeEventListener("mousemove", this.moveListener);
		document.removeEventListener("mouseup", this.upListener);

		this.endDragging();
	}

	private onTouchEnd(ev:TouchEvent){
		ev.changedTouches.unfuck().forEach((touch)=>{
			if(touch.identifier==this.currentTouchID){
				this.currentTouchID=undefined;
				document.removeEventListener("touchmove", this.moveListener);
				document.removeEventListener("touchend", this.upListener);
				document.removeEventListener("touchcancel", this.upListener);

				this.endDragging();
			}
		});
	}

	private startDragging(pageX:number, pageY:number, target:HTMLElement){
		while(!target.classList.contains("reorderableItemWrap")){
			target=target.parentElement;
		}
		this.draggedWrap=target;
		this.draggedEl=target.qs(".reorderableItem");
		var wrapRect=this.root.getBoundingClientRect();

		wrapRect=this.draggedWrap.getBoundingClientRect();
		this.offsetX=pageX-(wrapRect.left+window.pageXOffset);
		this.offsetY=pageY-(wrapRect.top+window.pageYOffset);
		this.initialIdx=this.idx=this.items.indexOf(this.draggedWrap);

		this.draggedWrap.classList.add("beingDragged");
	}

	private drag(pageX:number, pageY:number){
		var wrapRect=this.draggedWrap.getBoundingClientRect();
		var dx=Math.round(pageX-(wrapRect.left+window.pageXOffset)-this.offsetX);
		var dy=Math.round(pageY-(wrapRect.top+window.pageYOffset)-this.offsetY);
		var update=false;

		// If the currently dragged item vertically overlaps more than half of a neighboring item, switch them around
		if(dy<0){
			if(this.idx>0){
				var neighborRect=this.items[this.idx-1].getBoundingClientRect();
				if(wrapRect.top+dy<neighborRect.top+neighborRect.height*0.5){
					this.root.insertBefore(this.draggedWrap, this.items[this.idx-1]);
					this.items=this.root.querySelectorAll(".reorderableItemWrap").unfuck() as HTMLElement[];
					update=true;
					this.idx--;
				}
			}
		}else if(this.idx<this.items.length-1){
			var neighborRect=this.items[this.idx+1].getBoundingClientRect();
			if(wrapRect.bottom+dy>neighborRect.top+neighborRect.height*0.5){
				if(this.idx+1==this.items.length-1){
					this.root.appendChild(this.draggedWrap);
				}else{
					this.root.insertBefore(this.draggedWrap, this.items[this.idx+2]);
				}
				this.items=this.root.querySelectorAll(".reorderableItemWrap").unfuck() as HTMLElement[];
				update=true;
				this.idx++;
			}
		}

		if(update){
			wrapRect=this.draggedWrap.getBoundingClientRect();
			dx=Math.round(pageX-(wrapRect.left+window.pageXOffset)-this.offsetX);
			dy=Math.round(pageY-(wrapRect.top+window.pageYOffset)-this.offsetY);
		}

		this.draggedEl.style.transform=`translate(${dx}px, ${dy}px)`;
	}

	private endDragging(){
		this.draggedEl.anim({transform: [this.draggedEl.style.transform, "translate(0, 0)"]}, {duration: 200, easing: "ease-in-out"}, ()=>{
			this.draggedWrap.classList.remove("beingDragged");
			this.draggedEl.style.transform="";
		});

		if(this.idx!=this.initialIdx){
			ajaxPost(this.reorderUrl, {id: this.draggedEl.getAttribute("data-reorder-id"), order: this.idx, csrf: userConfig.csrf}, ()=>{}, ()=>{});
		}
	}
}
