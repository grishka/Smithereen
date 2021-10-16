///<reference path="./PopupMenu.ts"/>

class PostForm{

	private id:string;
	private root:HTMLElement;
	private input:HTMLTextAreaElement;
	private form:HTMLFormElement;
	private dragOverlay:HTMLElement;
	private attachWrap:HTMLElement;
	private attachContainer:HTMLElement;
	private attachContainer2:HTMLElement;
	private fileField:HTMLInputElement;
	private attachField:HTMLInputElement;
	private replyToField:HTMLInputElement;
	private attachmentIDs:string[]=[];
	private uploadingAttachmentCount:number=0;
	private currentReplyName:string="";
	private attachPopupMenu:PopupMenu;
	private cwLayout:HTMLElement;
	private collapsed:boolean;
	private mouseInside:boolean=false;
	private isMobileComment:boolean;
	private mobileCommentCWAttach:HTMLElement;
	private replyBar:HTMLElement;
	private replyName:HTMLElement;
	private replyCancel:HTMLElement;

	private pollLayout:HTMLElement;
	private pollQuestionField:HTMLInputElement;
	private pollOptionFields:HTMLInputElement[];
	private pollOptionsWrap:HTMLElement;
	private pollTimeSelect:HTMLSelectElement;

	private origReplyID:string;

	public constructor(el:HTMLElement){
		this.id=el.getAttribute("data-unique-id");
		this.root=el;
		this.input=ge("postFormText_"+this.id) as HTMLTextAreaElement;
		this.form=el.getElementsByTagName("form")[0];
		this.dragOverlay=el.querySelector(".dropOverlay");
		this.attachContainer=ge("postFormAttachments_"+this.id);
		this.attachContainer2=ge("postFormAttachments2_"+this.id);
		this.fileField=ce("input", {type: "file"});
		this.fileField.accept="image/*";
		this.fileField.multiple=true;
		this.attachField=el.querySelector("input[name=attachments]") as HTMLInputElement;
		this.replyToField=ge("postFormReplyTo_"+this.id);
		this.replyBar=ge("commentReplying_"+this.id);
		this.replyName=ge("replyingName_"+this.id);
		this.replyCancel=ge("cancelReply_"+this.id);
		if(!this.form)
			return;

		this.form.addEventListener("submit", this.onFormSubmit.bind(this), false);
		this.input.addEventListener("keydown", this.onInputKeyDown.bind(this), false);
		this.input.addEventListener("paste", this.onInputPaste.bind(this), false);
		this.input.addEventListener("focus", this.onInputFocus.bind(this), false);
		this.input.addEventListener("blur", this.onInputBlur.bind(this), false);
		this.root.addEventListener("mouseenter", ()=>{this.mouseInside=true;}, false);
		this.root.addEventListener("mouseleave", ()=>{this.mouseInside=false;}, false);
		autoSizeTextArea(this.input);
		if(this.input.dataset.replyName){
			this.currentReplyName=this.input.dataset.replyName;
		}

		this.isMobileComment=this.root.classList.contains("mobileCommentForm");

		if(this.dragOverlay){
			this.dragOverlay.addEventListener("dragenter", (ev:DragEvent)=>{
				this.dragOverlay.classList.add("over");
			}, false);
			this.dragOverlay.addEventListener("dragleave", (ev:DragEvent)=>{
				this.dragOverlay.classList.remove("over");
			}, false);
			this.root.addEventListener("drop", this.onDrop.bind(this), false);
		}

		this.fileField.addEventListener("change", (ev:Event)=>{
			this.handleFiles(this.fileField.files);
			this.fileField.value="";
		}, false);

		if(this.attachContainer.children.length){
			for(var i=0;i<this.attachContainer.children.length;i++){
				var attach:HTMLElement=this.attachContainer.children[i] as HTMLElement;
				var aid:string=attach.dataset.id;
				this.attachmentIDs.push(aid);
				(attach.querySelector(".deleteBtn") as HTMLElement).onclick=(ev:MouseEvent)=>{
					ev.preventDefault();
					this.deleteAttachment(aid);
				};
			}
		}

		window.addEventListener("beforeunload", (ev:BeforeUnloadEvent)=>{
			if(this.isDirty()){
				var msg:string=lang("confirm_discard_post_draft");
				(ev || window.event).returnValue=msg;
				return msg;
			}
		});

		if(mobile){
			ge("postFormAttachBtn_"+this.id).onclick=this.showMobileAttachMenu.bind(this);
		}else{
			this.attachPopupMenu=new PopupMenu(el.qs(".popupMenuW"), this.onAttachMenuItemClick.bind(this));
			this.attachPopupMenu.setPrepareCallback(this.onPrepareAttachMenu.bind(this));
		}
		if(!this.isDirty())
			this.setCollapsed(true);
		if(this.replyToField){
			this.origReplyID=this.replyToField.value;
		}
		if(this.replyBar){
			this.replyBar.onclick=()=>{
				this.resetReply();
			};
		}
		this.pollOptionFields=[];
	}

