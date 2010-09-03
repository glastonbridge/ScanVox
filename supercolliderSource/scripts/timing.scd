//s = Server.default = Server.local;

// Synthdefs involved with playback scheduling will live here

SynthDef("clockodile",{ arg out=0, rate=1;
       var trig, phase, sig;

       trig = Impulse.kr(rate);
       phase = Stepper.kr(trig, max: trig);
       sig = trig * phase;

       sig.poll(trig, "clockodile");

       Out.kr(out, sig);
}).writeDefFile;

"timing.scd done".postln;
