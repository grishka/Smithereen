class AudioManager{
	private music:HTMLAudioElement|null=null;
	private defaultVolume:number=0.8;
	private autoStart:boolean=true;
	private playProgressID:number|null=null;
	private loadProgressID:number|null=null;
	private audioPlayer:AudioPlayer;

	constructor(audioPlayer:AudioPlayer){
		this.audioPlayer=audioPlayer;
	}

	public loadAudio(url:string, audioElement:HTMLAudioElement){
		this.pauseAudio();
		this.autoStart=true;
		this.music=audioElement;
		audioElement.addEventListener('canplay', this.onCanPlay.bind(this));
		audioElement.addEventListener('error', this.onErr.bind(this));

		// Normally, the HTML from the server should already contain the duration.
		// However, if for some reason the server didn't send a duration,
		// or the one it sent doesn't match the actual duration, we install this listener to have a chance to correct it.
		audioElement.addEventListener('durationchange', this.onDurationChange.bind(this));

		audioElement.src=url;
		audioElement.load();
		if(!this.loadProgressID) this.loadProgressID=setInterval(this.onLoadProgress.bind(this), 200);
		audioElement.volume=this.defaultVolume;
		try{
			this.playAudio();
		}catch(e){}
	}

	public playAudio(time?:number|null|undefined){
		if(!this.music) return;
		if(time!=undefined){
			try{
				this.music.currentTime=time;
			}catch(e){}
		}
		this.autoStart=true;
		//noinspection JSIgnoredPromiseFromCall
		this.music.play();
		if(!this.playProgressID){
			this.playProgressID=setInterval(this.onPlayProgress.bind(this), 1000);
		}
	}

	public pauseAudio(){
		if(!this.music) return;
		this.music.pause();
		this.stopPlayProgress();
	}

	public stopAudio(){
		if(!this.music) return;
		try{
			this.music.currentTime=0;
		}catch(e){}
		this.autoStart=false;
		this.music.pause();
		this.stopPlayProgress();
	}

	public setVolume(volume:number){
		this.defaultVolume=volume;
		if(!this.music) return;
		this.music.volume=volume;
	}

	public getVolume():number{
		if(!this.music) return 0;
		return this.music.volume;
	}

	public paused():boolean{
		if(!this.music) return true;
		return this.music.paused;
	}

	private stopPlayProgress(){
		clearInterval(this.playProgressID)
		this.playProgressID=null;
	}

	private stopLoadProgress(){
		clearInterval(this.loadProgressID)
		this.loadProgressID=null;
	}

	public onPlayProgress(forceUpdateProgressBar?:boolean){
		if(!this.music) return;
		const curTime=Math.floor(this.music.currentTime*1000)/1000;
		const totalTime=Math.floor(this.music.duration*1000)/1000;
		this.audioPlayer.onPlayProgress(curTime, totalTime, forceUpdateProgressBar);
		if(Math.abs(totalTime-curTime)<0.1){
			this.pauseAudio();
			this.audioPlayer.onPlayFinish();
		}
	}

	private onLoadProgress(){
		if(!this.music || this.music.error) return;
		const totalTime=Math.floor(this.music.duration*1000)/1000;
		let bufferedTime:number;
		try{
			bufferedTime=Math.floor(this.music.buffered.end(0)*1000)/1000 || 0;
		}catch(e){}
		if(totalTime && Math.abs(totalTime-bufferedTime)<0.1){
			this.audioPlayer.onLoadProgress(totalTime, totalTime);
			this.stopLoadProgress();
		}else{
			this.audioPlayer.onLoadProgress(bufferedTime, totalTime);
		}
	}

	private onDurationChange(){
		if(!this.music) return;
		this.audioPlayer.durationChanged(Math.floor(this.music.duration*1000)/1000);
	}

	private onCanPlay(){
		if(this.autoStart){
			try{
				//noinspection JSIgnoredPromiseFromCall
				this.music!.play();
			}catch(e){}
			if(!this.playProgressID){
				this.playProgressID=setInterval(this.onPlayProgress.bind(this), 1000);
			}
		}
	}

	private onErr(e:ErrorEvent){
		this.audioPlayer.onError((e.target as HTMLMediaElement).error.code)
	}
}