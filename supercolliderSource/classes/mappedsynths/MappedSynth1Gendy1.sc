/**
* (c) 2010 Dan Stowell. Released under the GPLv3.
*/
// NOTE: 8 continuous params gives so many combinations (20**8) that it's greater than SC's integer size limit. 
// Therefore I've pulled back to 7 params.
MappedSynth1Gendy1 : MappedSynth1 {
/*
s.waitForBoot{MappedSynth1Gendy1.new.wander}
*/

*synthDefName{
	^\_maptsyn_gendy1
}

*probeTime {
	^ 0.2
}
*timbreMedianRange {
	^ 6 
	// average over 6 frames since this synth is quite nonstationary
}

*synthDefParamLabels{
	^#[\ampdist, \durdist, \adparam, \ddparam, \minfreq, \maxfreq, \ampscale, /* \durscale */]
}
*synthDefParams{
	^[
		//\ampdist  -> 
		ControlSpec(0, 5, \lin, 1),
		//\durdist  -> 
		ControlSpec(0, 5, \lin, 1),
		//\adparam  -> 
		ControlSpec(0.0001, 1, \lin),
		//\ddparam  -> 
		ControlSpec(0.0001, 1, \lin),
		//\minfreq  -> 
		ControlSpec(10,  2000,  \exp),
		//\maxfreq  -> 
		ControlSpec(200, 10000, \exp),
		//\ampscale -> 
		ControlSpec(0.1, 1, \lin),
/*		//\durscale -> 
		ControlSpec(0.4, 0.6, \lin) */
	]
}

*initClass {
	StartUp.add{
		SynthDef(this.synthDefName, { |out=0, amp=1, 
						ampdist=1, durdist=1, adparam=1.0, ddparam=1.0, 
						minfreq=20, maxfreq=1000, ampscale= 0.5, durscale=0.5|
			var son;
			son = Gendy1.ar(ampdist, durdist, adparam, ddparam, minfreq, maxfreq, ampscale, /* durscale */ 0.5);

			/*
			ampdist .poll(10, "ampdist");
			durdist .poll(10, "durdist");
			adparam .poll(10, "adparam");
			ddparam .poll(10, "ddparam");
			minfreq .poll(10, "minfreq");
			maxfreq .poll(10, "maxfreq");
			ampscale.poll(10, "ampscale");
			durscale.poll(10, "durscale");
			
			ampdist .poll(HPZ1.kr(ampdist).abs>0, "ampdist");
			durdist .poll(HPZ1.kr(durdist).abs>0, "durdist");
			adparam .poll(HPZ1.kr(adparam).abs>0, "adparam");
			ddparam .poll(HPZ1.kr(ddparam).abs>0, "ddparam");
			minfreq .poll(HPZ1.kr(minfreq).abs>0, "minfreq");
			maxfreq .poll(HPZ1.kr(maxfreq).abs>0, "maxfreq");
			ampscale.poll(HPZ1.kr(ampscale).abs>0, "ampscale");
			durscale.poll(HPZ1.kr(durscale).abs>0, "durscale");
			*/
			
			//Amplitude.ar(son).poll(1);
			
			son = son.softclip; // Because the amplitude sometimes goes mental
			Out.ar(out, son * amp);
		}).writeDefFile
	}
}

*guiColour{
	^"0xff6699ff"
}

}
