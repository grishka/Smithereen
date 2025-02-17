class PostLayer extends BaseMediaViewerLayer{
	private contentWrap:HTMLElement;
	private postID:string;
	private commentID:string;
	private randomID:string;

	public constructor(contentHTML:string, postID:string, commentID:string, fromPopState:boolean){
		super(fromPopState);
		this.postID=postID;
		this.commentID=commentID;
		this.contentWrap=ce("div", {className: "simpleLayer postLayer", innerHTML: contentHTML});
		var bottomForm=this.contentWrap.qs(".postLayerCommentForm");
		if(bottomForm){
			var observer=new IntersectionObserver((entries)=>{
				for(var entry of entries){
					if(entry.isIntersecting)
						bottomForm.classList.remove("floating");
					else
						bottomForm.classList.add("floating");
				}
			});
			observer.observe(this.contentWrap.qs(".scrollDetector"));
		}
		this.randomID=this.contentWrap.qs(".postLayerPost").dataset.rid;
		this.id="postLayer"+this.postID+"_"+this.randomID;
		this.contentWrap.qs(".closeBtn").addEventListener("click", (ev)=>this.dismiss());
	}

	public onCreateContentView():HTMLElement{
		return ce("div", {}, [this.contentWrap]);
	}

	public wantsDarkerScrim(){
		return true;
	}

	public getCustomDismissAnimation(){
		return {
			keyframes: [{transform: "translateY(0)", opacity: 1}, {transform: "translateY(15px)", opacity: 0}],
			options: {duration: 150, easing: "cubic-bezier(0.32, 0, 0.67, 0)"}
		};
	}

	public getCustomAppearAnimation(){
		return {
			keyframes: [{transform: "translateY(25px)", opacity: 0}, {transform: "translateY(0)", opacity: 1}],
			options: {duration: 250, easing: "cubic-bezier(0.22, 1, 0.36, 1)"}
		};
	}

	public onShown(){
		initDynamicControls();
		this.updateHistory({layer: "Post", id: this.postID, commentID: this.commentID}, "/posts/"+this.postID);
		if(this.commentID){
			var commentEl=ge(`post${this.commentID}_${this.randomID}`);
			console.log("comment: "+this.commentID, commentEl);
			if(commentEl){
				commentEl.classList.add("highlight");
				commentEl.scrollIntoView();
			}
		}
	}
}

function openPostLayer(id:string, commentID:string=null, fromPopState:boolean=false):boolean{
	LayerManager.getInstance().showBoxLoader();
	ajaxGet("/posts/"+id+"?ajaxLayer", (r)=>{
		if(r[0]=='['){
			var cmds=JSON.parse(r);
			for(var c of cmds){
				applyServerCommand(c);
			}
		}else{
			ge("boxLoader").hide();
			new PostLayer(r, id, commentID, fromPopState).show();
		}
	}, (err)=>new MessageBox(lang("error"), err || lang("network_error"), lang("close")).show(), "text");
	return false;
}
