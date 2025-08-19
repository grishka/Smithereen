
interface PrivacySetting{
	r:string;
	au:number[];
	xu:number[];
	al:number[];
	xl:number[];
}

function getFriendForPrivacy(id:number){
	for(var user of cur.friendListForPrivacy){
		if(user[0]==id)
			return user;
	}
	return [id, "DELETED", "/id"+id, "DELETED", null];
}

function showPrivacyMenu(el:HTMLAnchorElement, key:string, onlyMe:boolean, onlyFriends:boolean){
	if(!el.customData)
		el.customData={};
	var menuEl=el.customData.menuEl || (el.customData.menuEl=ce("div", {"className": "popupMenu compact"}, [
		ce("ul", {"className": "privacyMenu"})
	]));
	menuEl.hide();
	el.insertAdjacentElement("afterend", menuEl);
	var menuList=menuEl.querySelector("ul");
	if(!menuList.children.length){
		var opts:string[];
		var checkboxes:HTMLInputElement[]=[];
		if(onlyFriends){
			opts=["friends", onlyMe ? "only_me" : "no_one", "friends_except", "certain_friends"];
		}else{
			opts=["everyone", "friends", "friends_of_friends", onlyMe ? "only_me" : "no_one", "everyone_except", "certain_friends"];
		}
		for(var opt of opts){
			var li;
			menuList.appendChild(li=ce("li", {}, [lang("privacy_value_"+opt)]));
			li.dataset.act=opt;
			if(opt=="certain_friends"){
				li.appendChild(ce("div", {className: "ddIcon iconPlus"}));
			}
		}
		var listsSublist;
		var listsItem=ce("li", {className: "hasSubmenu"}, [
			ce("div", {className: "itemContent"}, [
				ce("span", {innerHTML: lang("privacy_value_certain_friend_lists")}),
				ce("div", {className: "ddIcon iconSubmenu"})
			]),
			listsSublist=ce("ul", {className: "submenu"})
		]);
		listsItem.dataset.act="certain_friend_lists";
		for(var lid in cur.friendLists){
			var cbox;
			var item=ce("li", {}, [
				ce("label", {}, [
					cbox=ce("input", {type: "checkbox", id: `friendList${lid}_${key}`}),
					ce("span", {innerText: cur.friendLists[lid]})
				])
			]);
			listsSublist.appendChild(item);
			cbox.customData={listID: parseInt(lid)};
			checkboxes.push(cbox);
		}
		menuList.appendChild(listsItem);
		listsSublist.addEventListener("change", ev=>{
			var value:PrivacySetting={r: "n", au: [], xu: [], al: [], xl: []};
			for(var cbox of checkboxes){
				if(cbox.checked){
					value.al.push(cbox.customData.listID);
				}
			}
			setValue(value);
		});
	}
	var valueFld=ge(key+"_value") as HTMLInputElement;
	var extendedValue=ge("privacyExtended_"+key);
	var setValue=(v:PrivacySetting)=>{
		valueFld.value=JSON.stringify(v);
		switch(v.r){
			case 'e':
				el.innerText=lang("privacy_value_everyone");
				break;
			case 'f':
				el.innerText=lang("privacy_value_friends");
				break;
			case 'ff':
				el.innerText=lang("privacy_value_friends_of_friends");
				break;
			case 'n':
				if(!v.au || (v.au.length==0 && v.al.length==0)){
					el.innerText=lang(onlyMe ? "privacy_value_only_me" : "privacy_value_no_one");
				}else{
					el.innerText=lang("privacy_value_certain_friends");
				}
				break;
		}
		var extHtml:string="";
		if(v.au.length || v.al.length){
			extHtml+=lang("privacy_settings_value_certain_friends_before");
			var links:string[]=[];
			for(var id of v.al){
				links.push(`<span class="friendListLabel l${(id-1)%8}">${cur.friendLists[id].escapeHTML()}</span>`);
			}
			for(var id of v.au){
				var user=getFriendForPrivacy(id);
				links.push(`<a href="${user[2]}">${user[1].escapeHTML()}</a>`);
			}
			extHtml+=links.join(lang("privacy_settings_value_name_separator"));
		}
		if(v.xu.length || v.xl.length){
			extHtml+=lang("privacy_settings_value_except");
			var links:string[]=[];
			for(var id of v.xl){
				links.push(`<span class="friendListLabel l${(id-1)%8}">${cur.friendLists[id].escapeHTML()}</span>`);
			}
			for(var id of v.xu){
				var user=getFriendForPrivacy(id);
				links.push(`<a href="${user[2]}">${user[3].escapeHTML()}</a>`);
			}
			extHtml+=links.join(lang("privacy_settings_value_name_separator"));
		}
		extendedValue.innerHTML=extHtml;
	};
	var previousValue=JSON.parse(valueFld.value);
	var menu=el.customData.menu || (el.customData.menu=new PopupMenu(el.parentElement, (id)=>{
		if(!id || id=="certain_friend_lists"){
			return true;
		}else if(id=="certain_friends"){
			loadFriendsForBoxes(()=>{
				var box=new FriendListChoiceBox(lang("select_friends_title"), ids=>{
					setValue({r: "n", au: ids, xu: [], al: [], xl: []});
					box.dismiss();
				}, previousValue.au);
				box.show();
			});
		}else if(id=="everyone_except" || id=="friends_except"){
			loadFriendsForBoxes(()=>{
				new ExtendedPrivacyBox(setValue.bind(this), previousValue, onlyFriends).show();
			});
		}else{
			var rule:string;
			switch(id){
				case "everyone":
					rule="e";
					break;
				case "friends":
					rule="f";
					break;
				case "friends_of_friends":
					rule="ff";
					break;
				case "only_me":
				case "no_one":
					rule="n";
					break;
			}
			setValue({r: rule, au: [], xu: [], al: [], xl: []});
		}
		return false;
	}, false, true));
	var curSel=menuList.qs(".selected");
	if(curSel){
		curSel.classList.remove("selected");
	}
	var selectedItem:string;
	if(previousValue.xu.length || previousValue.xl.length){
		selectedItem=onlyFriends ? "friends_except" : "everyone_except";
	}else{
		switch(previousValue.r){
			case "e":
				selectedItem="everyone";
				break;
			case "f":
				selectedItem="friends";
				break;
			case "ff":
				selectedItem="friends_of_friends";
				break;
			case "n":
				if(previousValue.au.length){
					selectedItem="certain_friends";
				}else if(previousValue.al.length){
					selectedItem="certain_friend_lists";
				}else{
					selectedItem=onlyMe ? "only_me" : "no_one";
				}
				break;
		}
	}
	var selectedEl=menuList.qs("li[data-act="+selectedItem+"]");
	if(selectedEl){
		selectedEl.classList.add("selected");
	}
	for(var cbox of menuList.querySelectorAll(".hasSubmenu input")){
		cbox.checked=previousValue.al.indexOf(cbox.customData.listID)!=-1;
	}
	menu.show();
}

