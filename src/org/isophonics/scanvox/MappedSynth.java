package org.isophonics.scanvox;

/**
* The MappedSynth class represents a sounding synthesiser, 
* originally defined in SuperCollider. The subclasses of this
* will be auto-generated from SuperCollider code so as to 
* pass details through from the synthdef-generation to the Java.
*/
public abstract class MappedSynth {
	/**
	 * How many controls the synth has. The actual synthdef always has TWO beforehand (outbus, amp)
	 * and then this many controls that affect the sound.
	 */
	public abstract int getNumControls();
	/**
	 * Which parameter may we hardwire as pitch? Will return -1 if none, otherwise an index into the number from @getNumControls().
	 * Remember to add two because of the (outbus, amp) controls.
	 */
	public abstract int getParamShouldBePitch();
	/**
	 * User-visible name for the synth. Short, fun, mysterious, intriguing, pithy, full of character.
	 */
	public abstract String getLabel();
	/**
	 * Name for the synthdef which makes the "synth synth" (i.e. the actual sounding one)
	 */
	public abstract String getSynthDefName();
	/**
	 * Each MappedSynth should have its own representative colour - used e.g. for the 'amber' blocks
	 */
	public abstract int getGuiColour();
	/**
	 * In reality this is the 'old' class name, used in the AIFF data file paths to distinguish the maps for the different synth sounds
	 */
	public abstract String getBufFileNameRoot();
	/**
	 * the filename for the "tree" data to be used by PlaneTree
	 */
	public String getTreeFileName(){
		return ("mixedvoicedata_" + getBufFileNameRoot() + "_tcbuf_d5m12p99.0.aiff");
	}
	/**
	 * the filename for the "reverse map" data to be indexed into by the output of PlaneTree
	 */
	public String getTrevmapFileName(){
		return ("mixedvoicedata_" + getBufFileNameRoot() + "_tcbuf_d5m12p99.trevmap1.aiff");
	}
}
