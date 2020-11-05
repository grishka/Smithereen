class PostForm{

	private id:string;
	private root:HTMLElement;
	private input:HTMLTextAreaElement;
	private form:HTMLFormElement;
	private dragOverlay:HTMLElement;
	private attachContainer:HTMLElement;
	private fileField:HTMLInputElement;
	private attachField:HTMLInputElement;
	private replyToField:HTMLInputElement;
	private attachmentIDs:string[]=[];
	private currentReplyName:string="";

	public constructor(el:HTMLElement){
		this.id=el.getAttribute("data-unique-id");
		this.root=el;
		this.input=ge("postFormText_"+this.id) as HTMLTextAreaElement;
		this.form=el.getElementsByTagName("form")[0];
		this.dragOverlay=el.querySelector(".dropOverlay");
		this.attachContainer=ge("postFormAttachments_"+this.id);
		this.fileField=ge("uploadField_"+this.id);
		this.attachField=el.querySelector("input[name=attachments]") as HTMLInputElement;
		this.replyToField=ge("postFormReplyTo_"+this.id);

		this.form.addEventListener("submit", this.onFormSubmit.bind(this), false);
		this.input.addEventListener("keydown", this.onInputKeyDown.bind(this), false);
		this.input.addEventListener("paste", this.onInputPaste.bind(this), false);
		if(this.input.hasAttribute("data-reply-name")){
			this.currentReplyName=this.input.getAttribute("data-reply-name");
		}

		if(this.dragOverlay){
			this.dragOverlay.addEventListener("dragenter", function(ev:DragEvent){
				this.dragOverlay.classList.add("over");
			}.bind(this), false);
			this.dragOverlay.addEventListener("dragleave", function(ev:DragEvent){
				this.dragOverlay.classList.remove("over");
			}.bind(this), false);
			this.root.addEventListener("drop", this.onDrop.bind(this), false);
		}

		this.fileField.addEventListener("change", function(ev:Event){
			this.handleFiles(this.fileField.files);
			this.fileField.form.reset();
		}.bind(this), false);

		if(this.attachContainer.children.length){
			for(var i=0;i<this.attachContainer.children.length;i++){
				var attach:HTMLElement=this.attachContainer.children[i] as HTMLElement;
				var aid:string=attach.getAttribute("data-id");
				this.attachmentIDs.push(aid);
				(attach.querySelector(".deleteBtn") as HTMLElement).onclick=function(ev:Event){
					ev.preventDefault();
					this.deleteAttachment(aid);
				}.bind(this);
			}
		}

		window.addEventListener("beforeunload", function(ev:BeforeUnloadEvent){
			if((this.input.value.length>0 && this.input.value!=this.currentReplyName) || this.attachmentIDs.length>0){
				var msg:string=lang("confirm_discard_post_draft");
				(ev || window.event).returnValue=msg;
				return msg;
			}
		}.bind(this));
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

	private onDrop(ev:DragEvent):void{
		ev.preventDefault();
		this.dragOverlay.classList.remove("over");
		this.handleFiles(ev.dataTransfer.files);
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

		var cont=ce("div");
		cont.className="attachment uploading";
		var img=ce("img");
		img.src=objURL;
		cont.appendChild(img);
		var scrim=ce("div");
		scrim.className="scrim";
		cont.appendChild(scrim);
		var pbar=ce("div");
		pbar.className="progressBarFrame";
		var pbarInner=ce("div");
		pbarInner.className="progressBar";
		pbar.appendChild(pbarInner);
		cont.appendChild(pbar);
		var del=ce("a");
		del.className="deleteBtn";
		del.title=lang("delete");
		del.href="javascript:void(0)";
		cont.appendChild(del);

		pbarInner.style.transform="scaleX(0)";

		this.attachContainer.appendChild(cont);

		var formData=new FormData();
		formData.append("file", f);
		var xhr=new XMLHttpRequest();
		xhr.open("POST", "/system/upload/postPhoto?_ajax=1&csrf="+userConfig.csrf);
		xhr.onload=function(){
			cont.classList.remove("uploading");
			var resp=xhr.response;
			del.href="/system/deleteDraftAttachment?id="+resp.id;
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
		ajaxGet("/system/deleteDraftAttachment?id="+id, function(){}, function(){});
		this.attachmentIDs.remove(id);
		this.attachField.value=this.attachmentIDs.join(",");
	}

	private send():void{
		if(this.input.value.length==0 && this.attachmentIDs.length==0)
			return;
		ajaxSubmitForm(this.form, function(){
			this.attachmentIDs=[];
			this.attachField.value="";
		}.bind(this));
	}

	public setupForReplyTo(id:number):void{
		this.replyToField.value=id+"";
		var name:string=document.getElementById("post"+id).getAttribute("data-reply-name");
		if(name){
			if(this.input.value.length==0 || (this.input.value==this.currentReplyName)){
				this.input.value=name+", ";
			}
			this.currentReplyName=name+", ";
		}
		this.input.focus();
	}
}
