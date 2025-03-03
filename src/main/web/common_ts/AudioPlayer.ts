const enum AudioElementIDs{
	PLAYER="audioPlayer",
}

const enum AudioLocalStorageKey{
	VOLUME="audio_volume",
	TIME_FORMAT_LEFT="audio_time_format_left",
}

const enum AudioPlayerAction{
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

class AudioPlayer{
	private mgr:AudioManager|null=null;
	private eventsInitialized:boolean=false;
	private curAudioID:string|null=null;
	private pausedByVideo:string|null=null;
	private time:number|null|undefined;
	private lastSong:SongInfo|null=null;
	private isDraggingProgressLine:boolean=false;
	private isDraggingVolumeLine:boolean=false;
	private timeFormatLeft:boolean=true;

	private static instance:AudioPlayer;

	private static getInstance():AudioPlayer{
		if(!AudioPlayer.instance) AudioPlayer.instance=new AudioPlayer();
		return AudioPlayer.instance;
	}

	public static play(id:string){
		this.getInstance().operate(id);
	}

	public static volumeClick(e:MouseEvent){
		this.getInstance().volumeClick(e);
	}

	public static progressClick(e:MouseEvent){
		this.getInstance().progressClick(e);
	}

	public static switchTimeLeftFormat(audioID:string, event:MouseEvent){
		this.getInstance().switchTimeLeftFormat(audioID, event);
	}

	private getCurrentProgressPercentage(e:MouseEvent, elID:string):number{
		const progressElement=ge(elID);
		let val=e.clientX-progressElement.getBoundingClientRect().x;
		val=val/progressElement.clientWidth*100;
		val=Math.min(100, Math.max(0, val));
		return val;
	}

	private volumeClick(e:MouseEvent){
		if(e.button==2) return;
		const id=this.curAudioID;
		const val=this.getCurrentProgressPercentage(e, "audio_volume_back_line"+id);
		this.isDraggingVolumeLine=true;
		// TODO: Show tip
		if(id && ge("player"+id)){
			ge("audio_volume_line"+id).style.width=val+"%";
		}
		if(this.mgr){
			this.mgr.setVolume(val/100);
		}
		localStorage.setItem(AudioLocalStorageKey.VOLUME, Math.round(val).toString());
	}

	private progressClick(e:MouseEvent){
		if(e.button==2) return;
		const id=this.curAudioID;
		this.isDraggingProgressLine=true;
		const val=this.getCurrentProgressPercentage(e, "audio_back_line"+id);
		this.time=val/100*this.lastSong.duration;
		if(id && ge("player"+id)){
			ge("audio_progress_line"+id).style.width=val+"%";
		}
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
		// Here we should add event handlers for the following events:
		// - audio_start (for showing the player in the page header)
		// - audio_mediakey (for handling the keyboard media keys)
		// - video_start (when the user starts a video, the currently playing audio should be paused)
		// - video_hide (when the user stops the video, the currently playing audio should be resumed)
		// - logged_off (naturally, logging off should remove the player and stop the audio)
		// We don't do any of it for now.

		document.addEventListener("mouseup", this.mouseUp.bind(this));
		document.addEventListener("mousemove", this.mouseMove.bind(this));

		this.timeFormatLeft=Boolean(parseInt(localStorage.getItem(AudioLocalStorageKey.TIME_FORMAT_LEFT) || "1"));

		this.eventsInitialized=true;
	}

	private mouseUp(e:MouseEvent){
		if(this.mgr){
			if(this.isDraggingProgressLine && this.time!==undefined){
				try{
					if(!this.mgr.paused()){
						this.mgr.playAudio(this.time);
						this.time=null;
					}
				}catch(e){}
			}
			this.mgr.onPlayProgress();
		}
		this.isDraggingVolumeLine=false;
		this.isDraggingProgressLine=false;
	}

	private mouseMove(e:MouseEvent){
		if(this.isDraggingVolumeLine) this.volumeClick(e);
		if(this.isDraggingProgressLine) this.progressClick(e);
	}

	private setPlayer(mgr:AudioManager, id:string){
		this.mgr=mgr;
		const volume=mobile ? 80 : parseInt(localStorage.getItem(AudioLocalStorageKey.VOLUME));
		if(!isNaN(volume)) mgr.setVolume(volume/100);
		this.operate(id);
	}

	private operate(id:string){
		if(!this.mgr || !ge(AudioElementIDs.PLAYER)){
			this.initPlayer(id);
			return;
		}
		const curAudioID=this.curAudioID;
		if(id==curAudioID){
			if(this.mgr.paused()){
				// TODO: If a video is playing, actually pause the video.
				if(this.pausedByVideo) this.pausedByVideo=null;
				this.mgr.playAudio(this.time);
				if(this.time!=undefined) this.time=null;
				this.setGraphics(AudioPlayerAction.PLAY);
				this.playback();
			}else{
				this.mgr.pauseAudio();
				this.setGraphics(AudioPlayerAction.PAUSE);
				this.playback(true);
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
			this.setGraphics(AudioPlayerAction.LOAD);
			this.playback();
		}
	}

	public onPlayProgress(curTime:number, totalTime:number){
		const id=this.curAudioID;
		if(Math.abs(totalTime-this.lastSong.duration)>1 || isNaN(totalTime)) totalTime=this.lastSong.duration;
		if(this.time && this.mgr.paused()) curTime=this.time;
		this.setCurTime(Math.round(curTime), Math.round(totalTime));
		if(this.mgr.paused()) return;
		let percentage=curTime/totalTime*100;
		percentage=Math.min(Math.max(percentage, 0), 100);
		if(!this.isDraggingProgressLine){
			if(ge("player"+id)){
				ge("audio_progress_line"+id).style.width=percentage+"%";
			}
		}
	}

	public durationChanged(duration:number){
		if(this.lastSong && this.lastSong.duration==null){
			this.lastSong.duration=duration;
		}
	}

	public onPlayFinish(){
		this.stop();
	}

	onLoadProgress(loaded:number, total:number){
		const id=this.curAudioID;
		if(isNaN(total)) total=this.lastSong.duration;
		let percentage=Math.ceil(loaded/total*100);
		percentage=Math.min(Math.max(percentage, 0), 100);
		if(ge("player"+id)){
			ge("audio_load_line"+id).style.width=percentage+"%";
		}
	}

	onError(code:number){
		// TODO
	}

	private setGraphics(action:AudioPlayerAction){
		const id=this.curAudioID;
		const row=ge("audio"+id);
		if(!id) return;
		switch(action){
			case AudioPlayerAction.PLAY:
				if(row){
					ge("play"+id).classList.add("playing");
					row.classList.add("current");
				}
				break;
			case AudioPlayerAction.PAUSE:
				if(row){
					ge("play"+id).classList.remove("playing");
				}
				break;
			case AudioPlayerAction.STOP:
				if(row){
					ge("play"+id).classList.remove("playing");
					row.classList.remove("current");
					const formattedDuration=this.formatTime(Math.round(this.lastSong && this.lastSong.duration || this.parseAudioInfo(id).duration));
					const durationDiv=row.querySelector("*.duration");
					if(durationDiv){
						durationDiv.innerHTML=formattedDuration;
					}
					if(ge("player"+id)){
						ge("audio_progress_line"+id).style.width="0";
					}
				}
				break;
			case AudioPlayerAction.LOAD:
				const volume=this.mgr.getVolume();
				if(row){
					ge("play"+id).classList.add("playing");
					row.classList.add("current");
					const volumeControl=ge("audio_volume"+id);
					if(volumeControl && mobile){
						volumeControl.hide();
					}
					const volumeLine=ge("audio_volume_line"+id);
					if(volumeLine){
						volumeLine.style.width=volume*100+"%";
					}
				}
				break;
		}
	}

	private playback(paused:boolean=false){
		// TODO: Send the song to the user's status.
	}

	private stop(){
		const id=this.curAudioID;
		if(!id) return;
		if(this.mgr && !this.mgr.paused()) this.mgr.stopAudio();
		this.setGraphics(AudioPlayerAction.STOP);
		this.setAudioID(null);
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
		const id=this.curAudioID;
		let formatted=this.formatTime(this.timeFormatLeft ? totalTime-curTime : curTime);
		if(this.timeFormatLeft) formatted="-"+formatted;
		const row=ge("audio"+id);
		if(row){
			const durationDiv=row.querySelector("*.duration");
			if(durationDiv){
				durationDiv.innerHTML=formatted;
			}
		}
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
