MappedSynth1 : MappedSource1 {
/* ABSTRACT CLASS
Like MappedSource1, but represents a source we're generating ourselves, and so we can know what the control parameters are.
We can store these parameters, then reverse-map them later.

What you would do to map a new synth:
 - Subclass this thing, make it write a SynthDef, and override synthDefName
 - Train the map
 - Add the mapping data to your subclass so it can become a translator

This class knows:
 - The synthdef name
 - The number of parameters it takes
 - The values of the parameters that we would probe (using ControlSpec's for continuous, otherwise an array of discrete vals)

*/

var server;

////////////////////////////////////////////////////////////////////////////////////
// Overridable stuff:

*synthDefName{
	// override this in subclass and return a symbol for the synthdef name
	^this.subclassResponsibility;
}
*synthDefParamLabels{
	^this.subclassResponsibility; // pls return an array of symbols
}
synthDefParams{
	^this.class.synthDefParams // convenience
}
*synthDefParams{
	// override this in subclass and return something like:
	/*
	^[
		ControlSpec(100, 4000, \exponential),
		ControlSpec(  0,    1, \linear),
		#[ 1, 3, 8, 14] // For arbitrary discrete-valued control. duplicate entries could theoretically be used to bias tendency to choose.
	]
	*/
	// Note: here we ignore the compulsory first TWO params in the SynthDef: "out=0, amp=1"
	^this.subclassResponsibility;
}

// If one of the params is a "freq" or "pitch" input then often you don't want an approx
// pitch derived from remapping. Instead you want the *actual* pitch value. If so, override
// this to return the index in *synthDefParams of the input which should really be driven 
// using pitch.
*paramShouldBePitch {
	^nil
}

// If the synth instance should have further args (AFTER the mapped ones) it can return them.
extraArgs {
	^nil
}
extraArgsIndex {	// In practice, the name of the first extraarg. This allows us to push all the extraArgs to them using arrayed args.
	^nil
}

// The interval that should elapse between different settings.
// If 50ms is not enough time for the synthesis process to settle down after param change, make this longer.
// At normal settings, this will be an epoch spanned by 3 or 4 FFT frames, which is fine.
*probeTime {
//	^0.3       // still far too long
//	^2       // ridiculously long
	//^0.05
	^0.1   // about what I think it should take
	//^0.15   // slower, to see if mapping is better
}

// If a particular synth is very nonstationary then we override here
// to average out the timbre measurements over a few frames (and lengthen the probeTime too).
// You'll know this needs to be increased if a synth gives poor map-to-self results (but others don't).
*timbreMedianRange {
	^1
}

////////////////////////////////////////////////////////////////////////////////////
// Instantiation

*new { |server|
	^super.new.init(server);
}
init { |argserver|
	server = argserver ?? {Server.default};
}

// Launches a synth, correctly setting the extraArgs, and also setting the mappedVals if you like:
launchSynth { |out=0, amp=1, target, addAction=\addToHead, mappedvals |
	target = target ?? server;
	if(mappedvals.notNil and:{mappedvals.size != this.synthDefParams.size}){
		^Error("launchSynth: mappedvals.size != this.synthDefParams.size. % != %"
						.format(mappedvals.size, this.synthDefParams.size)).throw
	};

	^Synth(this.class.synthDefName, 
		// Construct argument list:
		if(mappedvals.notNil){
			[\out, out, \amp, [amp] ++ mappedvals]
		}{
			[\out, out, \amp, amp] 
		}.postln.postln.postln
			++ (this.extraArgsIndex !? [this.extraArgsIndex, this.extraArgs])
			, target, addAction);
}

////////////////////////////////////////////////////////////////////////////////////
// Instance methods:


// Generates a list of indices into the possible settings, trimmed to the limits if given
/*
MappedSynth1Gendy1.makeSettingsIndices(10)
MappedSynth1Gendy1.makeSettingsIndices(10).collect{|i| MappedSynth1Gendy1.settingsFromIndex(i, 10)}.do(_.postln)
*/
*makeSettingsIndices { |resolution=20, proportion=1, maxnumsettings=10000|
	var numsettings;
	numsettings = this.numSettings(resolution) * proportion;
	if(numsettings > maxnumsettings){
		"makeSettingsIndices: numsettings of % is too many, clipping to %".format(numsettings, maxnumsettings).warn;
		numsettings = maxnumsettings;
	};
	if(numsettings < 1){
		"makeSettingsIndices: numsettings of % indicates numerical error, fixing to %".format(numsettings, maxnumsettings).warn;
		numsettings = maxnumsettings;
	};
	^(0..this.numSettings(resolution)).scramble[0..numsettings-1]
}


// Creates the synth, then randomly changes the parameters according to the ControlSpec's
wander { | resolution=20, rate=2, target, addAction=\addToHead, post=true |
	var paramsynth, parambus, synth, fxsynth;//, task;
	target = if(target.isNil){server}{target}.asTarget;
	
	synth      = this.launchSynth(0, 1, target, addAction);
	
	fxsynth = {ReplaceOut.ar(1, (In.ar(0, 1))) }.play(target: synth, addAction: \addAfter); // silly way to make mono into stereo
	
	parambus = Bus.control(server, this.class.synthDefParams.size + 2);
	
	paramsynth = this.class.randParamSynth(resolution, parambus, rate).play(target: synth, addAction: \addBefore);
	
	Task{
		1.0.wait;
		server.sync;
		// Only NOW do we hook up the wanderer to the synth
		"wandering".postln;
		synth.busMap(2, 
				parambus.subBus(2, this.class.synthDefParams.size)
				//RM Bus.new(\control, parambus.index + 2, this.class.synthDefParams.size)
				, server);
	}.play(AppClock);
	
	//^[synth, fxsynth, task];
	^[paramsynth, parambus, synth, fxsynth];
}

pushExtraArgs{ |tothissynth|
	if(this.extraArgs.notNil){
		tothissynth.set(2 + this.class.synthDefParams.size, this.extraArgs);
	};
}

////////////////////////////////////////////////////////////////////////////////////
// Class methods
// For each param in *synthDefParams, generates a random value mapped on the spec
*randParams { |resolution=20|
	^this.synthDefParams.collect{|spec|
		if(spec.isArray){
			spec.choose
		}{
			spec.value.map((resolution + 1).rand / resolution)
		}
	};
}

// Given the list of discrete and continuous params, how many combinations are possible?
*numSettings { |resolution=20|
	var num = this.synthDefParams.product{|spec|
		if(spec.isArray){ spec.size }{ resolution }
	};
	if(num<0){num=inf};
	^num
}

// Useful for subsampling. Given a number 0..numSettings-1 (and a resolution), return the corresponding settings
/*
MappedSynth1SuperSimple.numSettings.do{|i| MappedSynth1SuperSimple.settingsFromIndex(i).postln}
MappedSynth1AY1.numSettings.do{|i| MappedSynth1AY1.settingsFromIndex(i).postln}
MappedSynth1Gendy1.numSettings.do{|i| MappedSynth1Gendy1.settingsFromIndex(i).postln}
*/
*settingsFromIndex { |index, resolution=20|

	/*
	The index is understood as being index0 + (index1 * numindices0) + (index2 * numindices1 * numindices0) ...
	So we can find the value of index0 by taking (index % numindices0).
	Then  can find the value of index1 by taking ((index/numindices0) % numindices1)...
	*/
	var numindiceseach, anindex;
	^this.synthDefParams.collect{ |spec|
		numindiceseach = if(spec.isArray){ spec.size }{ resolution };
		anindex = index % numindiceseach;
		index = (index - anindex) / numindiceseach; // shrunken index will be used in next iter
		if(spec.isArray){spec[anindex]}{spec.map(anindex / (resolution - 1))};
	};
}

*randParamSynth { |resolution=20, outbus=0, rate, poll=false| // poll not yet implemented
	var demandunits, paramtrig, rectrig;
	
	^{
		// two trigger signals: params must change JUST AFTER the recording trig has fired
		rate = rate ?? {this.probeTime.reciprocal};
		"randParamSynth: rate is %".format(rate).postln;
		rectrig = Impulse.kr(rate);
		paramtrig = Delay1.kr(rectrig);
		rectrig = (rectrig * Line.kr(-1,1, this.probeTime * 4)).max(0); // force no recording at the very beginning
		
		demandunits = this.synthDefParams.collect{|spec, pindex|
			if(spec.isArray.not){
				// Use Dwhite, which is then pushed through the ControlSpec mapping
				spec.map(
					Demand.kr(paramtrig, 0, Dwhite(0, 1, inf))
				)
			}{
					Demand.kr(paramtrig, 0, Drand(spec, inf))
			}
		};
		
		Out.kr(outbus, [paramtrig, rectrig] ++ demandunits);
	}
}



*initClass{
	this.writeMappedSynth1SynthDefs;
}
*writeMappedSynth1SynthDefs{
/*
	StartUp.add{
	
	SynthDef(\_maptsyn_hearit    , { |in, out=0, amp=0.1, pan=0|
		Out.ar(out, Pan2.ar((In.ar(in)*amp), pan));
	}).writeDefFile;
	SynthDef(\_maptsyn_hearit_dly, { |in, out=0, amp=0.1, pan=0, delay=2|
		Out.ar(out, Pan2.ar(DelayC.ar(In.ar(in)*amp, delay, delay), pan));
	}).writeDefFile;
	
	} // End StartUp.add
*/
} // end *writeMappedSynth1SynthDefs

}// end class
