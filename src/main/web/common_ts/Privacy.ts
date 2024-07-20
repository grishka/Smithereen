var friendListForPrivacy:any[];

interface PrivacySetting{
	r:string;
	au:number[];
	xu:number[];
}

function getFriendForPrivacy(id:number){
	for(var user of friendListForPrivacy){
		if(user[0]==id)
			return user;
	}
	return [id, "DELETED", "/id"+id, "DELETED", null];
}

function showPrivacyMenu(el:HTMLAnchorElement, key:string, onlyMe:boolean){
	if(!el.customData)
		el.customData={};
	var menuEl=el.customData.menuEl || (el.customData.menuEl=ce("div", {"className": "popupMenu compact"}, [
		ce("ul", {"className": "privacyMenu"})
	]));
	menuEl.hide();
	el.insertAdjacentElement("afterend", menuEl);
	var menuList=menuEl.querySelector("ul");
	if(!menuList.children.length){
		for(var opt of ["everyone", "friends", "friends_of_friends", onlyMe ? "only_me" : "no_one", "everyone_except", "certain_friends"]){
			var li;
			menuList.appendChild(li=ce("li", {}, [lang("privacy_value_"+opt)]));
			li.dataset.act=opt;
			if(opt=="certain_friends"){
				li.appendChild(ce("div", {className: "ddIcon iconPlus"}));
			}
		}
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
				if(!v.au || v.au.length==0){
					el.innerText=lang(onlyMe ? "privacy_value_only_me" : "privacy_value_no_one");
				}else{
					el.innerText=lang("privacy_value_certain_friends");
				}
				break;
		}
		var extHtml:string="";
		if(v.au.length){
			extHtml+=lang("privacy_settings_value_certain_friends_before");
			var links:string[]=[];
			for(var id of v.au){
				var user=getFriendForPrivacy(id);
				links.push(`<a href="${user[2]}">${user[1].escapeHTML()}</a>`);
			}
			extHtml+=links.join(lang("privacy_settings_value_name_separator"));
		}
		if(v.xu.length){
			extHtml+=lang("privacy_settings_value_except");
			var links:string[]=[];
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
		if(id=="certain_friends"){
			loadFriendsForBoxes(()=>{
				new PrivacyFriendChoiceBox(lang("select_friends_title"), setValue.bind(this), previousValue).show();
			});
		}else if(id=="everyone_except"){
			loadFriendsForBoxes(()=>{
				new ExtendedPrivacyBox(setValue.bind(this), previousValue).show();
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
			setValue({r: rule, au: [], xu: []});
		}
	}, false, true));
	var curSel=menuList.qs(".selected");
	if(curSel){
		curSel.classList.remove("selected");
	}
	var selectedItem:string;
	if(previousValue.xu.length){
		selectedItem="everyone_except";
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
	menu.show();
}

function loadFriendsForBoxes(onDone:{():void}){
	if(!friendListForPrivacy){
		LayerManager.getInstance().showBoxLoader();
		ajaxGet("/my/friends/ajaxFriendsForPrivacyBoxes", (r)=>{
			friendListForPrivacy=r;
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

	public constructor(callback:{(v:PrivacySetting):void}, currentValue:PrivacySetting){
		super(lang("privacy_settings_title"), [lang("save"), lang("cancel")], (idx)=>{
			if(idx==0){
				var allow:number[]=[];
				var deny:number[]=[];
				if(this.rule=="n"){
					for(var id of this.allowField.getTokenIDs()){
						allow.push(parseInt(id));
					}
				}
				for(var id of this.denyField.getTokenIDs()){
					deny.push(parseInt(id));
				}
				callback({r: this.rule, au: allow, xu: deny});
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
		}, false, true);
		this.allowedLink.onclick=(ev)=>{
			this.menu.show();
		};
		this.menu.addItems([
			{id: "e", title: lang("privacy_value_to_everyone")},
			{id: "f", title: lang("privacy_value_to_friends")},
			{id: "ff", title: lang("privacy_value_to_friends_of_friends")},
			{id: "n", title: lang("privacy_value_to_certain_friends")}
		]);

		for(var friend of friendListForPrivacy){
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
			for(var f of this.friends){
				res.push(f.token);
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

class PrivacyFriendChoiceBox extends BaseScrollableBox{
	private allFriendsList:HTMLElement;
	private selectedFriendsList:HTMLElement;
	private searchInput:HTMLInputElement;
	private idNameMap:{[key:string]:string}={};
	private selectedEmpty:HTMLElement;
	private selectedIDs:number[]=[];

	public constructor(title:string, callback:{(v:PrivacySetting):void}, currentValue:PrivacySetting){
		super(title, [lang("save"), lang("cancel")], (idx)=>{
			if(idx==0){
				callback({r: "n", au: this.selectedIDs, xu: []});
			}
			this.dismiss();
		});

		var root=ce("div", {}, [
			ce("div", {className: "gray borderBottom"}, [
				ce("div", {className: "searchFieldWrap singleColumn"}, [
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

		for(var friend of friendListForPrivacy){
			var row=this.makeRow(friend);
			if(currentValue.au.indexOf(friend[0])!=-1){
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

class MobilePrivacyFriendChoiceBox extends BaseScrollableBox{
	private allFriendsList:HTMLElement;
	private searchInput:HTMLInputElement;
	private idNameMap:{[key:string]:string}={};
	private selectedIDs:number[]=[];

	public constructor(title:string, isAllowedFriends:boolean, callback:{(v:PrivacySetting):void}, currentValue:PrivacySetting){
		super(title, [lang("save"), lang("cancel")], (idx)=>{
			if(idx==0){
				if(isAllowedFriends){
					callback({r: "n", au: this.selectedIDs, xu: []});
				}else{
					var v=currentValue;
					v.xu=this.selectedIDs;
					callback(v);
				}
			}
			this.dismiss();
		});

		var root=ce("div", {className: "selectFriendsListBox"}, [
			ce("div", {className: "searchFieldWrap singleColumn"}, [
				this.searchInput=ce("input", {type: "text", className: "searchField", autocomplete: "off", placeholder: lang("friends_search_placeholder")})
			]),
			this.wrapScrollableElement(this.allFriendsList=ce("div", {className: "selectFriendsList"}, []))
		]);
		this.setContent(root);
		this.searchInput.addEventListener("input", this.onSearchInputChanged.bind(this));

		var ids:number[]=isAllowedFriends ? currentValue.au : currentValue.xu;
		for(var friend of friendListForPrivacy){
			var row=this.makeRow(friend);
			if(ids.indexOf(friend[0])!=-1){
				this.selectedIDs.push(parseInt(friend[0]));
				var cbox=row.qs("input") as HTMLInputElement;
				cbox.checked=true;
			}
			this.allFriendsList.appendChild(row);
			this.idNameMap[friend[0].toString()]=friend[1].toString();
		}
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

	function setValue(value:PrivacySetting){
		currentValue=value;
		if(updateField)
			valueField.value=JSON.stringify(value);
		if(onChange)
			onChange(value);
		if(!friendListForPrivacy)
			return;
		allowedList.innerHTML="";
		deniedList.innerHTML="";
		for(var friend of friendListForPrivacy){
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
		if(!friendListForPrivacy){
			var el=ge("userRow"+id);
			if(el)
				el.remove();
		}
	}

	ge("options").addEventListener("change", (ev)=>{
		var target=ev.target as HTMLInputElement;
		var allowedVisible=(target.value=="certain_friends");
		var deniedVisible=(["only_me", "no_one", "certain_friends"].indexOf(target.value)==-1);
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
		setValue({r: rule, au: allowedVisible ? currentValue.au : [], xu: deniedVisible ? currentValue.xu : []});
	});
	for(var btn of document.querySelectorAll(".compactUserRow .remove").unfuck()){
		var id=parseInt(btn.parentElement.dataset.uid);
		btn.addEventListener("click", ((id:number, ev:Event)=>removeUser(id)).bind(this, id));
	}
	document.body.qs("#allowedFriends .selectFriends").addEventListener("click", (ev)=>loadFriendsForBoxes(()=>{
		new MobilePrivacyFriendChoiceBox(lang("select_friends_title"), true, setValue, currentValue).show();
	}));
	document.body.qs("#deniedFriends .selectFriends").addEventListener("click", (ev)=>loadFriendsForBoxes(()=>{
		new MobilePrivacyFriendChoiceBox(lang("select_friends_title"), false, setValue, currentValue).show();
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
						if(setting.au.length){
							uiStr=lang("privacy_value_certain_friends");
						}else{
							uiStr=lang(onlyMe ? "privacy_value_only_me" : "privacy_value_no_one");
						}
						break;
				}
				if(setting.au.length){
					uiStr+=lang("privacy_settings_value_certain_friends_before");
					var names=[];
					for(var id of setting.au){
						names.push(getFriendForPrivacy(id)[1]);
					}
					uiStr+=names.join(lang("privacy_settings_value_name_separator"));
				}
				if(setting.xu.length){
					uiStr+=lang("privacy_settings_value_except");
					var names=[];
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

