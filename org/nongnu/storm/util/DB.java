/*
DB.java
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

package org.nongnu.storm.util;
import java.io.*;
import java.util.*;


public interface DB {
    void clear() throws IOException ;
    boolean contains(String key);
    InputStream get(String key);
    void put(String key, InputStream out) throws IOException;
    void remove(String key) throws IOException ;
    Iterator iterator();

    class Impl implements DB {
	private void p(String s) { System.out.println("DB:: "+s); }

	public void clear() throws IOException {
	    for(Iterator i = iterator(); i.hasNext();) {
		try {
		    Index ind = (Index)index.get(i.next());
		    ind.name.delete();
		} catch (Exception e) { }
	    }
	    index.clear();
	    writeIndex();
	}


	static class Index {
	    long begin;
	    long size;
	    File name;
	    Index(long b, long s, File n) {
		begin = b;
		size = s;
		name = n;
	    }
	}


	Map index = new HashMap();
	File dir;
	public Impl(File dir) throws IOException {
	    this(dir, 32);
	}
	public Impl(File dir, int FILES) throws IOException {
	    this.FILES = FILES;
	    if (dir == null) throw new NullPointerException();
	    if (! dir.isDirectory())
		throw new Error("Directory must exists '"+dir+"'");
	    this.dir = dir;

	    File indexF = new File(dir, "index");
	    if (!indexF.exists()) (new FileOutputStream(indexF)).close();
	    if (!indexF.canWrite()) throw new Error("No access to write index.");

	    BufferedReader indexReader = new BufferedReader(
							    new FileReader(indexF));
	    String str = indexReader.readLine();
	    while (str != null) {
		putIndex(str);
		str = indexReader.readLine();
	    }
	}

	int FILES;

	int ind = 0;
	File currentFile;
	private File getFile() throws IOException {
	    if ((ind++ % FILES) == 0) {
		currentFile = File.createTempFile("data", "tmp", dir);
	    }
	    return currentFile;
	}

	String delim = " ### ";

	private void putIndex(String indStr) {
	    StrTokenizer st = new StrTokenizer(indStr, delim, 4);
	    String key = st.next();
	    long begin = Long.parseLong(st.next());
	    long size = Long.parseLong(st.next());
	    File name = new File(dir, st.next());
	    Index index = new Index(begin, size, name);
	    this.index.put(key, index);
	}
	private String getIndex(String key, Index value) {
	    return 
		key + delim + 
		value.begin + delim + 
		value.size + delim+
		value.name.getName();
	}


	public boolean contains(String key) {
	    return index.containsKey(key);
	}
	public InputStream get(String key) { 
	    if (!contains(key)) return null;
	    Index i = (Index) index.get(key);
	    return new InS(i);
	}
	public void put(String key, InputStream src) 
	    throws IOException {
	    if (contains(key)) return;

	    File dest = getFile();
	    long begin = dest.length();
	    FileOutputStream f = new FileOutputStream(dest, true); // append
	    CopyUtil.copy(src, f);

	    long size = dest.length() - begin;
	    index.put(key, new Index(begin, size, dest));

	    writeIndex();
	}
	private void writeIndex() throws IOException {
	    PrintWriter p = new PrintWriter(
					    new FileOutputStream(new File(dir, "index"), false));
	    for(Iterator i=iterator(); i.hasNext();) {
		String k = (String) i.next();
		Index ind = (Index) index.get(k);
		p.println(getIndex(k, ind));
		//p(getIndex(k, ind));
	    }
	    p.close();
	}
	public void remove(String key) throws IOException { 
	    if (!contains(key)) throw new Error("No such element");
	    Index rmInd = (Index) index.get(key);
	    index.remove(key);

	    // this is not very fast.. O(N)
	    List sameFile = new ArrayList();
	    for(Iterator i = iterator(); i.hasNext();) {
		String k = (String) i.next();
		Index inde = (Index) index.get(k);
		//p("file: "+inde.name);
		if (inde.name.equals(rmInd.name)) {
		    sameFile.add(inde);
		    //p("found: "+k);
		}
	    }
	    sameFile.remove(rmInd);

	    // don't use the same index number!
	    currentFile = File.createTempFile("data", "tmp", dir);

	    File newF = getFile();
	    for (Iterator i = sameFile.iterator(); i.hasNext(); ) {
		Index inde = (Index) i.next();

		long begin = newF.length();
		// append
		FileOutputStream f = new FileOutputStream(newF, true); 
		CopyUtil.copy(new InS(inde), f);
		inde.begin = begin;
		inde.name = newF;
	    }
	    rmInd.name.delete();
	    writeIndex();
	}
	public Iterator iterator() { 
	    return index.keySet().iterator();
	}


	class InS extends InputStream {
	    FileInputStream in;
	    long size;
	    long curr = 0;
	    InS(Index i) {
		try {
		    in = new FileInputStream(i.name);
		    in.skip(i.begin);
		} catch (Exception e) {
		    e.printStackTrace();
		    throw new Error("Something really wrong!");
		}
		size = i.size;
	    }
	    public int available() throws IOException {
		int a = in.available();
		if (size-curr < a) return ((int)(size-curr));
		return a;
	    }
	    public void close() throws IOException { in.close(); }
	    public boolean markSupported() { return false; }
	    public int read() throws IOException {
		if (curr >= size) return -1;
		int r = in.read();
		curr++;
		return r;
	    }
	}
    }
}
