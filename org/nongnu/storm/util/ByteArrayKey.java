/*
ByteArrayKey.java
 *    
 *    Copyright (c) 2003, Benja Fallenstein
 *    
 *    This file is part of Storm.
 *    
 *    Storm is free software; you can redistribute it and/or modify it under
 *    the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *    
 *    Storm is distributed in the hope that it will be useful, but WITHOUT
 *    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 *    Public License for more details.
 *    
 *    You should have received a copy of the GNU General
 *    Public License along with Storm; if not, write to the Free
 *    Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *    MA  02111-1307  USA
 *    
 *    
 */
/*
 * Written by Benja Fallenstein
 */
package org.nongnu.storm.util;
import java.util.Arrays;

/** A convenience class for using a byte array
 *  as a hashtable key.
 */
public class ByteArrayKey {
    public final byte[] key;
    protected final int hashCode;
    
    public ByteArrayKey(byte[] key) {
	this.key = key;
	int hashCode = 0;
	
	for(int i=0; i<key.length; i++) {
	    hashCode += 77;
	    hashCode ^= key[i];
	}

	this.hashCode = hashCode;
    }
    
    public boolean equals(Object o) {
	if(!(o instanceof ByteArrayKey)) return false;
	ByteArrayKey k = (ByteArrayKey)o;
	if(hashCode != k.hashCode) return false;
	if(key == k.key) return true;
	return Arrays.equals(key, k.key);
    }
    
    public int hashCode() {
	return hashCode;
    }
}
