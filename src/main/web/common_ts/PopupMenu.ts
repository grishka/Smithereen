interface PopupMenuItemSpec{
	title:string;
	id:string;
}

class PopupMenu{

	private root:HTMLElement;
	private menu:HTMLElement;
	private actualMenu:HTMLElement;
	private title:HTMLElement;
	private listener:{(id:string, args:any):void};
	private prepareCallback:{():void};
	private isPopupDown=true;
	private opener:HTMLElement;
	private titleValue:HTMLElement;
	private isVisible:boolean=false;

	public constructor(el:HTMLElement, listener:{(id:string):void}, useMouseOver:boolean=true, dropdownArrow:boolean=false){
		this.root=el;
		this.listener=listener;
		if(useMouseOver){
			el.addEventListener("mouseenter", this.onMouseOver.bind(this), false);
		}
		el.addEventListener("mouseleave", this.onMouseOut.bind(this), false);

		this.menu=el.qs(".popupMenu");
		if(this.menu){
			this.actualMenu=this.menu.qs("ul");
		}else{
			this.menu=ce("div", {className: "popupMenu compact"}, [
				this.actualMenu=ce("ul")
			]);
			this.menu.hide();
			el.appendChild(this.menu);
		}
		this.opener=el.qs(".opener");
		this.title=ce("div", {className: "menuTitle"}, [
			this.titleValue=ce("span", {innerText: this.opener.innerText})
		]);
		if(dropdownArrow){
			this.title.appendChild(ce("div", {className: "ddIcon"}));
			this.menu.classList.add("hasDdIcons");
		}
		this.menu.insertAdjacentElement("afterbegin", this.title);
		this.menu.classList.add("popupDown");

		this.actualMenu.addEventListener("click", this.onItemClick.bind(this), false);
		this.actualMenu.addEventListener("mouseenter", (ev)=>{
			var selectedItem=this.actualMenu.qs(".selected");
			if(selectedItem)
				selectedItem.classList.remove("selected");
		});
	}

	private onMouseOver(ev:MouseEvent){
		this.show();
	}

	public show(){
		if(this.isVisible)
			return;
		this.isVisible=true;
		if(this.prepareCallback)
			this.prepareCallback();
		this.titleValue.innerText=this.opener.innerText;

		// show first, to know the height
		this.menu.showAnimated();

		var rect=this.menu.getBoundingClientRect();
		var scrollH=document.documentElement.clientHeight;
		var newDown=rect.bottom+26<=scrollH;
		if(newDown!=this.isPopupDown){
			var items=this.actualMenu.children.unfuck();
			this.actualMenu.innerHTML="";
			for(var i=items.length-1;i>=0;i--){
				this.actualMenu.appendChild(items[i]);
			}
			this.menu.insertAdjacentElement(newDown ? "afterbegin" : "beforeend", this.title);
			this.menu.classList.remove(newDown ? "popupUp" : "popupDown");
			this.menu.classList.add(newDown ? "popupDown" : "popupUp");
			this.isPopupDown=newDown;
		}
	}

	private onMouseOut(ev:MouseEvent){
		this.hide();
	}

	public hide(){
		if(this.isVisible){
			this.menu.hideAnimated();
			this.isVisible=false;
		}
	}

	private onItemClick(ev:MouseEvent){
		var t=ev.target as HTMLElement;
		var li=t.tagName=="LI" ? t : t.parentElement;
		li.classList.add("forceUnselected");
		setTimeout(()=>{
			li.classList.remove("forceUnselected");
		}, 50);
		setTimeout(()=>{
			this.hide();
		}, 100);

		this.listener(li.dataset.act, li.dataset.args ? JSON.parse(li.dataset.args) : {});
	}

	public setItemVisibility(act:string, visible:boolean){
		for(var item of this.actualMenu.children.unfuck()){
			if(item.dataset.act==act){
				if(visible)
					item.show();
				else
					item.hide();
				break;
			}
		}
	}

	public setPrepareCallback(prepareCallback:{():void}){
		this.prepareCallback=prepareCallback;
	}

	public addItems(items:PopupMenuItemSpec[]){
		for(var item of items){
			var li=ce("li", {}, [item.title]);
			li.dataset.act=item.id;
			this.actualMenu.appendChild(li);
		}
	}

	public addItem(item:PopupMenuItemSpec){
		this.addItems([item]);
	}
}
