/*
BambooP2PMap.java
 *    
 *    Copyright (c) 2004, Benja Fallenstein
 *    
 *    Portions Copyright (c) 2001-2003 Regents of the University of California.
 *    All rights reserved.
 *
 *    Redistribution and use in source and binary forms, with or without
 *    modification, are permitted provided that the following conditions
 *    are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *    3. Neither the name of the University nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *    
 *    THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS
 *    IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *    FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 *    REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 *    OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *    EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * Written by Benja Fallenstein
 */
package org.nongnu.storm.modules.bamboo;
import org.nongnu.storm.*;
import org.nongnu.storm.impl.*;
import org.nongnu.storm.impl.p2p.*;
import org.nongnu.storm.util.ByteArrayKey;

import java.io.*;
import java.net.InetAddress;
import java.lang.ref.SoftReference;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import bamboo.dht.*;
import org.acplt.oncrpc.OncRpcProtocols;
import org.acplt.oncrpc.OncRpcException;

/** A <a href="http://www.bamboo-dht.org/">Bamboo</a>-based implementation
 *  of <code>P2PMap</code>.
 *  <p>
 *  Under BSD license because some code was copied from Bamboo
 *  (and I'm too lazy right now to find out whether/how I can
 *  re-license that to LGPL).
 */
public class BambooP2PMap implements P2PMap {
    static public boolean dbg = true;
    static private void p(String s) { System.out.println(s); }

    protected InetAddress addr;
    protected int port;

    protected bamboo.dht.gateway_protClient client;

    protected void newClient() throws IOException, OncRpcException {
	client =
	    new gateway_protClient(addr, port, OncRpcProtocols.ONCRPC_TCP);
    }

    public BambooP2PMap(int port) throws IOException {
	this(InetAddress.getByName("127.0.0.1"), port);
    }

    public BambooP2PMap(InetAddress addr, int port) throws IOException {
	this.addr = addr;
	this.port = port;

	try {
	    newClient();
	} catch(OncRpcException e) {
	    e.printStackTrace();
	    throw new IOException(""+e);
	}
    }



    public int put(String key, String value, int timeout) throws IOException {
        bamboo_put_args args = new bamboo_put_args ();

	args.key = key(key);
	args.value = value(value);
	args.ttl_sec = timeout / 1000;
	args.secret = new bamboo_secret ();
	args.secret.value = new byte [20];
	
	int status;
	try {
	    status = client.BAMBOO_DHT_PROC_PUT_1(args);
	} catch(OncRpcException _) {
	    try {
		newClient();
		status = client.BAMBOO_DHT_PROC_PUT_1(args);
	    } catch(OncRpcException e) {
		e.printStackTrace();
		throw new IOException(""+e);
	    }
	}

        if (status != bamboo_stat.BAMBOO_OK)
	    throw new IOException("Bamboo put() failed");

	return timeout;
    }

    public void remove(String key, String value) throws IOException {
	// Bamboo doesn't have remove, do nothing
    }

    public Collector get(String key) throws IOException {
        bamboo_placemark mark = new bamboo_placemark();
	mark.value = new byte[0];

        bamboo_get_args args = new bamboo_get_args();

	// XXX make async
	Set result = new HashSet();

	boolean firstRound = true;

	while(true) {
	    args.key = key(key);
	    args.maxvals = Integer.MAX_VALUE;
	    args.all = true;
	    args.placemark = mark;
	    
	    bamboo_get_res res;

	    try {
		res = client.BAMBOO_DHT_PROC_GET_1(args);
	    } catch(OncRpcException e) {
		if(firstRound) {
		    try {
			newClient();
			res = client.BAMBOO_DHT_PROC_GET_1(args);
		    } catch(OncRpcException f) {
			f.printStackTrace();
			throw new IOException(""+f);
		    }
		} else {
		    e.printStackTrace();
		    throw new IOException(""+e);
		}
	    }


	    if(res.values.length == 0) break;
	    
	    for(int i=0; i<res.values.length; i++) {
		String value = new String(res.values[i].value, "UTF-8");
		result.add(value);
	    }
	    
	    mark = res.placemark;
	    firstRound = false;
	}

	return new SimpleSetCollector(result);
    }
    


    protected static bamboo_key key(String key) throws IOException {
        MessageDigest md;
	try {
	    md = MessageDigest.getInstance ("SHA");
	} catch(NoSuchAlgorithmException e) {
	    throw new IOException(""+e);
	}
        bamboo_key k = new bamboo_key ();
        k.value = md.digest(key.getBytes("UTF-8"));
	return k;
    }

    protected static bamboo_value value(String value) throws IOException {
	bamboo_value v = new bamboo_value();
	v.value = value.getBytes("UTF-8");
	return v;
    }
}
