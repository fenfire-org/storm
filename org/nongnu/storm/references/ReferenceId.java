/*
ReferenceId.java
 *
 *    Copyright (c) 2002, Benja Fallenstein
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
package org.nongnu.storm.references;
import org.nongnu.storm.BlockId;

public final class ReferenceId {
    public static String PREFIX = "vnd-storm-ref:";
    public static int PREFIX_LEN = PREFIX.length();
    public static String CONTENT_TYPE = "text/plain";

    private String uri;
    private BlockId graphId;

    public ReferenceId(String uri) throws IllegalArgumentException {
	uri = uri.toLowerCase();
	this.uri = uri.intern();

	if(!uri.startsWith(PREFIX))
	    throw new IllegalArgumentException("Ref URI must start "+PREFIX);

	this.graphId = new BlockId("vnd-storm-hash:" + CONTENT_TYPE + "," +
				   uri.substring(PREFIX_LEN));
    }

    public ReferenceId(BlockId graphId) {
	this(PREFIX + graphId.getBitprint());
    }

    public String getURI() { return uri; }
    public String toString() { return uri; }

    public BlockId getGraphId() { return graphId; }

    public boolean equals(Object o) {
	if(!(o instanceof ReferenceId)) return false;
	return uri == ((ReferenceId)o).uri;
    }

    public int hashCode() {
	return 5310789 ^ uri.hashCode();
    }
}
