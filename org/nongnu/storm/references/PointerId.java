/*
PointerId.java
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

public final class PointerId {
    public static String PREFIX = "vnd-storm-ptr:";
    public static int PREFIX_LEN = PREFIX.length();

    private String uri;
    private BlockId graphId;
    private String path;

    public PointerId(String uri) throws IllegalArgumentException {
	int slash = uri.indexOf('/');
	if(slash < 0) slash = uri.length();

	uri = uri.substring(0, slash).toLowerCase() + uri.substring(slash);
	this.uri = uri.intern();

	if(!uri.startsWith(PREFIX))
	    throw new IllegalArgumentException("Ref URI must start "+PREFIX);

	this.graphId = new BlockId("vnd-storm-hash:" + 
				   ReferenceId.CONTENT_TYPE + "," +
				   uri.substring(PREFIX_LEN, slash));
	this.path = uri.substring(slash);
    }

    public PointerId(PointerId parent, String path) {
	this(parent.getURI() + path);
    }

    public PointerId(BlockId graphId) {
	this(PREFIX + graphId.getBitprint());
    }

    public PointerId(BlockId graphId, String path) {
	this(PREFIX + graphId.getBitprint() + path);
    }

    public String getURI() { return uri; }
    public String toString() { return uri; }

    public BlockId getGraphId() { return graphId; }
    public ReferenceId getReferenceId() { return new ReferenceId(graphId); }
    public String getPath() { return path; }

    public PointerId getRoot() { return new PointerId(graphId); }

    public boolean equals(Object o) {
	if(!(o instanceof PointerId)) return false;
	return uri == ((PointerId)o).uri;
    }

    public int hashCode() {
	return 5310789 ^ uri.hashCode();
    }
}
