//s = Server.default = Server.local;

// Synthdefs involved with playback scheduling will live here

// a full 'bar' takes size/rate seconds to complete
SynthDef("clockodile",{ arg out=0, rate=1, size=16, tr_uid=24;
       var trig, phase;

       trig = Impulse.kr(rate);
       phase = LFSaw.kr(rate/size, 1).range(0,1);

       SendTrig.kr(trig, tr_uid, phase);

       Out.kr(out, phase);
}).writeDefFile;


// for a PlayingSound, this listens to the clock and pauses/unpauses/resets the players
SynthDef(\_scanvox_supervisor, { | clockbus=0, myphase=0, timbrebuf=0, tgtgroupid=0, trigbus=0 |
	var clock, resettrig, duration, activeness;
	clock = In.kr(clockbus);
	//clock.poll(clock, label: "clock (recv in supervisor)");

	resettrig = Trig1.kr((clock - myphase).wrap(0,1) < (1/16), 0);
	clock.poll(resettrig, label: "clock MATCH (recv in supervisor)");

	// We calculate the awake duration from the timbrebuf
	duration = BufFrames.ir(timbrebuf) * SampleDur.ir * 512;

	activeness = Trig1.kr(resettrig, duration);

	// sleep/wake the synths
	Pause.kr(activeness, tgtgroupid);

	// tell the playbacker to go back to the start of its buf
	Out.kr(trigbus, resettrig);

}).writeDefFile;



"timing.scd done".postln;
