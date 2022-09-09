class PopupMenu{

	private root:HTMLElement;
	private menu:HTMLElement;
	private actualMenu:HTMLElement;
	private title:HTMLElement;
	private listener:{(id:string, args:any):void};
	private prepareCallback:{():void};

	public constructor(el:HTMLElement, listener:{(id:string):void}){
		this.root=el;
		this.listener=listener;
		el.addEventListener("mouseenter", this.onMouseOver.bind(this), false);
		el.addEventListener("mouseleave", this.onMouseOut.bind(this), false);

		this.menu=el.qs(".popupMenu");
		this.actualMenu=this.menu.qs("ul");
		this.title=ce("div", {className: "menuTitle", innerText: el.qs(".opener").innerText});

		this.actualMenu.addEventListener("click", this.onItemClick.bind(this), false);
	}

	private onMouseOver(ev:MouseEvent){
		if(this.prepareCallback)
			this.prepareCallback();
		if(this.menu.contains(this.title))
			this.menu.removeChild(this.title);

		// show first, to know the height
		this.menu.showAnimated();

		var rect=this.menu.getBoundingClientRect();
		var scrollH=document.documentElement.clientHeight;

		if(rect.bottom+26>scrollH){
			this.menu.classList.add("popupUp");
			this.menu.classList.remove("popupDown");
			this.menu.insertAdjacentElement("beforeend", this.title);
		}else{
			this.menu.classList.remove("popupUp");
			this.menu.classList.add("popupDown");
			this.menu.insertAdjacentElement("afterbegin", this.title);
		}

	}

	private onMouseOut(ev:MouseEvent){
		this.menu.hideAnimated();
	}

	private onItemClick(ev:MouseEvent){
		var t=ev.target as HTMLElement;
		var li=t.tagName=="LI" ? t : t.parentElement;
		li.style.background="none";
		setTimeout(()=>{
			li.style.background="";
		}, 50);
		setTimeout(()=>{
			this.menu.hideAnimated();
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
}
