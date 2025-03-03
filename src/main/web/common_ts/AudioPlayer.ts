const enum AudioElementIDs{
	PLAYER="audioPlayer",
	TIP_WRAP="audioTipWrap",
	TIP="audioTip",
	TIP_ARROW="audioTipArrow",
}

const enum AudioLocalStorageKey{
	VOLUME="audioVolume",
	TIME_FORMAT_LEFT="audioTimeFormatLeft",
}

const enum AudioPlayerState{
	PLAY="play",
	PAUSE="pause",
	STOP="stop",
	LOAD="load",
}

interface BaseSongInfo{
	id:string;
	duration:number|null;
	title:string;
	artist:string;
}

type SongInfo=BaseSongInfo&{url:string; unavailabilityReason?:never;}|BaseSongInfo&{url?:never; unavailabilityReason:string;};

interface AudioControlContainer{
	idSuffix:string,
	isFixedPosition:boolean,
}

interface AudioControl{
	audioID:string,
	idSuffix:string,
	isFixedPosition:boolean,
	row:HTMLElement
	play:HTMLElement
	duration:HTMLElement
	progressControl:HTMLElement
	progressBackLine:HTMLElement,
	loadLine:HTMLElement
	progressLine:HTMLElement
	volumeControl:HTMLElement
	volumeBackLine:HTMLElement
	volumeLine:HTMLElement
}

class AudioPlayer{
	private mgr:AudioManager|null=null;
	private eventsInitialized:boolean=false;
	private curAudioID:string|null=null;
	private state:AudioPlayerState=AudioPlayerState.STOP;
	private pausedByVideo:string|null=null;
	private time:number|null|undefined;
	private lastSong:SongInfo|null=null;
	private draggingProgressLine:AudioControl|null=null;
	private draggingVolumeLine:AudioControl|null=null;
	private timeFormatLeft:boolean=true;
	private hideTipTimeout:number|undefined;
	private cancelClick:boolean=false;

	private controlsForCurrentAudio:AudioControl[]=[];
	private containers:AudioControlContainer[]=[{idSuffix: '', isFixedPosition: false}];

	private static instance:AudioPlayer;

	public static getInstance():AudioPlayer{
		if(!AudioPlayer.instance) AudioPlayer.instance=new AudioPlayer();
		return AudioPlayer.instance;
	}

	public static playOrPause(id:string){
		this.getInstance().playOrPause(id);
	}

	public registerPlayerContainer(containerRandomID:string, isFixedPosition:boolean){
		if(!containerRandomID) return;
		const idSuffix="_"+containerRandomID;
		for(const container of this.containers){
			if(container.idSuffix===idSuffix) return;
		}
		const container:AudioControlContainer={idSuffix: idSuffix, isFixedPosition: isFixedPosition};
		this.containers.push(container);
		const control=this.newAudioControl(this.curAudioID, container);
		if(control){
			this.controlsForCurrentAudio.push(control);
			this.setGraphics(control);
			if(this.mgr){
				this.mgr.onPlayProgress(true);
			}
		}
	}

	public deregisterPlayerContainer(containerRandomID:string){
		if(!containerRandomID) return;
		const idSuffix="_"+containerRandomID;
		this.containers=this.containers.filter(v=>v.idSuffix!==idSuffix);
		this.controlsForCurrentAudio=this.controlsForCurrentAudio.filter(control=>control.idSuffix!==idSuffix);
	}

	private newAudioControl(audioID:string|null, container:AudioControlContainer|null):AudioControl|null{
		if(!container) return null;
		const row=ge("audio"+audioID+container.idSuffix);
		return row && {
			audioID: audioID,
			idSuffix: container.idSuffix,
			isFixedPosition: container.isFixedPosition,
			row: row,
			play: row.qs(".play"),
			duration: row.qs(".duration"),
			progressControl: row.qs(".audioProgress"),
			loadLine: row.qs(".audioLoadLine"),
			progressBackLine: row.qs(".audioBackLine"),
			progressLine: row.qs(".audioProgressValue"),
			volumeControl: row.qs(".audioVolume"),
			volumeBackLine: row.qs(".audioVolumeBackLine"),
			volumeLine: row.qs(".audioVolumeValue"),
		}
	}

