/*
DirDB.java
 *    
 *    Copyright (c) 2003, Benja Fallenstein
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
import com.bitzi.util.Base32;
import java.io.*;
import java.util.*;

/** An IndexedPool.DB implementation for DirPools.
 *  XXX IndexedPool.DB doesn't exist any more, but the
 *  function of the class hasn't changed -- explain :)
 */
public class DirDB {
    public static boolean dbg = false;
    private static void p(String s) { System.out.println("DirDB: "+s); }

    protected File dbDir;
    //protected Map cache = new HashMap();

    static final String allowed = "abcdefghijklmnopqrstuvwxyz0123456789";

    static public String escape(String s) {
	StringBuffer b = new StringBuffer();
	for(int i=0; i<s.length(); i++) {
	    char c = s.charAt(i);
	    if(allowed.indexOf(c) >= 0)
		b.append(c);
	    else
		b.append("-" + ((int)c) + "-");
	}
	return b.toString();
    }

    /** The maximum file name length allowed.
     */
    protected int maxlen = 250;
    
    /**
     *  @param inDir The directory of the DirPool.
     *  @param indexTypeURI The URI of the index type, used to
     *                      determine the subdirectory indexing info
     *                      is to be stored in:
     *                      <inDir>/idx_<hex-of-indexTypeURI>
     */
    public DirDB(File inDir, IndexedPool.IndexType indexType) throws IOException {
	if(inDir == null) throw new NullPointerException("null directory");
	
	String hex = escape(indexType.getIndexTypeURI());
	dbDir = new File(inDir, "idx_"+hex);
	
	if(!dbDir.exists()) {
	    dbDir.mkdir();
	    
	    Writer w = new FileWriter(new File(dbDir, "index_type"));
	    w.write(indexType.getIndexTypeURI()); w.write('\n');
	    w.write(indexType.getHumanReadableName());
	    w.write('\n');
	    w.close();

	    w = new FileWriter(new File(dbDir, "indexed_blocks"));
	    w.close();
	}
    }
    
    protected File getKeyFile(String key) throws IOException {
	String esc = escape(key);
	if(esc.length()+4 > maxlen) {
	    String verbatim = esc.substring(0, maxlen-4-32-1);
	    String rest = esc.substring(maxlen-4-32-1);
	    java.security.MessageDigest dig = BlockId.makeSHA1Digest();
	    dig.update(key.getBytes("UTF-8"));
	    String hash = com.bitzi.util.Base32.encode(dig.digest());
	    esc = verbatim + "-" + hash.toLowerCase();
	}
	return new File(dbDir, "key_"+esc);
    }

    /**
    protected class CacheEntry {
	protected String key;
	protected File file;
	protected Collector values;
	protected long lastModified;

	protected CacheEntry(String key) throws IOException {
	    this.key = key;
	    file = getKeyFile(key);
	    reread();
	    System.out.println("First read: lastModified "+lastModified+" "+key);
	}

	protected Collector get() throws IOException {
	    long m = file.lastModified();
	    System.out.println("\nm "+m+" lastModified "+lastModified+" "+key);
	    if(m == 0 || m != lastModified)
		reread();
	    return values;
	}
	
	protected void reread() throws IOException {
	    System.out.println("reread");

	    if(!file.exists()) {
		lastModified = 0;
		values = new SimpleSetCollector(Collections.EMPTY_SET);
		return;
	    }

	    System.out.println("do reread");
	    
	    Set set = new HashSet();
	    lastModified = file.lastModified();
	    InputStream in = new FileInputStream(file);
	    Reader ir = new InputStreamReader(in, "UTF-8");
	    BufferedReader r = new BufferedReader(ir);
	    
	    String line = r.readLine();
	    while(line != null && !line.equals("")) {
		int i = line.indexOf(' ');
		BlockId block = new BlockId(line.substring(0, i));
		String value = line.substring(i+1);
		
		set.add(new IndexedPool.Mapping(block, key, value));
		
		line = r.readLine();
	    }

	    values = new SimpleSetCollector(set);
	}
    }

    public Collector get(String key) throws IOException {
	CacheEntry e = (CacheEntry)cache.get(key);
	if(e == null) {
	    e = new CacheEntry(key);
	    cache.put(key, e);
	}
	return e.get();
    }
    **/

    public Collector get(String key) throws IOException {
	Set set = new HashSet();
	File file = getKeyFile(key);
	if(file.exists()) {
	    if(dbg) p("Start reading index");
	    InputStream in = new FileInputStream(file);
	    Reader ir = new InputStreamReader(in, "UTF-8");
	    BufferedReader r = new BufferedReader(ir);
	    
	    String line = r.readLine();
	    while(line != null && !line.equals("")) {
		int i = line.indexOf(' ');
		BlockId block = new BlockId(line.substring(0, i));
		String value = line.substring(i+1);
		
		set.add(new IndexedPool.Mapping(block, key, value));
		
		line = r.readLine();
	    }
	    if(dbg) p("End reading index");
	}
	return new SimpleSetCollector(set);
    }
    
    public Set getIndexed() throws IOException {
	HashSet result = new HashSet();

	InputStream in = new FileInputStream(new File(dbDir, "indexed_blocks"));
	Reader ir = new InputStreamReader(in, "UTF-8");
	BufferedReader r = new BufferedReader(ir);
	    
	String line = r.readLine();
	while(line != null && !line.equals("")) {
	    result.add(new BlockId(line));
	    line = r.readLine();
	}

	return result;
    }

    public void add(IndexedPool.Mapping m) throws IOException {
	if(m.value.indexOf('\n') >= 0)
	    throw new UnsupportedOperationException("values containing newlines");

	if(getIndexed().contains(m.block))
	    return;

	String path = getKeyFile(m.key).getPath();
	OutputStream os = new FileOutputStream(path, true);
	Writer w = new OutputStreamWriter(os, "UTF-8");
	w.write(m.block.getURI());
	w.write(' ');
	w.write(m.value);
	w.write('\n');
	w.close();
    }

    public void addIndexed(BlockId id) throws IOException {
	if(getIndexed().contains(id))
	    return;

	String path = new File(dbDir, "indexed_blocks").getPath();
	OutputStream os = new FileOutputStream(path, true);
	Writer w = new OutputStreamWriter(os, "UTF-8");
	w.write(id.getURI());
	w.write('\n');
	w.close();
    }

    public String toString() {
	return "<DirDB '"+dbDir+"'>";
    }
}

