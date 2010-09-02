Server.default = s = Server.local;

// synthdefs for recording and playing back audio go here
//
// This file's days are numbered!  Soon we will be using Dan's cool synths

SynthDef("playbuffer",{ arg out=0, buffnum, trigger, startPos;
var bufsnd;
bufsnd = PlayBuf.ar(1,buffnum,BufRateScale.kr(buffnum),loop:1, trigger:trigger, startPos:startPos);
Out.ar(0,bufsnd);
}).load(s);

SynthDef("recordbuffer",{ arg out=0, buffnum;
RecordBuf.ar(SoundIn.ar(),buffnum, loop:0, doneAction: 0);
}).load(s);

