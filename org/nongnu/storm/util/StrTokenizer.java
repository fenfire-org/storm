/*
StrTokenizer.java
 *    
 *    Copyright (c) 2004, Matti J. Katila
 *    This file is part of Strom.
 *    
 *    Strom is free software; you can redistribute it and/or modify it under
 *    the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *    
 *    Strom is distributed in the hope that it will be useful, but WITHOUT
 *    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 *    Public License for more details.
 *    
 *    You should have received a copy of the GNU General
 *    Public License along with Strom; if not, write to the Free
 *    Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *    MA  02111-1307  USA
 *    
 */
/*
 * Written by Matti J. Katila
 */

package org.nongnu.storm.util;

public class StrTokenizer {
    static public boolean dbg = false;

    String in;
    String delim;
    int count;
    int currentCount = 0;
    int begin = 0;
    public StrTokenizer(String toBeSplit, String delim, int count) {
	in = toBeSplit;
	this.delim = delim;
	this.count = count;
    }

    public String next() {
	if (dbg) System.out.println("'"+in.substring(begin)+"'");
	int ind = in.indexOf(delim, begin);
	if (ind < 0) {
	    if (currentCount == count - 1) 
		return in.substring(begin);
	    throw new Error("Count is overrun and begin index is: "+begin);
	} 

	int oldBegin = begin;
	begin = ind + delim.length();
	currentCount++;
	if (dbg) System.out.println("'"+in.substring(oldBegin, ind)+"'");
	if (currentCount >= count) throw new Error("Count is reached already.");
	return in.substring(oldBegin, ind);
    }


    static public void main(String [] argv) {
	StrTokenizer s = 
	    new StrTokenizer("asdf ### jkl ###  ### ab # # # gh rty", " ### ", 4);
	assertB(s.next().equals("asdf"));
	assertB(s.next().equals("jkl"));
	assertB(s.next().equals(""));
	assertB(s.next().equals("ab # # # gh rty"));
	System.out.println("all fine");
    }

    static void assertB(boolean not) {
	if (!not)
	    throw new Error("Assert error!");
    }

}
