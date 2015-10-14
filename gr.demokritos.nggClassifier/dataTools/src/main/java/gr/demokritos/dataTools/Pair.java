/*
This file is part of nggSpamFilter.

nggSpamFilter is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

nggSpamFilter is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with nggSpamFilter.  If not, see <http://www.gnu.org/licenses/>.

Copyright (C) Vasileios Charisopoulos, 2015

*/

package gr.demokritos.dataTools;

import java.util.Objects;

public class Pair {
	
	public final int x;
	public final int y;

	/**
	 * Create a Pair from a given start and end
	 */
	public Pair(int _x, int _y) {
		x = _x; y = _y;
	}

	/**
	 * @return the range of the pair
	 */
	public int range() {
		return ((y - x) > 0) ? y - x : x - y; 
	}
	
	/**
	 * Checks if a given number is between the 
	 * range defined by the pair
	 * @param test the number to test
	 * @return a boolean indicating whether the number
	 * 		   is included in the range or not
	 */
	public boolean includes(int test) {
		return (test >= x) && (test < y);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
	
	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
