s = Server.default = Server.local;

// Synthdefs involved with playback scheduling will live here

SynthDef("clockodile",{ arg out=0;
	Out.kr(out,LFPulse.kr(0.125,0,0.1));
}).load(s);

