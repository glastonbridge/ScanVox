/*
  This file is part of ScanVox
  (c) 2010 Queen Mary University of London

    ScanVox is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ScanVox is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ScanVox.  If not, see <http://www.gnu.org/licenses/>.
*/
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
    private int supervisorNode=-1;
    private int playNode=-1;
    private int synthNode=-1;
    private int ampMatchNode=-1;
    private int recordBuffer=-1;
    private int trigBus=-1;
    private Allocator nodeAllocator, bufferAllocator, krBusAllocator;
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
    		Allocator krBusAllocator,
    		MappedSynth synthType, 
    		int ampArrayLen) {
    	this.nodeAllocator = nodeAllocator;
    	this.bufferAllocator = bufferAllocator;
    	this.krBusAllocator = krBusAllocator;
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
	public int getPlayGroupNode()  { if (playGroupNode  == -1) playGroupNode  = nodeAllocator.nextID(); return playGroupNode ; }
	public int getSupervisorNode() { if (supervisorNode == -1) supervisorNode = nodeAllocator.nextID(); return supervisorNode; }
	public int getPlayNode()       { if (playNode       == -1) playNode       = nodeAllocator.nextID(); return playNode      ; }
	public int getSynthNode()      { if (synthNode      == -1) synthNode      = nodeAllocator.nextID(); return synthNode     ; }
	public int getAmpMatchNode()   { if (ampMatchNode   == -1) ampMatchNode   = nodeAllocator.nextID(); return ampMatchNode  ; }
	public int getRecordBuffer() {
		if (recordBuffer == -1) recordBuffer = bufferAllocator.nextID();
		return recordBuffer;
	}
	public int getTrigBus() {
		if (trigBus == -1) trigBus = krBusAllocator.nextID();
		return trigBus;
	}
	public void pushDbampValue(float val){
		int index = dbampAllocator.nextID();
		if (index<dbamps.length){ // NB silent fail if too many values...
			dbamps[index] = val;
			intamps[index] = Math.max(0, Math.min(maxIntAmp, (int)((val + 60.f) * floatToIntRescaler)));
		}
	}
}
