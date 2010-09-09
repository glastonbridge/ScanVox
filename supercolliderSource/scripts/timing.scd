//s = Server.default = Server.local;

// Synthdefs involved with playback scheduling will live here

SynthDef("clockodile",{ arg out=0, rate=1, size=16, tr_uid=24;
       var trig, phase;

       trig = Impulse.kr(rate);
       phase = Stepper.kr(trig,0,0,15,1);

       SendTrig.kr(trig, tr_uid, phase);

       Out.kr(out, phase);
}).writeDefFile;

"timing.scd done".postln;
