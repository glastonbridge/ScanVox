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

"timing.scd done".postln;