	private forEachControl(callback:(control:AudioControl)=>any){
		const id=this.curAudioID;
		if(id===null) return;
		const controls=this.controlsForCurrentAudio;
		if(controls.length==0){
			// lazily populate the array of controls
			for(const container of this.containers){
				const control=this.newAudioControl(id, container);
				if(control) controls.push(control);
			}
		}
		for(const control of controls){
			if(callback(control)===false) return
		}
	}

	private findControl(event:Event):AudioControl|null{
		const t=event.target as HTMLElement;
		const row=t.closest(".audio");
		if(!row) return null;
		let control:AudioControl|null=null;
		this.forEachControl(c=>{
			if(c.row==row){
				control=c;
				return false;
			}
		});
		return control;
	}

	private getCurrentProgressPercentage(e:MouseEvent|TouchEvent, progressElement:HTMLElement):number{
		const x=e instanceof MouseEvent ? e.clientX : e.touches[0].clientX;
		let val=x-progressElement.getBoundingClientRect().x;
		val=val/progressElement.clientWidth*100;
		val=Math.min(100, Math.max(0, val));
		return val;
	}

	private changeVolume(e:MouseEvent|TouchEvent, control:AudioControl){
		if(e instanceof MouseEvent && e.button==2) return;
		const val=this.getCurrentProgressPercentage(e, control.volumeBackLine);
		this.draggingVolumeLine=control;
		this.showTip(e, Math.round(val)+"%", control.volumeBackLine, control.isFixedPosition);
		control.volumeLine.style.width=val+"%";
		if(this.mgr){
			this.mgr.setVolume(val/100);
		}
	}

	private changeProgress(e:MouseEvent|TouchEvent, control:AudioControl){
		if(e instanceof MouseEvent && e.button==2) return;
		this.draggingProgressLine=control;
		const val=this.getCurrentProgressPercentage(e, control.progressBackLine);
		this.showProgressTip(e, control.progressControl, val, control.isFixedPosition);
		this.time=val/100*this.lastSong.duration;
		control.progressLine.style.width=val+"%";
	}

	private showTip(e:MouseEvent|TouchEvent, text:string, progressLine:HTMLElement, isFixedPosition:boolean){
		const progressPosition=getXY(progressLine, isFixedPosition);
		const eventX=(e instanceof MouseEvent ? e.clientX : e.touches[0].clientX)+window.scrollX;
		const x=Math.min(progressPosition[0]+progressLine.offsetWidth, Math.max(progressPosition[0], eventX));
		const y=progressPosition[1];
		let tipWrap=ge(AudioElementIDs.TIP_WRAP);
		let tip=ge(AudioElementIDs.TIP);
		let arrow=ge(AudioElementIDs.TIP_ARROW);
		if(!tipWrap){
			tipWrap=ce("div", {id: AudioElementIDs.TIP_WRAP});
			tip=tipWrap.appendChild(ce("div", {id: AudioElementIDs.TIP}));
			arrow=tipWrap.appendChild(ce("div", {id: AudioElementIDs.TIP_ARROW}));
			document.body.appendChild(tipWrap);
		}
		tip.innerHTML=text;
		const width=Math.round(tipWrap.offsetWidth) || 32;
		const height=Math.round(tipWrap.offsetHeight) || 18;
		tipWrap.style.left=x-(width+1)/2+"px";
		tipWrap.style.top=y-height-7+"px";
		tipWrap.style.position=isFixedPosition ? "fixed" : "absolute";
		arrow.style.left=width/2-3+"px";
		tipWrap.style.display="block";
	}

