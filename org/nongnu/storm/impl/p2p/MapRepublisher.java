/*
MapRepublisher.java
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
 *    
 */
/*
 * Written by Benja Fallenstein
 */
package org.nongnu.storm.impl.p2p;
import org.nongnu.storm.*;
import org.nongnu.storm.impl.*;
import org.nongnu.storm.util.ByteArrayKey;

import java.io.*;
import java.net.InetAddress;
import java.lang.ref.SoftReference;
import java.util.*;

/** A wrapper around <code>P2PMap</code> that automatically
 *  re-publishes mappings before they time out.
 *  Mappings are republished until <code>remove()</code> is called
 *  or 
 */
public class MapRepublisher {
    static public boolean dbg = true;
    static private void pa(String s) { System.out.println(s); }

    protected P2PMap map;
    protected Set entries = new HashSet();

    protected int republishInterval, initRepublishInterval;
    
    public MapRepublisher(P2PMap map,
			  int initRepublishInterval) {
	this.map = map;
	this.initRepublishInterval = initRepublishInterval;
	this.republishInterval = initRepublishInterval;

	// thread used to republish items into the map
	keepaliveThread.start();
    }

    public MapRepublisher(P2PMap map) {
	this(map, 25*60*1000);
    }

    public P2PMap getMap() { return map; }

    public void put(String key, String value) throws IOException {
	map.put(key, value, initRepublishInterval+5*60*1000);
	synchronized(entries) {
	    entries.add(new Entry(key, value));
	}
	/*
	if(!map.get(key).block().contains(value))
	    throw new Error("value not retained");
	*/
    }

    public void remove(String key, String value) throws IOException {
	map.remove(key, value);
	synchronized(entries) {
	    entries.remove(new Entry(key, value));
	}
    }

    /** Publish all entries from the map in the DHT again.
     *  Used to keep them alive after timeout.
     */
    public void republish() throws IOException {
	synchronized(entries) {
	    int newInterval = Integer.MAX_VALUE;
	    for(Iterator i=entries.iterator(); i.hasNext();) {
		Entry e = (Entry)i.next();
		int t = map.put(e.key, e.value, 
				initRepublishInterval+5*60*1000);
		newInterval = min(newInterval, t);
	    }
	    if(newInterval == Integer.MAX_VALUE) // no entries
		republishInterval = initRepublishInterval;
	    else
		republishInterval = newInterval;
	}
    }

    public Collector get(String key) throws IOException {
	return map.get(key);
    }

    protected class Entry {
	protected final String key, value;
	protected Entry(String k, String v) { key=k; value=v; }
    }

    protected Thread keepaliveThread = new Thread(new Keepalive(this));
    protected static class Keepalive implements Runnable {
	SoftReference ref;
	protected Keepalive(MapRepublisher map) {
	    this.ref = new SoftReference(map);
	}
	public void run() {
	    boolean hadError = false;
	    while(true) {
		// wait republishInterval millis before republishing everything
		// e.g. GISP timeout is 30 minutes -- 25 min are good there
		int republishInterval =
		    ((MapRepublisher)ref.get()).republishInterval;
		try {
		    if(!hadError) 
			Thread.sleep(republishInterval);
		    else
			Thread.sleep(5*60*1000); // only 5 minutes
		} catch(InterruptedException _) {}
		MapRepublisher map = (MapRepublisher)ref.get();
		hadError = false;
		if(map == null) break;
		if(dbg) pa("Republish MapRepublisher");
		try {
		    map.republish();
		} catch(IOException e) {
		    e.printStackTrace();
		    hadError = true;
		}
	    }
	    if(dbg) pa("MapRepublisher object garbage collected");
	}
    }

    protected void finalize() {
	keepaliveThread.interrupt();
    }

    private int min(int a, int b) { return a>b ? b : a; }
}
