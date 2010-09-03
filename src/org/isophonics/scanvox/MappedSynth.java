package org.isophonics.scanvox;

/**
* The MappedSynth class represents a sounding synthesiser, 
* originally defined in SuperCollider. The subclasses of this
* will be auto-generated from SuperCollider code so as to 
* pass details through from the synthdef-generation to the Java.
*/
public abstract class MappedSynth {
	public abstract int getNumControls();
	public abstract int getParamShouldBePitch();
	public abstract String getLabel();
}