	private onFormSubmit(ev:Event):void{
		ev.preventDefault();
		this.send();
	}

	private onInputKeyDown(ev:KeyboardEvent):void{
		if(ev.keyCode==13 && (isApple ? ev.metaKey : ev.ctrlKey)){
			this.send();
		}
	}

	private onInputPaste(ev:ClipboardEvent):void{
		if(ev.clipboardData.files.length){
			ev.preventDefault();
			this.handleFiles(ev.clipboardData.files);
		}
	}

	private onInputFocus(ev:FocusEvent):void{
		if(this.collapsed)
			this.setCollapsed(false);
	}

	private onInputBlur(ev:FocusEvent):void{
		if(ev.target===document.activeElement)
			return;
		if(!this.isDirty() && !this.mouseInside){
			this.setCollapsed(true);
		}
	}

	private onDrop(ev:DragEvent):void{
		ev.preventDefault();
		this.dragOverlay.classList.remove("over");
		this.handleFiles(ev.dataTransfer.files);
		if(this.collapsed)
			this.setCollapsed(false);
	}

	private handleFiles(files:FileList):void{
		for(var i=0;i<files.length;i++){
			var f=files[i];
			if(f.type.indexOf("image/")==0){
				this.uploadFile(f);
			}
		}
	}

	private uploadFile(f:File):void{
		if(f.size>10*1024*1024){
			new MessageBox(lang("error"), lang("max_file_size_exceeded", {size: 10}), lang("ok")).show();
			return;
		}
		if(!this.checkAttachmentCount()){
			return;
		}
		var objURL=URL.createObjectURL(f);

		var img:HTMLElement;
		var pbarInner:HTMLElement;
		var del:HTMLAnchorElement;
		var cont=ce("div", {className: "attachment uploading"}, [
			img=ce("img", {src: objURL}),
			ce("div", {className: "scrim"}),
			ce("div", {className: "progressBarFrame"}, [
				pbarInner=ce("div", {className: "progressBar"})
			]),
			del=ce("a", {className: "deleteBtn", title: lang("delete")})
		]);

		pbarInner.style.transform="scaleX(0)";

		this.attachContainer.appendChild(cont);
		this.uploadingAttachmentCount++;

		var formData=new FormData();
		formData.append("file", f);
		var xhr=new XMLHttpRequest();
		xhr.open("POST", "/system/upload/postPhoto?_ajax=1&csrf="+userConfig.csrf);
		xhr.onload=()=>{
			cont.classList.remove("uploading");
			var resp=xhr.response;
			del.href="/system/deleteDraftAttachment?id="+resp.id+"&csrf="+userConfig.csrf;
			img.outerHTML='<picture><source srcset="'+resp.thumbs.webp+'" type="image/webp"/><source srcset="'+resp.thumbs.jpeg+'" type="image/jpeg"/><img src="'+resp.thumbs.jpeg+'"/></picture>';
			del.onclick=(ev:Event)=>{
				ev.preventDefault();
				this.deleteAttachment(resp.id);
			};
			cont.id="attachment_"+resp.id;
			this.attachmentIDs.push(resp.id);
			this.attachField.value=this.attachmentIDs.join(",");
			this.uploadingAttachmentCount--;
		};
		xhr.onerror=(ev:ProgressEvent)=>{
			console.log(ev);
		};
		xhr.upload.onprogress=(ev:ProgressEvent)=>{
			pbarInner.style.transform="scaleX("+(ev.loaded/ev.total)+")";
		};
		xhr.responseType="json";
		xhr.send(formData);
		del.onclick=()=>{
			xhr.abort();
			cont.parentNode.removeChild(cont);
			this.uploadingAttachmentCount--;
		};
	}

