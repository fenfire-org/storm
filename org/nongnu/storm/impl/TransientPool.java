/*
TransientPool.java
 *    
 *    Copyright (c) 2002, Benja Fallenstein and Anton Feldmann
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
 * Written by Benja Fallenstein and Anton Feldmann
 */

package org.nongnu.storm.impl;
import org.nongnu.storm.*;
import org.nongnu.storm.util.ByteArrayKey;
import java.io.*;
import java.util.*;

/** A StormPool whose contents are exclusively stored in memory.
 */
public class TransientPool extends AbstractLocalPool {
    /** The blocks in this pool.
     *  A mapping from <code>BlockId</code> objects
     *  to <code>TransientBlock</code>s.
     */
    protected Map blocks = new HashMap();

    protected Map mappings = new HashMap();

    public void clear() { 
	blocks = new HashMap(); 
	mappings = new HashMap();
    }

    protected class TransientBlockOutputStream extends AbstractBlockOutputStream {
	protected TransientBlockOutputStream(String contentType)
	                                              throws IOException {
            super(new ByteArrayOutputStream(), contentType);
        }

	public Block makeBlock() throws IOException {
	    ByteArrayOutputStream baos = (ByteArrayOutputStream)out;
	    block = new TransientBlock(makeIdFromDigest(),
				       baos.toByteArray());
	    blocks.put(block.getId(), block);

	    added(block);
	    return block;
	}
    }

    protected class TransientBlock extends AbstractBlock {
        protected byte[] bytes;

	protected TransientBlock(BlockId id, byte[] bytes) 
	                                  throws IOException {
	    super(id);
            this.bytes = bytes;

	    id.check(bytes);
	}

	public InputStream getInputStream() throws IOException {
	    return new ByteArrayInputStream(bytes);
	}
    }

    protected Set getMappingsSet(String typeURI, String key) {
	key = typeURI + " " + key;

	Set s = (Set)mappings.get(key);
	if(s == null) {
	    s = new HashSet();
	    mappings.put(key, s);
	}
	return s;
    }

    public Collector getMappings(String typeURI, String key) {
	return 
	    new SimpleSetCollector(new HashSet(getMappingsSet(typeURI, key)));
    }

    /** XXX Benja
     */
    public TransientPool(Set indexTypes) throws IOException {
	super(indexTypes);
    }

    public Block get(BlockId id) throws FileNotFoundException {
	if(!blocks.keySet().contains(id))
	    throw new FileNotFoundException("No such block: "+id);

    	return (Block)blocks.get(id);
    }
    public void add(Block b) throws IOException {
	byte[] bytes = org.nongnu.storm.util.CopyUtil.readBytes(b.getInputStream());

	BlockId id = b.getId();
	id.check(bytes);

	Block block = new TransientBlock(id, bytes);
	blocks.put(id, block);

	added(block);
    }
    public void delete(Block b) {
	blocks.keySet().remove(b.getId());
    }
    public SetCollector getIds() { 
	return new SimpleSetCollector(new HashSet(blocks.keySet()));
    }
    public BlockOutputStream getBlockOutputStream(String contentType)
                                                          throws IOException {
    	return new TransientBlockOutputStream(contentType);
    }

    protected void added(Block block) throws IOException {
	for(Iterator i = indexTypes.iterator(); i.hasNext();) {
	    IndexType it = (IndexType)i.next();
	    Set mappings = it.getMappings(block);
	    for(Iterator j = mappings.iterator(); j.hasNext();) {
		Mapping m = (Mapping)j.next();
		if(!m.block.equals(block.getId()))
		    throw new Error("Wrong block in mapping: "+m);

		getMappingsSet(it.getIndexTypeURI(), m.key).add(m);
	    }
	}
    }
}