function loadFriendsForBoxes(onDone:{():void}){
	if(!cur.friendListForPrivacy){
		LayerManager.getInstance().showBoxLoader();
		ajaxGet("/my/friends/ajaxFriendsForPrivacyBoxes", (r)=>{
			cur.friendListForPrivacy=r;
			onDone();
		}, null);
	}else{
		onDone();
	}
}

class ExtendedPrivacyBox extends Box{
	private allowedLink:HTMLAnchorElement;
	private menu:PopupMenu;
	private friends:{normalizedName:string, token:TokenInputToken}[]=[];
	private allowField:TokenInput;
	private denyField:TokenInput;
	private allowFriendsField:HTMLElement;
	private denyFriendsField:HTMLElement;
	private rule:string;

	public constructor(callback:{(v:PrivacySetting):void}, currentValue:PrivacySetting, onlyFriends:boolean){
		super(lang("privacy_settings_title"), [lang("save"), lang("cancel")], (idx)=>{
			if(idx==0){
				var allow:number[]=[];
				var deny:number[]=[];
				var allowLists:number[]=[];
				var denyLists:number[]=[];
				if(this.rule=="n"){
					for(var id of this.allowField.getTokenIDs()){
						if(id.startsWith("list"))
							allowLists.push(parseInt(id.substr(4)));
						else
							allow.push(parseInt(id));
					}
				}
				for(var id of this.denyField.getTokenIDs()){
					if(id.startsWith("list"))
						denyLists.push(parseInt(id.substr(4)));
					else
						deny.push(parseInt(id));
				}
				callback({r: this.rule, au: allow, xu: deny, al: allowLists, xl: denyLists});
			}
			this.dismiss();
		});
		var allowedWrap;
		this.setContent(ce("div", {className: "privacyExtendedBox"}, [
			ce("span", {className: "plusFriends"}),
			ce("h3", {}, [lang("privacy_allowed_title")]),
			allowedWrap=ce("div", {innerHTML: lang("privacy_allowed_to_X")}, []),
			this.allowFriendsField=ce("div"),
			ce("span", {className: "minusFriends"}),
			ce("h3", {}, [lang("privacy_denied_title")]),
			this.denyFriendsField=ce("div"),
		]));
		this.allowedLink=allowedWrap.qs("#value");
		this.allowedLink.removeAttribute("id");
		this.allowedLink.href="javascript:void(0)";
		this.allowedLink.className="opener";
		var menuWrap=ce("span", {className: "popupMenuW"});
		allowedWrap.insertBefore(menuWrap, this.allowedLink);
		menuWrap.appendChild(this.allowedLink);
		this.setRule(currentValue.r);
		this.menu=new PopupMenu(menuWrap, (id)=>{
			this.setRule(id);
			return false;
		}, false, true);
		this.allowedLink.onclick=(ev)=>{
			this.menu.show();
		};
		if(onlyFriends){
			this.menu.addItems([
				{id: "f", title: lang("privacy_value_to_friends")},
				{id: "n", title: lang("privacy_value_to_certain_friends")}
			]);
		}else{
			this.menu.addItems([
				{id: "e", title: lang("privacy_value_to_everyone")},
				{id: "f", title: lang("privacy_value_to_friends")},
				{id: "ff", title: lang("privacy_value_to_friends_of_friends")},
				{id: "n", title: lang("privacy_value_to_certain_friends")}
			]);
		}

		for(var friend of cur.friendListForPrivacy){
			this.friends.push({
				normalizedName: friend[1],
				token: {
					id: friend[0].toString(),
					title: friend[1]
				}
			});
		}

		this.allowField=new TokenInput(this.allowFriendsField, lang("privacy_enter_friend_name"), this.onProvideCompletions.bind(this));
		this.denyField=new TokenInput(this.denyFriendsField, lang("privacy_enter_friend_name"), this.onProvideCompletions.bind(this));

		for(var id of currentValue.al){
			this.allowField.addToken("list"+id, cur.friendLists[id], false);
		}
		for(var id of currentValue.xl){
			this.denyField.addToken("list"+id, cur.friendLists[id], false);
		}
		for(var id of currentValue.au){
			var user=getFriendForPrivacy(id);
			this.allowField.addToken(user[0], user[1], false);
		}
		for(var id of currentValue.xu){
			var user=getFriendForPrivacy(id);
			this.denyField.addToken(user[0], user[1], false);
		}
	}

