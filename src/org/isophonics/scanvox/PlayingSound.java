package org.isophonics.scanvox;

/**
 * Contains runtime-created data about a synthesizer, and
 * provides basic accessors to control it through supercollider
 * @author alex
 *
 */
class PlayingSound {
    private int recordNode=-1;
    private int playNode=-1;
    private int synthNode=-1;
    private int ampMatchNode=-1;
    private int recordBuffer=-1;
    private Allocator nodeAllocator, bufferAllocator;
    protected MappedSynth synth;
    protected boolean isValid = false; // SoundManager will set this true on completion
    public PlayingSound(
    		Allocator nodeAllocator, 
    		Allocator bufferAllocator, 
    		MappedSynth synthType) {
    	this.nodeAllocator = nodeAllocator;
    	this.bufferAllocator = bufferAllocator;
    	this.synth = synthType;
    }
	public int getRecordNode() {
		if (recordNode == -1) recordNode = nodeAllocator.nextID();
		return recordNode;
	}
	public int getPlayNode() {
		if (playNode == -1) playNode = nodeAllocator.nextID();
		return playNode;
	}
	public int getSynthNode() {
		if (synthNode == -1) synthNode = nodeAllocator.nextID();
		return synthNode;
	}
	public int getRecordBuffer() {
		if (recordBuffer == -1) recordBuffer = bufferAllocator.nextID();
		return recordBuffer;
	}
	public int getAmpMatchNode() {
		if (ampMatchNode == -1) ampMatchNode = nodeAllocator.nextID();
		return ampMatchNode;
	}
}
