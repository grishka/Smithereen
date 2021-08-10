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
			if((this.input.value.length>0 && this.input.value!=this.currentReplyName) || this.attachmentIDs.length>0){
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
			new MessageBox(lang("error"), lang(["max_file_size_exceeded", 10]), lang("ok")).show();
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

		var formData=new FormData();
		formData.append("file", f);
		var xhr=new XMLHttpRequest();
		xhr.open("POST", "/system/upload/postPhoto?_ajax=1&csrf="+userConfig.csrf);
		xhr.onload=function(){
			cont.classList.remove("uploading");
			var resp=xhr.response;
			del.href="/system/deleteDraftAttachment?id="+resp.id+"&csrf="+userConfig.csrf;
			img.outerHTML='<picture><source srcset="'+resp.thumbs.webp+'" type="image/webp"/><source srcset="'+resp.thumbs.jpeg+'" type="image/jpeg"/><img src="'+resp.thumbs.jpeg+'"/></picture>';
			del.onclick=function(ev:Event){
				ev.preventDefault();
				this.deleteAttachment(resp.id);
			}.bind(this);
			cont.id="attachment_"+resp.id;
			this.attachmentIDs.push(resp.id);
			this.attachField.value=this.attachmentIDs.join(",");
		}.bind(this);
		xhr.onerror=function(ev:ProgressEvent){
			console.log(ev);
		};
		xhr.upload.onprogress=function(ev:ProgressEvent){
			pbarInner.style.transform="scaleX("+(ev.loaded/ev.total)+")";
		};
		xhr.responseType="json";
		xhr.send(formData);
		del.onclick=function(){
			xhr.abort();
			cont.parentNode.removeChild(cont);
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
		if(this.input.value.length==0 && this.attachmentIDs.length==0)
			return;
		ajaxSubmitForm(this.form, ()=>{
			this.attachmentIDs=[];
			this.attachField.value="";
			this.input.resizeToFitContent();
			this.hideCWLayout();
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
		}
	}

	private onPrepareAttachMenu(){
		this.attachPopupMenu.setItemVisibility("cw", this.cwLayout==null);
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
				ce("a", {className: "attachDelete flR", onclick: this.hideCWLayout.bind(this), title: lang("delete")}),
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

	private showMobileAttachMenu(){
		var opts:any[]=[];
		opts.push({label: lang("attach_menu_photo"), onclick: ()=>{
			this.fileField.click();
		}});
		if(!this.cwLayout){
			opts.push({label: lang("attach_menu_cw"), onclick: this.showCWLayout.bind(this)});
		}
		new MobileOptionsBox(opts).show();
	}

	public isDirty():boolean{
		return this.input.value.length>0 || this.attachmentIDs.length>0 || this.cwLayout!=null;
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
}