	private setRule(rule:string){
		this.rule=rule;
		var valueKey:string;
		switch(rule){
			case "e":
				valueKey="privacy_value_to_everyone";
				break;
			case "f":
				valueKey="privacy_value_to_friends";
				break;
			case "ff":
				valueKey="privacy_value_to_friends_of_friends";
				break;
			case "n":
				valueKey="privacy_value_to_certain_friends";
				break;
		}
		this.allowedLink.innerText=lang(valueKey);
		if(rule=="n")
			this.allowFriendsField.show();
		else
			this.allowFriendsField.hide();
	}

	private onProvideCompletions(q:string):TokenInputToken[]{
		if(q.trim()==""){
			var res:TokenInputToken[]=[];
			for(var lid in cur.friendLists){
				res.push({id: "list"+lid, title: cur.friendLists[lid]});
			}
			return res;
		}
		var re=new RegExp("\\b"+quoteRegExp(q), "i");
		var res:TokenInputToken[]=[];
		for(var f of this.friends){
			if(re.test(f.normalizedName)){
				res.push(f.token);
			}
		}
		return res;
	}
}

class FriendListChoiceBox extends BaseScrollableBox{
	private allFriendsList:HTMLElement;
	private selectedFriendsList:HTMLElement;
	private searchInput:HTMLInputElement;
	private idNameMap:{[key:string]:string}={};
	private selectedEmpty:HTMLElement;
	private selectedIDs:number[]=[];
	private searchFieldWrap:HTMLElement;
	private extraField:HTMLInputElement;