	private deleteAttachment(id:string):void{
		var el=ge("attachment_"+id);
		el.parentNode.removeChild(el);
		ajaxGet("/system/deleteDraftAttachment?id="+id+"&csrf="+userConfig.csrf, function(){}, function(){});
		this.attachmentIDs.remove(id);
		this.attachField.value=this.attachmentIDs.join(",");
	}

	private send():void{
		if(this.input.value.length==0 && this.attachmentIDs.length==0){
			if(this.pollLayout!=null){
				if(!this.pollQuestionField.reportValidity())
					return;
				var optionCount=0;
				for(var opt of this.pollOptionFields){
					if(opt.value.length>0)
						optionCount++;
				}
				if(optionCount<2){
					for(var opt of this.pollOptionFields){
						if(opt.value.length==0){
							opt.focus();
							return;
						}
					}
				}
			}else{
				return;
			}
		}
		ajaxSubmitForm(this.form, (resp)=>{
			if(resp===false)
				return;
			this.attachmentIDs=[];
			this.attachField.value="";
			this.input.resizeToFitContent();
			this.hideCWLayout();
			this.hidePollLayout();
			if(this.isMobileComment){
				this.resetReply();
			}
		});
	}

	private resetReply(){
		this.replyBar.hide();
		this.replyToField.value=this.origReplyID;
		if(this.input.value==this.currentReplyName){
			this.input.value="";
		}
	}

	public setupForReplyTo(id:number):void{
		this.replyToField.value=id+"";
		var postEl=ge("post"+id);
		var name:string=postEl.dataset.replyName;
		if(name){
			if(this.input.value.length==0 || (this.input.value==this.currentReplyName)){
				this.input.value=name+", ";
			}
			this.currentReplyName=name+", ";
		}
		this.input.focus();
		this.input.selectionEnd=this.input.selectionStart=this.input.value.length;
		if(this.isMobileComment){
			this.replyBar.show();
			this.replyName.innerText=postEl.dataset.replyingName;
		}
	}

	private onAttachMenuItemClick(id:string){
		if(id=="photo"){
			this.fileField.click();
		}else if(id=="cw"){
			this.showCWLayout();
		}else if(id=="poll"){
			this.showPollLayout();
		}
	}

	private onPrepareAttachMenu(){
		this.attachPopupMenu.setItemVisibility("cw", this.cwLayout==null);
		this.attachPopupMenu.setItemVisibility("poll", this.pollLayout==null);
	}

	private makeNonThumbLayout(title:string, content:string, onRemoveClick:{():void}):HTMLElement{
		var layout=ce("div", {className: "attachment nonThumbAttachment"}, [
			ce("div", {className: "attTitle", innerText: title}),
			ce("div", {className: "attContent ellipsize", innerText: content}),
			ce("a", {className: "deleteBtn", title: lang("delete"), onclick: ()=>{
				onRemoveClick();
			}})
		]);
		return layout;
	}

