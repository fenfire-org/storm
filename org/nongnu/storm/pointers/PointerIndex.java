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
import java.util.*;
import com.bitzi.util.*;
import java.io.*;
import java.security.*;

public class PointerIndex {
    public static boolean dbg = false;
    private static void p(String s) { System.out.println("PointerIndex:: "+s); }

    public static final String uri =
	"http://fenfire.org/2003/05/pointer-index-0.1";

    public static final IndexType type = new IndexType();

    protected IndexedPool pool;

    public Collector getIds() throws IOException {
	Collector c = pool.getMappings(uri, "");
	final AsyncSetCollector result = new AsyncSetCollector();
	c.addCollectionListener(new CollectionListener() {
		public boolean item(Object item) {
		    IndexedPool.Mapping m = (IndexedPool.Mapping)item;
		    result.receive(new PointerId(m.value));
		    return true;
		}
		public void finish(boolean timeout) {
		    result.finish(timeout);
		}
	    });
	return result;
    }

    public BlockId get(PointerId id)
	throws IOException, GeneralSecurityException {
	PointerBlock b = getPointerBlock(id);
	if(b == null) return null;
	return b.getTarget();
    }

    public String getTitle(PointerId id)
	throws IOException, GeneralSecurityException {
	PointerBlock b = getPointerBlock(id);
	if(b == null) return null;
	return b.getName();
    }

    /** Get a list of past pointer blocks of a pointer (newest first) */
    public SortedSet getHistory(PointerId id) throws IOException {
	SortedSet s = new TreeSet(new Comparator() {
		public int compare(Object o1, Object o2) {
		    PointerBlock p1 = (PointerBlock)o1;
		    PointerBlock p2 = (PointerBlock)o2;
		    if(p1.getTimestamp() == p2.getTimestamp())
			return 0;
		    else if(p1.getTimestamp() < p2.getTimestamp())
			return +1;
		    else
			return -1;
		}
	    });
	
	Collector c = pool.getMappings(uri, id.toString()).block();
	for(Iterator i=c.iterator(); i.hasNext();) {	
	    IndexedPool.Mapping m = (IndexedPool.Mapping)i.next();
	    if(dbg) p("Process: "+m.block+" "+m.value);

	    long timestamp;
	    try {
		timestamp = Long.parseLong(m.value);
	    } catch(NumberFormatException _) {
		// malformed entry
		continue;
	    }
	    PointerBlock p;
	    try {
		p = new PointerBlock(pool.get(m.block));
	    } catch(Throwable _) {
		System.out.println("Couldn't use '"+m.block+"' because: "+_);
		continue;
	    }

	    if(p.getPointer().equals(id) &&
	       p.getTimestamp() == timestamp) {
		s.add(p);
	    }
	}

	return s;
    }

    /** Return whether a particular pointer block is the
     *  newest one for its pointer.
     */
    public boolean isCurrent(PointerBlock pb) throws IOException, GeneralSecurityException {
	return pb.equals(getPointerBlock(pb.getPointer()));
    }

    public PointerBlock getPointerBlock(PointerId id) 
	throws IOException, GeneralSecurityException {
	if(dbg) p("Get: "+id);

	Collector c = pool.getMappings(uri, id.toString()).block();
	long maxstamp = 0;
	PointerBlock result = null;
	for(Iterator i=c.iterator(); i.hasNext();) {
	    IndexedPool.Mapping m = (IndexedPool.Mapping)i.next();
	    if(dbg) p("Process: "+m.block+" "+m.value);

	    long timestamp;
	    try {
		timestamp = Long.parseLong(m.value);
	    } catch(NumberFormatException _) {
		// malformed entry
		continue;
	    }
	    if(timestamp > maxstamp) {
		PointerBlock p;
		try {
		    p = new PointerBlock(pool.get(m.block));
		} catch(Throwable _) {
		    System.out.println("Couldn't use '"+m.block+"' because: "+_);
		    continue;
		}

		if(p.getPointer().equals(id) &&
		   p.getTimestamp() == timestamp) {
		    result = p;
		    maxstamp = timestamp;
		}
	    }
	}
	if(result == null) throw new FileNotFoundException();
	return result;
    }

    public void set(PointerId id, BlockId target, KeyPair keyPair) 
	throws IOException, GeneralSecurityException {
	PointerBlock current = (PointerBlock)getPointerBlock(id);
	if(current == null)
	    set(id, target, keyPair, null);
	else
	    set(id, target, keyPair, current.getName());
    }

    public void set(PointerId id, BlockId target, KeyPair keyPair,
		    String newName) 
	throws IOException, GeneralSecurityException {
	// XXX this assumes that the computer clock
	// is always set correctly: if there is an existing
	// pointer block with a later timestamp (because
	// a clock was set wrongly), this may not
	// actually change the pointer...
	long timestamp = System.currentTimeMillis();

	byte[] keyBytes = 
	    PointerId.getKeyBytes(keyPair.getPublic());

	String data =
	    id.toString() + "\n" +
	    Base32.encode(keyBytes) + "\n" +
	    timestamp + "\n" +
	    target.toString();

	if(newName != null) {
	    if(newName.indexOf("\n") >= 0)
		throw new IllegalArgumentException("Newline in ptr name");
	    data += "\n" + newName;
	}

	Signature s = Signature.getInstance("SHA1withDSA");
	s.initSign(keyPair.getPrivate());
	s.update(data.getBytes("US-ASCII"));
	byte[] signature = s.sign();

	BlockOutputStream bos = pool.getBlockOutputStream("text/plain");
	String header = 
	    PointerBlock.COOKIE + "\n" +
	    Base32.encode(signature) + "\n";
	bos.write(header.getBytes("US-ASCII"));
	bos.write(data.getBytes("US-ASCII"));
	bos.close();
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

	    // Mapping pointer id -> pointer blocks
	    // for resolving a pointer
	    mappings.add(new IndexedPool.Mapping(block.getId(),
						 p.getPointer().toString(),
						 ""+p.getTimestamp()));

	    // Mapping "" -> pointer id
	    // for finding all pointers
	    mappings.add(new IndexedPool.Mapping(block.getId(), "",
	    					 p.getPointer().toString()));

	    return mappings;
	}

	public Object createIndex(IndexedPool pool) {
	    return new PointerIndex(pool);
	}
	
	public String getIndexTypeURI() {
	    return uri;
	}
	
	public String getHumanReadableName() {
	    return ("An index of 0.1 pointer blocks by pointer URN.");
	}
    }

    public PointerIndex(IndexedPool pool) {
	this.pool = pool;
    }
}