	public constructor(title:string, callback:{(ids:number[]):void}, currentValue:number[]){
		super(title, [lang("save"), lang("cancel")], (idx)=>{
			if(idx==0){
				if(!this.extraField || this.extraField.reportValidity())
					callback(this.selectedIDs);
			}else{
				this.dismiss();
			}
		});

		var root=ce("div", {}, [
			ce("div", {className: "gray borderBottom"}, [
				this.searchFieldWrap=ce("div", {className: "searchFieldWrap singleColumn"}, [
					this.searchInput=ce("input", {type: "text", className: "searchField", autocomplete: "off", placeholder: lang("friends_search_placeholder")})
				])
			]),
			this.wrapScrollableElement(ce("div", {className: "friendSelectionBoxContent"}, [
				ce("div", {className: "inner"}, [
					this.allFriendsList=ce("div", {className: "list allFriends"}, [
						ce("h3", {}, [lang("friend_list_your_friends")])
					]),
					ce("div", {className: "separator"}),
					this.selectedFriendsList=ce("div", {className: "list selectedFriends"}, [
						ce("h3", {}, [lang("friends_in_list")]),
						this.selectedEmpty=ce("div", {className: "emptyState", innerText: lang("select_friends_empty_selection")})
					])
				])
			]))
		]);
		this.setContent(root);
		this.searchInput.addEventListener("input", this.onSearchInputChanged.bind(this));

		for(var friend of cur.friendListForPrivacy){
			var row=this.makeRow(friend);
			if(currentValue.indexOf(friend[0])!=-1){
				row.hide();
				var selRow=this.makeRow(friend);
				selRow.id+="sel";
				this.selectedFriendsList.appendChild(selRow);
				this.selectedEmpty.hide();
				this.selectedIDs.push(parseInt(friend[0]));
			}
			this.allFriendsList.appendChild(row);
			this.idNameMap[friend[0].toString()]=friend[1].toString();
		}
	}

	public setExtraField(placeholder:string, value:string, required:boolean){
		if(this.extraField)
			throw new Error("Already set");
		this.extraField=ce("input", {type: "text", className: "extraField", placeholder: placeholder, value: value, required: required});
		this.searchFieldWrap.classList.add("hasExtraField");
		this.searchFieldWrap.appendChild(this.extraField);
	}

	public setExtraContent(html:string){
		this.searchFieldWrap.parentElement.insertAdjacentHTML("afterend", html);
	}

	protected onCreateContentView():HTMLElement{
		var cont=super.onCreateContentView();
		this.boxLayer.style.width=this.boxLayer.style.minWidth="548px";
		return cont;
	}

	private makeRow(friend:any[]):HTMLElement{
		var row=ce("div", {className: "row", id: "selectFriendsRow"+friend[0]}, [
			makeAvatar(friend[4], "s", 32),
			ce("div", {className: "name ellipsize", innerText: friend[1]}),
			ce("span", {className: "icon"})
		]);
		row.addEventListener("click", this.onRowClick.bind(this, parseInt(friend[0])));
		row.dataset.uid=friend[0];
		return row;
	}