	private showCWLayout(){
		if(this.isMobileComment){
			var input=ce("input", {type: "text", name: "contentWarning", placeholder: lang("post_form_cw_placeholder"), required: true, autocomplete: "off"});
			input.style.width="100%";
			var box=new Box(lang("post_form_cw"), [lang("ok"), lang("cancel")], (btn)=>{
				if(btn==1){
					box.dismiss();
					return;
				}
				if(!input.reportValidity())
					return;
				this.attachContainer2.appendChild(this.mobileCommentCWAttach=this.makeNonThumbLayout(lang("post_form_cw"), input.value, ()=>{
					this.hideCWLayout();
				}));
				this.cwLayout=ce("input", {type: "hidden", name: "contentWarning", value: input.value});
				this.form.appendChild(this.cwLayout);
				box.dismiss();
			});
			box.setContent(input);
			box.show();
			input.focus();
		}else{
			this.cwLayout=ce("div", {className: "postFormCW postFormNonThumb"}, [
				ce("a", {className: "attachDelete flR", onclick: this.hideCWLayout.bind(this), title: lang("remove_attachment")}),
				ce("h3", {innerText: lang("post_form_cw")}),
				ce("input", {type: "text", name: "contentWarning", placeholder: lang("post_form_cw_placeholder"), required: true, autocomplete: "off"})
			]);
			this.attachContainer2.appendChild(this.cwLayout);
		}
	}

	private hideCWLayout(){
		if(!this.cwLayout)
			return;
		if(this.isMobileComment){
			this.attachContainer2.removeChild(this.mobileCommentCWAttach);
			this.form.removeChild(this.cwLayout);
			this.mobileCommentCWAttach=null;
		}else{
			this.attachContainer2.removeChild(this.cwLayout);
		}
		this.cwLayout=null;
	}

	private showPollLayout(){
		if(!this.checkAttachmentCount())
			return;
		var optionField1, optionField2, optionFieldAdd;
		var optionDel1, optionDel2, optionDelAdd;
		this.pollLayout=ce("div", {className: "postFormPoll postFormNonThumb"}, [
			ce("a", {className: "attachDelete flR", onclick: this.hidePollLayout.bind(this), title: lang("remove_attachment")}),
			ce("h4", {innerText: lang("create_poll_question")}),
			this.pollQuestionField=ce("input", {type: "text", required: true, name: "pollQuestion", autocomplete: "off"}),
			ce("h4", {innerText: lang("create_poll_options")}),
			this.pollOptionsWrap=ce("div", {}, [
				ce("div", {className: "pollOptionW"}, [
					optionField1=ce("input", {type: "text", name: "pollOption", autocomplete: "off"}),
					optionDel1=ce("a", {className: "attachDelete", title: lang("create_poll_delete_option"), onclick: this.deletePollOption.bind(this)})
				]),
				ce("div", {className: "pollOptionW"}, [
					optionField2=ce("input", {type: "text", name: "pollOption", autocomplete: "off"}),
					optionDel2=ce("a", {className: "attachDelete", title: lang("create_poll_delete_option"), onclick: this.deletePollOption.bind(this)})
				]),
				this.makeAddPollOptionLayout()
			]),
			ce("label", {className: "radioButtonWrap pollSetting"}, [
				ce("input", {type: "checkbox", name: "pollAnonymous"}),
				lang("create_poll_anonymous")
			]),
			ce("label", {className: "radioButtonWrap pollSetting"}, [
				ce("input", {type: "checkbox", name: "pollMultiChoice"}),
				lang("create_poll_multi_choice")
			]),
			ce("label", {className: "radioButtonWrap pollSetting"}, [
				ce("input", {type: "checkbox", name: "pollTimeLimit", onchange: (ev)=>{
					if((ev.target as HTMLInputElement).checked){
						this.pollTimeSelect.show();
					}else{
						this.pollTimeSelect.hide();
					}
				}}),
				lang("create_poll_time_limit")
			]),
			this.pollTimeSelect=ce("select", {name: "pollTimeLimitValue"}, [
				ce("option", {value: "3600", innerText: lang("X_hours", {count: 1})}),
				ce("option", {value: "43200", innerText: lang("X_hours", {count: 12})}),
				ce("option", {value: "86400", innerText: lang("X_days", {count: 1}), selected: true}),
				ce("option", {value: "259200", innerText: lang("X_days", {count: 3})}),
				ce("option", {value: "604800", innerText: lang("X_days", {count: 7})}),
				ce("option", {value: "1209600", innerText: lang("X_days", {count: 14})}),
				ce("option", {value: "2592000", innerText: lang("X_days", {count: 30})}),
			]),
		]);
		this.pollTimeSelect.hide();
		this.attachContainer2.appendChild(this.pollLayout);

		this.pollOptionFields.push(optionField1, optionField2);
	}