	private hideTipWithTimeOut(e:MouseEvent|TouchEvent){
		const t=e.target as HTMLElement;
		if(!t.matches(".audioProgress, .audioProgress *, .audioVolume, .audioVolume *") || this.draggingProgressLine || this.draggingVolumeLine) return;
		this.hideTipTimeout=setTimeout(()=>{
			const tipWrap=ge(AudioElementIDs.TIP_WRAP);
			if(tipWrap) tipWrap.hide();
		}, 100);
	}

	private resetHideTipTimeout(e:MouseEvent|TouchEvent){
		const t=e.target as HTMLElement;
		if(t.matches(".audioProgress, .audioProgress *, .audioVolume, .audioVolume *") && this.hideTipTimeout) clearTimeout(this.hideTipTimeout);
	}

	private initPlayer(id:string){
		if(!ge(AudioElementIDs.PLAYER)){
			document.body.appendChild(ce("audio", {id: AudioElementIDs.PLAYER}));
		}
		if(!this.eventsInitialized){
			this.initEvents();
		}
		this.setPlayer(new AudioManager(this), id);
	}

	private initEvents(){
		if(this.eventsInitialized) return;

		document.body.addEventListener("mousedown", this.mouseDown.bind(this));
		document.body.addEventListener("touchstart", this.mouseDown.bind(this));
		document.body.addEventListener("mouseup", this.mouseUp.bind(this));
		document.body.addEventListener("touchend", this.mouseUp.bind(this));
		document.body.addEventListener("mousemove", this.mouseMove.bind(this));
		document.body.addEventListener("touchmove", this.mouseMove.bind(this));

		document.body.addEventListener("mouseover", this.resetHideTipTimeout.bind(this));
		document.body.addEventListener("touchstart", this.resetHideTipTimeout.bind(this));
		document.body.addEventListener("mouseout", this.hideTipWithTimeOut.bind(this));
		document.body.addEventListener("touchcancel", this.hideTipWithTimeOut.bind(this));
		document.body.addEventListener("touchend", this.hideTipWithTimeOut.bind(this));

		this.timeFormatLeft=Boolean(parseInt(localStorage.getItem(AudioLocalStorageKey.TIME_FORMAT_LEFT) || "1"));

		this.eventsInitialized=true;
	}

	public static maybeShowToolTip(t:HTMLElement){
		// If the song title is truncated, show a tooltip with the full title.
		if(t.offsetWidth<t.scrollWidth){
			t.setAttribute("title", t.innerText);
		}else{
			t.removeAttribute("title");
		}
	}

	private mouseDown(e:MouseEvent|TouchEvent){
		const t=e.target as HTMLElement;
		const control=this.findControl(e);
		if(!control) return;
		if(t.matches(".audioProgress, .audioProgress *")){
			control.progressControl.classList.add("dragging");
			this.changeProgress(e, control);
			this.cancelClick=true;
		}else if(t.matches(".audioVolume, .audioVolume *")){
			control.volumeControl.classList.add("dragging");
			this.changeVolume(e, control);
			this.cancelClick=true;
		}else if(t.matches(".duration") && e.type!="touchstart"){
			this.switchTimeLeftFormat(control.audioID, e as MouseEvent);
		}
	}

	private mouseUp(e:MouseEvent){
		if(this.draggingProgressLine){
			if(this.mgr && this.time!==undefined){
				try{
					if(!this.mgr.paused()){
						this.mgr.playAudio(this.time);
						this.time=null;
					}
				}catch(e){}
				this.mgr.onPlayProgress();
			}
			this.draggingProgressLine.progressControl.classList.remove("dragging");
			// Update other controls with the relevant value as soon as the mouse is released.
			this.forEachControl(c=>c.progressLine.style.width=this.draggingProgressLine.progressLine.style.width);
		}else if(this.draggingVolumeLine){
			this.draggingVolumeLine.volumeControl.classList.remove("dragging");
			// Update other controls with the relevant value as soon as the mouse is released.
			this.forEachControl(c=>c.volumeLine.style.width=this.draggingVolumeLine.volumeLine.style.width);
			if(this.mgr){
				localStorage.setItem(AudioLocalStorageKey.VOLUME, Math.round(this.mgr.getVolume()*100).toString());
			}
		}

		if(this.draggingVolumeLine || this.draggingProgressLine && !(e.target as HTMLElement).matches(".audioProgress, .audioProgress *")){
			// For the volume control, hide the tip as soon as the mouse button is released.
			// For the progress control, only hide it if the mouse button was released outside the progress control
			// (because we still show the timestamp tip as long as the pointer at least hovers over the progress control).
			const tip=ge(AudioElementIDs.TIP_WRAP);
			if(tip) tip.hide();
		}
		this.draggingVolumeLine=null;
		this.draggingProgressLine=null;
		this.cancelClick=false;
	}

