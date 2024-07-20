class PhotoAlbumUploader{
	private uploadURL:string;
	private uploadEl:HTMLElement;
	private dropText:HTMLElement;
	private input:HTMLInputElement;
	private resultEl:HTMLElement;
	private progressEl:HTMLElement;
	private progressBar:ProgressBar;
	private progressText:HTMLElement;

	private initialQueueSize=0;
	private uploadQueue:File[]=[];
	private uploading:boolean;
	private uploadedFileCount=0;

	public constructor(uploadURL:string){
		this.uploadURL=uploadURL;
		this.uploadEl=ge("photoAlbumUpload");
		this.dropText=ge("uploadDropText");
		this.input=ce("input", {type: "file", accept: "image/*", multiple: true});
		this.input.hide();
		this.uploadEl.appendChild(this.input);
		this.resultEl=ge("uploadResult");

		this.uploadEl.addEventListener("click", (ev)=>this.input.click(), false);
		var dragCount=0;
		this.uploadEl.addEventListener("drop", (ev)=>{
			dragCount=0;
			ev.preventDefault();
			this.dropText.innerText=lang("drop_files_here");
			this.handleFiles(ev.dataTransfer.files);
		}, false);
		this.uploadEl.addEventListener("dragenter", (ev)=>{
			if(dragCount==0){
				this.dropText.innerText=lang("release_files_to_upload");
			}
			dragCount++;
		});
		this.uploadEl.addEventListener("dragleave", (ev)=>{
			dragCount--;
			if(dragCount==0){
				this.dropText.innerText=lang("drop_files_here");
			}
		});
		this.input.addEventListener("change", (ev)=>{
			this.handleFiles(this.input.files);
		}, false);

		var progressBarEl;
		this.progressEl=ce("div", {className: "photoUploadProgressW"}, [
			this.progressText=ce("div"),
			progressBarEl=ce("div", {className: "progressBar"})
		]);
		this.progressBar=new ProgressBar(progressBarEl);
		this.resultEl.appendChild(this.progressEl);
		this.progressEl.hide();
	}

	private handleFiles(files:FileList){
		var validFileCount=0;
		for(var i=0;i<files.length;i++){
			if(files[i].type.indexOf("image/")==0){
				this.uploadQueue.push(files[i]);
				validFileCount++;
			}
		}
		if(validFileCount==0)
			return;
		ge("uploadHide").hide();
		this.progressEl.show();
		this.initialQueueSize+=validFileCount;
		this.progressText.innerText=lang("uploading_photo_X_of_Y", {current: this.initialQueueSize-this.uploadQueue.length+1, total: this.initialQueueSize});
		ge("uploadText").innerText=lang("add_more_photos");
		this.maybeUploadNextFile();
	}

	private uploadNextFile(){
		if(this.uploading){
			throw new Error("Already uploading");
		}
		this.uploading=true;
		var file=this.uploadQueue.shift();
		var xhr=new XMLHttpRequest();
		var url=this.uploadURL+"?_ajax=1&csrf="+userConfig.csrf;
		var formData=new FormData();
		formData.append("file", file, file.name);
		xhr.open("POST", url);
		xhr.onload=(ev)=>{
			if(Math.floor(xhr.status/100)==2){
				var resp:any=JSON.parse(xhr.response);
				var textarea, descriptionHeading;
				var fileEl=ce("div", {className: "photoEditRow"}, [
					ce("div", {className: "thumb", innerHTML: resp.html}),
					ce("div", {className: "descriptionW"}, [
						descriptionHeading=ce("h4", {innerText: lang("photo_description"), className: "marginAfter"}),
						textarea=ce("textarea")
					])
				]);
				this.resultEl.appendChild(fileEl);
				autoSizeTextArea(textarea);
				PhotoAlbumUploader.setupPhotoEditRow(textarea, descriptionHeading, resp.id);
			}else{
				var fileEl=ce("div", {className: "photoUploadError"}, [
					ce("h4", {innerText: file.name}),
					ce("div", {className: "marginBefore", innerText: xhr.responseText})
				]);
				this.resultEl.appendChild(fileEl);
			}
			this.uploading=false;
			this.uploadedFileCount++;
			this.maybeUploadNextFile();
		};
		xhr.onerror=(ev)=>{
			var fileEl=ce("div", {className: "photoUploadError"}, [
				ce("h4", {innerText: file.name}),
				ce("div", {className: "marginBefore", innerText: lang("err_network")})
			]);
			this.resultEl.appendChild(fileEl);
			this.uploading=false;
			this.uploadedFileCount++;
			this.maybeUploadNextFile();
		};
		xhr.upload.onprogress=(ev)=>{
			var fileProgress=ev.loaded/ev.total;
			this.progressBar.setProgress(this.uploadedFileCount/this.initialQueueSize+fileProgress/this.initialQueueSize);
		};
		xhr.send(formData);
		this.progressBar.setProgress(this.uploadedFileCount/this.initialQueueSize);
		this.progressText.innerText=lang("uploading_photo_X_of_Y", {current: this.initialQueueSize-this.uploadQueue.length, total: this.initialQueueSize});
	}

	private maybeUploadNextFile(){
		if(this.uploading)
			return;
		if(this.uploadQueue.length){
			this.uploadNextFile();
		}else{
			this.initialQueueSize=0;
			this.uploadedFileCount=0;
			this.progressEl.hide();
		}
	}

	public static setupPhotoEditRow(textarea:HTMLTextAreaElement, heading:HTMLElement, photoID:string, textFormat:string=null){
		var debounceTimeout:number;
		var wasSaved=false;
		function save(){
			if(debounceTimeout){
				clearTimeout(debounceTimeout);
			}
			if(wasSaved){
				return;
			}
			var params:any={description: textarea.value};
			if(textFormat)
				params.format=textFormat;
			ajaxPost(`/photos/${photoID}/updateDescription?csrf=${userConfig.csrf}`, params, (r:string)=>{
				heading.innerText=lang("photo_description_saved");
				wasSaved=true;
			}, (msg:string)=>{
				new MessageBox(lang("error"), msg, lang("close")).show();
			}, "text");
		}

		textarea.addEventListener("blur", (ev)=>{
			save();
		});
		textarea.addEventListener("input", (ev)=>{
			if(wasSaved){
				wasSaved=false;
				heading.innerText=lang("photo_description");
			}
			if(debounceTimeout){
				clearTimeout(debounceTimeout);
			}
			debounceTimeout=setTimeout(()=>{
				debounceTimeout=null;
				save();
			}, 700);
		});
	}

	public static setupServerRenderedRows(){
		for(var _row of document.querySelectorAll(".photoEditRow").unfuck()){
			var row=_row as HTMLElement;
			PhotoAlbumUploader.setupPhotoEditRow(row.qs("textarea"), row.qs("h4"), row.dataset.photoId, row.dataset.textFormat);
		}
	}
}