	private onSearchInputChanged(ev:Event){
		var q=this.searchInput.value;
		if(q.trim()==""){
			for(var ch of this.allFriendsList.children.unfuck()){
				ch.show();
			}
			for(var ch of this.selectedFriendsList.children.unfuck()){
				ch.show();
			}
			return;
		}
		var re=new RegExp("\\b"+quoteRegExp(q), "i");
		for(var ch of this.allFriendsList.children.unfuck()){
			if(!ch.dataset.uid)
				continue;
			if(re.test(this.idNameMap[ch.dataset.uid]) && this.selectedIDs.indexOf(parseInt(ch.dataset.uid))==-1)
				ch.show();
			else
				ch.hide();
		}
		for(var ch of this.selectedFriendsList.children.unfuck()){
			if(!ch.dataset.uid)
				continue;
			if(re.test(this.idNameMap[ch.dataset.uid]))
				ch.show();
			else
				ch.hide();
		}
	}

	private onRowClick(id:number, ev:MouseEvent){
		var row=ge("selectFriendsRow"+id);
		if(this.selectedIDs.indexOf(id)!=-1){
			var selRow=ge("selectFriendsRow"+id+"sel");
			selRow.remove();
			row.show();
			this.selectedIDs.remove(id);
			if(!this.selectedIDs.length)
				this.selectedEmpty.show();
		}else{
			var selRow=row.cloneNode(true) as HTMLElement;
			row.hide();
			selRow.id+="sel";
			selRow.addEventListener("click", this.onRowClick.bind(this, id));
			this.selectedFriendsList.appendChild(selRow);
			this.selectedEmpty.hide();
			this.selectedIDs.push(id);
		}
	}
}

class MobileFriendListChoiceBox extends BaseScrollableBox{
	private allFriendsList:HTMLElement;
	private searchInput:HTMLInputElement;
	private idNameMap:{[key:string]:string}={};
	private selectedIDs:number[]=[];
	private searchFieldWrap:HTMLElement;
	private extraField:HTMLInputElement;

	public constructor(title:string, callback:{(selectedIDs:number[]):void}, currentValue:number[], includeLists:boolean=false){
		super(title, [lang("save"), lang("cancel")], (idx)=>{
			if(idx==0){
				if(!this.extraField || this.extraField.reportValidity())
					callback(this.selectedIDs);
			}else{
				this.dismiss();
			}
		});

		var root=ce("div", {className: "selectFriendsListBox"}, [
			this.searchFieldWrap=ce("div", {className: "searchFieldWrap singleColumn"}, [
				this.searchInput=ce("input", {type: "text", className: "searchField", autocomplete: "off", placeholder: lang("friends_search_placeholder")})
			]),
			this.wrapScrollableElement(this.allFriendsList=ce("div", {className: "selectFriendsList"}, []))
		]);
		this.setContent(root);
		this.searchInput.addEventListener("input", this.onSearchInputChanged.bind(this));

		if(includeLists){
			for(var _lid in cur.friendLists){
				var lid=parseInt(_lid);
				var row=this.makeListRow(lid);
				if(currentValue.indexOf(-lid)!=-1){
					this.selectedIDs.push(-lid);
					var cbox=row.qs("input") as HTMLInputElement;
					cbox.checked=true;
				}
				this.allFriendsList.appendChild(row);
			}
		}

		for(var friend of cur.friendListForPrivacy){
			var row=this.makeRow(friend);
			if(currentValue.indexOf(friend[0])!=-1){
				this.selectedIDs.push(parseInt(friend[0]));
				var cbox=row.qs("input") as HTMLInputElement;
				cbox.checked=true;
			}
			this.allFriendsList.appendChild(row);
			this.idNameMap[friend[0].toString()]=friend[1].toString();
		}
	}

