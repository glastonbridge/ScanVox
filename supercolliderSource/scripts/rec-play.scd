//Server.default = s = Server.local;

// synthdefs for recording and playing back audio go here

// "recording" synthdef - takes input from mic and writes data to multichannel buffer
SynthDef(\_scanvox_rec, {  |timbrebuf=0, clockbus=0|
	var tplsound, chain, chaintrig, ana, db, logtrig;
	
	tplsound = SoundIn.ar();
	
	// timbre analysis
	chain = FFT(LocalBuf(1024), tplsound, wintype: 1);
	chaintrig = chain > -0.5;
	ana = FeatureSet1.scanvox1.graph.value(chain, tplsound);
	
	// this bit sends decibel amplitude values back to clients for monitoring:
	db = ana[1].max(0.001).ampdb; // NB 0.001 limits to -60db floor
	SendTrig.kr(PulseDivider.kr(chaintrig, 4), 7, db); // 7 is arbitrary ID for these db trigs
	//db.poll(chaintrig, label: "db");
	//db.poll(PulseDivider.kr(chaintrig, 4), label: "db pulsediv");
	// trigger pulse-divided by 4 makes about 21 Hz of sending triggers, at 44kHz that is
	
	// We also grab the clock position at the moment we very start, and send that home so we can be positioned on the grid
	SendTrig.kr(Impulse.kr(0), 5, In.kr(clockbus)); // 5 is arbitrary ID for these clock trigs
	
	logtrig = Logger.kr(ana, chaintrig, timbrebuf);
	FreeSelf.kr(logtrig < 0.5); // Logger output goes from 1 to 0 when it's full
}).writeDefFile;


(1..9).do{|numparams|
	
	// "playback" synthdef - takes multichannel buffer and writes controls to some bus.
	// Assumes that it resets its playback position to the start whenever the clockbus contains a value matching myphase.
	SynthDef(("_scanvox_playcontrols"++numparams).asSymbol, { |timbrebuf=0, treebuf=0, trevbuf=0, 
				controlsbus=0, ampbus=0, trigbus=0, paramShouldBePitch= -1|
		var ana, amp, traj, index, controls, resettrig, bufferIsDone;
		
		resettrig = In.kr(trigbus);
		resettrig.poll(resettrig, label: "clock MATCH (recv in playcontrols)");
		
		ana = PlayBuf.kr(FeatureSet1.scanvox1.list.size, timbrebuf, (SampleRate.ir / ControlRate.ir) / 512, resettrig);
		bufferIsDone = Done.kr(ana);
		
		amp = ana[1];
		amp = amp * SetResetFF.kr(resettrig); // silence amplitude until first trigger caught
//TO REMOVE (USE PAUSING INSTEAD)		amp = amp * (bufferIsDone < 0.1); // silence amplitude when done
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
