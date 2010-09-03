/**
* (c) 2010 Dan Stowell. Released under the GPLv3.
*/
/*
Re-usable lists of features for computation in Synth graphs
*/
FeatureSet1 {
	var <graph, <list;
	*new {|g,l| ^super.new.init(g,l) }
	init {|g,l| graph = g; list = l}
///////////////////////////////////////////////////////////////

// FeatureSet.scanvox1.list
*scanvox1 {
^this.new({
	|chain, source|
	var pitch, zcr, pciles,
			pow, flatness, centroid, bandpows;
	
	// Now perform all our analyses
	pitch    = Pitch.kr(source, execFreq: 250).at(0);
	pow      = FFTPower.kr(chain);
	centroid = SpecCentroid.kr(chain);
	flatness = SpecFlatness.kr(chain);
	zcr      = A2K.kr(ZeroCrossing.ar(source));
	pciles   = SpecPcile.kr(chain, #[0.25, 0.95], 1);
	//bandpows = (FFTSubbandPower.kr(chain, #[50, 400, 800, 1600, 3200, 6400])[1..5] / pow.max(1e-10));
 
	
	// Output:
	// NB KEEP UN-NORMALISED [PITCH, POW] FIRST
	([pitch, pow, centroid, flatness, zcr] ++ pciles)
	// with normalisation determined from mixedvoicedata:
	* #[1.00000000, 1.00000000, 0.00098303, 21.14341256, 0.00063821, 0.00144387, 0.00035757]	- #[0.00000000, 0.00000000, 1.48499595, 1.23478075, 0.67746988, 0.59661832, 1.96464594]
},
	#["pitch", "pow", "centroid", "flatness", "zcr", "pcile25", "pcile95" /* , "pow1", "pow2", "pow3", "pow4", "pow5" */ ]
		);
}


}