	private deletePollOption(ev:MouseEvent){
		if(this.pollOptionFields.length<=2)
			return;
		var el=(ev.target as HTMLElement).parentElement;
		var input=el.qs("input") as HTMLInputElement;
		if(this.pollOptionFields.length==10)
			this.pollOptionsWrap.appendChild(this.makeAddPollOptionLayout());
		this.pollOptionFields.remove(input);
		this.pollOptionsWrap.removeChild(el);
	}

	private onAddPollOptionInput(ev:Event){
		this.addPollOption(ev.target as HTMLInputElement);
	}

	private onAddPollOptionClick(ev:MouseEvent){
		this.addPollOption(ev.target as HTMLInputElement);
	}

	private makeAddPollOptionLayout():HTMLElement{
		var input, del;
		var el=ce("div", {className: "pollOptionW addOption"}, [
			input=ce("input", {type: "text", placeholder: lang("create_poll_add_option"), autocomplete: "off"}),
			del=ce("a", {className: "attachDelete", title: lang("create_poll_delete_option"), onclick: this.deletePollOption.bind(this)})
		]);
		del.hide();
		var inputListener, clickListener;
		input.addEventListener("input", inputListener=this.onAddPollOptionInput.bind(this), false);
		input.addEventListener("click", clickListener=this.onAddPollOptionClick.bind(this), false);
		input.customData={'tmpListeners': {input: inputListener, click: clickListener}};
		return el;
	}

	private addPollOption(el:HTMLInputElement){
		el.removeEventListener("input", el.customData['tmpListeners'].input);
		el.removeEventListener("click", el.customData['tmpListeners'].click);
		delete el.customData['tmpListeners'];
		var wrap=el.parentElement;
		wrap.qs(".attachDelete").show();
		wrap.classList.remove("addOption");
		el.placeholder="";
		el.name="pollOption";
		this.pollOptionFields.push(el);
		if(this.pollOptionFields.length<10)
			this.pollOptionsWrap.appendChild(this.makeAddPollOptionLayout());
	}

	private hidePollLayout(){
		if(!this.pollLayout)
			return;
		this.attachContainer2.removeChild(this.pollLayout);
		this.pollLayout=null;
		this.pollOptionFields=[];
		this.pollQuestionField=null;
		this.pollOptionsWrap=null;
		this.pollTimeSelect=null;
	}

	private showMobileAttachMenu(){
		var opts:any[]=[];
		opts.push({label: lang("attach_menu_photo"), onclick: ()=>{
			this.fileField.click();
		}});
		if(!this.pollLayout){
			opts.push({label: lang("attach_menu_poll"), onclick: this.showPollLayout.bind(this)});
		}
		if(!this.cwLayout){
			opts.push({label: lang("attach_menu_cw"), onclick: this.showCWLayout.bind(this)});
		}
		new MobileOptionsBox(opts).show();
	}

	public isDirty():boolean{
		return this.input.value.length>0 || this.attachmentIDs.length>0 || this.cwLayout!=null || this.pollLayout!=null;
	}

	public focus(){
		this.input.focus();
	}

	private setCollapsed(collapsed:boolean){
		if(this.isMobileComment)
			return;
		this.collapsed=collapsed;
		if(collapsed)
			this.root.classList.add("collapsed");
		else
			this.root.classList.remove("collapsed");
		this.input.resizeToFitContent();
	}

	private checkAttachmentCount():boolean{
		var count=this.attachmentIDs.length+this.uploadingAttachmentCount;
		if(this.pollLayout)
			count++;
		var maxCount=this.replyToField ? 2 : 10;
		if(count<maxCount){
			return true;
		}
		new MessageBox(lang("error"), lang("max_attachment_count_exceeded", {count: maxCount}), lang("ok")).show();
		return false;
	}
}
