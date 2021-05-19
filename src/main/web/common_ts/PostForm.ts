///<reference path="./PopupMenu.ts"/>

class PostForm{

	private id:string;
	private root:HTMLElement;
	private input:HTMLTextAreaElement;
	private form:HTMLFormElement;
	private dragOverlay:HTMLElement;
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

	public constructor(el:HTMLElement){
		this.id=el.getAttribute("data-unique-id");
		this.root=el;
		this.input=ge("postFormText_"+this.id) as HTMLTextAreaElement;
		this.form=el.getElementsByTagName("form")[0];
		this.dragOverlay=el.querySelector(".dropOverlay");
		this.attachContainer=ge("postFormAttachments_"+this.id);
		this.attachContainer2=ge("postFormAttachments2_"+this.id);
		// this.fileField=ge("uploadField_"+this.id);
		this.fileField=ce("input", {type: "file"});
		this.fileField.accept="image/*";
		this.fileField.multiple=true;
		this.attachField=el.querySelector("input[name=attachments]") as HTMLInputElement;
		this.replyToField=ge("postFormReplyTo_"+this.id);

		this.form.addEventListener("submit", this.onFormSubmit.bind(this), false);
		this.input.addEventListener("keydown", this.onInputKeyDown.bind(this), false);
		this.input.addEventListener("paste", this.onInputPaste.bind(this), false);
		this.input.addEventListener("focus", this.onInputFocus.bind(this), false);
		this.input.addEventListener("blur", this.onInputBlur.bind(this), false);
		autoSizeTextArea(this.input);
		if(this.input.dataset.replyName){
			this.currentReplyName=this.input.dataset.replyName;
		}

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
		if(!this.isDirty()){
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
		ajaxSubmitForm(this.form, function(){
			this.attachmentIDs=[];
			this.attachField.value="";
			this.input.resizeToFitContent();
			this.hideCWLayout();
		}.bind(this));
	}

	public setupForReplyTo(id:number):void{
		this.replyToField.value=id+"";
		var name:string=document.getElementById("post"+id).dataset.replyName;
		if(name){
			if(this.input.value.length==0 || (this.input.value==this.currentReplyName)){
				this.input.value=name+", ";
			}
			this.currentReplyName=name+", ";
		}
		this.input.focus();
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

	private showCWLayout(){
		this.cwLayout=ce("div", {className: "postFormCW postFormNonThumb"}, [
			ce("a", {className: "attachDelete flR", onclick: this.hideCWLayout.bind(this), title: lang("delete")}),
			ce("h3", {innerText: lang("post_form_cw")}),
			ce("input", {type: "text", name: "contentWarning", placeholder: lang("post_form_cw_placeholder"), required: true, autocomplete: "off"})
		]);
		this.attachContainer2.appendChild(this.cwLayout);
	}

	private hideCWLayout(){
		if(!this.cwLayout)
			return;
		this.attachContainer2.removeChild(this.cwLayout);
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

	private setCollapsed(collapsed:boolean){
		this.collapsed=collapsed;
		if(collapsed)
			this.root.classList.add("collapsed");
		else
			this.root.classList.remove("collapsed");
		this.input.resizeToFitContent();
	}
}