	public setExtraField(placeholder:string, value:string, required:boolean){
		if(this.extraField)
			throw new Error("Already set");
		this.extraField=ce("input", {type: "text", className: "extraField marginAfter", placeholder: placeholder, value: value, required: required});
		this.searchFieldWrap.classList.add("hasExtraField");
		this.searchFieldWrap.insertAdjacentElement("afterbegin", this.extraField);
	}

	public setExtraContent(html:string){
		this.searchFieldWrap.parentElement.insertAdjacentHTML("beforebegin", html);
	}

	private makeRow(friend:any[]):HTMLElement{
		var cbox:HTMLInputElement;
		var row=ce("label", {className: "row compactUserRow", id: "selectFriendsRow"+friend[0]}, [
			cbox=ce("input", {type: "checkbox"}),
			makeAvatar(friend[4], "s", 32),
			ce("div", {className: "name ellipsize", innerText: friend[1]})
		]);
		row.dataset.uid=friend[0];
		var id=parseInt(friend[0]);
		cbox.addEventListener("change", (ev)=>{
			if(cbox.checked){
				if(this.selectedIDs.indexOf(id)==-1)
					this.selectedIDs.push(id);
			}else{
				this.selectedIDs.remove(id);
			}
		});
		return row;
	}

	private makeListRow(id:number):HTMLElement{
		var cbox:HTMLInputElement;
		var ava;
		var row=ce("label", {className: "row compactUserRow", id: "selectFriendsRowList"+id}, [
			cbox=ce("input", {type: "checkbox"}),
			ava=ce("span", {className: "ava avaListPlaceholder sizeA l"+((id-1)%8)}),
			ce("div", {className: "name ellipsize", innerText: cur.friendLists[id]})
		]);
		ava.style.setProperty("--ava-width", "32px");
		ava.style.setProperty("--ava-height", "32px");
		row.dataset.uid=(-id)+"";
		cbox.addEventListener("change", (ev)=>{
			if(cbox.checked){
				if(this.selectedIDs.indexOf(-id)==-1)
					this.selectedIDs.push(-id);
			}else{
				this.selectedIDs.remove(-id);
			}
		});
		return row;
	}

	protected onCreateContentView():HTMLElement{
		var cont=super.onCreateContentView();
		cont.classList.add("scrollable");
		return cont;
	}

	private onSearchInputChanged(ev:Event){
		var q=this.searchInput.value;
		if(q.trim()==""){
			for(var ch of this.allFriendsList.children.unfuck()){
				ch.show();
			}
			this.onContentScroll(null);
			return;
		}
		var re=new RegExp("\\b"+quoteRegExp(q), "i");
		for(var ch of this.allFriendsList.children.unfuck()){
			if(!ch.dataset.uid)
				continue;
			if(re.test(this.idNameMap[ch.dataset.uid]) && this.selectedIDs.indexOf(parseInt(ch.dataset.uid))==-1)
				ch.show();
			else
				ch.hide();
		}
		this.onContentScroll(null);
	}
}

