/*
AbstractPool.java
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
package org.nongnu.storm.impl;
import org.nongnu.storm.*;
import org.nongnu.storm.references.*;
import java.io.*;
import java.util.*;

/** An abstract implementation of <code>IndexedPool</code>.
 */
public abstract class AbstractPool implements IndexedPool {
    private static void pa(String s) { System.out.println(s); }

    protected Set indexTypes;
    protected Map indexTypesByURI;

    protected Map indices = null;

    public AbstractPool(Set indexTypes) throws IOException {
	this.indexTypes = indexTypes;
	indexTypesByURI = new HashMap();
	Map indices = new HashMap();

	for(Iterator i=indexTypes.iterator(); i.hasNext();) {
	    IndexType type = (IndexType)i.next();
	    String uri = type.getIndexTypeURI();
	    indexTypesByURI.put(uri, type);
	    indices.put(uri, type.createIndex(this));
	}

	this.indices = Collections.unmodifiableMap(indices);
    }

    public Map getIndices() { return indices; }

    public Object getIndex(String indexTypeURI) {
	Object index = getIndices().get(indexTypeURI);
	if(index == null)
	    throw new NoSuchElementException("Index "+indexTypeURI);

	return index;
    }
    
    public Set getIndexTypes() {
	return Collections.unmodifiableSet(indexTypes);
    }

    public Block get(ReferenceId id) throws IOException {
	return new Reference(this, id).resolve(this);
    }

    public Block get(PointerId id) throws IOException {
	return Pointers.get(id, this);
    }

    /**
    public Pointer getPointer(String uri) throws IOException {
	Object index = getIndex(PointerIndexType.indexTypeURI);
	return ((PointerIndexType.Index)index).getPointer(uri);
    }
    **/

    protected abstract class AbstractBlockOutputStream 
	extends BlockOutputStream {

	protected Block block;

	/** Whether <code>close()</code> has already been called.
	 *  If <code>false</code>, <code>getBlock()</code> and so on
	 *  cannot be called yet.
	 */
	protected boolean closed;

	/** Create a new AbstractBlockOutputStream object.
	 */
	protected AbstractBlockOutputStream(OutputStream os, 
					    String contentType) 
	    throws IOException {
	    super(os, contentType);
	}

	public void close() throws IOException {
	    if(closed) return;
	    closed = true;
	    out.close();
	    block = makeBlock();
	}

	/** Create the block and add it to the pool.
	 *  When <code>close()</code> is called for the first time,
	 *  it calls this method internally.
	 *  @return The new block.
	 */
	protected abstract Block makeBlock() throws IOException;

        public Block getBlock() { 
	    if(!closed) throw new IllegalStateException("Not closed");
	    return block; 
	}
    }

    protected abstract class AbstractBlock implements Block {
	protected BlockId id;

	protected AbstractBlock(BlockId id) {
	    this.id = id;
	}
	
    	public BlockId getId() { return id; }
	public StormPool getPool() { return AbstractPool.this; }
    }
}
