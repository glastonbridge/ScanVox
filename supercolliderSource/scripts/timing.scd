//s = Server.default = Server.local;

// Synthdefs involved with playback scheduling will live here

SynthDef("clockodile",{ arg out=0, rate=1, size=16;
       var trig, phase;

       trig = Impulse.kr(rate);
       phase = Stepper.kr(trig,0,0,15,1);

       phase.poll(phase, "clockodile");

       Out.kr(out, phase);
}).writeDefFile;

"timing.scd done".postln;