	private mouseMove(e:MouseEvent|TouchEvent){
		if(this.draggingVolumeLine!==null) this.changeVolume(e, this.draggingVolumeLine);
		if(this.draggingProgressLine!==null){
			this.changeProgress(e, this.draggingProgressLine);
		}
		if(!this.lastSong || !this.lastSong.duration) return;
		if(!this.draggingProgressLine){
			// If not dragging the playhead, just hovering over the progress line,
			// show the timestamp tip anyway.
			if((e.target as HTMLElement).matches(".audioProgress *")){
				const control=this.findControl(e);
				this.showProgressTip(e, control.progressControl, this.getCurrentProgressPercentage(e, control.progressBackLine), control.isFixedPosition);
			}
		}
	}

	private showProgressTip(e:MouseEvent|TouchEvent, progress:HTMLElement, progressPercentage:number, isFixedPosition:boolean){
		const formattedTime=this.formatTime(Math.round(progressPercentage/100*this.lastSong.duration));
		this.showTip(e, formattedTime, progress, isFixedPosition);
	}

	private setPlayer(mgr:AudioManager, id:string){
		this.mgr=mgr;
		const volume=mobile ? 80 : parseInt(localStorage.getItem(AudioLocalStorageKey.VOLUME));
		if(!isNaN(volume)) mgr.setVolume(volume/100);
		this.playOrPause(id);
	}

	private playOrPause(id:string){
		if(this.cancelClick){
			this.cancelClick=false;
			return;
		}
		if(!this.mgr || !ge(AudioElementIDs.PLAYER)){
			this.initPlayer(id);
			return;
		}
		const curAudioID=this.curAudioID;
		if(id==curAudioID){
			if(this.mgr.paused()){
				// TODO: If a video is playing, actually pause the video.
				if(this.pausedByVideo) this.pausedByVideo=null;
				if(this.time!=undefined){
					try{
						this.mgr.playAudio(this.time);
						this.time=null;
					}catch(e){}
				}else{
					this.mgr.playAudio(this.time);
				}
				this.state=AudioPlayerState.PLAY;
			}else{
				this.mgr.pauseAudio();
				this.state=AudioPlayerState.PAUSE;
			}
		}else{
			// TODO: If a video is playing, actually pause the video.
			if(this.pausedByVideo) this.pausedByVideo=null;
			if(curAudioID) this.stop();
			this.setAudioID(id);
			if(!this.lastSong) this.setSongInfo();
			if(!this.lastSong) return;
			const lastSong=this.lastSong
			if(lastSong.unavailabilityReason){
				this.setAudioID(null);
				this.showAudioUnavailableMessage(lastSong);
				return;
			}
			try{
				this.mgr.loadAudio(lastSong.url, ge(AudioElementIDs.PLAYER));
			}catch(e){}
			this.state=AudioPlayerState.LOAD;
		}
		this.forEachControl(this.setGraphics.bind(this))
	}

	public onPlayProgress(curTime:number, totalTime:number, forceUpdateProgressBar?:boolean){
		if(isNaN(totalTime) || Math.abs(totalTime-this.lastSong.duration)>1) totalTime=this.lastSong.duration;
		if(this.time && this.mgr.paused()) curTime=this.time;
		this.setCurTime(Math.round(curTime), Math.round(totalTime));
		if((!this.mgr.paused() || forceUpdateProgressBar) && !this.draggingProgressLine){
			let percentage=curTime/totalTime*100;
			percentage=Math.min(Math.max(percentage, 0), 100);
			this.forEachControl(c=>c.progressLine.style.width=percentage+"%");
		}
	}

