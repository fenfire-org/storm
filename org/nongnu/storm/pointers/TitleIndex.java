/*
PointerBlock.java
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
package org.nongnu.storm.pointers;
import org.nongnu.storm.*;
import org.nongnu.storm.impl.AsyncSetCollector;
import org.nongnu.storm.util.Pair;
import java.util.*;
import com.bitzi.util.*;
import java.io.*;
import java.security.*;

public class TitleIndex {
    public static boolean dbg = false;
    private static void p(String s) { System.out.println("TitleIndex:: "+s); }

    public static final String uri =
	"http://fenfire.org/2003/05/pointer-title-index-0.1";

    public static final IndexType type = new IndexType();

    protected IndexedPool pool;

    /** Returns a set of *current* pointer blocks.
     *  XXX not async at all
     */
    public Set getPointers(String query) throws IOException {
	Iterator i=splitWords(query).iterator();
	if(!i.hasNext()) return new HashSet();
	Set refs = new HashSet(getWordRefs((String)i.next()));
	while(i.hasNext())
	    refs.retainAll(getWordRefs((String)i.next()));

	Set result = new HashSet();
	for(i=refs.iterator(); i.hasNext();) {
	    Pair pair = (Pair)i.next();
	    try {
		PointerId id = new PointerId((String)pair.second);
		PointerBlock cur = pointerIndex().getPointerBlock(id);
		if(cur.getBlockId().equals(pair.first))
		    result.add(cur);
	    } catch(Throwable _) {
		// bad data, ignore
		continue;
	    }
	}

	return result;
    }

    /** Return a set of words from a title.
     *  E.g. "Hifi sp82;kzz1 anT hifi anT"
     *  would become {"hifi","sp82","kzzl","ant"}.
     */
    public static Set splitWords(String title) {
	Set words = new HashSet();

	String s = "";
	for(int i=0; i<title.length(); i++) {
	    char c = title.charAt(i);
	    if(Character.isLetterOrDigit(c))
		s += c;
	    else {
		if(s.length() > 0) words.add(s.toLowerCase());
		s = "";
	    }
	}

	if(s.length() > 0)
	    words.add(s.toLowerCase());

	return words;
    }

    protected Set getWordRefs(String word) throws IOException {
	Collection mappings = pool.getMappings(uri, word).block();
	Set result = new HashSet();
	for(Iterator i=mappings.iterator(); i.hasNext();) {
	    IndexedPool.Mapping m = (IndexedPool.Mapping)i.next();
	    result.add(new Pair(m.block, m.value));
	}
	return result;
    }

    protected static class IndexType implements IndexedPool.IndexType {
	public Set getMappings(Block block) throws IOException {
	    PointerBlock p;
	    try {
		p = new PointerBlock(block);
	    } catch(Throwable _) {
		if(dbg) _.printStackTrace();
		return Collections.EMPTY_SET;
	    }

	    Set mappings = new HashSet();

	    String name = p.getName();
	    if(name == null) return mappings;

	    Set words = splitWords(name);

	    for(Iterator i=words.iterator(); i.hasNext();) {
		// Mapping word in title -> pointer
		mappings.add(new IndexedPool.Mapping(block.getId(), 
						     (String)i.next(),
						     p.getPointer().getURI()));
	    }

	    return mappings;
	}

	public Object createIndex(IndexedPool pool) {
	    return new TitleIndex(pool);
	}
	
	public String getIndexTypeURI() {
	    return uri;
	}
	
	public String getHumanReadableName() {
	    return ("An index of 0.1 pointers by pointer title.");
	}
    }

    protected final PointerIndex pointerIndex() {
	return (PointerIndex)pool.getIndex(PointerIndex.uri);
    }

    public TitleIndex(IndexedPool pool) {
	this.pool = pool;
    }
}
