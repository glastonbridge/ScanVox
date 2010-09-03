MappedSynth1AY1 : MappedSynth1 {
/*
s.waitForBoot{MappedSynth1AY1.new.wander}
*/

*synthDefName{
	^\_maptsyn_ay1
}

*synthDefParamLabels{
	^#[\control, \noise, \freq]
}
*synthDefParams{
	^[
		// "control":
		#[1, 8, 9],
		// "noise: 
		ControlSpec(0, 31, \lin, 1),
				// "tonea":
				//ControlSpec(0, 4095, \lin, 1)
		// "freq" (feeds into tonea):
		\midfreq.asSpec
	]
}
*paramShouldBePitch {
	^2
}

*initClass {
	StartUp.add{
		SynthDef.writeOnce(this.synthDefName, { |out=0, amp=1, 
						control=1, noise=15, freq=440|
			var son, cbv;
			son =  AY.ar(
				control:	control,
				noise:	noise,
				tonea:	AY.freqtotone(freq).round,
				toneb: 0,
				tonec: 0,
				vola: 15,
				volb: 0,
				volc: 0
			);
			
//			cbv = CheckBadValues.ar(son);
//			son.poll(cbv, "************************
//AY PRODUCED BAD VALUE");
//			son = if(cbv>0, 0, son);
			//Amplitude.ar(son).poll(1);
			
			Out.ar(out, son * (amp * 2.8));
		})
	}
}

}