	public durationChanged(duration:number){
		if(this.lastSong){
			this.lastSong.duration=duration;
		}
	}

	public onPlayFinish(){
		this.stop();
	}

	onLoadProgress(loaded:number, total:number){
		if(isNaN(total)) total=this.lastSong.duration;
		let percentage=Math.ceil(loaded/total*100);
		percentage=Math.min(Math.max(percentage, 0), 100);
		this.forEachControl(c=>c.loadLine.style.width=percentage+"%")
	}

	onError(code:number){
		// TODO
	}

	private setGraphics(control:AudioControl){
		const id=this.curAudioID;
		if(!id) return;
		switch(this.state){
			case AudioPlayerState.PLAY:
				control.row.classList.add("current");
				control.play.classList.add("playing");
				break;
			case AudioPlayerState.PAUSE:
				control.row.classList.add("current");
				control.play.classList.remove("playing");
				break;
			case AudioPlayerState.STOP:
				const formattedDuration=this.formatTime(Math.round(this.lastSong && this.lastSong.duration || this.parseAudioInfo(id).duration));
				control.row.classList.remove("current");
				control.play.classList.remove("playing");
				control.duration.innerText=formattedDuration;
				control.progressLine.style.width="0";
				break;
			case AudioPlayerState.LOAD:
				const volume=this.mgr.getVolume();
				control.row.classList.add("current");
				control.play.classList.add("playing");
				if(mobile) control.volumeControl.hide();
				control.volumeLine.style.width=volume*100+"%";
				break;
		}
	}

	private stop(){
		const id=this.curAudioID;
		if(!id) return;
		if(this.mgr && !this.mgr.paused()) this.mgr.stopAudio();
		this.state=AudioPlayerState.STOP;
		this.forEachControl(this.setGraphics.bind(this));
		this.setAudioID(null);
		this.controlsForCurrentAudio=[];
	}

	private setAudioID(id:string|null){
		this.curAudioID=id;
	}

	private parseAudioInfo(id:string):SongInfo|null{
		const audioEl=ge("audio"+id);
		if(!audioEl) return null;
		return {
			id: id,
			url: audioEl.dataset.url,
			duration: audioEl.dataset.duration ? parseInt(audioEl.dataset.duration) : null,
			artist: audioEl.dataset.artist,
			title: audioEl.dataset.title,
		};
	}

	private setSongInfo(){
		this.lastSong=this.parseAudioInfo(this.curAudioID);
	}

	private showAudioUnavailableMessage(lastSong:SongInfo){
		// TODO: Currently the server cannot specify the unavailability reason.
	}

	private setCurTime(curTime:number, totalTime:number){
		let formatted=this.formatTime(this.timeFormatLeft ? totalTime-curTime : curTime);
		if(this.timeFormatLeft) formatted="-"+formatted;
		this.forEachControl(c=>c.duration.innerText=formatted);
	}

	private formatTime(time:number):string{
		time=Math.max(time, 0);
		const sec=time%60;
		let res=(sec<10) ? '0'+sec : sec;
		time=Math.floor(time/60);
		const min=time%60;
		res=min+':'+res;
		const hour=Math.floor(time/60);
		if(hour>0){
			if(min<10) res='0'+res;
			res=hour+':'+res;
		}
		return res;
	}

	private switchTimeLeftFormat(audioID:string, event:MouseEvent){
		// Only allow switching the time format on the currently playing audio.
		// Otherwise, the user won't be able to spot the difference.
		if(audioID && this.curAudioID!==audioID) return;
		this.timeFormatLeft= !this.timeFormatLeft;
		if(this.mgr){
			this.mgr.onPlayProgress();
		}
		localStorage.setItem(AudioLocalStorageKey.TIME_FORMAT_LEFT, this.timeFormatLeft ? "1" : "0");
		event.stopPropagation();
	}
}
