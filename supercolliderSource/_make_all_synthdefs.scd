
~dir = thisProcess.nowExecutingPath.dirname;

// execute the code
["rec-play", "timing"].do{|which|
	(~dir +/+ which ++ ".scd").load
};

// now copy the resulting synthdefs into the assets
"cp %_scanvox*.scsyndef %/assets".format(SynthDef.synthDefDir.quote, ~dir.dirname.quote).postln.unixCmd;



// quit if called from bare sclang
//if(Platform.ideName.postln == "none"){ 
//	exit(0) ;
//};