function initMobileAlbumUploader(uploadURL:string){
	var uploadQueue:File[]=[];
	var box:Box;
	var progressBar:ProgressBar;
	var progressText:HTMLElement;
	var resultEl:HTMLElement;
	var progressBarEl:HTMLElement;
	var uploadedFileCount=0, initialQueueSize=0, succeededFileCount=0;

	function uploadNextFile(){
		if(!uploadQueue.length){
			progressBarEl.hide();
			progressText.innerText=lang("you_uploaded_X_photos", {count: succeededFileCount});
			box.setCloseable(true);
			box.setOnDismissListener(()=>location.reload());
			box.setButtons([lang("close")], (i)=>box.dismiss());
			return;
		}
		var file=uploadQueue.shift();
		var xhr=new XMLHttpRequest();
		var url=uploadURL+"?_ajax=1&csrf="+userConfig.csrf;
		var formData=new FormData();
		formData.append("file", file, file.name);
		xhr.open("POST", url);
		xhr.onload=(ev)=>{
			if(Math.floor(xhr.status/100)!=2){
				var fileEl=ce("div", {className: "photoUploadError"}, [
					ce("h4", {innerText: file.name}),
					ce("div", {className: "marginBefore", innerText: xhr.responseText})
				]);
				resultEl.appendChild(fileEl);
			}else{
				succeededFileCount++;
			}
			uploadedFileCount++;
			uploadNextFile();
		};
		xhr.onerror=(ev)=>{
			var fileEl=ce("div", {className: "photoUploadError"}, [
				ce("h4", {innerText: file.name}),
				ce("div", {className: "marginBefore", innerText: lang("err_network")})
			]);
			resultEl.appendChild(fileEl);
			uploadedFileCount++;
			uploadNextFile();
		};
		xhr.upload.onprogress=(ev)=>{
			var fileProgress=ev.loaded/ev.total;
			progressBar.setProgress(uploadedFileCount/initialQueueSize+fileProgress/initialQueueSize);
		};
		xhr.send(formData);
		progressBar.setProgress(uploadedFileCount/initialQueueSize);
		progressText.innerText=lang("uploading_photo_X_of_Y", {current: initialQueueSize-uploadQueue.length, total: initialQueueSize});
	}

	var btn=ge("addPhotosBtn");
	var input=ge("photosFileInput") as HTMLInputElement;
	btn.addEventListener("click", (ev)=>input.click());
	input.addEventListener("change", (ev)=>{
		var files=input.files;
		for(var i=0;i<files.length;i++){
			if(files[i].type.indexOf("image/")==0){
				uploadQueue.push(files[i]);
			}
		}
		if(uploadQueue.length){
			box=new Box(lang("uploading_photos"));
			var boxContent;
			box.setContent(boxContent=ce("div", {}, [
				progressText=ce("div", {align: "center", className: "marginAfter"}),
				progressBarEl=ce("div", {className: "progressBar"}),
				resultEl=ce("div")
			]));
			boxContent.style.padding="32px";
			progressBar=new ProgressBar(progressBarEl);
			box.setCloseable(false);
			box.show();
			initialQueueSize=uploadQueue.length;
			uploadNextFile();
		}
	});
}

