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
package org.isophonics.scanvox.allocators;


/*
 * Simple allocator for busses/nodes
 * Naive in that it doesn't reuse IDs when freed, it simply ups and ups the index
 * 
 * @author Dan
 */
public class NaiveAllocator implements Allocator {
	private int startID;
	private int nextID;
	
	public NaiveAllocator(){
		this(0);
	}
	public NaiveAllocator(int startID){
		this.startID = startID;
		nextID = startID;
	}
	
	public int nextID() {
		return nextIDs(1);
	}
	public int nextIDs(int numToAlloc){
		int ret = nextID;
		nextID += numToAlloc;
		return ret;
	}
	public boolean reset(){
		nextID = startID;
		return true;
	}
}
