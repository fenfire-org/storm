/*
GispP2PMap.java
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
import org.nongnu.storm.util.ByteArrayKey;

import java.io.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.lang.ref.SoftReference;
import java.util.*;
import com.axlight.jnushare.gisp.*;

/** A <a href="http://gisp.jxta.org/">GISP</a>-based implementation
 *  of <code>P2PMap</code>.
 *  <p>
 *  Note that because of license difficulties (cannot use 
 *  JXTA-licensed code in LGPLed code because of 
 *  the advertisement clause), this class is under the X11 license.
 *  That's also the main reason this is in a module.
 */
public class GispP2PMap implements P2PMap {
    static public boolean dbg = false;
    static private void p(String s) { System.out.println(s); }

    protected GISP gisp;
    protected Map cache = new HashMap();
    
    /** The "STORM" port. */
    public static final int PORT = 57083;

    public GispP2PMap(InetAddress ip, int port, 
		      String[] seedAddresses) throws Exception {
	this(ip, port, seedAddresses, true);
    }

    public GispP2PMap(InetAddress ip, int port, String[] seedAddresses, 
		      boolean testPublic) throws Exception {
	if(testPublic && !org.nongnu.storm.util.IPUtil.isGlobal(ip))
	    throw new IllegalArgumentException("GispP2PMap initialized with nonglobal host ip: "+ip);

	if(port < 0)
	    port = 12415 + (int)(2000 * new java.util.Random().nextDouble());

	GISPUDP gisp = new GISPUDP(ip.getHostAddress(), port);
	this.gisp = gisp;
	if(seedAddresses != null)
	    gisp.addSeedAddresses(seedAddresses);
	gisp.start(new String[] {"strength_min=1"});

	/** Doesn't work...maybe requested too quickly
	 p("Number of known peers in the network: " + String.valueOf(this.gisp.getNumOfPeers()));
	*/	
    }

    public GispP2PMap(InetAddress ip, int port) throws Exception {
	this(ip, port, (String[])null);
    }

    public GISP getGisp() { return gisp; }

    public int put(String key, String value, int timeout) throws IOException {
	timeout = min(timeout, 30*60*1000);
	gisp.insert(URLEncoder.encode(key), URLEncoder.encode(value), timeout);
	cache.remove(key);
	return timeout;
    }

    public void remove(String key, String value) throws IOException {
	cache.remove(key);
    }

    /** Publish all entries from the map in the DHT again.
     *  Used to keep them alive after timeout.
     */
    public Collector get(String key) throws IOException {
	if(dbg) p("get collector for "+key);
	Collector c = (Collector)cache.get(key);
	// after two minutes, we don't use the cached results;
	// instead, we re-query the hashtable
	if(c == null || c.getAge() > 2 * 60 * 1000) {
	    c = new MyCollector(key);
	    cache.put(key, c);
	}
	return c;
    }

    protected class MyCollector extends AsyncSetCollector implements ResultListener {

	protected MyCollector(String key) {
	    if(dbg) p("Querying key from the P2P network: " + key);
	    gisp.query(URLEncoder.encode(key), this, 2500);    
	}

	public void stringResult(String data){
	    data = URLDecoder.decode(data);
	    if(dbg) p("Receive from P2P network: "+data);
	    if(state != 0) {
		p("GispP2PMap.MyCollector already closed -> "+data);
		return;
	    }
	    receive(data);
	}

	public void xmlResult(byte[] data){
	    // ignore xml results from GISP
	}	

	public void queryExpired() {
	    finish(true);
	}
    }

    private int min(int a, int b) { return a>b ? b : a; }

    public static void main(String argv[]) throws Exception { 
	System.out.print("Args: ");
	for(int i=0; i<argv.length; i++) System.out.print(argv[i]+" ");
	p("\n");

	InetAddress ip = null;
	int port = PORT;
	int http_port = -1;

	int i=0;
	while(i<argv.length) {
	    if(argv[i].equals("-myip")) {
		i++;
		ip = InetAddress.getByName(argv[i]);
	    } else if(argv[i].equals("-port")) {
		i++;
		port = Integer.parseInt(argv[i]);
	    } else if(argv[i].equals("-serve")) {
		i++;
		http_port = Integer.parseInt(argv[i]);
	    } else if(argv[i].equals("-h") || argv[i].equals("-help")) {
		p("Usage: GispP2PMap [-serve httpport] [-myip address] [-port port] seed1 seed2 ...");
		System.exit(0);
	    } else {
		break;
	    }
	    i++;
	}

	if(ip == null)
	    ip = InetAddress.getLocalHost();

	String[] seeds = new String[argv.length-i];
	System.arraycopy(argv, i, seeds, 0, seeds.length);

	GispP2PMap map = new GispP2PMap(ip, PORT, seeds);
	if(http_port >= 0)
	    new MapServer(map, http_port).run();
    }
}
