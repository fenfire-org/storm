/*
ContentTypeIndexType.java
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
import org.nongnu.storm.*;
import java.io.IOException;
import java.util.*;

/** For testing purposes: An index of blocks by content-type 
 */
public class ContentTypeIndexType implements IndexedPool.IndexType {

    public static final String contentTypeIndexTypeURI = 
	"urn:urn-5:JQLckWcNz56B6GSQZyvA6HkEzAI5:1";

    public static class Index {
	protected IndexedPool pool;

	public Index(IndexedPool pool) {
	    this.pool = pool;
	}

	/** Get all blocks matching a given Content-Type.
	 *  XXX For real use, this should not block.
	 */
	public Set getBlocks(String contentType) throws IOException {
	    String key = contentType;
	    Collection mappings = pool.getMappings(contentTypeIndexTypeURI, key).block();

	    Set result = new HashSet(mappings.size());

	    for(Iterator i=mappings.iterator(); i.hasNext();) {
		IndexedPool.Mapping m = (IndexedPool.Mapping)i.next();
		if(!key.equals(m.key))
		    throw new Error("Key does not match: <"+m.key+">");
		if(!m.value.equals(""))
		    throw new Error("Value does not match: <"+m.value+">");
		
		result.add(m.block);
	    }

	    return result;
	}
    }

    public Set getMappings(Block block) throws IOException {
	String key = block.getId().getContentType();
	String value = "";

	IndexedPool.Mapping mapping = new IndexedPool.Mapping(block.getId(),
							      key, value);
	Set result = Collections.singleton(mapping);

	return result;
    }

    public Object createIndex(IndexedPool pool) {
	return new Index(pool);
    }

    public String getIndexTypeURI() {
	return contentTypeIndexTypeURI;
    }

    public String getHumanReadableName() {
	return ("An index of blocks by their content type. " +
		"For testing purposes.");
    }
}
