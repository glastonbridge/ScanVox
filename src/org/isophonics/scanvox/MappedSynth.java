package org.isophonics.scanvox;

/**
* The MappedSynth class represents a sounding synthesiser, 
* originally defined in SuperCollider. The subclasses of this
* will be auto-generated from SuperCollider code so as to 
* pass details through from the synthdef-generation to the Java.
*/
public abstract class MappedSynth {
	/*
	 * How many controls the synth has. The actual synthdef always has TWO beforehand (outbus, amp)
	 * and then this many controls that affect the sound.
	 */
	public abstract int getNumControls();
	/*
	 * Which parameter may we hardwire as pitch? Will return -1 if none, otherwise an index into the number from @getNumControls().
	 * Remember to add two because of the (outbus, amp) controls.
	 */
	public abstract int getParamShouldBePitch();
	/*
	 * User-visible name for the synth. Short, fun, mysterious, intriguing, pithy, full of character.
	 */
	public abstract String getLabel();
}
