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
	
	public Arrangement(int numRows) {
		for (int i=0;i<numRows; ++i) rows.add(new Row());
	}
	
	/**
	 * Represents one sound, and where it begins and ends 
	 * @author alex
	 *
	 */
	public static class Sound {
		private int startTime = 0;
		private int length = 0;
		public Sound(int s, int l) {
			if (s>0) startTime = s; // quietly force into the +ve domain 
			length = l;
			}
		// @TODO: some kind of association with the underlying sound playback
		public int getStartTime() {	return startTime; }
		public int getEndTime()   { return startTime+length; }
		public int getLength()  { return length; }
	}
	public Vector<Row> rows=new Vector<Row>();
	
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
		public Sound grabSoundAt(int needle) {
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
			Log.d(TAG,"Trying to add sound ("+newSound.startTime+","+newSound.length+")");
			if (newSound.getEndTime()>Arrangement.this.length) return false;
			int newSoundStartTime = newSound.getStartTime();
			int index = 0;
			// Do you belong right at the beginning?
			if (contents.size() == 0 ) {
				contents.add(newSound);
				return true;
			} else {
				int firstStartTime = contents.get(0).getStartTime();
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
			if (newSound.getEndTime() > contents.get(index+1).getStartTime())
				return false;
			// Well done, you have defeated the guardians!
			contents.add(index,newSound);
			return true;
		}

		/**
		 * Exposes an iterator to the underlying linkedlist.
		 */
		public Iterator<Sound> iterator() { return contents.iterator(); }
		
		
	}
}
