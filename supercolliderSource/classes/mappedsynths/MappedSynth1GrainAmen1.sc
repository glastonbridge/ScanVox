/**
* (c) 2010 Dan Stowell. Released under the GPLv3.
*/
MappedSynth1GrainAmen1 : MappedSynth1 {
/*
s.waitForBoot{MappedSynth1GrainAmen1.new.wander}
m = MappedSynth1GrainAmen1.new
m.amenbuf
m.extraArgs
m.wander
m.amenbuf.play
m.launchSynth
*/

var <amenbuf;

init { |argserver|
	super.init(argserver);
	amenbuf = Buffer.read(server, "sounds/amen*.wav".pathMatch.first);
}

extraArgsIndex {	// In practice, the name of the first extraarg. This allows us to push all the extraArgs to them using arrayed args.
	^\bufnum
}
extraArgs {
	^[amenbuf.bufnum]
}

*synthDefName{
	^\_maptsyn_grainamen1
}

*probeTime {
	^0.3
	// just cos i think there's a bit of lag in what goes on inside the granulator
}
*timbreMedianRange {
	^6 
	// average over 6 frames since this synth is quite nonstationary
}


*synthDefParamLabels{
	^#[\phasegross, \phasefine, \trate]
}
*synthDefParams{
	^[
		//\phasegross  -> 
		ControlSpec(0, 0.95, \lin),
		//\phasefine  -> 
		ControlSpec(0, 0.05, \lin),
		//\trate  -> 
		ControlSpec(16, 120, \exp),
	]
}

*initClass {
	StartUp.add{
		SynthDef.writeOnce(this.synthDefName, { |out=0, amp=1,
						// mapped:
						phasegross=0.5, phasefine=0.05, trate=50,
						// extraArgs:
						bufnum=0
						|
			var phase, son, clk, pos, dur;
			
			dur = 12 / trate;
			clk = Impulse.kr(trate);
			pos = (phasegross + phasefine) * BufDur.kr(bufnum) + TRand.kr(0, 0.01, clk);
			
			son = TGrains.ar(2, clk, bufnum.poll(0.1, label:"amenbuf"), 1.25, pos, dur, 0, interp: 0)[0];
			
			Out.ar(out, son * (amp * 20));
		})
	}
}

*guiColour{
	^0xff9966ff
}

}
