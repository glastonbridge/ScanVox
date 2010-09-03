package org.isophonics.scanvox;

/**
* The MappedSynth class represents a sounding synthesiser, 
* originally defined in SuperCollider. The subclasses of this
* will be auto-generated from SuperCollider code so as to 
* pass details through from the synthdef-generation to the Java.
*/
abstract class MappedSynth {
	public abstract static int getNumControls();
	public abstract static int getParamShouldBePitch();
	public abstract static String getLabel();
}
