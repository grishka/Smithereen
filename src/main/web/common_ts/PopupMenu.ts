interface PopupMenuItemSpec{
	title:string;
	id:string;
}

class PopupMenu{

	protected root:HTMLElement;
	private menu:HTMLElement;
	private actualMenu:HTMLElement;
	private title:HTMLElement;
	private listener:{(id:string, args:any):boolean};
	private prepareCallback:{():void};
	private isPopupDown=true;
	private opener:HTMLElement;
	private titleValue:HTMLElement;
	private isVisible:boolean=false;
	private lastClickEventTimestamp:number;

	public constructor(el:HTMLElement, listener:{(id:string):boolean}, useMouseOver:boolean=true, dropdownArrow:boolean=false){
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
				var item=items[i];
				this.actualMenu.appendChild(item);
				if(item.classList.contains("hasSubmenu")){
					var submenuContent=item.children.unfuck();
					item.innerHTML="";
					for(var j=submenuContent.length-1;j>=0;j--){
						var subEl=submenuContent[j];
						item.appendChild(subEl);
						if(subEl.tagName=="UL"){
							var subItems=subEl.children.unfuck();
							subEl.innerHTML="";
							for(var k=subItems.length-1;k>=0;k--){
								subEl.appendChild(subItems[k]);
							}
						}
					}
				}
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
		if(this.lastClickEventTimestamp==ev.timeStamp)
			return;
		this.lastClickEventTimestamp=ev.timeStamp;
		var t=ev.target as HTMLElement;
		var li=t.closest("li");
		if(!this.listener(li.dataset.act, li.dataset.args ? JSON.parse(li.dataset.args) : {})){
			li.classList.add("forceUnselected");
			setTimeout(()=>{
				li.classList.remove("forceUnselected");
			}, 50);
			setTimeout(()=>{
				this.hide();
			}, 100);
		}
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

interface MultipleChoicePopupMenuOptions{
	baseID:string;
	allTitle:string;
	selectTitle:string;
	noneTitle:string;
	showNone:boolean;
	options:PopupMenuItemSpec[];
	selectedOptions:string[];
	allCheckboxID:string;
}

class MultipleChoicePopupMenu extends PopupMenu{
	private checkboxes:HTMLInputElement[];
	private allCB:HTMLInputElement;
	private allItem:HTMLElement;
	private noneItem:HTMLElement;
	private selectItem:HTMLElement;

	public constructor(el:HTMLElement, opts:MultipleChoicePopupMenuOptions){
		var checkboxes:HTMLInputElement[]=[];
		var opener=el.qs(".opener");
		var menuList;
		var menuEl=ce("div", {"className": "popupMenu compact"}, [
			menuList=ce("ul", {"className": "privacyMenu"})
		]);
		menuEl.hide();
		opener.insertAdjacentElement("afterend", menuEl);

		var allItem:HTMLElement, noneItem:HTMLElement, selectItem:HTMLElement;
		menuList.appendChild(allItem=ce("li", {innerHTML: opts.allTitle}));
		allItem.dataset.act="all";
		if(opts.showNone){
			menuList.appendChild(noneItem=ce("li", {innerHTML: opts.noneTitle}));
			noneItem.dataset.act="none";
		}
		var sublist;
		menuList.appendChild(selectItem=ce("li", {className: "hasSubmenu"}, [
			ce("div", {className: "itemContent"}, [
				ce("span", {innerHTML: opts.selectTitle}),
				ce("div", {className: "ddIcon iconSubmenu"})
			]),
			sublist=ce("ul", {className: "submenu"})
		]));
		selectItem.dataset.act="select";

		var allCB=ge(opts.allCheckboxID) as HTMLInputElement;
		var i=0;
		for(var option of opts.options){
			var checkbox;
			var si=ce("li", {}, [
				ce("label", {}, [
					checkbox=ce("input", {type: "checkbox", name: opts.baseID+"_"+option.id, id: opts.baseID+"_"+option.id, checked: allCB.checked || opts.selectedOptions.indexOf(option.id)!=-1}),
					ce("span", {innerHTML: option.title})
				])
			]);
			checkbox.customData={
				option: {id: option.id, index: i, title: option.title}
			};
			checkboxes.push(checkbox);
			sublist.appendChild(si);
			i++;
		}

		super(el, (id)=>{
			if(id=="all"){
				allCB.checked=true;
				for(var cb of this.checkboxes){
					cb.checked=true;
				}
				ge(opts.baseID).innerHTML="";
				opener.innerHTML=opts.allTitle;
				return false;
			}else if(id=="none"){
				allCB.checked=false;
				for(var cb of this.checkboxes){
					cb.checked=false;
				}
				ge(opts.baseID).innerHTML="";
				opener.innerHTML=opts.noneTitle;
				return false;
			}
			return true;
		}, false, true);

		this.checkboxes=checkboxes;
		this.allCB=allCB;
		this.allItem=allItem;
		this.noneItem=noneItem;
		this.selectItem=selectItem;

		sublist.addEventListener("change", (ev)=>{
			var allChecked=true;
			var allUnchecked=true;
			for(var cb of checkboxes){
				if(!cb.checked){
					allChecked=false;
				}else{
					allUnchecked=false;
				}
			}
			var descrEl=ge(opts.baseID);
			if(allChecked){
				descrEl.innerHTML="";
				opener.innerHTML=opts.allTitle;
				allCB.checked=true;
				return;
			}
			allCB.checked=false;
			if(allUnchecked){
				descrEl.innerHTML="";
				opener.innerHTML=opts.noneTitle;
				return;
			}
			opener.innerHTML=opts.selectTitle;
			descrEl.innerHTML=": ";
			var first=true;
			for(var cb of checkboxes){
				if(cb.checked){
					if(first){
						first=false;
					}else{
						descrEl.appendChild(document.createTextNode(", "));
					}
					var opt=cb.customData.option;
					descrEl.appendChild(ce("span", {className: "friendListLabel l"+(opt.index%8), innerHTML: opt.title}));
				}
			}
		});
	}

	public show(){
		var anyChecked=false;
		for(var cb of this.checkboxes){
			if(cb.checked){
				anyChecked=true;
				break;
			}
		}
		this.allItem.classList.remove("selected");
		if(this.noneItem)
			this.noneItem.classList.remove("selected");
		this.selectItem.classList.remove("selected");
		if(this.allCB.checked){
			this.allItem.classList.add("selected");
		}else if(anyChecked || !this.noneItem){
			this.selectItem.classList.add("selected");
		}else{
			this.noneItem.classList.add("selected");
		}
		super.show();
	}
}

class FriendListsPopupMenu extends PopupMenu{
	private checkboxes:HTMLInputElement[];
	private userID:string;
	private listsCont:HTMLElement;
	private saveTimeout:number;

	public constructor(el:HTMLElement, userID:string){
		var opener=el.qs(".opener");
		var menuList;
		var menuEl=ce("div", {"className": "popupMenu compact checkboxes"}, [
			menuList=ce("ul", {"className": ""})
		]);
		menuEl.hide();
		opener.insertAdjacentElement("afterend", menuEl);

		super(el, id=>true, false, true);
		this.userID=userID;
		this.listsCont=ge("frowLists"+userID);
		this.checkboxes=[];

		for(var id in cur.friendLists){
			var cbox;
			var item=ce("li", {}, [
				ce("label", {}, [
					cbox=ce("input", {type: "checkbox", id: `friendList${id}_${userID}`}),
					ce("span", {innerText: cur.friendLists[id]})
				])
			]);
			menuList.appendChild(item);
			cbox.customData={listID: id};
			this.checkboxes.push(cbox);
		}
		menuList.addEventListener("change", ev=>{
			var selectedLists:string[]=[];
			if(this.listsCont)
				this.listsCont.innerHTML="";
			for(var cbox of this.checkboxes){
				if(cbox.checked){
					var id=cbox.customData.listID;
					selectedLists.push(id);
					if(this.listsCont){
						this.listsCont.appendChild(ce("span", {className: "friendListLabel l"+((id-1)%8), innerText: cur.friendLists[id]}));
						this.listsCont.append(" ");
					}
				}
			}
			this.root.dataset.lists=selectedLists.join(',');
			if(this.saveTimeout)
				clearTimeout(this.saveTimeout);
			this.saveTimeout=setTimeout(()=>this.saveLists(selectedLists), 1000);
		});
	}

	public setSelectedLists(ids:string[]){
		for(var cbox of this.checkboxes){
			cbox.checked=ids.indexOf(cbox.customData.listID)!=-1;
		}
	}

	private saveLists(ids:string[]){
		this.saveTimeout=null;
		ajaxPost("/users/"+this.userID+"/setFriendLists", {lists: ids.join(','), csrf: userConfig.csrf}, ()=>{}, ()=>{});
	}
}

