///<reference path="./PopupMenu.ts"/>

interface UploadingAttachment{
	el:HTMLElement;
	cancelBtn:HTMLAnchorElement;
	image:HTMLElement;
	pbar:ProgressBar;
	file:Blob;
	fileName:string;
	onDone:{():void};
	onError:{():void};
	extraParams:any;
}

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
	private submitButton:HTMLElement;
	private completionsContainer:HTMLElement;
	private altTextsField:HTMLInputElement;

	private pollLayout:HTMLElement;
	private pollQuestionField:HTMLInputElement;
	private pollOptionFields:HTMLInputElement[];
	private pollOptionsWrap:HTMLElement;
	private pollTimeSelect:HTMLSelectElement;

	private origReplyID:string;
	private editing:boolean;

	private uploadQueue:UploadingAttachment[]=[];
	private currentUploadingAttachment:UploadingAttachment;
	private showingAttachCountWarning:boolean=false;
	private isCollapsible:boolean=true;
	private photoUploadURL:string;
	private additionalRequiredFields:HTMLInputElement[]=[];
	private forceOverrideDirty:boolean=false;
	private allowedAttachmentTypes:string[]=null;
	public onSendDone:{(success:boolean):void};
	private allowEmpty=false;
	private mentionRegex=/@(\S+)$/gu;
	private lastSelectionEnd:number;
	private completionsDebounceTimeout:number;
	private completionsXHR:XMLHttpRequest;
	private completionList:CompletionList;
	private attachmentAltTexts:{[key:string]:string}={};

	public constructor(el:HTMLElement){
		this.id=el.dataset.uniqueId;
		this.editing=el.dataset.editing!=undefined;
		this.allowEmpty=el.dataset.allowEmpty!=undefined;
		this.root=el;
		this.input=ge("postFormText_"+this.id) as HTMLTextAreaElement;
		this.form=ge("wallPostFormForm_"+this.id);
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
		this.submitButton=ge("postFormSubmit_"+this.id);
		this.altTextsField=el.querySelector("input[name=attachAltTexts]") as HTMLInputElement;
		if(el.classList.contains("nonCollapsible"))
			this.isCollapsible=false;
		this.photoUploadURL=el.dataset.photoUploadUrl || "/system/upload/postPhoto";
		if(!this.form)
			return;

		if(this.form.dataset && this.form.dataset.requiredFields){
			for(var fid of this.form.dataset.requiredFields.split(",")){
				this.additionalRequiredFields.push(ge(fid) as HTMLInputElement);
			}
		}
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
				var delBtn=(attach.querySelector(".deleteBtn") as HTMLElement);
				delBtn.customData={aid: aid};
				delBtn.onclick=(ev:MouseEvent)=>{
					var el=ev.target as HTMLElement;
					ev.preventDefault();
					this.deleteAttachment(el.customData.aid);
				};
				if(!mobile && attach.dataset.pv){
					attach.addEventListener("click", (ev)=>{
						var info=JSON.parse(attach.dataset.pv) as PhotoViewerInlineData;
						this.openDesktopPhotoViewerForAltText(aid, info);
					});
					attach.style.cursor="pointer";
				}
			}
			this.attachmentAltTexts=JSON.parse(this.altTextsField.value);
		}

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

		this.cwLayout=this.root.qs(".postFormCW");
		if(this.cwLayout){
			this.cwLayout.qs(".attachDelete").onclick=this.hideCWLayout.bind(this);
		}

		this.pollLayout=this.root.qs(".postFormPoll");
		if(this.pollLayout){
			this.pollLayout.qs(".attachDelete").onclick=this.hidePollLayout.bind(this);
			this.pollQuestionField=this.pollLayout.qs("input[name=pollQuestion]");
			var optionWraps=this.pollLayout.querySelectorAll(".pollOptionW").unfuck();
			this.pollOptionsWrap=optionWraps[0].parentElement;
			for(var optWrap of optionWraps){
				this.pollOptionFields.push((optWrap as HTMLElement).qs("input"));
			}
			if(this.pollOptionFields.length<10){
				var addLayout=this.makeAddPollOptionLayout();
				this.pollOptionsWrap.appendChild(addLayout);
			}
			this.pollTimeSelect=this.pollLayout.qs("select[name=pollTimeLimitValue]");
			this.pollLayout.qs("input[name=pollTimeLimit]").onchange=this.pollTimeLimitOnChange.bind(this);
		}
		if(this.form.dataset.allowedAttachments){
			this.allowedAttachmentTypes=this.form.dataset.allowedAttachments.split(",");
		}

		if(!mobile){
			this.completionsContainer=el.qs(".completionsContainer");
			if(this.completionsContainer){
				this.input.addEventListener("input", (ev)=>this.updateCompletions());
				this.input.addEventListener("mouseup", (ev)=>this.updateCompletions());
				this.input.addEventListener("keyup", (ev)=>this.updateCompletions());
				this.completionList=new CompletionList(this.input, (el)=>{
					this.insertMention(el.dataset.username);
				});
				this.completionsContainer.appendChild(this.completionList.completionsWrap);
			}
		}
	}

	private onFormSubmit(ev:Event):void{
		ev.preventDefault();
		this.send(this.onSendDone);
	}

	private onInputKeyDown(ev:KeyboardEvent):void{
		if(ev.keyCode==13 && (isApple ? ev.metaKey : ev.ctrlKey)){
			this.send(this.onSendDone);
		}
		if(ev.keyCode==9 && this.completionList && this.completionList.selectedCompletion){ // tab
			ev.preventDefault();
			this.insertMention(this.completionList.selectedCompletion.dataset.username);
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
		LayerManager.getInstance().dismissByID("photoAttach");
		for(var i=0;i<files.length;i++){
			var f=files[i];
			if(f.type.indexOf("image/")==0){
				this.uploadFile(f, f.name);
			}
		}
	}

	public uploadFile(f:Blob, name:string, extraParams:any={}, onDone:{():void}=null, onError:{():void}=null):void{
		if(f.size>10*1024*1024){
			new MessageBox(lang("error"), lang("err_file_upload_too_large", {maxSize: langFileSize(10*1024*1024)}), lang("ok")).show();
			return;
		}
		if(!this.checkAttachmentCount()){
			return;
		}
		var objURL=URL.createObjectURL(f);

		var img:HTMLElement;
		var pbarEl:HTMLElement;
		var del:HTMLAnchorElement;
		var cont=ce("div", {className: "attachment uploading"}, [
			img=ce("img", {src: objURL}),
			ce("div", {className: "scrim"}),
			pbarEl=ce("div", {className: "progressBar small"}),
			ce("div", {className: "fileName", innerText: name}),
			del=ce("a", {className: "deleteBtn", ariaLabel: lang("remove_attachment")}),
		]);

		this.attachContainer.appendChild(cont);

		var pbar=new ProgressBar(pbarEl);
		var att:UploadingAttachment={el: cont, cancelBtn: del, image: img, pbar: pbar, file: f, fileName: name, onDone: onDone, onError: onError, extraParams: extraParams};
		del.onclick=()=>{
			this.uploadQueue.remove(att);
			cont.parentNode.removeChild(cont);
		};
		del.dataset.tooltip=lang("remove_attachment");
		if(this.currentUploadingAttachment){
			this.uploadQueue.push(att);
		}else{
			this.actuallyUploadFile(att);
		}
	}

	private actuallyUploadFile(f:UploadingAttachment){
		if(this.currentUploadingAttachment){
			throw new Error("actuallyUploadFile called during active upload");
		}
		this.currentUploadingAttachment=f;
		var formData=new FormData();
		formData.append("file", f.file, f.fileName);
		var xhr=new XMLHttpRequest();
		var url=this.photoUploadURL+"?_ajax=1&csrf="+userConfig.csrf;
		for(var key in f.extraParams){
			url+="&"+key+"="+encodeURIComponent(f.extraParams[key]);
		}
		xhr.open("POST", url);
		xhr.onload=()=>{
			if(Math.floor(xhr.status/100)!=2){
				new MessageBox(lang("error"), xhr.response || lang("network_error"), lang("ok")).show();
				f.el.parentNode.removeChild(f.el);
				this.maybeUploadNextAttachment();
				return;
			}
			f.el.classList.remove("uploading");
			var resp=JSON.parse(xhr.response);
			f.cancelBtn.href="/system/deleteDraftAttachment?id="+resp.id+"&csrf="+userConfig.csrf;
			f.image.outerHTML='<picture><source srcset="'+resp.thumbs.webp+'" type="image/webp"/><source srcset="'+resp.thumbs.jpeg+'" type="image/jpeg"/><img src="'+resp.thumbs.jpeg+'" width="'+resp.width+'" height="'+resp.height+'"/></picture>';
			f.cancelBtn.onclick=(ev:Event)=>{
				ev.preventDefault();
				ev.stopPropagation();
				this.deleteAttachment(resp.id);
			};
			if(!mobile){
				f.el.addEventListener("click", (ev)=>{
					var info=resp.pv as PhotoViewerInlineData;
					this.openDesktopPhotoViewerForAltText(resp.id, info);
				});
			}
			f.el.style.cursor="pointer";
			f.el.id="attachment_"+resp.id;
			this.attachmentIDs.push(resp.id);
			this.attachField.value=this.attachmentIDs.join(",");
			this.maybeUploadNextAttachment();
			if(f.onDone)
				f.onDone();
		};
		xhr.onerror=(ev:ProgressEvent)=>{
			console.log(ev);
			new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
			this.maybeUploadNextAttachment();
			if(f.onError)
				f.onError();
		};
		xhr.upload.onprogress=(ev:ProgressEvent)=>{
			f.pbar.setProgress(ev.loaded/ev.total);
		};
		xhr.send(formData);
		f.cancelBtn.onclick=()=>{
			xhr.abort();
			f.el.parentNode.removeChild(f.el);
			this.maybeUploadNextAttachment();
		};
	}

	private maybeUploadNextAttachment(){
		this.currentUploadingAttachment=null;
		if(this.uploadQueue.length){
			this.actuallyUploadFile(this.uploadQueue.shift());
		}
	}

	private deleteAttachment(id:string):void{
		var el=ge("attachment_"+id);
		el.parentNode.removeChild(el);
		this.attachmentIDs.remove(id);
		delete this.attachmentAltTexts[id];
		this.attachField.value=this.attachmentIDs.join(",");
		this.updateAltTextsField();
	}

	public send(onDone:{(success:boolean):void}=null):boolean{
		if(!this.allowEmpty && this.input.value.length==0 && this.attachmentIDs.length==0){
			if(this.pollLayout!=null){
				if(!this.pollQuestionField.reportValidity())
					return false;
				var optionCount=0;
				for(var opt of this.pollOptionFields){
					if(opt.value.length>0)
						optionCount++;
				}
				if(optionCount<2){
					for(var opt of this.pollOptionFields){
						if(opt.value.length==0){
							opt.focus();
							return false;
						}
					}
				}
			}else{
				return false;
			}
		}
		for(var fld of this.additionalRequiredFields){
			if(!fld.value.length)
				return false;
		}
		ajaxSubmitForm(this.form, (resp)=>{
			if(resp===false){
				if(onDone) onDone(false);
				return;
			}
			this.attachmentIDs=[];
			this.attachmentAltTexts={};
			this.attachField.value="";
			this.input.resizeToFitContent();
			this.hideCWLayout();
			this.hidePollLayout();
			if(this.isMobileComment){
				this.resetReply();
			}
			this.forceOverrideDirty=false;
			if(onDone) onDone(true);
		}, this.submitButton, {onResponseReceived: (resp:any)=>{
			this.forceOverrideDirty=true;
		}});
		return true;
	}

	private resetReply(){
		this.replyBar.hide();
		this.replyToField.value=this.origReplyID;
		if(this.input.value==this.currentReplyName){
			this.input.value="";
		}
	}

	public setupForReplyTo(id:(number|string), type:string="post", randomID:string=null):void{
		this.replyToField.value=id+"";
		var suffix=randomID ? "_"+randomID : "";
		var postEl=ge(type+id+suffix);
		var name:string=postEl.dataset.replyName;
		if(name){
			if(this.input.value.length==0 || (this.input.value==this.currentReplyName)){
				this.input.value=name+", ";
			}
			this.currentReplyName=name+", ";
		}
		this.input.focus();
		this.input.selectionEnd=this.input.selectionStart=this.input.value.length;
		if(this.replyBar){
			this.replyBar.show();
			this.replyName.innerText=postEl.dataset.replyingName;
		}
	}

	private onAttachMenuItemClick(id:string, args:any){
		if(id=="photo"){
			this.onAttachPhotoClick();
		}else if(id=="cw"){
			this.showCWLayout();
		}else if(id=="poll"){
			this.showPollLayout();
		}else if(id=="graffiti"){
			this.showGraffitiEditor(args);
		}
		return false;
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

	private onAttachPhotoClick(){
		LayerManager.getInstance().showBoxLoader();
		ajaxGetAndApplyActions("/photos/attachBox?id="+this.id);
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
				ce("h4", {innerText: lang("post_form_cw")}),
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
				ce("input", {type: "checkbox", name: "pollTimeLimit", onchange: this.pollTimeLimitOnChange.bind(this)}),
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

	private pollTimeLimitOnChange(ev:Event){
		if((ev.target as HTMLInputElement).checked){
			this.pollTimeSelect.show();
		}else{
			this.pollTimeSelect.hide();
		}
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

	private showGraffitiEditor(args:any){
		if(!this.checkAttachmentCount())
			return;
		// @ts-ignore
		if(window.GraffitiEditorBox!==undefined){
			// @ts-ignore
			new GraffitiEditorBox(args.title, this).show();
		}else{
			LayerManager.getInstance().showBoxLoader();
			var script=ce("script", {src: "/res/graffiti.js?"+args.jsHash, onload:()=>{
				// @ts-ignore
				new GraffitiEditorBox(args.title, this).show();
			}});
			document.body.appendChild(script);
		}
	}

	private canAddAttachment(type:string):boolean{
		return !this.allowedAttachmentTypes || this.allowedAttachmentTypes.indexOf(type)!=-1;
	}

	private showMobileAttachMenu(){
		var opts:any[]=[];
		if(this.canAddAttachment("photo")){
			opts.push({label: lang("attach_menu_photo_upload"), onclick: ()=>{
				this.fileField.click();
			}});
			opts.push({label: lang("attach_menu_photo_from_album"), onclick: ()=>{
				this.onAttachPhotoClick();
			}});
		}
		if(this.canAddAttachment("poll") && !this.pollLayout && !this.isMobileComment){
			opts.push({label: lang("attach_menu_poll"), onclick: this.showPollLayout.bind(this)});
		}
		if(this.canAddAttachment("cw") && !this.cwLayout){
			opts.push({label: lang("attach_menu_cw"), onclick: this.showCWLayout.bind(this)});
		}
		new MobileOptionsBox(opts).show();
	}

	public isDirty():boolean{
		if(this.forceOverrideDirty || this.editing)
			return false;
		var trimmedValue=this.input.value.trim();
		return (trimmedValue.length>0 && trimmedValue!=this.currentReplyName.trim()) || this.attachmentIDs.length>0 || this.cwLayout!=null || this.pollLayout!=null;
	}

	public focus(){
		this.input.focus();
	}

	private setCollapsed(collapsed:boolean){
		if(this.isMobileComment || !this.isCollapsible)
			return;
		this.collapsed=collapsed;
		if(collapsed)
			this.root.classList.add("collapsed");
		else
			this.root.classList.remove("collapsed");
		this.input.resizeToFitContent();
	}

	private checkAttachmentCount():boolean{
		var count=this.attachmentIDs.length+this.uploadQueue.length;
		if(this.currentUploadingAttachment)
			count++;
		if(this.pollLayout)
			count++;
		var maxCount=this.replyToField && this.replyToField.value ? 2 : 10;
		if(count<maxCount){
			return true;
		}
		if(!this.showingAttachCountWarning){
			this.showingAttachCountWarning=true;
			var box=new MessageBox(lang("error"), lang("max_attachment_count_exceeded", {count: maxCount}), lang("ok"));
			box.setOnDismissListener(()=>{
				this.showingAttachCountWarning=false;
			});
			box.show();
		}
		return false;
	}

	private updateCompletions(){
		if(this.input.selectionStart!=this.input.selectionEnd){
			this.resetCompletions();
			return;
		}
		if(this.input.selectionEnd==this.lastSelectionEnd)
			return;
		if(this.completionsDebounceTimeout){
			clearTimeout(this.completionsDebounceTimeout);
			this.completionsDebounceTimeout=0;
		}
		if(this.completionsXHR){
			this.completionsXHR.abort();
			this.completionsXHR=null;
		}
		this.lastSelectionEnd=this.input.selectionEnd;
		var part=this.input.value.substr(0, this.input.selectionEnd);
		this.mentionRegex.lastIndex=0;
		var match=this.mentionRegex.exec(part);
		if(match){
			var query=match[1];
			this.completionsDebounceTimeout=setTimeout(()=>{
				this.completionsDebounceTimeout=0;
				this.completionsXHR=ajaxGet("/system/mentionCompletions?q="+encodeURIComponent(query), (r)=>{
					this.completionsXHR=null;
					this.completionList.completionsList.innerHTML=r;
					this.completionList.updateCompletions();
				}, (err)=>{
					this.completionsXHR=null;
				}, "text");
			}, 300);
		}else{
			this.resetCompletions();
		}
	}

	private insertMention(username:string){
		var part=this.input.value.substr(0, this.input.selectionEnd);
		this.mentionRegex.lastIndex=0;
		var match=this.mentionRegex.exec(part);
		if(!match)
			return;
		var replacement="@"+username+" ";
		this.input.value=this.input.value.substr(0, match.index)+replacement+this.input.value.substr(this.input.selectionEnd);
		var newCursorPos=match.index+replacement.length;
		this.input.setSelectionRange(newCursorPos, newCursorPos);
		this.input.focus();
		this.resetCompletions();
	}

	private resetCompletions(){
		this.completionList.completionsList.innerHTML="";
		this.completionList.updateCompletions();
	}

	private doAttachAlbumPhoto(id:string, urlWebp:string, urlJpeg:string){
		var ev=event as MouseEvent;
		if(!(isApple ? ev.metaKey : ev.ctrlKey)){
			LayerManager.getInstance().dismissByID("photoAttach");
			LayerManager.getInstance().dismissByID("photoAttachAlbum");
		}
		if(!this.checkAttachmentCount())
			return;
		if(ge("attachment_photo:"+id))
			return;

		var img:HTMLElement;
		var del:HTMLAnchorElement;
		var cont=ce("div", {className: "attachment", id: "attachment_photo:"+id}, [
			ce("picture", {}, [
				ce("source", {srcset: urlWebp, type: "image/webp"}),
				ce("source", {srcset: urlJpeg, type: "image/jpeg"}),
				img=ce("img", {src: urlJpeg})
			]),
			del=ce("a", {className: "deleteBtn", ariaLabel: lang("remove_attachment")}),
		]);
		this.attachContainer.appendChild(cont);
		this.attachmentIDs.push("photo:"+id);
		this.attachField.value=this.attachmentIDs.join(",");
		del.dataset.tooltip=lang("remove_attachment");
		del.onclick=(ev:Event)=>{
			ev.preventDefault();
			this.deleteAttachment("photo:"+id);
		};
	}

	public static attachAlbumPhoto(formID:string, link:HTMLElement):boolean{
		ge("wallPostForm_"+formID).customData.postFormObj.doAttachAlbumPhoto(link.dataset.photoId, link.dataset.photoUrlWebp, link.dataset.photoUrlJpeg);
		return false;
	}

	public static initPhotoAttachBox(formID:string){
		var upload=ge("photoAttachUpload");
		if(!upload)
			return;

		var form=ge("wallPostForm_"+formID).customData.postFormObj as PostForm;
		var dropText=ge("attachUploadDropText");
		var dragCount=0;
		upload.addEventListener("click", (ev)=>{
			form.fileField.click();
		});
		upload.addEventListener("drop", (ev)=>{
			dragCount=0;
			ev.preventDefault();
			dropText.innerText=lang("drop_files_here");
			form.handleFiles(ev.dataTransfer.files);
		}, false);
		upload.addEventListener("dragenter", (ev)=>{
			if(dragCount==0){
				dropText.innerText=lang("release_files_to_upload");
			}
			dragCount++;
		});
		upload.addEventListener("dragleave", (ev)=>{
			dragCount--;
			if(dragCount==0){
				dropText.innerText=lang("drop_files_here");
			}
		});
	}

	private updateAltTextsField(){
		this.altTextsField.value=JSON.stringify(this.attachmentAltTexts);
	}

	private openDesktopPhotoViewerForAltText(id:string, info:PhotoViewerInlineData){
		var viewer=doOpenPhotoViewer(info);
		if(viewer instanceof DesktopPhotoViewer){
			viewer.bottomPartUpdateCallback=(el)=>{
				viewer.showLocalDescriptionEditor(this.attachmentAltTexts[id] || "", (description:string)=>{
					this.attachmentAltTexts[id]=description;
					viewer.dismiss();
					this.updateAltTextsField();
				});
			};
		}
	}
}
