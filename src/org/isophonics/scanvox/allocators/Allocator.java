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
