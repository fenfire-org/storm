/*
StormSync.java
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
package org.nongnu.storm.util;
import org.nongnu.storm.*;
import org.nongnu.storm.impl.*;
import org.nongnu.storm.references.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

/** Synchronization between Storm pools.
 */
public class StormSync {

    public static final class Mode { private Mode() {} }
    public static final Mode 
	LOCAL_TO_REMOTE = new Mode(),
	REMOTE_TO_LOCAL = new Mode(),
	BOTH_WAYS       = new Mode();
    

    public static class AutoSync extends Thread {
	protected StormPool local, remote;
	protected boolean localToRemote = false;
	protected boolean remoteToLocal = false;
	protected int interval;

	public AutoSync(StormPool local, StormPool remote,
			int interval) {
	    this(local, remote, interval, BOTH_WAYS);
	}

	public AutoSync(StormPool local, StormPool remote,
			int interval, Mode mode) {
	    if(mode == LOCAL_TO_REMOTE) localToRemote = true;
	    else if(mode == REMOTE_TO_LOCAL) remoteToLocal = true;
	    else if(mode == BOTH_WAYS) {
		localToRemote = true;
		remoteToLocal = true;
	    }

	    this.local = local; this.remote = remote; 
	    this.interval = interval;
	}

	public void run() {
	    while(true) {
		try {
		    Thread.sleep(interval);
		} catch(InterruptedException _) {}
		if(remoteToLocal)
		    try { sync(remote, local); }
		    catch(IOException _) { _.printStackTrace(); }
		if(localToRemote)
		    try { sync(local, remote); }
		    catch(IOException _) { _.printStackTrace(); }
	    }
	}
    }

    public static void sync(StormPool local, 
			    StormPool remote) throws IOException {
	copyMissing(remote, local);
	copyMissing(local, remote);
    }

    /** A pool which auto-synchronizes itself with another, remote pool.
     *  This is a wrapper for a local pool which, in addition to
     *  synchronizing itself with a remote pool in regular intervals,
     *  also automatically adds blocks to and deletes blocks from
     *  the remote pool when they are added to/deleted from the
     *  local pool through this interface.
     */
    public static class AutoSyncPool extends AbstractFilterPool {
	protected IndexedPool remote;
	protected AutoSync autoSync;

	public AutoSyncPool(IndexedPool local, IndexedPool remote,
			    int interval) {
	    super(local);
	    this.remote = remote;
	    this.autoSync = new AutoSync(local, remote, interval);
	    this.autoSync.start();
	}

	public void add(Block b) throws IOException {
	    System.out.println("Add locally: "+b.getId());
	    super.add(b);
	    System.out.println("Add remotely");
	    remote.add(b);
	    System.out.println("Addded");
	}

	public void delete(Block b) throws IOException {
	    super.delete(b);
	    remote.delete(b);
	}

	public BlockOutputStream getBlockOutputStream(String contentType)
	    throws IOException {

	    final BlockOutputStream localBOS =
		pool.getBlockOutputStream(contentType);

	    return new BlockOutputStream(localBOS, contentType) {
		    public void close() throws IOException {
			localBOS.close();
			remote.add(localBOS.getBlock());
		    }
		    public Block getBlock() throws IOException, IllegalStateException {
			return localBOS.getBlock();
		    }
		};
	}
    }

    /** Copy all blocks from src to target that are in the former
     *  but not in the latter (according to getIds()).
     */
    public static void copyMissing(StormPool src, 
				   StormPool target) throws IOException{
	Collector c1 = src.getIds(), c2 = target.getIds();
	c1.block(); c2.block();
	HashSet ids = new HashSet(c1);
	ids.removeAll(c2);
	
	for(Iterator i=ids.iterator(); i.hasNext();) {
	    try {
		BlockId id = (BlockId)i.next();
		target.add(src.get(id));
	    } catch(IOException _) {
		_.printStackTrace();
	    }
	}
    }

    public static void main(String[] argv) throws Exception {
	Set indexTypes = Collections.EMPTY_SET;
	StormPool pool1 = HTTPProxy.getPool(argv[0], indexTypes);
	StormPool pool2 = HTTPProxy.getPool(argv[1], indexTypes);

	sync(pool1, pool2);
    }
}
