/**
* (c) 2010 Dan Stowell. Released under the GPLv3.
*/
MappedSynth1Moogy1 : MappedSynth1 {
/*
s.waitForBoot{MappedSynth1Moogy1.new.wander}
*/

*synthDefName{
	^\_maptsyn_moogy1
}

*synthDefParamLabels{
	^#[\freq, \noise, \dust, \filtfreq, filtgain]
}
*synthDefParams{
	^[
		// freq
		\midfreq.asSpec,
		// noise
		ControlSpec(0, 1, \lin),
		// dustiness
		ControlSpec(0, 1, \lin),
		// filtfreq
		\freq.asSpec,
		// filtgain
		ControlSpec(0, 3.5, \lin)
	]
}
*paramShouldBePitch {
	^0
}

*initClass {
	StartUp.add{
		SynthDef(this.synthDefName, { |out=0, amp=1, 
						freq=440, noise=0.1, dustiness=0.1, filtfreq=1000, filtgain=1|
			var son;
			
			//freq.poll(HPZ1.kr(freq).abs, "freq changed to");
			//In.kr(0).poll(10, "MOOGYfreq as expected from control bus zero");
			//In.kr(46).poll(10, "MOOGYfreq as expected from control bus 46");
			//freq.poll(10, "MOOGYfreq");
			//noise.poll(10, "MOOGYnoise");
			
			son = MoogFF.ar(Saw.ar(freq) * PinkNoise.ar.range(1 - noise, 1) 
					+ Dust2.ar(dustiness), filtfreq, filtgain);
			
			//Amplitude.ar(son).poll(1);
			
			Out.ar(out, son * (amp * 2.8));
		}).writeDefFile
	}
}

*guiColour{
	^"0xff66ff99"
}

}
