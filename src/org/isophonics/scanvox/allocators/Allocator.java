package org.isophonics.scanvox.allocators;

public interface Allocator {
	/*
	 * Get a single ID. Should be equivalent to calling @nextIDs(1)
	 */
	public int nextID();
	/*
	 * Get a contiguous block of IDs
	 */
	public int nextIDs(int numToAlloc);
	/*
	 * Discard all IDs and back to the start
	 */
	public boolean reset();
}
