MappedSource1 {
/*
See also
MappedSynth1

MappedSource1 is the main superclass for a signal source that can be analysed to create a timbre space.

Its purpose is to take some mono audio file via DiskIn and analyse that. (Functionality not included in this scanvox excerpt, at time of writing.)

It can then take some input (e.g. live audio in), and on the assumption that it's a similar kind of signal, applies a previously-calculated transformation. (You'd do this in a subclass though.)

It is ALSO the superclass of MappedSynth1, which contains a SynthDef with specified parameters and parameter ranges.
*/

	classvar <fftsize=1024, // MFCC analysis requires 1024 frames @ 44100 Hz!
			<ffthop=0.5,
			<>featuresetindices=nil // We can use ONLY SOME COLUMNS - set to nil to use all, otherwise a list of indices
			;

// most should NOT call this directly; rather, use featurenames and featuregraph which will correctly select indices.
// (analyseAudioFile does analyse all features)
*featureset {
	^FeatureSet1.scanvox1
}

*featurenames {
	^if(featuresetindices.isNil){
		this.featureset.list
	}{
		this.featureset.list[featuresetindices]
	}
}

*featuregraph{ |chain, source|
	^if(featuresetindices.isNil){
		this.featureset.graph.value(chain, source)
	}{
		this.featureset.graph.value(chain, source)[featuresetindices]
	}
}

numfeatures  { ^this.class.numfeatures } // for convenience
*numfeatures { ^this.featurenames.size }
*featurenamesALL { ^this.featureset.list }
*numfeaturesALL { ^this.featureset.list.size }


*initClass{
	this.writeMappedSource1SynthDefs;
}

*writeMappedSource1SynthDefs{
	StartUp.add{
	
/*
	/////////////////////////////////////////////////////////////////////////
	// TreeRd synthdef
	// Takes planetree buffer ref, plus all the kr busses written by the timbre analyser.
	// Outputs the classification index (integer for reading from the revmap)
	SynthDef(\_maptsrc_treerd, { |out=0, treebuf=0, featuresbus=0, triginbus=0|
		var trig     = In.kr(triginbus,  1);
		var features = In.kr(featuresbus, this.numfeatures);
		var coords = PlaneTree.kr(treebuf, features, gate: trig) - 1; /* NB THE MINUS ONE */
		Out.kr(out, coords);
	}).writeDefFile;
*/
	// This one is to help hardwire the AMPLITUDE when controlling a synth.
	// Reads "target" single-channel bus, and writes it back out, after coercing its amplitude to match that in "source".
	SynthDef(\_maptsrc_ampmatch, {|source=0, target=0|
		var sig = In.ar(target);
		var srcamp = Amplitude.ar(In.ar(source),0.01, 0.1).max(0.00001);
		var sigamp = Amplitude.ar(sig          ,0.01, 0.1).max(0.00001);
		var ratio = (srcamp / sigamp);
		sig = sig * ratio;
// DEAC		CheckBadValues.ar(sig, 765);
		ReplaceOut.ar(target, sig);
	}).writeDefFile;
	
	}
} // end *writeMappedSource1SynthDefs

/*
MappedSource1.trashSynthDefs
*/
*trashSynthDefs {
	("rm" + "%_mapts*.scsyndef".format(SynthDef.synthDefDir).escapeChar($ )).postln.unixCmd;
}

}// end class
