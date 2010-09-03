/**
* (c) 2010 Dan Stowell. Released under the GPLv3.
*/
MappedSynth1SuperSimple : MappedSynth1 {
/*
s.waitForBoot{MappedSynth1SuperSimple.new.wander}
*/

*synthDefName{
	^\_maptsyn_supersimple
}

*synthDefParamLabels{
	^#[\freq, \noise]
}
*synthDefParams{
	^[
		// freq
		\midfreq.asSpec,
		// noise
		ControlSpec(-1, 1, \lin)
	]
}
*paramShouldBePitch {
	^0
}

*initClass {
	StartUp.add{
		SynthDef(this.synthDefName, { |out=0, amp=1, 
						freq=440, noise=0|
			var son;
			
			//freq.poll(label: "freq");
			//noise.poll(label: "noise");
			
			son = XFade2.ar(SinOsc.ar(freq), PinkNoise.ar, noise);
			
			//Amplitude.ar(son).poll(1);
			
			Out.ar(out, son * (amp));
		}).writeDefFile
	}
}

}
