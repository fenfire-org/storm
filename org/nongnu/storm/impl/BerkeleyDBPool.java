/*
BerkeleyDBPool.java
 *    
 *    Copyright (c) 2004, Matti J. Katila
 *                  2004, Benja Fallenstain
 *
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
 * Written by Matti J. Katila and Benja Fallenstain
 */

package org.nongnu.storm.impl;
import org.nongnu.storm.util.*;
import org.nongnu.storm.*;
import com.sleepycat.je.*;

import java.io.*;
import java.util.*;


/** An inplementation of <code>StormPool</code> which uses 
 *  Java(TM) Edition of Berkeley hash DB for disk operations.
 */
public class BerkeleyDBPool extends AbstractLocalPool {
    static public boolean dbg = false;
    static private void p(String s) { System.out.println("BerkeleyDBPool:: "+s); }



    /** The directory we store our database in.  */
    protected File dir;

    protected boolean closed;

    /** The Berkeley hash DB  */
    protected Database block_ids_db; 
    protected Database blocks_db; 
    protected Database indices_db; 
    protected Database indexed_blocks_db; 
    protected Environment dbEnvironment;
    public BerkeleyDBPool(File dir, Set indexTypes) throws Exception {
	super(indexTypes);

	p("open bdb");

	this.dir = dir;
	if(!dir.exists())
	    throw new FileNotFoundException("BerkeleyDBPool directory '"+dir+"' "+
					    "does not exist.");

	// poor man's environment...
	p("e1");
	EnvironmentConfig envConfig = new EnvironmentConfig();
	p("e2");
	envConfig.setAllowCreate(true);
	p("e3");
	envConfig.setReadOnly(false);
	p("e4");
	envConfig.setCacheSize(10 * 1024 * 1024);
	p("e5");
	envConfig.setTransactional(false);
	p("e6");

	dbEnvironment = new Environment(dir, envConfig);
	p("e7");

	DatabaseConfig dbConf = new DatabaseConfig();
	dbConf.setAllowCreate(true);
	p("open blockIds");
	block_ids_db = dbEnvironment.openDatabase(null, "blockIds", dbConf);
	p("open blocks");
	blocks_db = dbEnvironment.openDatabase(null, "blocks", dbConf);

	dbConf = new DatabaseConfig();
	dbConf.setAllowCreate(true);
	dbConf.setSortedDuplicates(true);
	p("open indices");
	indices_db = dbEnvironment.openDatabase(null, "indices", dbConf);
	p("open indexedBlocks");
	indexed_blocks_db = dbEnvironment.openDatabase(null, "indexedBlocks", 
						       dbConf);
	
	p("get ids");
	Set ids = getIds();
	Set indexed = new HashSet(ids);
	p("remove indexed ids");
	for(Iterator i=indexTypes.iterator(); i.hasNext();) {
	    IndexType indexType = (IndexType)i.next();
	    p("...index type "+indexType);

	    indexed.retainAll(getIndexed(indexType.getIndexTypeURI()));
	}

	p("compute missing ids");
	Set missing = new HashSet(ids);
	missing.removeAll(indexed);

	int k = 0, n = missing.size();

	p("start indexing");
	for(Iterator j=missing.iterator(); j.hasNext();) {
	    k++;
	    if(k==1 || k%50 == 0)
		p("Indexing block "+k+" of "+n+"...");
	    BlockId id = (BlockId)j.next();
	    added(get(id));
	}
	    
	if(n>0) {
	    dbEnvironment.sync();
	    p(n+" blocks indexed.");
	}

	p("indexing complete.");
    }

    
    /*
     * **********************************************************
     *   Implement StormPool
     * **********************************************************
     */
   
    public void add(Block block) throws IOException {
	try {
	    blocks_db.put(null, 
		new DatabaseEntry(block.getId().toString().getBytes("UTF-8")),
		new DatabaseEntry(CopyUtil.readBytes(block.getInputStream()))
		);
	    block_ids_db.put(null,
		new DatabaseEntry(block.getId().toString().getBytes("UTF-8")),
		new DatabaseEntry(new byte[0]));
	    added(block);
	    dbEnvironment.sync();
	    if (dbg) p("block "+block.getId()+" added!");
	} catch (Exception e) {
	    throw new IOException("Problem while performing add operation: "
				  + e.getMessage());
	}
    }


    public void delete(Block block)  throws IOException {
	try {
	    if (dbg) p("deleting id: "+block.getId());
	    DatabaseEntry key = new DatabaseEntry(block.getId().toString().getBytes("UTF-8"));
	    block_ids_db.delete(null, key);
	    blocks_db.delete(null, key);
	    dbEnvironment.sync();
	} catch (Exception e) {
	    throw new IOException("Problem while performing delete operation: "
				  + e.getMessage());
	}
    }

    public Block get(BlockId id)  throws IOException {
	try {
	    if (dbg) p("gettting id: "+id);
	    DatabaseEntry key = new DatabaseEntry(id.toString().getBytes("UTF-8"));
	    DatabaseEntry data = new DatabaseEntry();
	    OperationStatus status = blocks_db.get(null, key, data, (LockMode)null);
	    if (status == OperationStatus.SUCCESS)
		return new PoolBlock(id, data.getData());
	    else 
		throw new IOException("No block found! '"+id+"'.");
	} catch (Exception e) {
	    throw new FileNotFoundException("Problem while performing get operation: "
				  + e.getMessage());
	}
    }
    