function initMobilePrivacyForm(valueField:HTMLInputElement, updateField:boolean=true, onChange:{(s:PrivacySetting):void}=null){
	var allowedList=ge("allowedFriendsItems");
	var deniedList=ge("deniedFriendsItems");
	var allowedListW=ge("allowedFriends");
	var deniedListW=ge("deniedFriends");
	var currentValue=JSON.parse(valueField.value) as PrivacySetting;

	function makeUserRow(user:any[]):HTMLElement{
		var id=parseInt(user[0]);
		var row=ce("div", {className: "compactUserRow", id: "userRow"+id}, [
			makeAvatar(user[4], "s", 32),
			ce("div", {className: "name ellipsize", innerText: user[1]}),
			ce("a", {href: "javascript:void(0)", className: "remove actionIcon", title: lang("delete"), onclick: (ev)=>removeUser(id)})
		]);
		row.dataset.uid=user[0];
		return row;
	}

	function makeListRow(id:number):HTMLElement{
		var ava;
		var row=ce("div", {className: "compactUserRow", id: "listRow"+id}, [
			ava=ce("span", {className: "ava avaListPlaceholder sizeA l"+((id-1)%8)}),
			ce("div", {className: "name ellipsize", innerText: cur.friendLists[id]}),
			ce("a", {href: "javascript:void(0)", className: "remove actionIcon", title: lang("delete"), onclick: (ev)=>removeList(id)})
		]);
		ava.style.setProperty("--ava-width", "32px");
		ava.style.setProperty("--ava-height", "32px");
		row.dataset.lid=id+"";
		return row;
	}

	function setValue(value:PrivacySetting){
		currentValue=value;
		if(updateField)
			valueField.value=JSON.stringify(value);
		if(onChange)
			onChange(value);
		if(!cur.friendListForPrivacy)
			return;
		allowedList.innerHTML="";
		deniedList.innerHTML="";
		for(var _lid in cur.friendLists){
			var lid=parseInt(_lid);
			if(value.al.indexOf(lid)!=-1){
				allowedList.appendChild(makeListRow(lid));
			}
			if(value.xl.indexOf(lid)!=-1){
				deniedList.appendChild(makeListRow(lid));
			}
		}
		for(var friend of cur.friendListForPrivacy){
			if(value.au.indexOf(friend[0])!=-1){
				allowedList.appendChild(makeUserRow(friend));
			}
			if(value.xu.indexOf(friend[0])!=-1){
				deniedList.appendChild(makeUserRow(friend));
			}
		}
	}

	function removeUser(id:number){
		currentValue.au.remove(id);
		currentValue.xu.remove(id);
		setValue(currentValue);
		if(!cur.friendListForPrivacy){
			var el=ge("userRow"+id);
			if(el)
				el.remove();
		}
	}

	function removeList(id:number){
		currentValue.al.remove(id);
		currentValue.xl.remove(id);
		setValue(currentValue);
	}

	ge("options").addEventListener("change", (ev)=>{
		var target=ev.target as HTMLInputElement;
		var allowedVisible=(target.value=="certain_friends");
		var deniedVisible=(["only_me", "no_one"].indexOf(target.value)==-1);
		if(allowedVisible){
			allowedListW.show();
		}else{
			allowedListW.hide();
			allowedList.innerHTML="";
		}
		if(deniedVisible){
			deniedListW.show();
		}else{
			deniedListW.hide();
			deniedList.innerHTML="";
		}
		var rule:string;
		switch(target.value){
			case "everyone":
				rule="e";
				break;
			case "friends":
				rule="f";
				break;
			case "friends_of_friends":
				rule="ff";
				break;
			case "only_me":
			case "no_one":
			case "certain_friends":
				rule="n";
				break;
		}
		setValue({r: rule, au: allowedVisible ? currentValue.au : [], xu: deniedVisible ? currentValue.xu : [], al: allowedVisible ? currentValue.al : [], xl: deniedVisible ? currentValue.xl : []});
	});
	for(var btn of document.querySelectorAll(".compactUserRow .remove").unfuck()){
		if(btn.parentElement.dataset.uid){
			var id=parseInt(btn.parentElement.dataset.uid);
			btn.addEventListener("click", ((id:number, ev:Event)=>removeUser(id)).bind(this, id));
		}else{
			var id=parseInt(btn.parentElement.dataset.lid);
			btn.addEventListener("click", ((id:number, ev:Event)=>removeList(id)).bind(this, id));
		}
	}
	document.body.qs("#allowedFriends .selectFriends").addEventListener("click", (ev)=>loadFriendsForBoxes(()=>{
		var ids=[...currentValue.au];
		for(var lid of currentValue.al)
			ids.push(-lid);
		var box:MobileFriendListChoiceBox=new MobileFriendListChoiceBox(lang("select_friends_title"), ids=>{
			currentValue.au=[];
			currentValue.al=[];
			for(var id of ids){
				if(id>0)
					currentValue.au.push(id);
				else
					currentValue.al.push(-id);
			}
			setValue(currentValue);
			box.dismiss();
		}, ids, true);
		box.show();
	}));
	document.body.qs("#deniedFriends .selectFriends").addEventListener("click", (ev)=>loadFriendsForBoxes(()=>{
		var ids=[...currentValue.xu];
		for(var lid of currentValue.xl)
			ids.push(-lid);
		var box:MobileFriendListChoiceBox=new MobileFriendListChoiceBox(lang("select_friends_title"), ids=>{
			currentValue.xu=[];
			currentValue.xl=[];
			for(var id of ids){
				if(id>0)
					currentValue.xu.push(id);
				else
					currentValue.xl.push(-id);
			}
			setValue(currentValue);
			box.dismiss();
		}, ids, true);
		box.show();
	}));
}

