interface RealtimeNotification{
	id:string;
	type:string;
	objectType:string;
	objectID:string;
	actorID:number;
	title:string;
	content:string;
	url:string;
	avatar:RealtimeNotificationImage;
	image:RealtimeNotificationImage;
	linkExtraAttrs:{[key:string]:string};
}

interface RealtimeNotificationImage{
	jpeg1x:string;
	webp1x:string;
	jpeg2x:string;
	webp2x:string;
}

class Notifier{
	private static MAX_NOTIFICATIONS=4;

	private static running=false;
	private static bc:BroadcastChannel;
	private static tabID=Math.random().toString().split('.')[1];
	private static allTabs:string[]=[Notifier.tabID];
	private static isMainTab=false;
	private static becomeMainTimeout:number;
	private static socket:WebSocket;
	private static keepAliveInterval:number;
	private static retryDelay:number=1;
	private static notificationsEl:HTMLElement;
	private static isIdle=false;
	private static idleTimeout:number;
	private static idleCheckTimeout:number;
	private static notificationAudio:HTMLAudioElement;
	private static messageAudio:HTMLAudioElement;
	private static notificationQueue:RealtimeNotification[]=[];
	private static animating=false;
	private static frozen=false;
	private static notificationUnderMouse:HTMLElement;

	public static start(){
		if(Notifier.running){
			throw new Error("Already started");
		}
		Notifier.running=true;

		Notifier.bc=new BroadcastChannel("notifier");
		Notifier.bc.onmessage=Notifier.onBroadcastMessage;
		Notifier.bc.postMessage({type: "newTab", id: Notifier.tabID});

		window.addEventListener("beforeunload", Notifier.onBeforeUnload);
		Notifier.becomeMainTimeout=setTimeout(()=>Notifier.becomeMainTab(), 500);

		var el=ce("div", {id: "notifier"});
		document.body.appendChild(el);
		Notifier.notificationsEl=el;

		Notifier.idleCheckTimeout=setTimeout(Notifier.checkIdle, 30000);
		window.addEventListener("blur", (ev)=>Notifier.setInactive());
		window.addEventListener("focus", (ev)=>Notifier.setActive());

		if(userConfig.notifier.sound){
			Notifier.notificationAudio=new Audio("/res/notification1.mp3");
			Notifier.messageAudio=new Audio("/res/notification2.mp3");
		}

		/*document.addEventListener("keydown", (ev)=>{
			if(ev.keyCode==187)
				Notifier.handleNotification({id: "test"+Math.random(), type: "LIKE", objectType: "POST", objectID: "1", actorID: 1, title: "Test notification", content: "<a href=\"/users/1\">Test User</a> would like to show you how notifications work", url: "/users/1", avatar: null, image: null});
		});*/
	}

	private static checkIdle(){
		// console.log("check idle");
		document.addEventListener("mousemove", Notifier.onInputEvent);
		document.addEventListener("keydown", Notifier.onInputEvent);
		if(Notifier.idleCheckTimeout){
			clearTimeout(Notifier.idleCheckTimeout);
			Notifier.idleCheckTimeout=null;
		}
		Notifier.idleTimeout=setTimeout(Notifier.setInactive, 30000);
	}

	private static onInputEvent(ev:Event){
		Notifier.setActive();
	}

	private static setActive(){
		document.removeEventListener("mousemove", Notifier.onInputEvent);
		document.removeEventListener("keydown", Notifier.onInputEvent);
		if(Notifier.idleTimeout){
			clearTimeout(Notifier.idleTimeout);
			Notifier.idleTimeout=null;
		}
		Notifier.idleCheckTimeout=setTimeout(Notifier.checkIdle, 30000);
		Notifier.isIdle=false;
		// console.log("set active");
	}

	private static setInactive(){
		if(Notifier.idleTimeout){
			clearTimeout(Notifier.idleTimeout);
			Notifier.idleTimeout=null;
		}
		if(Notifier.idleCheckTimeout){
			clearTimeout(Notifier.idleCheckTimeout);
			Notifier.idleCheckTimeout=null;
		}
		Notifier.isIdle=true;
		// console.log("set inactive");
	}

	private static onBroadcastMessage(ev:MessageEvent){
		// console.log("broadcast msg", ev.data);
		var m=ev.data;
		switch(m.type){
			case "newTab": {
				var id=m.id;
				if(Notifier.allTabs.indexOf(id)==-1){
					Notifier.allTabs.push(id);
					if(Notifier.isMainTab){
						Notifier.bc.postMessage({type: "allTabs", tabs: Notifier.allTabs});
					}
				}
				break;
			}
			case "tabClosing": {
				Notifier.allTabs.remove(m.id);
				if(m.newMainTabID==Notifier.tabID){
					Notifier.becomeMainTab();
				}
				break;
			}
			case "allTabs": {
				Notifier.allTabs=m.tabs;
				if(Notifier.becomeMainTimeout){
					clearTimeout(Notifier.becomeMainTimeout);
					Notifier.becomeMainTimeout=null;
				}
				break;
			}
			case "notification": {
				Notifier.handleNotification(m.notification as RealtimeNotification);
				break;
			}
			case "dismiss": {
				var el=ge("notifierNotification_"+m.id);
				if(el){
					Notifier.dismissNotification(el, false, false);
				}
				break;
			}
			case "counters": {
				Notifier.handleCounters(m.counters);
				break;
			}
		}
	}

