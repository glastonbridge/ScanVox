//Server.default = s = Server.local;

// synthdefs for recording and playing back audio go here

// "recording" synthdef - takes input from mic and writes data to multichannel buffer
SynthDef(\_scanvox_rec, {  |timbrebuf=0|
	var tplsound, chain, chaintrig, ana;
	
	tplsound = SoundIn.ar();
	
	// timbre analysis
	chain = FFT(LocalBuf(1024), tplsound, wintype: 1);
	chaintrig = chain > -0.5;
	ana = FeatureSet1.scanvox1.graph.value(chain, tplsound);
	Logger.kr(ana, chaintrig, timbrebuf);
	Line.kr(1,1,10, doneAction: 2) // bleh
}).writeDefFile;


(1..9).do{|numparams|
	
	// "playback" synthdef - takes multichannel buffer and writes controls to some bus.
	// Assumes that it resets itself to zero whenever the clockbus contains a value matching myphase.
	SynthDef(("_scanvox_playcontrols"++numparams).asSymbol, { |timbrebuf=0, treebuf=0, trevbuf=0, 
				controlsbus=0, ampbus=0, clockbus=0, myphase=1, paramShouldBePitch= -1|
		var clock, ana, amp, traj, index, controls, resettrig;
		
		clock = In.kr(clockbus);
		clock.poll(clock, label: "clock (recv in playcontrols)");
		
		resettrig = (clock - myphase).abs < 0.1;
		clock.poll(resettrig, label: "clock MATCH (recv in playcontrols)");
		
		ana = PlayBuf.kr(FeatureSet1.scanvox1.list.size, timbrebuf, (SampleRate.ir / ControlRate.ir) / 512, resettrig);
		
		amp = ana[1];
		Out.kr(ampbus, amp);
		
		traj = ana[2..];
		index = PlaneTree.kr(treebuf, traj, 1) - 1; /* NB THE MINUS ONE */
		controls = BufRd.kr(numparams, trevbuf, index, loop: 1, interpolation: 1);
		
		// hardwiring the pitch:
		controls = controls.size.collect{|itemindex|
			Select.kr( ((paramShouldBePitch-itemindex).abs<0.1), [controls[itemindex], ana[0]] )  
		};
		
		Out.kr(controlsbus, controls);
	}).writeDefFile;

};

// This one is to help hardwire the AMPLITUDE when controlling a synth.
// Reads "soundsource" single-channel bus, and writes it to "out", 
// after coercing its amplitude to match that being read from the control-rate ampbus.
SynthDef(\_scanvox_ampmatch, {|out=0, soundsource=0, ampbus=0|
	var sig = In.ar(soundsource);
	var srcamp = In.kr(ampbus)             .max(0.00001);
	var sigamp = Amplitude.ar(sig,0.1, 0.1).max(0.00001);
	var ratio = (srcamp / sigamp);
	sig = sig * ratio;
// DEAC		CheckBadValues.ar(sig, 765);
	Out.ar(out, sig);
}).writeDefFile;


"rec-play.scd done".postln;
