/*   
HTTPConnection.java
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
 * Written by Antti-Juhani Kaijanaho
 */

package org.nongnu.storm.http.client;
import org.nongnu.storm.http.*;
import org.nongnu.storm.util.*;
import java.io.*;
import java.net.*;
import java.util.*;

/** A HTTP connection, client POV
 */
public class HTTPConnection {

    static public boolean dbg = false;
    static private void p(String s) { System.out.println(s); }

    public HTTPConnection(String host) throws IOException {
        this(host, 80);
    }

    public HTTPConnection(String host, int port) throws IOException {
        this(host, new Socket(host, port));
    }

    public HTTPConnection(String host, Socket socket) throws IOException {
        this(host, socket.getInputStream(), socket.getOutputStream());
        this.port = socket.getPort();
    }

    public HTTPConnection(String host, InputStream cis, OutputStream cos) {
        this.host = host;
        this.cis = cis;
        this.cos = cos;
        this.port = -1;
    }

    public HTTPRequest newRequest(String method, String uri,
                                  String httpvers, boolean withBody) throws IOException {
        return newRequest(host, method, uri, httpvers, withBody);
    }

    public HTTPRequest newRequest(String host,
                                  String method, String uri,
                                  String httpvers, boolean withBody) throws IOException {
        HTTPRequest rv = new HTTPRequest(cos, method, uri, httpvers, withBody);
        rv.setField("Host", host);
        return rv;
    }

    public synchronized HTTPResponse send(HTTPRequest req) throws IOException {
        ensureOpen();
        if (currentRequest != null) {
            currentRequest.close();
            currentRequest = null;
        }
        if (currentResponse != null) {
            currentResponse.flush();
            currentResponse = null;
        }
        req.commit();
        currentRequest = req;
        HTTPResponse rv;
        try {
            rv = new HTTPResponse(cis);
            currentResponse = rv;
        } catch (ParseException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
        //if (!rv.httpVersion.equals("HTTP/1.1")
        //    || rv.getField("Connection", "").equals("close"))
        //    close();
        return rv;
    }

    public synchronized void close() throws IOException {
        if (currentRequest != null) {
            currentRequest.close();
            currentRequest = null;
        }
        if (currentResponse != null) {
            currentResponse.flush();
            currentResponse = null;
        }
        try { if (cis != null) cis.close(); } catch (IOException _) {}
        cis = null;
        try { if (cos != null) cos.close(); } catch (IOException _) {}
        cos = null;
    }

    private String host;
    private int port;
    private InputStream cis;
    private OutputStream cos;
    private HTTPRequest currentRequest = null;
    private HTTPResponse currentResponse = null;

    private synchronized void ensureOpen() throws IOException {
        if (cis != null && cos != null) return;
        if (port == -1) throw new IOException("cannot reopen");
        Socket sock = new Socket(host, port);
        cis = sock.getInputStream();
        cos = sock.getOutputStream();
        currentRequest = null;
        currentResponse = null;
    }

    private static void showRes(HTTPResponse res ) {
        for (Iterator i = res.enumerateFieldNames(); i.hasNext();) {
            String n = (String)i.next();
            if(dbg) p(n + ": " + res.getField(n));
        }
    }

    private static void showBody(HTTPResponse res) throws IOException {
        InputStream is = res.getInputStream();
        while (true) {
            int c = is.read();
            if (c == -1) break;
            System.out.write((char)c);
        }
    }

    public static void main(String[] args) {
        try {
            HTTPConnection conn = new HTTPConnection("himalia.it.jyu.fi");
            HTTPRequest req = conn.newRequest("OPTIONS", "*", "HTTP/1.1", false);
            HTTPResponse res = conn.send(req);
            showRes(res);
            showBody(res);
            req = conn.newRequest("GET", "/", "HTTP/1.1", false);
            res = conn.send(req);
            showRes(res);
            showBody(res);
            conn.close();
            conn = new HTTPConnection("himalia.it.jyu.fi", 5555);
            req = conn.newRequest("GET", "/mediaserver/data.txt", "HTTP/1.1", false);
            res = conn.send(req);
            showRes(res);
            showBody(res);
            conn.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
