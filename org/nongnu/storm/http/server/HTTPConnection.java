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

package org.nongnu.storm.http.server;
import org.nongnu.storm.http.*;
import java.io.*;
import java.net.*;

/** A HTTP connection handler.  An application wishing to implement
 * HTTP serving should derive from this class and override some or all
 * of the do* methods - and also it should derive from the Factory
 * class and give an instance of the derivative to the HTTPServer
 * class constructor.  A new thread is spawned by instantiating this
 * class.  <p> By default, this class implements a minimal HTTP server
 * connection that does not serve any content.
 * @see HTTPServer
 * @see HTTPConnection.Factory
 */
public class HTTPConnection {

    static public boolean dbg = false;
    static private void p(String s) { if(dbg) System.out.println(s); }
    static private void pa(String s) { System.out.println(s); }
    static private String remoteaddress;
    
    /** A factory of application-specific HTTP connection objects.
     * @see HTTPConnection
     */
    public static class Factory {
        /** Create a new connection object.
         * @param s The socket for this connection
         * @throws IOException XXX
        */
        public HTTPConnection newConnection(Socket s) throws IOException {
            p("httpconn");	    	    
            return new HTTPConnection(s);
        }
    }

    /** Create a new connection object.
     * @param s The socket for this connection
     * @throws IOException Indicates a problem with the socket
     */
    protected HTTPConnection(Socket s) throws IOException {
        this(s.getInputStream(), s.getOutputStream(), 
	s.getInetAddress().toString().substring(1));	
    }

    /** Create a new connection object.
     * @param is The input stream for this connection
     * @param os The output stream for this connection
     */
    protected HTTPConnection(InputStream is, OutputStream os, String ipaddress) {
        this.is = is;
        this.os = os;
	this.remoteaddress = ipaddress;
        thread.start();
    }
        
