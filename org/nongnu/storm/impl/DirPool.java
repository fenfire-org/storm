/*
DirPool.java
 *    
 *    Copyright (c) 2002, Benja Fallenstein
 *    
 *    This file is part of Fenfire.
 *    
 *    Fenfire is free software; you can redistribute it and/or modify it under
 *    the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *    
 *    Fenfire is distributed in the hope that it will be useful, but WITHOUT
 *    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 *    Public License for more details.
 *    
 *    You should have received a copy of the GNU General
 *    Public License along with Fenfire; if not, write to the Free
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
import org.nongnu.storm.util.*;
import java.io.*;
import java.util.*;

/** A StormPool storing blocks in individual files in a directory.
 *  File names have the form <code>data_</code><i>idstring</i>.
 *  Blocks in this pool are <code>DirPoolBlock</code>s.
 */
public class DirPool extends AbstractLocalPool {
    public static boolean dbg = false;
    private static void p(String s) { System.out.println("DirPool:: "+s); }
    
    /** The directory we store our blocks in.
     */
    File dir;

    /** The DB objects by index type URI.
     */
    protected Map dbs = new HashMap();

    /** Get the File object for the block
     *  corresponding to a given id.
     *  This is needed in many places and wrapping it in a convenience
     *  function makes code easier to read & maintain.
     */
    protected final File getFile(BlockId id) {
	return new File(dir, "data_" + id.getBitprint());
    }
    

    protected class DirPoolBlockOutputStream extends AbstractBlockOutputStream {
	protected File tempFile;

 	protected DirPoolBlockOutputStream(File tempFile, String contentType) 
	                                                 throws IOException {
            super(new BufferedOutputStream(new FileOutputStream(tempFile)),
		  contentType);
	    this.tempFile = tempFile;
        }

	public Block makeBlock() throws IOException {
	    BlockId id = makeIdFromDigest();
	    File file = getFile(id);

	    if(!tempFile.renameTo(file)) {
		tempFile.delete();
		throw new IOException("Could not rename temporary file");
	    }

	    block = new DirPoolBlock(id);
	    added(block);
	    return block;
	}
    }

    protected class DirPoolBlock extends AbstractBlock implements FileBlock {
        protected File file;

	protected DirPoolBlock(BlockId id) throws IOException {
	    super(id);
	    this.file = DirPool.this.getFile(id);
	    if(!file.exists())
		throw new FileNotFoundException("Block: "+id);
	}

	public InputStream getInputStream() throws IOException {
	    return new BufferedInputStream(
                id.getCheckedInputStream(new FileInputStream(file)));
	}

	public File getFile() throws IOException {
	    // check  the id
	    id.getCheckedInputStream(new FileInputStream(file)).close();

	    return file;
	}
    }

    /** Create a new DirPool.
     *  @param dir The directory blocks are stored in.
     *             Must already exist.
     *  @throws IllegalArgumentException if the file isn't a directory
     *                                   or does not exist yet.
     */
    public DirPool(File dir, Set indexTypes) throws IOException {
	super(indexTypes);
	this.dir = dir;

	if(!dir.exists())
	    throw new FileNotFoundException("DirPool directory '"+dir+"' "+
					    "does not exist");

	Set ids = getIds();
	Set indexed = new HashSet(ids);
	for(Iterator i=indexTypes.iterator(); i.hasNext();) {
	    IndexType indexType = (IndexType)i.next();

	    DirDB db = new DirDB(dir, indexType);
	    dbs.put(indexType.getIndexTypeURI(), db);

	    indexed.retainAll(db.getIndexed());
	}

	Set missing = new HashSet(ids);
	missing.removeAll(indexed);

	int k = 0, n = missing.size();

	for(Iterator j=missing.iterator(); j.hasNext();) {
	    k++;
	    if(k==1 || k%50 == 0)
		p("Indexing block "+k+" of "+n+"...");
	    BlockId id = (BlockId)j.next();
	    added(get(id));
	}
	    
	if(n>0) p(n+" blocks indexed.");
    }

    public Block get(BlockId id) throws IOException { 
	return new DirPoolBlock(id); 
    }
    public void add(Block b) throws IOException {
	File temp = TempFileUtil.tmpFile(dir);
	
	BlockId id = b.getId();
	InputStream is = id.getCheckedInputStream(b.getInputStream());
	OutputStream os = 
	    new BufferedOutputStream(new FileOutputStream(temp));
	
	CopyUtil.copy(is, os);

	if(!temp.renameTo(getFile(id)))
	    throw new IOException("Could not rename temporary file");
	
	added(get(id));
    }
    public void delete(Block b) throws IOException {
	getFile(b.getId()).delete();
    }
    public SetCollector getIds() throws IOException {
	HashSet ids = new HashSet();
	String[] list = dir.list();
	
	for(int i=0; i<list.length; i++)
	    if(list[i].startsWith("data_")) {
		String bitprint = list[i].substring(5);
		Set types = readTypes(bitprint);
		for(Iterator j=readTypes(bitprint).iterator(); j.hasNext();) {
		    ids.add(new BlockId(BlockId.PREFIX + j.next() + 
					"," + bitprint));
		}
	    }

	return new SimpleSetCollector(ids);
    }
    public BlockOutputStream getBlockOutputStream(String contentType) 
                                                          throws IOException {
	File tempFile = TempFileUtil.tmpFile(dir);
    	return new DirPoolBlockOutputStream(tempFile, contentType);
    }

    public Collector getMappings(String typeURI, String key) 
	throws IOException {

	return ((DirDB)dbs.get(typeURI)).get(key);
    }

    protected Set readTypes(String bitprint) throws IOException {
	HashSet types = new HashSet();
	File file = new File(dir, "types_"+bitprint);
	if(!file.exists()) return types;
	String t;
	BufferedReader r = new BufferedReader(new FileReader(file));
	while((t = r.readLine()) != null) types.add(t);
	r.close();
	return types;
    }

    protected void added(Block block) throws IOException {
	String file = "types_"+block.getId().getBitprint();
	Set types = readTypes(block.getId().getBitprint());
	types.add(block.getId().getContentType());
	PrintWriter p = new PrintWriter(new FileWriter(new File(dir, file)));
	for(Iterator i = types.iterator(); i.hasNext();)
	    p.println(i.next());
	p.close();

	for(Iterator i = indexTypes.iterator(); i.hasNext();) {
	    IndexType it = (IndexType)i.next();
	    DirDB db = (DirDB)dbs.get(it.getIndexTypeURI());
	    Set mappings;
	    try {
		mappings = it.getMappings(block);
	    } catch(Throwable t) {
		t.printStackTrace();
		db.addIndexed(block.getId());
		continue;
	    }
	    for(Iterator j = mappings.iterator(); j.hasNext();) {
		Mapping m = (Mapping)j.next();
		if(!m.block.equals(block.getId()))
		    throw new Error("Wrong block in mapping: "+m);

		db.add(m);
	    }
	    if(dbg) p("db "+db+" block "+block);
	    db.addIndexed(block.getId());
	}
    }
}