	private static onBeforeUnload(ev:BeforeUnloadEvent){
		var sortedIDs=Notifier.allTabs.sort();
		sortedIDs.remove(Notifier.tabID);
		if(sortedIDs.length){
			Notifier.bc.postMessage({type: "tabClosing", id: Notifier.tabID, newMainTabID: sortedIDs[0]});
		}
	}

	private static becomeMainTab(){
		if(Notifier.isMainTab)
			return;
		Notifier.isMainTab=true;
		// console.log("becoming main tab");
		Notifier.openSocket();
	}

	private static openSocket(){
		Notifier.socket=new WebSocket(userConfig.notifier.ws);
		Notifier.socket.onerror=Notifier.onSocketError;
		Notifier.socket.onclose=Notifier.onSocketClose;
		Notifier.socket.onmessage=Notifier.onSocketMessage;
		Notifier.socket.onopen=Notifier.onSocketOpen;
	}

	private static onSocketError(ev:Event){
		// console.log(ev);
	}

	private static onSocketClose(ev:CloseEvent){
		// console.log(ev);
		if(Notifier.keepAliveInterval){
			clearInterval(Notifier.keepAliveInterval);
		}
		// console.log("retrying in "+Notifier.retryDelay+"s");
		setTimeout(Notifier.openSocket, Notifier.retryDelay*1000);
		Notifier.retryDelay=Math.min(256, Notifier.retryDelay*2);
	}

	private static onSocketOpen(ev:Event){
		// console.log("socket opened");
		Notifier.keepAliveInterval=setInterval(()=>Notifier.socket.send(JSON.stringify({type: "keepAlive"})), 25000);
		Notifier.retryDelay=1;
	}

	private static onSocketMessage(ev:MessageEvent){
		var m=JSON.parse(ev.data);
		// console.log("socket msg", m);
		if(m.type=="notification"){
			Notifier.handleNotification(m.notification as RealtimeNotification);
		}else if(m.type=="counters"){
			Notifier.handleCounters(m.counters);
		}
	}

	private static handleNotification(n:RealtimeNotification){
		if(!Notifier.animating && !Notifier.frozen && Notifier.notificationsEl.childElementCount<Notifier.MAX_NOTIFICATIONS){
			Notifier.showNotifications([n]);
		}else{
			Notifier.notificationQueue.push(n);
		}

		if(Notifier.isMainTab){
			Notifier.bc.postMessage({type: "notification", notification: n});
			if(userConfig.notifier.sound){
				var sound=n.type=="MAIL_MESSAGE" ? Notifier.messageAudio : Notifier.notificationAudio;
				sound.currentTime=0;
				try{
					sound.play();
				}catch(e){}
			}
		}
	}

	private static showNotifications(notifications:RealtimeNotification[]){
		var wrap=Notifier.notificationsEl;
		var heightBefore=wrap.offsetHeight;

		for(var n of notifications){
			var ava;
			if(n.avatar){
				ava=ce("span", {className: "ava avaHasImage sizeA"}, [
					ce("picture", {}, [
						ce("source", {srcset: `${n.avatar.webp1x}, ${n.avatar.webp2x} 2x`, type: "image/webp"}),
						ce("source", {srcset: `${n.avatar.jpeg1x}, ${n.avatar.jpeg2x} 2x`, type: "image/jpeg"}),
						ce("img", {src: n.avatar.jpeg1x, className: "avaImage"})
					])
				]);
			}else{
				ava=ce("span", {className: "ava avaPlaceholder sizeA"});
			}
			var cont, closeBtn:HTMLElement, underlayLink;
			let el=ce("div", {className: "popupNotification", id: "notifierNotification_"+n.id}, [
				underlayLink=ce("a", {href: n.url, className: "underlayLink"}),
				ce("div", {className: "titleBar"}, [
					ce("div", {className: "title ellipsize", innerHTML: n.title}),
					closeBtn=ce("a", {className: "closeBtn", ariaLabel: lang("close")})
				]),
				cont=ce("div", {className: "notificationContent"}, [
					ava,
					ce("div", {className: "text", innerHTML: n.content})
				])
			]);
			el.customData={notification: n};
			if(n.image){
				cont.appendChild(ce("picture", {}, [
					ce("source", {srcset: `${n.image.webp1x}, ${n.image.webp2x} 2x`, type: "image/webp"}),
					ce("source", {srcset: `${n.image.jpeg1x}, ${n.image.jpeg2x} 2x`, type: "image/jpeg"}),
					ce("img", {src: n.image.jpeg1x, className: "image"})
				]));
			}

			if(n.linkExtraAttrs){
				for(var attr in n.linkExtraAttrs){
					underlayLink.setAttribute(attr, n.linkExtraAttrs[attr]);
				}
			}
			
			closeBtn.addEventListener("click", (ev)=>Notifier.dismissNotification(el, true));
			el.addEventListener("mouseenter", (ev)=>{
				Notifier.notificationUnderMouse=el;
				Notifier.freeze();
			});
			el.addEventListener("mouseleave", (ev)=>{
				if(Notifier.notificationUnderMouse==el)
					Notifier.notificationUnderMouse=null;
				Notifier.unfreeze();
			});
			el.addEventListener("click", (ev)=>{
				var target=ev.target as HTMLElement;
				if(target.tagName=="A" && target!=closeBtn){
					Notifier.dismissNotification(el, true);
				}
			});
			wrap.appendChild(el);
			if(!Notifier.isIdle){
				el.customData.fadeTimeout=setTimeout(()=>Notifier.startFadingNotification(el), 7000);
			}
		}

		var heightDiff=wrap.offsetHeight-heightBefore;
		var first=true;
		Notifier.animating=true;
		for(var ntf of wrap.children.unfuck()){
			ntf.anim([{transform: `translateY(${heightDiff}px)`}, {transform: "translateY(0)"}], {duration: 200, easing: "ease"}, first ? ()=>{
				Notifier.animating=false;
				Notifier.maybeShowNotificationsFromQueue();
			} : null);
			first=false;
		}
	}

