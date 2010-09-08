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
