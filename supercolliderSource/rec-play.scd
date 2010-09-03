//Server.default = s = Server.local;

// synthdefs for recording and playing back audio go here
//
// These synths' days are numbered!  Soon we will be using Dan's cool synths

SynthDef("playbuffer",{ arg out=0, buffnum, trigger, startPos;
var bufsnd;
bufsnd = PlayBuf.ar(1,buffnum,BufRateScale.kr(buffnum),loop:1, trigger:trigger, startPos:startPos);
Out.ar(0,bufsnd);
}).writeDefFile;

SynthDef("recordbuffer",{ arg out=0, buffnum;
RecordBuf.ar(SoundIn.ar(),buffnum, loop:0, doneAction: 0);
}).writeDefFile;

////////////////////////////////////////////////////////////////////////////////////////////////////////
// Dan's cool synths:

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
	SynthDef(("_scanvox_playcontrols"++numparams).asSymbol, { |timbrebuf=0, treebuf=0, trevbuf=0, controlsbus=0, clockbus=0, myphase=1, paramShouldBePitch= -1|
		var clock, ana, traj, index, controls, resettrig;
		
		clock = In.kr(clockbus).poll(label: "clock");
		
		resettrig = (clock - myphase).abs < 0.1;
		
		ana = PlayBuf.kr(FeatureSet1.scanvox1.list.size, timbrebuf, (SampleRate.ir / ControlRate.ir) / 512, resettrig);
		
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
