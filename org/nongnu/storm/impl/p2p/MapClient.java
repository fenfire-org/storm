/*
MapClient.java
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
import org.nongnu.storm.Collector;
import org.nongnu.storm.impl.SimpleSetCollector;
import org.nongnu.storm.util.CopyUtil;
import org.nongnu.storm.http.*;
import org.nongnu.storm.http.client.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 */
public class MapClient implements P2PMap {
    protected HTTPConnection conn;

    public MapClient(String host, int port) throws IOException {
	this.conn = new HTTPConnection(host, port);
    }

    public MapClient(HTTPConnection conn) {
	this.conn = conn;
    }

    public Collector get(String key) throws IOException {
	// XXX should be asynchronous
	HTTPRequest req = conn.newRequest("GET", uri(key), 
					  "HTTP/1.1", false);
	HTTPResponse res = conn.send(req);
	BufferedReader r = 
	    new BufferedReader(new InputStreamReader(res.getInputStream(),
						     "UTF-8"));
	Set s = new HashSet();
	while(true) {
	    String value = r.readLine();
	    if(value == null) break;
	    s.add(value); // We need to provide added value to secure funding
	}
	return new SimpleSetCollector(s);
    }

    public int put(String key, String value, int timeout) throws IOException {
	String s = doPost("PUT "+timeout, key, value);
	if(!s.startsWith("OK ") || !s.endsWith("\n"))
	    throw new IOException("Malformed PUT reply: "+s);
	return Integer.parseInt(s.substring(3, s.length()-1));
    }

    public void remove(String key, String value) throws IOException {
	String s = doPost("REMOVE", key, value);
	if(!s.equals("OK\n"))
	    throw new IOException("Malformed REMOVE reply: "+s);
    }

    protected String doPost(String method, String key, 
			    String value) throws IOException {
	String msg = method+"\n"+value+"\n";
	byte[] bytes = msg.getBytes("UTF-8");

	HTTPRequest req = conn.newRequest("POST", uri(key), 
					  "HTTP/1.1", true);
	req.setField("Content-Type", "text/plain; charset=UTF-8");
	req.commit();
	req.getOutputStream().write(bytes);
	req.getOutputStream().close();
	HTTPResponse res = conn.send(req);
	if(res.status != 200)
	    throw new IOException(method.toLowerCase()+"() not successful: "+
				  res.status+" "+res.reason);
	Reader r = new InputStreamReader(res.getInputStream(), "US-ASCII");
	StringBuffer buf = new StringBuffer();
	while(true) {
	    int c = r.read();
	    if(c < 0) break;
	    buf.append((char)c);
	}
	return buf.toString();
    }

    protected String uri(String key) throws IOException {
	return "/?key=" + URLEncoder.encode(key, "UTF-8");
    }


    public static void main(String[] argv) throws Exception {
	MapClient m = new MapClient(argv[0], Integer.parseInt(argv[1]));
	m.put("foo", "aaah", 10*60*1000);
	m.put("foo", "beeeeh", 10*60*1000);
	m.put("foo", "ceeh", 10*60*1000);
	m.remove("foo", "ceeh");
	Collector c = m.get("foo").block();
	for(Iterator i=c.iterator(); i.hasNext();)
	    System.out.println(i.next());
    }
}