    private class PoolBlock extends AbstractBlock {
        byte[] b;
	protected PoolBlock(BlockId id, byte[] b) throws IOException {
	    super(id);
	    this.b = b;
	}
	public InputStream getInputStream() throws IOException {
	    return new ByteArrayInputStream(b);
	}
    }

    protected class BerkeleyDBOutputStream extends AbstractBlockOutputStream {
	protected BerkeleyDBOutputStream(String contentType)
	    throws IOException 
	{
            super(new ByteArrayOutputStream(), contentType);
        }

	public Block makeBlock() throws IOException {
	    ByteArrayOutputStream baos = (ByteArrayOutputStream)out;
	    block = new PoolBlock(makeIdFromDigest(),
				  baos.toByteArray());
	    add(block);
	    return block;
	}
    }

    public BlockOutputStream getBlockOutputStream(String contentType)
	throws IOException {
	return new BerkeleyDBOutputStream(contentType);
    }


    public void close() throws Exception {
	if(closed) return;
	block_ids_db.close();
	blocks_db.close();
	indices_db.close();
	indexed_blocks_db.close();
	dbEnvironment.sync();
	dbEnvironment.close();
	closed = true;
    }

    protected void finalize() throws Throwable {
	try {
	    close();
	} finally {
	    super.finalize();
	}
    }


    public SetCollector getIds() throws IOException {
	Set set = new HashSet();
	Cursor c = null;
	try {
	    c = block_ids_db.openCursor(null, null);


	    DatabaseEntry key = new DatabaseEntry();
	    DatabaseEntry data = new DatabaseEntry();

	    while (c.getNext(key, data, LockMode.DEFAULT) ==
		   OperationStatus.SUCCESS) {
		set.add(new BlockId(new String(key.getData())));
		if(dbg) p(""+set);
	    }
	} catch (Exception e) {
	    throw new IOException("Problem while performing get operation: "
				  + e.getMessage());
	} finally {
	    try {
		if (c!=null)
		    c.close();
	    } catch (Exception e) { throw new IOException("Error: "+e); }
	}
	
	return new SimpleSetCollector(set);
    }


    /*
     * **********************************************************
     *   Implement IndexedPool
     * **********************************************************
     */

    private void put(Database db, byte[] keyBytes, byte[] dataBytes) 
	throws IOException {
	try {
	    DatabaseEntry key = new DatabaseEntry(keyBytes);
	    DatabaseEntry data = new DatabaseEntry(dataBytes);

	    db.put(null, key, data);
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new IOException("Database exception: "
				  + e.getMessage());
	}
    }

    private void put(Database db, String key, String data)
	throws IOException {

	put(db, key.getBytes("UTF-8"), data.getBytes("UTF-8"));
    }

    protected void added(Block block) throws IOException {
	for(Iterator i = indexTypes.iterator(); i.hasNext();) {
	    IndexType it = (IndexType)i.next();
	    Set mappings;
	    try {
		mappings = it.getMappings(block);
	    } catch(Throwable t) {
		t.printStackTrace();
		put(indexed_blocks_db, 
		    it.getIndexTypeURI(), block.getId().toString());
		p("Set indexed: "+block.getId());
		if(!getAll(indexed_blocks_db, it.getIndexTypeURI()).contains(block.getId().toString()))
		    throw new Error("ARGH");
		else
		    p("All ok.");
		continue;
	    }
	    for(Iterator j = mappings.iterator(); j.hasNext();) {
		Mapping m = (Mapping)j.next();
		if(!m.block.equals(block.getId()))
		    throw new Error("Wrong block in mapping: "+m);

		put(indices_db, 
		    it.getIndexTypeURI()+" "+m.key,
		    m.block.getURI()+" "+m.value);
	    }

	    put(indexed_blocks_db, 
		it.getIndexTypeURI(), block.getId().toString());
	}
    }

    protected Set getIndexed(String indexTypeURI) throws IOException {
	Set result = new HashSet();
	Set s = getAll(indexed_blocks_db, indexTypeURI);
	for(Iterator i=s.iterator(); i.hasNext();) {
	    result.add(new BlockId((String)i.next()));
	}
	return result;
    }

    protected Map mappingsSet = new HashMap();

    public Collector getMappings(String typeURI, String key)
	throws IOException 
    {
	String dbkey = typeURI+" "+key;

	Set mappings = new HashSet();
	for(Iterator i=getAll(indices_db, dbkey).iterator(); i.hasNext();) {
	    String s = (String)i.next();
	    int sp = s.indexOf(' ');
		    
	    BlockId blockId = new BlockId(s.substring(0, sp));
	    String value = s.substring(sp+1);

	    mappings.add(new IndexedPool.Mapping(blockId,
						 key, value));
	}

	return new SimpleSetCollector(mappings);
    }

    protected Set getAll(Database db, String key) throws IOException {
	Set result = new HashSet();

	Cursor c = null;
	try {
	    c = db.openCursor(null, null);

	    DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
	    DatabaseEntry data = new DatabaseEntry();

	    if (c.getSearchKey(keyEntry, data, LockMode.DEFAULT) ==
		OperationStatus.SUCCESS) {
		do {
		    result.add(new String(data.getData()));
		} while (c.getNextDup(keyEntry, data, LockMode.DEFAULT) !=
			 OperationStatus.NOTFOUND);
	    }
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new IOException("Problem while performing getTypes operation: "
				  + e.getMessage());
	} finally {
	    try {
		if (c != null)
		    c.close();
	    } catch (Exception e) { 
		e.printStackTrace();
		throw new IOException("Error: "+e); 
	    }
	}
	
    }
}