    /** Close this connection forcefully.  This ends the thread
     * handling this connection and sends a notice to the client if
     * necessary.  */
    public void close() {
        if (thread != null) {
            try {
                thread.interrupt();
                thread.join();
                thread = null;
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
     /** Return the IP-address of remote host */
    
    public String getRemoteIPAddress() {
	
	return this.remoteaddress;
	
    }    
   

    /** An end-of-connection hook.  A subclass can override this
     * method and thus be informed when the connection is closed for
     * some reason or another.  By default, this method is a
     * no-op.  */
    protected void endOfConnection() {}

    /** HTTP OPTIONS method handler.  This method is called when the
     * HTTP OPTIONS request is to be processed.  By default, doUnknown
     * is called.  
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doOptions(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }
    
    /** HTTP GET method handler.  This method is called when the HTTP
     * GET request is to be processed.  By default, it is responded to
     * with a 404.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doGet(HTTPRequest req, HTTPResponse.Factory resf)
        throws IOException {
        return resf.makeError(404, "Not found");
    }

    /** HTTP HEAD method handler.  This method is called when the HTTP
     * HEAD request is to be processed.  By default, doGet is called.
     * Note that the response factory given will create automatically
     * a response that will not include a body, so it is quite safe
     * (and recommended for most situations) to leave handling of the
     * HTTP HEAD requests to doGet - GET and HEAD should respond with
     * identical headers.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doHead(HTTPRequest req, HTTPResponse.Factory resf)
        throws IOException {
        return doGet(req, resf);
    }

    /** HTTP POST method handler.  This method is called when the HTTP
     * POST request is to be processed.  By default, doUnknown is
     * called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doPost(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** HTTP PUT method handler.  This method is called when the HTTP
     * PUT request is to be processed.  By default, doUnknown is
     * called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doPut(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** HTTP DELETE method handler.  This method is called when the
     * HTTP DELETE request is to be processed.  By default, doUnknown
     * is called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doDelete(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** HTTP TRACE method handler.  This method is called when the
     * HTTP TRACE request is to be processed.  By default, doUnknown
     * is called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doTrace(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** HTTP CONNECT method handler.  This method is called when the
     * HTTP CONNECT request is to be processed.  By default, doUnknown
     * is called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doConnect(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    // WebDAV methods

    /** HTTP PROPFIND method handler.  This method is called when the
     * HTTP PROPFIND request is to be processed.  By default, doUnknown
     * is called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doPropfind(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** HTTP PROPPATCH method handler.  This method is called when the
     * HTTP PROPPATCH request is to be processed.  By default, doUnknown
     * is called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doProppatch(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** HTTP MKCOL method handler.  This method is called when the
     * HTTP MKCOL request is to be processed.  By default, doUnknown
     * is called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doMkcol(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** HTTP COPY method handler.  This method is called when the
     * HTTP COPY request is to be processed.  By default, doUnknown
     * is called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doCopy(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** HTTP MOVE method handler.  This method is called when the
     * HTTP MOVE request is to be processed.  By default, doUnknown
     * is called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doMove(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** HTTP LOCK method handler.  This method is called when the
     * HTTP LOCK request is to be processed.  By default, doUnknown
     * is called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doLock(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** HTTP UNLOCK method handler.  This method is called when the
     * HTTP UNLOCK request is to be processed.  By default, doUnknown
     * is called.
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doUnlock(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return doUnknown(req, resf);
    }

    /** Unknown method handler.  This method is called when the
     * an unknown HTTP request is to be processed.  By default, it is
     * responded with a 501.  
     * @param req The HTTP request from the client
     * @param resf A factory of HTTP responses
     * @return A HTTP response to be sent to the client
     * @throws IOException
     */
    protected HTTPResponse doUnknown(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        return resf.makeResponse(501, "Not implemented");
    }

    /** HTTP method dispatcher.  This method is called when a HTTP
     * request is to be processed.  By default it delegates the
     * request to the individual do* methods.  Note that subclasses
     * should override the do* methods whenever possible and override
     * this method only when there is no suitable do* method
     * available.
     * @param req The HTTP request from the client
     * @param resf A factory of 
     */
    protected HTTPResponse dispatch(HTTPRequest req, HTTPResponse.Factory resf) 
        throws IOException {
        String method = req.getMethod();
	/* HTTP proper methods */
        if (method.equals("HEAD"))    return doHead(req, resf);
        if (method.equals("GET"))     return doGet(req, resf);
        if (method.equals("POST"))    return doPost(req, resf);
        if (method.equals("PUT"))     return doPut(req, resf);
        if (method.equals("DELETE"))  return doDelete(req, resf);
        if (method.equals("OPTIONS")) return doOptions(req, resf);
        if (method.equals("TRACE"))   return doTrace(req, resf);
        if (method.equals("CONNECT")) return doConnect(req, resf);

	/* WebDAV methods */
	if (method.equals("PROPFIND")) return doPropfind(req, resf);
	if (method.equals("PROPPATCH")) return doProppatch(req, resf);
	if (method.equals("MKCOL"))   return doMkcol(req, resf);
	if (method.equals("COPY"))    return doCopy(req, resf);
	if (method.equals("MOVE"))    return doMove(req, resf);
	if (method.equals("LOCK"))    return doLock(req, resf);
	if (method.equals("UNLOCK"))  return doUnlock(req, resf);

        /* else: unknown method */    return doUnknown(req, resf);
    }


    private final InputStream is;
    private final OutputStream os;
    private Thread thread = 
        new Thread() {
                public final void run() {
                    while (!isInterrupted()) {
                        try {
                            iter(this);
                        } catch (IOException _) {
			    // doesn't seem to be a big problem--
			    // don't print stack trace if dbg is off,
			    // in order not to frighten users XXX
			    if(dbg) _.printStackTrace();

			    // IOExceptions often occur when the pipe
			    // is somehow broken or closed-- if it's
			    // still open we send a message to the
			    // client, if it's closed there's nothing
			    // we can do.
			    try {
				error(os, 503, "IO error");
			    } catch(Throwable __) {
			    }
                            break;
                        } catch (ParseException _) {
                            error(os, 400, "Request syntax terror");
                            break;
                        } catch (Throwable t) {
			    System.out.println("Caught in HTTPConnection "+
					       "mainloop:");
			    t.printStackTrace();
			    error(os, 500, "Internal error");
			    break;
			}
                    }
                    endOfConnection();
                    try { is.close(); } catch (IOException _) {}
                    try { os.close(); } catch (IOException _) {}
                    thread = null;
                }
            };

    void iter(Thread t) throws IOException, ParseException {
        final HTTPRequest req = new HTTPRequest(is);
        final boolean is11 = req.getHTTPVersion().equals("HTTP/1.1");
        final boolean close = !is11
            || HTTPRequest.doesCSLContainThis(req.getField("Connection"),
                                              "close");
        final boolean chunked = is11 && !close;
        p("" + is11 + " " + close + " "  + chunked);
        HTTPResponse res = 
            handleRequest(req, new HTTPResponse.Factory() {
                    private void common(HTTPResponse r) {
                    }
                    public HTTPResponse makeResponse(int code,
                                                     String reason)
                        throws IOException{
                        HTTPResponse r;
                        if (req.getMethod().equals("HEAD")) {
                            r = new HTTPResponse(os, chunked,
                                                 code, reason,
                                                 true);
                        } else {
                            r= new HTTPResponse(os, chunked,
                                                code, reason);
                        }
                        return r;
                    }
                    public HTTPResponse makeError(int code, String reason) 
                        throws IOException  {
			String body =
			    "<title>" + reason + "</title>\n<h1>" + code
			    + " - " + reason + "</h1>\n";
			byte[] bytes = body.getBytes("US-ASCII");

                        HTTPResponse r;
                        if (req.getMethod().equals("HEAD")) {
                            r = new HTTPResponse(os, false, code, reason,
                                                 true);
                        } else {
                            r = new HTTPResponse(os, false, code, reason);
                        }
			r.setField("Content-Length", ""+bytes.length);
			r.setField("Content-Type", "text/html");
                        if (!chunked) r.setField("Connection", "close");

			OutputStream o = r.getOutputStream();
			o.write(bytes);
			o.close();

                        return r;
                    }
                });
        res.close();
        if (!chunked) t.interrupt();
    }

    private HTTPResponse handleRequest(HTTPRequest req,
                                       HTTPResponse.Factory resf)
        throws IOException {
        if (req.getHTTPVersion().equals("HTTP/1.1")
            && req.getField("Host") == null) {
            return resf.makeError(400, "Missing Host field, " +
                                  "read RFC 2616 section 14.23");
        }
        return dispatch(req, resf);
    }

    private static void error(OutputStream s, int code, String reason) {
        try {
            HTTPResponse res = new HTTPResponse(s, false, code, reason);
            res.setField("Connection", "close");
            Writer o = res.getWriter("html");
            o.write("<title>" + reason + "</title>\n<h1>" + code
                    + "</h1> - " + reason + "\n");
            res.close();
        } catch (IOException e) { 
	    // XXX not only when dbg is on?
            if(dbg) e.printStackTrace();
        }
    }    
    


}
