/*
MapServer.java
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
import org.nongnu.storm.http.*;
import org.nongnu.storm.http.server.*;
import java.net.*;
import java.io.*;
import java.util.*;

/** A server serving the mappings in a P2PMap over HTTP.
 */
public class MapServer implements Runnable {
    protected HTTPServer server;
    protected P2PMap map;

    public MapServer(P2PMap map, int port) throws IOException {
	this.map = map;
	this.server = new HTTPServer(new ConnectionFactory(), port);
    }

    public void run() { server.run(); }

    protected class ConnectionFactory extends HTTPConnection.Factory {
	public HTTPConnection newConnection(Socket s) throws IOException {
	    return new Connection(s);
	}
    }

    protected class Connection extends HTTPConnection {
	protected Connection(Socket s) throws IOException { super(s); }

	protected HTTPResponse doGet(HTTPRequest req, 
				     HTTPResponse.Factory factory) 
	    throws IOException {

	    String uri = req.getRequestURI();
	    if(uri.toLowerCase().startsWith("http://"))
		uri = uri.substring(uri.indexOf('/', 7));
	    if(!uri.startsWith("/?key="))
		return factory.makeError(404, "Not found");

	    String key = URLDecoder.decode(uri.substring(5), "UTF-8");

	    Collector c = map.get(key);
	    HTTPResponse resp = factory.makeResponse(200, "Ok");
	    resp.setField("Content-Type", "text/plain; charset=UTF-8");
	    resp.commit();
	    PrintWriter w = 
		new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), 
						       "UTF-8"));
	    for(Iterator i = c.blockingIterator(); i.hasNext();) {
		String value = (String)i.next();
		w.println(value);
	    }
	    w.close();
	    return resp;
	}

	protected HTTPResponse doPost(HTTPRequest req,
				      HTTPResponse.Factory factory) 
	    throws IOException {
	    
	    
	    String uri = req.getRequestURI();
	    if(uri.substring(0, 7).toLowerCase().equals("http://"))
		uri = uri.substring(uri.indexOf('/', 7));
	    if(!uri.startsWith("/?key="))
		return factory.makeError(404, "Not found");

	    String key = URLDecoder.decode(uri.substring(5), "UTF-8");
		
	    // XXX check content type, return error if wrong
	    BufferedReader r =
		new BufferedReader(new InputStreamReader(req.getInputStream(),
							 "UTF-8"));
	    String action = r.readLine();
	    int returnTimeout = -1;
	    if(action.startsWith("PUT ")) {
		int timeout = Integer.parseInt(action.substring(4));
		returnTimeout = Integer.MAX_VALUE;
		while(true) {
		    String value = r.readLine();
		    if(value == null || value.equals("")) break;
		    int t = map.put(key, value, timeout);
		    returnTimeout = min(returnTimeout, t);
		}
		if(returnTimeout == Integer.MAX_VALUE)
		    // no entries put
		    returnTimeout = timeout;
	    } else if(action.equals("REMOVE")) {
		while(true) {
		    String value = r.readLine();
		    if(value == null || value.equals("")) break;
		    map.remove(key, value);
		}
	    } else {
		return factory.makeError(400, "First line of POST message must be 'PUT' or 'REMOVE'");
	    }

	    HTTPResponse resp = factory.makeResponse(200, "Ok");
	    resp.setField("Content-Type", "text/plain");
	    resp.commit();
	    String txt = "OK\n";
	    if(returnTimeout >= 0)
		txt = "OK "+returnTimeout+"\n";
	    resp.getOutputStream().write(txt.getBytes("US-ASCII"));
	    return resp;
	}
    }

    private int min(int a, int b) { return a>b ? b : a; }

    public static void main(String argv[]) throws Exception {
	MockP2PMap map = new MockP2PMap();
	MapServer serv = new MapServer(map, 7482);
	new Thread(serv).start();
    }
}
