/*
StaticReferenceResolver.java
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
import org.nongnu.storm.*;
import org.nongnu.storm.util.Graph;
import java.io.IOException;
import java.util.*;

class StaticReferenceResolver implements ReferenceResolver {
    public Block resolve(Reference ref, StormPool pool) throws IOException {
	SortedSet s = new TreeSet();
	s.addAll(ref.getAll("_:this", "http://purl.oclc.org/NET/storm/vocab/representations/representation"));
	s.addAll(ref.getAll("_:this", "http://purl.oclc.org/NET/storm/vocab/representations/instance"));
	s.addAll(ref.getAll("_:this", "http://purl.oclc.org/NET/storm/vocab/representations/description"));

	// XXX picks an instance deterministically,
	// but the algorithm makes it look pretty random--
	// need to channel through content type preferences &c?

	String uri = (String)s.iterator().next();
	if(uri.startsWith(BlockId.PREFIX))
	    return pool.get(new BlockId(uri));
	else if(uri.startsWith(ReferenceId.PREFIX))
	    return new Reference(pool, new ReferenceId(uri)).resolve(pool);

	throw new IOException("URI type not supported: "+uri);
    }
}
