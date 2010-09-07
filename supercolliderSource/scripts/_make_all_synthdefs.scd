#!/usr/bin/env sclang

~dir = thisProcess.nowExecutingPath.standardizePath.dirname;

if(\FeatureSet1.asClass.isNil){
	"ERROR in %: Scanvox classes not in the classpath (FeatureSet1 not found).".format(thisProcess.nowExecutingPath).postln;
	exit(1);
}{
	
	// execute the code
	"Generating SynthDefs".postln;
	["rec-play", "timing"].do{|which|
		(~dir +/+ which ++ ".scd").load
	};
	
	// generate some java files
	"Generating Java files".postln;
	MappedSynth1.genJava(~dir.dirname.dirname +/+ "gen/");
	
	// now copy the resulting synthdefs into the assets
	"Copying synthdefs into assets folder".postln;
	("cd % && for f in _scanvox*.scsyndef"
		+ "clockodile.scsyndef"
		+ "_maptsyn_supersimple.scsyndef _maptsyn_moogy1.scsyndef _maptsyn_gendy1.scsyndef"
		+ "_maptsyn_ay1.scsyndef _maptsrc_ampmatch.scsyndef;"
		+ "do cp $f %/assets/m$f; done")
		.format(SynthDef.synthDefDir.quote, ~dir.dirname.dirname.quote)
		.postln.unixCmd;
	
	// quit if called from bare sclang
	"% is finished. Exiting.".format(thisProcess.nowExecutingPath.basename).postln;
	if(Platform.ideName.postln == "none"){ 
		exit(0) ;
	};
}
