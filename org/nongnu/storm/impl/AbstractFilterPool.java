/*
AbstractFilterPool.java
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
import org.nongnu.storm.references.ReferenceId;
import org.nongnu.storm.references.PointerId;
import java.io.*;
import java.util.*;

/** Abstract implementation of a pool that proxies all requests 
 *  to another pool.
 *  The idea is that subclasses can override specific methods
 *  in order to add functionality to them.
 */
public abstract class AbstractFilterPool implements IndexedPool {

    protected IndexedPool pool;

    public AbstractFilterPool(IndexedPool pool) {
	this.pool = pool;
    }

    public Block get(BlockId id) throws FileNotFoundException, IOException {
	return pool.get(id);
    }

    public Block get(ReferenceId id) 
	throws FileNotFoundException, IOException {
	return pool.get(id);
    }

    public Block get(PointerId id) 
	throws FileNotFoundException, IOException {
	return pool.get(id);
    }

    public Block request(BlockId id, BlockListener listener) 
	throws IOException {
	return pool.request(id, listener);
    }

    public Block request(BlockId id) throws IOException {
	return pool.request(id);
    }

    public void add(Block b) throws IOException {
	pool.add(b);
    }

    public void delete(Block b) throws IOException {
	pool.delete(b);
    }

    public SetCollector getIds() throws IOException {
	return pool.getIds();
    }


    public BlockOutputStream getBlockOutputStream(String contentType) 
	throws IOException {
	return pool.getBlockOutputStream(contentType);
    }

    public Collector getMappings(String typeURI, String key) 
	throws IOException {
	return pool.getMappings(typeURI, key);
    }

    public Object getIndex(String typeURI) throws NoSuchElementException {
	return pool.getIndex(typeURI);
    }

    public Map getIndices() {
	return pool.getIndices();
    }

    public Set getIndexTypes() {
	return pool.getIndexTypes();
    }



    public static class FilterPool extends AbstractFilterPool {
	public FilterPool(IndexedPool pool) { super(pool); }
	public void setPool(IndexedPool pool) { this.pool = pool; }
    }
}
