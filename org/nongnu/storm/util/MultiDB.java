/*
MultiDB.java
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


public interface MultiDB {
    void clear() throws IOException ;
    boolean contains(String key);
    List get(String key);
    void put(String key, String value) throws IOException;
    void remove(String key) throws IOException ;
    Iterator iterator();

    class Impl implements MultiDB {
	private void p(String s) { System.out.println("MultiDB:: "+s); }

	public void clear() throws IOException {
	    index.clear();
	    writeIndex();
	}


	Map index = new HashMap();
	File dir;
	public Impl(File dir) throws IOException {
	    if (dir == null) throw new NullPointerException();
	    if (! dir.isDirectory())
		throw new Error("Directory must exists '"+dir+"'");
	    this.dir = dir;

	    File indexF = new File(dir, "index");
	    if (!indexF.exists()) (new FileOutputStream(indexF)).close();
	    if (!indexF.canWrite()) 
		throw new Error("No access to write index.");

	    BufferedReader indexReader = new BufferedReader(
							    new FileReader(indexF));
	    String str = indexReader.readLine();
	    while (str != null) {
		StrTokenizer st = new StrTokenizer(str, " ### ", 2);
		String key = st.next();
		int size = Integer.parseInt(st.next());
		if (size < 1) 
		    throw new Error("Aaargh, not enough values!");
		this.index.put(key, index);
		List l = new ArrayList();
		for (int i=0; i<size; i++)
		    l.add(indexReader.readLine());
		this.index.put(key, l);

		str = indexReader.readLine();
	    }
	}

	public boolean contains(String key) {
	    return index.containsKey(key);
	}
	public List get(String key) { 
	    if (!contains(key)) return null;
	    return (List) index.get(key);
	}
	public void put(String key, String val) 
	    throws IOException {

	    List l = get(key);
	    if (l == null) l = new ArrayList(1);
	    l.add(val);
	    index.put(key, l);

	    writeIndex();
	}
	private void writeIndex() throws IOException {
	    PrintWriter p = new PrintWriter(
					    new FileOutputStream(new File(dir, "index"), false));
	    for(Iterator i=iterator(); i.hasNext();) {
		String k = (String) i.next();
		List l = (List) index.get(k);
		p.println(k+" ### "+l.size());
		for (int j=0; j<l.size(); j++)
		    p.println((String)l.get(j));
	    }
	    p.close();
	}
	public void remove(String key) throws IOException { 
	    if (!contains(key)) throw new Error("No such element");
	    index.remove(key);
	    writeIndex();
	}
	public Iterator iterator() { 
	    return index.keySet().iterator();
	}
    }
}
