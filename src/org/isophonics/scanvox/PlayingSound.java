package org.isophonics.scanvox;

import java.util.Arrays;

import org.isophonics.scanvox.allocators.Allocator;
import org.isophonics.scanvox.allocators.NaiveAllocator;

/**
 * Contains runtime-created data about a synthesizer, and
 * provides basic accessors to control it through supercollider
 * @author alex
 *
 */
class PlayingSound {
    private int recordNode=-1;
    private int playGroupNode=-1;
    private int playNode=-1;
    private int synthNode=-1;
    private int ampMatchNode=-1;
    private int recordBuffer=-1;
    private Allocator nodeAllocator, bufferAllocator;
    protected int length; //ms
    protected MappedSynth synth;
    protected boolean isValid = false; // SoundManager will set this true on completion
    protected float[] dbamps; // amplitudes in decibels reported by the server, expected to be -60 to 0
    protected int[] intamps;  // amplitudes converted to integer useful for visualising
    public static final int   maxIntAmp = 100;
    public static final float floatToIntRescaler = ((float)maxIntAmp) / 60.f;
    private NaiveAllocator dbampAllocator;
	protected float phase = 0;
    public PlayingSound(
    		Allocator nodeAllocator, 
    		Allocator bufferAllocator, 
    		MappedSynth synthType, 
    		int ampArrayLen) {
    	this.nodeAllocator = nodeAllocator;
    	this.bufferAllocator = bufferAllocator;
    	this.synth = synthType;
    	
    	this.dbampAllocator = new NaiveAllocator(0);
    	this.dbamps = new float[ampArrayLen];
    	Arrays.fill(this.dbamps, -60.f); // TODO - maybe we don't need to store the dbamps long-term?
    	this.intamps = new int[ampArrayLen];
    	Arrays.fill(this.intamps, 0);
    }
	public int getRecordNode() {
		if (recordNode == -1) recordNode = nodeAllocator.nextID();
		return recordNode;
	}
	/*
	 * The synths that do the sound synthesis must be placed inside the playGroupNode, 
	 * so they can be paused/unpaused as one.
	 * The supervisor (which does the pausing/unpausing) must be outside (before) this group!
	 */
	public int getPlayGroupNode() {
		if (playGroupNode == -1) playGroupNode = nodeAllocator.nextID();
		return playGroupNode;
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
	public void pushDbampValue(float val){
		int index = dbampAllocator.nextID();
		if (index<dbamps.length){ // NB silent fail if too many values...
			dbamps[index] = val;
			intamps[index] = Math.max(0, Math.min(maxIntAmp, (int)((val + 60.f) * floatToIntRescaler)));
		}
	}
}