function showMobilePrivacyBox(key:string, value:PrivacySetting, onlyMe:boolean){
	LayerManager.getInstance().showBoxLoader();
	var field:HTMLInputElement=ge(key+"_value");
	var uiValue:HTMLElement=ge(key+"_uiValue");
	ajaxGet("/settings/privacy/mobileBox?value="+encodeURIComponent(JSON.stringify(value))+"&onlyMe="+onlyMe, (r)=>{
		var box:Box;
		var setting=value;
		box=new Box(lang("privacy_settings_title"), [lang("save"), lang("cancel")], (i)=>{
			if(i==0){
				field.value=JSON.stringify(setting);
				var uiStr:string;
				switch(setting.r){
					case "e":
						uiStr=lang("privacy_value_everyone");
						break;
					case "f":
						uiStr=lang("privacy_value_friends");
						break;
					case "ff":
						uiStr=lang("privacy_value_friends_of_friends");
						break;
					case "n":
						if(setting.au.length || setting.al.length){
							uiStr=lang("privacy_value_certain_friends");
						}else{
							uiStr=lang(onlyMe ? "privacy_value_only_me" : "privacy_value_no_one");
						}
						break;
				}
				if(setting.au.length || setting.al.length){
					uiStr+=lang("privacy_settings_value_certain_friends_before");
					var names=[];
					for(var id of setting.al){
						names.push(cur.friendLists[id]);
					}
					for(var id of setting.au){
						names.push(getFriendForPrivacy(id)[1]);
					}
					uiStr+=names.join(lang("privacy_settings_value_name_separator"));
				}
				if(setting.xu.length || setting.xl.length){
					uiStr+=lang("privacy_settings_value_except");
					var names=[];
					for(var id of setting.xl){
						names.push(cur.friendLists[id]);
					}
					for(var id of setting.xu){
						names.push(getFriendForPrivacy(id)[3]);
					}
					uiStr+=names.join(lang("privacy_settings_value_name_separator"));
				}
				uiValue.innerText=uiStr;
			}
			box.dismiss();
		});
		box.setContent(ce("div", {innerHTML: r as string}));
		box.show();
		initMobilePrivacyForm(field, false, (ps)=>setting=ps);
	}, (msg)=>{
		new MessageBox(lang("error"), msg, lang("close")).show();
	}, "text");
}

function chooseMultipleFriends(title:string, currentSelection:(number|string)[], options:any, onSelected:{(ids:number[], box:Box):void}){
	loadFriendsForBoxes(()=>{
		var box:(FriendListChoiceBox|MobileFriendListChoiceBox);
		var selectionAsNumbers:number[]=[];
		for(var id of currentSelection)
			selectionAsNumbers.push(Number(id));
		if(mobile){
			box=new MobileFriendListChoiceBox(title, ids=>onSelected(ids, box), selectionAsNumbers);
		}else{
			box=new FriendListChoiceBox(title, ids=>onSelected(ids, box), selectionAsNumbers);
		}
		if(options.extraField){
			box.setExtraField(options.extraField.placeholder, options.extraField.value, options.extraField.required);
		}
		if(options.extraContent){
			box.setExtraContent(options.extraContent);
		}
		box.show();
	});
}

