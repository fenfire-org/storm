/*
BambooPeer.java
 *    
 *    Copyright (c) 2003-2004, Benja Fallenstein
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
 */
/*
 * Written by Benja Fallenstein
 */
package org.nongnu.storm.modules.bamboo;
import org.nongnu.storm.*;
import org.nongnu.storm.impl.*;
import org.nongnu.storm.impl.p2p.*;
import org.nongnu.storm.references.PointerIndex;
import org.nongnu.storm.util.HTTPProxy;
import org.nongnu.storm.util.HtmlLinkIndex;
import org.nongnu.storm.util.IPUtil;

import java.io.*;
import java.net.InetAddress;
import java.util.*;
import com.axlight.jnushare.gisp.*;

/** A Storm peer based on the Bamboo DHT.
 *  This connects to a specified Bamboo node and uses it to access
 *  the DHT network.
 */
public class BambooPeer extends Peer {
    static public boolean dbg = false;
    static private void p(String s) { System.out.println(s); }

    public BambooPeer(IndexedPool publishedPool,
		      StormPool cachePool, InetAddress nodeAddr, 
		      int nodePort) throws Exception {
	super(publishedPool, cachePool, 
	      new BambooP2PMap(nodeAddr, nodePort));
    }

    public static void main(String argv[]) throws Exception { 
	IndexedPool publishedPool = null;

	InetAddress node_addr = InetAddress.getByName("himalia.it.jyu.fi");
	int node_port = 5556;

	int gateway_port = -1;
	boolean global = false; // allow http connections from non-localhost?
	Set indexTypes = new HashSet();
	indexTypes.add(HtmlLinkIndex.type);
	indexTypes.add(PointerIndex.type);

	int i=0;
	while(i<argv.length) {
	    if(argv[i].equals("-pub")) {
		i++;
		p("Publish pool: "+argv[i]);
		publishedPool = new DirPool(new File(argv[i]),
					    indexTypes);
	    } else if(argv[i].equals("-gw")) {
		i++;
		gateway_port = Integer.parseInt(argv[i]);
	    } else if(argv[i].equals("-node")) {
		i++;
		int p = argv[i].indexOf(':');

		String s_addr = (p<0) ? argv[i] : argv[i].substring(0,p);
		node_addr = InetAddress.getByName(s_addr);

		String s_port = (p<0) ? "5556" : argv[i].substring(p+1);
		node_port = Integer.parseInt(s_port);
	    } else if(argv[i].equals("-global")) {
		global = true;
	    } else if(argv[i].equals("-h") || argv[i].equals("-help")) {
		p("A program for XXX");
		p("Usage: BambooPeer [-pub directory] [-gw port] [-node address[:port]]");
		System.exit(0);
	    } else {
		break;
	    }
	    i++;
	}

	if(publishedPool == null && gateway_port < 0) {
	    p("");
	    p("No pools to publish and no gateway requested.");
	    p("Specify at least either -pub or -gw.");
	    p("");
	    System.exit(1);
	} 

	if(publishedPool == null)
	    publishedPool = new TransientPool(indexTypes);

	BambooPeer peer = new BambooPeer(publishedPool,
					 new TransientPool(indexTypes),
					 node_addr, node_port);

	if(gateway_port >= 0) {
	    HTTPProxy gateway = new HTTPProxy(peer.getPool(), gateway_port);
	    if(global) gateway.allowGlobalConnections(true);
	    new Thread(gateway).start();
	    p("HTTP gateway port: "+gateway_port);
	}

	p("");
	p("Peer running.");	
    }
}
