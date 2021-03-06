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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import android.util.Log;

/**
 * The structure of the sounds as they are laid out in the loop.
 * @author alex
 *
 */
public class Arrangement {
	public static enum EventType {
		SOUND_START, SOUND_END
	}

	public static final String TAG = "Arrangement";
	
	protected int length = 16; /** The length of the loop, in ticks */
	protected int ticksPerBeat = 1;
	
	public Arrangement(int numRows) {
		for (int i=0;i<numRows; ++i) rows.add(new Row());
	}
	
	/**
	 * Represents one sound, and where it begins and ends in this arrangement
	 * @author alex
	 *
	 */
	public class Sound {
		private float startTime = 0;
		private float length = 0;
		protected PlayingSound id;
		public Sound(PlayingSound id,float s, float l) {
			this.id = id;
			startTime = s % Arrangement.this.length; 
			if(startTime<0) startTime = s + Arrangement.this.length; // quietly force into the +ve domain 
			length = l;
		}
		public float getStartTime() {	return startTime; }
		public float getEndTime()   { return startTime+length; }
		public float getLength()  { return length; }
		@Override
		public int hashCode() { return id.getRecordBuffer(); } 
		public boolean equals(Object rhs) { 
			return (rhs instanceof Sound 
					&& this.hashCode() == rhs.hashCode());
		}
		public boolean wrapsLoop() {
			return getEndTime()>Arrangement.this.length;
		}
	}
	public Vector<Row> rows=new Vector<Row>();

	public int bpm;
	
	/**
	 * Represents a row of arranged sounds.  The choice of row for a
	 * sound does not affect its playback, it is purely to make it 
	 * easier for the user to lay stuff out.
	 * 
	 * @author alex
	 *
	 */
	public class Row implements Iterable<Sound> {
		private LinkedList<Sound> contents = new LinkedList<Sound>();
		/**
		 * Grab the sound that is being played back at the given 
		 * playback time, remove it from this row and return it.
		 *  
		 * @param needle
		 * @return sound being played, null if no sound is being played
		 */
		public Sound grabSoundAt(float needle) {
			int item = 0;
			Sound result = null;
			for (Sound s : contents) {
				if (needle>=s.getStartTime()) {
					if (needle <= s.getEndTime()) result = s;
					break;
				}
				++item;
			}
			if (result != null) contents.remove(item);
			return result;
		}
		
		/**
		 * Add a sound at the right place in the row.
		 * 
		 * @TODO: I'm not particularly fond of this algorithm but I
		 * can't think of anything better right now.
		 * 
		 * @return false if the sound cannot currently fit in that
		 * place.
		 */
		public boolean add(Sound newSound) {
			// Hello new sound, do you fit into this arrangement?
			if (newSound.getStartTime()>Arrangement.this.length) return false;
			float newSoundStartTime = newSound.getStartTime();
			int index = 0;
			// Do you belong right at the beginning?
			if (contents.size() == 0 ) {
				contents.add(newSound);
				return true;
			} else {
				float firstStartTime = contents.get(0).getStartTime();
				if (firstStartTime <= newSoundStartTime) {
					if (firstStartTime < newSound.getEndTime()) {
						return false;
					} else {
						// You fit!
						contents.add(0,newSound);
						return true;
					}
				}
			}
			// Who do you come after?
			for (Sound s : contents) {
				if (newSoundStartTime >= s.getEndTime()) {
					++index;
					break;
				}
				++index;
			}
			// Do you fit in front of the thing you come before?
			if ( contents.size()>index+1
			  && newSound.getEndTime() > contents.get(index+1).getStartTime())
				return false;
			// Well done, you have defeated the guardians!
			contents.add(index,newSound);
			return true;
		}

		/**
		 * Exposes an iterator to the underlying linkedlist.
		 */
		public Iterator<Sound> iterator() { return contents.iterator(); }
		
		public boolean isEmpty() {
			return contents.isEmpty();
		}
		
		
	}
}
