/*
P2PPool.java
 *    
 *    Copyright (c) 2003, Benja Fallenstein
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
package org.nongnu.storm.impl.p2p;
import org.nongnu.storm.*;
import org.nongnu.storm.impl.*;
import org.nongnu.storm.util.*;
import java.io.*;
import java.net.*;
import java.util.*;

/** P2P-based, indexed Storm pool.
 *  This pool retrieves blocks from a Storm P2P network.
 *  <p>
 *  The distributed search network is accessed through the
 *  <code>P2PMap</code> interface. Different implementations
 *  are possible.
 *  <p>
 *  XXX Become asynchronous -> don't subclass AbstractLocalPool...
 */
public class P2PPool extends AbstractLocalPool {
    static public boolean dbg = true;
    static private void p(String s) { System.out.println(s); }

    protected MapRepublisher map;
    protected StormPool cache;

    public P2PPool(MapRepublisher map, StormPool cache,
		   Set indexTypes) throws IOException {
	super(indexTypes);
	this.map = map;
	this.cache = cache;
    }

    public Block get(BlockId id) throws IOException {
	try {
	    p("Trying to find the block from the local pool...");
	    return cache.get(id);	    
	} catch(FileNotFoundException _) {
	    p("Block not found in local pool...");
	    Collector c = map.get(id.toString());
	    for(Iterator i=c.blockingIterator(); i.hasNext();) {
		String url = (String)i.next();
		try {
		    p("Trying to connect to: " + url);
		    URLConnection conn = new URL(url).openConnection();
		    conn.connect();
		    for(int x=0;;x++) {
			Object o = conn.getHeaderField(x);
			if(o == null) break;
			p("HF: "+o);
		    }
		    p("conn");
		    String contentType = conn.getContentType();		    
		    p("gettype");
		    if(contentType == null) continue;
		    p("hastype");

		    // The content type may contain spaces like this:
		    // "text/html; charset=utf-8"
		    // Storm would choke on these, but we can
		    // safely remove them...
		    int sp;
		    while((sp = contentType.indexOf(' ')) >= 0)
			contentType = contentType.substring(0, sp) +
			              contentType.substring(sp+1);

		    p("sp removed");
		    BlockOutputStream bos = 
			cache.getBlockOutputStream(contentType);
		    p("bos");
		    CopyUtil.copy(conn.getInputStream(), bos);
		    p("copied");
		    if(bos.getBlockId().equals(id)) { 
			p("Block found @ " + url);
			return bos.getBlock();
		    } else { 
			p("Block ids didn't match: "+id+" / "+
			bos.getBlockId()+" (from "+url+").");
		    }
		} catch(IOException e) {
		    e.printStackTrace();
		    // next iteration
		}
	    }
	    throw new FileNotFoundException(""+id);
	}
    }

    public SetCollector getIds() throws IOException {
	Collector c = map.get("http://fenfire.org/2003/05/published-blocks");
	final AsyncSetCollector result = new AsyncSetCollector();

	for(Iterator i=cache.getIds().iterator(); i.hasNext();)
	    result.receive(i.next());

	c.addCollectionListener(new CollectionListener() {
		public boolean item(Object item) {
		    result.receive(new BlockId((String)item));
		    return true;
		}
		public void finish(boolean timeout) {
		    result.finish(timeout);
		}
	    });
	return result;
    }

    public void add(Block b) {
	throw new UnsupportedOperationException("P2PPool is retrieval only");
    }

    public void delete(Block b) {
	throw new UnsupportedOperationException("P2PPool is retrieval only");
    }

    public BlockOutputStream getBlockOutputStream(String contentType) {
	throw new UnsupportedOperationException("P2PPool is retrieval only");
    }

    public Collector getMappings(String typeURI, String key) 
	throws IOException {

	if(dbg) p("p2p.Query <"+typeURI+" "+key+">");
	Collector collector = map.get(typeURI+" "+key);
	AsyncSetCollector result = new AsyncSetCollector();
	CollectionListener l = new MyListener(key, result);
	collector.addCollectionListener(l);
	return result;
    }

    private class MyListener implements CollectionListener {
	String key;
	AsyncSetCollector addTo;
	MyListener(String key, AsyncSetCollector addTo) {
	    this.key = key;
	    this.addTo = addTo;
	}
	public boolean item(Object o) {
	    try {
		String s = (String)o;
		if(dbg) p("p2p.Result <"+s+">");
		int sp = s.indexOf(' ');
		if(sp < 0) return true; // ignore bad data
		BlockId id = new BlockId(s.substring(0, sp));
		String value = s.substring(sp+1);
		addTo.receive(new IndexedPool.Mapping(id, key, value));
	    } catch(IllegalArgumentException _) {
		if(dbg) _.printStackTrace();
		// When there's an exception when creating the id, 
		// assume that the data we received from the net 
                // was bad; simply ignore.
	    }
	    return true;
	}
	public void finish(boolean timeout) {
	    if(dbg) p("p2p.Finish");
	    addTo.finish(timeout);
	}
    }
}
