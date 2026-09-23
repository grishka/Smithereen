class RemoteRepliesLoader{
	private readonly postID:string;
	private timeout:number;
	private readonly onDone:{(count:number):void};
	private xhr:XMLHttpRequest;
	private canceled:boolean=false;

	public constructor(postID:string, onDone:{(count:number):void}){
		this.postID=postID;
		this.onDone=onDone;
		this.doNextRequest();
	}

	public cancel(){
		this.canceled=true;
		if(this.xhr){
			this.xhr.abort();
			this.xhr=null;
		}
		if(this.timeout){
			clearTimeout(this.timeout);
			this.timeout=null;
		}
	}

	private doNextRequest(){
		var timeoutCallback=()=>{
			this.timeout=null;
			this.doNextRequest();
		};
		this.xhr=ajaxGet(`/posts/${this.postID}/fetchAllReplies`, r=>{
			this.xhr=null;
			if(this.canceled)
				return;
			if(r.status=="done"){
				this.onDone(r.new_count);
			}else if(r.status=="running"){
				this.timeout=setTimeout(timeoutCallback, 5000);
			}else{
				console.log("unexpected fetch replies status: "+r.status);
			}
		}, err=>{
			if(this.canceled)
				return;
			this.xhr=null;
			this.timeout=setTimeout(timeoutCallback, 5000);
		}, "json");
	}
}

function loadRemoteComments(postID:string, randomID:string=null){
	cur.repliesLoader=new RemoteRepliesLoader(postID, (count)=>{
		cur.repliesLoader=null;
		if(count>0){
			ge("postNewCommentsW"+postID+(randomID ? ("_"+randomID) : "")).show();
		}
	});
	ajaxNavCallbacks.push(()=>{
		if(cur.repliesLoader)
			cur.repliesLoader.cancel();
	});
}