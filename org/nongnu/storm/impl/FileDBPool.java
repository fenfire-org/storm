/*
FileDBPool.java
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

package org.nongnu.storm.impl;
import org.nongnu.storm.util.*;
import org.nongnu.storm.*;
import java.io.*;
import java.util.*;


/** An inplementation of <code>StormPool</code> which uses 
 *  DB for disk operations.
 */
public class FileDBPool extends AbstractLocalPool {
    static public boolean dbg = false;
    static private void p(String s) { System.out.println("FileDBPool:: "+s); }



    /** The directory we store our database in.  */
    protected File dir;

    /** The DB objects by index type URI.  */
    //protected Map dbs = new HashMap();

    /** The Berkeley hash DB  */
    protected DB blocks_db; 
    protected MultiDB indices_db; 
    protected DB indexed_blocks_db; 
    //protected Environment dbEnvironment;
    public FileDBPool(File dir, Set indexTypes) throws Exception {
	super(indexTypes);

	this.dir = dir;
	if(!dir.exists() || !dir.isDirectory())
	    throw new FileNotFoundException("BerkeleyDBPool directory '"+dir+"' "+
					    "does not exist.");

	// create some dirs
	File blocks = new File(dir, "blocks");
	File indices = new File(dir, "indices");
	File indexed_b = new File(dir, "indexed_blocks");
	blocks.mkdirs();
	indices.mkdirs();
	indexed_b.mkdirs();

	blocks_db = new DB.Impl(blocks, 128);
	indices_db = new MultiDB.Impl(indices);
	indexed_blocks_db = new DB.Impl(indexed_b, 128);
    }

    
    /*
     * **********************************************************
     *   Implement StormPool
     * **********************************************************
     */
   
    public void add(Block block) throws IOException {
	try {
	    blocks_db.put(block.getId().toString(),
			  block.getInputStream());
	    added(block);
	    if (dbg) p("block "+block.getId()+" added!");
	} catch (Exception e) {
	    throw new IOException("Problem while performing add operation: "
				  + e.getMessage());
	}
	    
    }


    public void delete(Block block)  throws IOException {
	try {
	    if (dbg) p("deleting id: "+block.getId());
	    blocks_db.remove(block.getId().toString());
	} catch (Exception e) {
	    throw new IOException("Problem while performing delete operation: "
				  + e.getMessage());
	}
    }

    public Block get(BlockId id)  throws IOException {
	try {
	    if (dbg) p("gettting id: "+id);
	    InputStream in = blocks_db.get(id.toString());
	    if (in != null)
		return new PoolBlock(id, in);
	    else 
		throw new IOException("No block found! '"+id+"'.");
	} catch (Exception e) {
	    throw new FileNotFoundException("Problem while performing get operation: "
				  + e.getMessage());
	}
    }
    
    private class PoolBlock extends AbstractBlock {
        byte[] b;
	protected PoolBlock(BlockId id, InputStream in) throws IOException {
	    super(id);
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    CopyUtil.copy(in, out);
	    this.b = out.toByteArray();
	}
	protected PoolBlock(BlockId id, byte[] b) throws IOException {
	    super(id);
	    this.b = b;
	}
	public InputStream getInputStream() throws IOException {
	    return new ByteArrayInputStream(b);
	}
    }

    protected class DBOutputStream extends AbstractBlockOutputStream {
	protected DBOutputStream(String contentType)
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
	return new DBOutputStream(contentType);
    }


    public SetCollector getIds() throws IOException {
	Set set = new HashSet();
	for(Iterator i = blocks_db.iterator();i.hasNext(); ) 
	    set.add(new BlockId((String) i.next()));
	return new SimpleSetCollector(set);
    }


    /*
     * **********************************************************
     *   Implement IndexedPool
     * **********************************************************
     */

    private void put(DB db, String key, String data)
	throws IOException {
	db.put(key, new ByteArrayInputStream(data.getBytes("UTF-8")));
    }

    protected void added(Block block) throws IOException {
	for(Iterator i = indexTypes.iterator(); i.hasNext();) {
	    IndexType it = (IndexType)i.next();
	    Set mappings = it.getMappings(block);
	    for(Iterator j = mappings.iterator(); j.hasNext();) {
		Mapping m = (Mapping)j.next();
		if(!m.block.equals(block.getId()))
		    throw new Error("Wrong block in mapping: "+m);

		indices_db.put(it.getIndexTypeURI()+" "+m.key,
		    m.block.getURI()+" "+m.value);
	    }

	    put(indexed_blocks_db, 
		it.getIndexTypeURI()+" "+block.getId(), "");
	}
    }

    protected Map mappingsSet = new HashMap();

    public Collector getMappings(String typeURI, String key) 
	throws IOException 
    {
	HashSet mappings = new HashSet();

	List l = indices_db.get(typeURI+" "+key);
	if (l != null) {
	    for (Iterator i = l.iterator(); i.hasNext();) {
		String s = (String) i.next();
		int sp = s.indexOf(' ');
		
		BlockId blockId = new BlockId(s.substring(0, sp));
		String value = s.substring(sp+1);
		
		mappings.add(new IndexedPool.Mapping(blockId,
						     key, value));
	    }
	}
	return new SimpleSetCollector(mappings);
    }

}
