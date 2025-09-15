class EndlessPagination{
	private el:HTMLElement;
	private link:HTMLAnchorElement;
	private loader:HTMLElement;
	private loading:boolean=false;
	private observer:IntersectionObserver;
	private id:string;

	public constructor(el:HTMLElement){
		this.el=el;
		this.link=el.getElementsByTagName("a")[0];
		this.loader=el.qs("span.loader");
		this.link.addEventListener("click", (ev)=>{
			ev.preventDefault();
			ev.stopPropagation();
			this.load();
		}, false);
		this.id=el.dataset.id;

		if(el.dataset.noScrollEvents==undefined){
			this.observer=new IntersectionObserver(this.observerCallback.bind(this));
			this.observer.observe(el);
		}
	}

	private load(){
		if(this.loading)
			return;
		this.loading=true;
		this.link.hide();
		this.loader.show();
		var url=addParamsToURL(this.link.href, {"pagination": this.id});
		ajaxGetAndApplyActions(url, ()=>{
			this.link.show();
			this.loader.hide();
			this.loading=false;
		}, ()=>{
			this.link.show();
			this.loader.hide();
			this.loading=false;
		});
	}

	private observerCallback(entries:IntersectionObserverEntry[], observer:IntersectionObserver){
		for(var entry of entries){
			if(entry.isIntersecting && !this.loading){
				this.load();
			}
		}
	}
}
