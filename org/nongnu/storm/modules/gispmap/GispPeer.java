/*
GispPeer.java
 *    
 *    Copyright (c) 2003-2004, Benja Fallenstein
 *    
 *    This file is part of Storm.
 *    
 *    Permission is hereby granted, free of charge, 
 *    to any person obtaining a copy of this software 
 *    and associated documentation files (the "Software"), 
 *    to deal in the Software without restriction, 
 *    including without limitation the rights to use, 
 *    copy, modify, merge, publish, distribute, sublicense, 
 *    and/or sell copies of the Software, and to permit persons 
 *    to whom the Software is furnished to do so, 
 *    subject to the following conditions:
 *    
 *    The above copyright notice and this permission notice 
 *    shall be included in all copies or substantial portions 
 *    of the Software.
 *    
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY 
 *    OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
 *    THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR 
 *    A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT 
 *    SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 *    FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 *    IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 *    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE 
 *    OR OTHER DEALINGS IN THE SOFTWARE.
 *    
 */
/*
 * Written by Benja Fallenstein
 */
package org.nongnu.storm.modules.gispmap;
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

/** A Storm peer based on the GISP hashtable.
 */
public class GispPeer extends Peer {
    static public boolean dbg = false;
    static private void p(String s) { System.out.println(s); }

    public GispPeer(IndexedPool publishedPool,
		    StormPool cachePool, InetAddress ip, int port,
		    String[] seedAddresses) throws Exception {
	super(publishedPool, cachePool, 
	      new GispP2PMap(ip, port, seedAddresses));
    }

    public static void main(String argv[]) throws Exception { 
	IndexedPool publishedPool = null;
	InetAddress ip = null;
	int gisp_port = GispP2PMap.PORT;
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
	    } else if(argv[i].equals("-myip")) {
		i++;
		ip = InetAddress.getByName(argv[i]);
	    } else if(argv[i].equals("-port")) {
		i++;
		gisp_port = Integer.parseInt(argv[i]);
	    } else if(argv[i].equals("-global")) {
		global = true;
	    } else if(argv[i].equals("-h") || argv[i].equals("-help")) {
		p("A program for XXX");
		p("Usage: GispPeer [-pub directory] [-gw port] [-myip address] [-port port] seed1 seed2 ...");
		System.exit(0);
	    } else {
		break;
	    }
	    i++;
	}

	if(ip == null)
	    ip = InetAddress.getLocalHost();

	if(!IPUtil.isGlobal(ip)) {
	    p("The IP address for use in GISP ("+ip+") is not a global");
	    p("IP address. Please use -myip to specify this host's");
	    p("IP address on the command line.");
	    System.exit(1);
	}

	String[] seeds = new String[argv.length-i];
	System.arraycopy(argv, i, seeds, 0, seeds.length);
	
	GispPeer peer = null;
	while(peer == null) {
	    try {
		if(publishedPool == null && gateway_port < 0) {
		    new GispP2PMap(ip, gisp_port, seeds);
		    p("");
		    p("No pools to publish and no gateway requested.");
		    p("Plain GISP P2P map started instead of a full peer.");
		    p("");
		    break; // Leaving peer null!
		} else {
		    peer = new GispPeer(publishedPool,
					new TransientPool(indexTypes),
					ip, gisp_port, seeds);
		}
	    } catch(java.net.BindException _) {
		p("Port "+gisp_port+" couldn't be used.");
		gisp_port++;
	    }
	}

	if(gisp_port == GispP2PMap.PORT)
	    p("GISP UDP port: default of "+gisp_port);
	else
	    p("GISP UDP port: "+gisp_port);
	if(gateway_port >= 0) {
	    HTTPProxy gateway = new HTTPProxy(peer.getPool(), gateway_port);
	    if(global) gateway.allowGlobalConnections(true);
	    new Thread(gateway).start();
	    p("HTTP gateway port: "+gateway_port);
	}
	if(seeds.length == 0) {
	    p("");
	    p("No seed peers given, starting new network.");
	    p("Give this peer as a seed to other peers.");
	} else 
	    p("Trying to use "+seeds[0]+" as the first seed peer.");
	p("");
	p("Peer running.");	
    }
}
