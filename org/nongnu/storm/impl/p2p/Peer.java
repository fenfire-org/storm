/*
Pool.java
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
import org.nongnu.storm.references.PointerIndex;
import java.io.*;
import java.net.*;
import java.util.*;

/** A class that publishes a Storm pool through p2p and
 *  makes the p2p network available as another pool.
 */
public class Peer {
    
    static private void p(String s) { System.out.println(s); }

    protected P2PPool pool;
    protected IndexedPool publishedPool;
    protected MapRepublisher map;
    protected HTTPProxy server;

    public Peer(IndexedPool publishedPool,
		StormPool cachePool,
		P2PMap map) throws IOException {
	if(publishedPool == null)
	    publishedPool = new TransientPool(Collections.EMPTY_SET);

	this.publishedPool = publishedPool;
	this.map = new MapRepublisher(map);

	pool = new P2PPool(this.map, cachePool, 
			   publishedPool.getIndexTypes());

	int port = 37000;
	while(server == null) {
	    try {
		server = new HTTPProxy(publishedPool, port);
	    } catch(java.net.BindException _) {
		port++;
	    }
	}
	server.allowGlobalConnections(true);
	publish();
	new Thread(server).start();
    }

    /** Re-publish the contents of <code>publishedPool</code>
     *  to the p2p map.
     */
    public void publish() throws IOException {
	Set types = publishedPool.getIndexTypes();
	for(Iterator i=publishedPool.getIds().block().iterator();
	    i.hasNext();) {
	    BlockId id = (BlockId)i.next();
	    Block block = publishedPool.get(id);
	    map.put(id.toString(), server.getURL()+id.toString());
	    map.put("http://fenfire.org/2003/05/published-blocks",
		    id.toString());
	    
	    p("Published: " + id.toString() + "(key):" + server.getURL() +
	    id.toString()+ "(value)\n");
	    
	    for(Iterator j=types.iterator(); j.hasNext();) {
		IndexedPool.IndexType type = 
		    (IndexedPool.IndexType)j.next();
		Set mappings = type.getMappings(block);
		for(Iterator k = mappings.iterator(); k.hasNext();) {
		    IndexedPool.Mapping m =
			(IndexedPool.Mapping)k.next();
		    p("Publish mapping: "+m.block+" "+m.key+" "+m.value+"\n");
		    map.put(type.getIndexTypeURI()+" "+m.key,
			    m.block+" "+m.value);
		}
	    }
	}
    }

    public IndexedPool getPool() {
	return pool;
    }

    public HTTPProxy getServer() {
	return server;
    }

    public static void main(String[] args) throws Exception {
	String dir = args[0]; 

	Set idx = new HashSet();
	idx.add(HtmlLinkIndex.type);
	idx.add(PointerIndex.type);
	DirPool pub = new DirPool(new File(dir), idx);
	StormPool cache = new TransientPool(idx);
	P2PMap map = new MockP2PMap();

	Peer p = new Peer(pub, cache, map);
	new HTTPProxy(p.getPool(), 5555).run();
    }
}