	private static startFadingNotification(el:HTMLElement){
		el.customData.fadeTimeout=null;
		el.customData.fadeAnim=el.anim([{opacity: 0}], {duration: 1000, easing: "ease"}, ()=>Notifier.dismissNotification(el, false));
	}

	private static dismissNotification(el:HTMLElement, fadeOut:boolean, sendEvent:boolean=true){
		var wrap=Notifier.notificationsEl;
		var siblingsAbove:HTMLElement[]=[];
		for(var i=0;i<wrap.children.length;i++){
			if(wrap.children[i]==el)
				break;
			siblingsAbove.push(wrap.children[i] as HTMLElement);
		}

		if(siblingsAbove.length){
			var offset=el.offsetHeight+10;
			for(var sibling of siblingsAbove){
				sibling.anim([{transform: "translateY(0)"}, {transform: `translateY(${offset}px)`}], {duration: 200, easing: "ease"});
			}
			Notifier.animating=true;
			el.anim([{opacity: fadeOut ? 1 : 0}, {opacity: 0}, {opacity: 0}], {duration: 200, easing: "ease"}, ()=>{
				el.remove();
				for(var sibling of siblingsAbove){
					sibling.style.transform="";
				}
				Notifier.animating=false;
				Notifier.maybeShowNotificationsFromQueue();
			});
		}else{
			if(fadeOut){
				el.anim([{opacity: 1}, {opacity: 0}], {duration: 150, easing: "ease"}, ()=>{
					el.remove();
					if(Notifier.notificationUnderMouse==el){
						Notifier.notificationUnderMouse=null;
						Notifier.unfreeze();
					}
					Notifier.maybeShowNotificationsFromQueue();
				});
			}else{
				if(Notifier.notificationUnderMouse==el){
					Notifier.notificationUnderMouse=null;
					Notifier.unfreeze();
				}
				el.remove();
				Notifier.maybeShowNotificationsFromQueue();
			}
		}
		if(sendEvent){
			Notifier.bc.postMessage({type: "dismiss", id: el.customData.notification.id});
		}
	}

	private static maybeShowNotificationsFromQueue(){
		if(Notifier.animating || Notifier.frozen || !Notifier.notificationQueue.length)
			return;
		var toShow:RealtimeNotification[]=[];
		var queueLen=Notifier.notificationQueue.length;
		for(var i=0;i<Math.min(Notifier.MAX_NOTIFICATIONS-Notifier.notificationsEl.childElementCount, queueLen);i++){
			toShow.push(Notifier.notificationQueue.shift());
		}
		Notifier.showNotifications(toShow);
	}

	private static freeze(){
		if(Notifier.frozen)
			return;
		Notifier.frozen=true;
		for(var el of Notifier.notificationsEl.children.unfuck()){
			if(el.customData.fadeTimeout){
				clearTimeout(el.customData.fadeTimeout);
				el.customData.fadeTimeout=null;
			}else if(el.customData.fadeAnim){
				var curOpacity=getComputedStyle(el).opacity;
				el.customData.fadeAnim.cancel();
				el.customData.fadeAnim=null;
				el.anim([{opacity: curOpacity}, {opacity: 1}], {duration: 100, easing: "ease"});
			}
		}
	}

	private static unfreeze(){
		if(!Notifier.frozen)
			return;
		Notifier.frozen=false;
		Notifier.maybeShowNotificationsFromQueue();
		for(var el of Notifier.notificationsEl.children.unfuck()){
			let _el=el;
			el.customData.fadeTimeout=setTimeout(()=>Notifier.startFadingNotification(_el), 5000);
		}
	}

	private static handleCounters(counters:{[key:string]:number}){
		setMenuCounters(counters);
		if(Notifier.isMainTab){
			Notifier.bc.postMessage({type: "counters", counters: counters});
		}
	}
}
