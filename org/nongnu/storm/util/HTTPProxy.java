/*
HTTPProxy.java
 *    
 *    Copyright (c) 2002-2004, Benja Fallenstein
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
import org.nongnu.storm.impl.p2p.*;
import org.nongnu.storm.http.*;
import org.nongnu.storm.http.server.*;
import org.nongnu.storm.references.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import org.python.util.PythonInterpreter;

/** An HTTP server serving blocks from a Storm pool. When started from command 
 *  line, it servers the directory given as first argument.
 *  If connections from hosts other than 127.0.0.1 are to be allowed,
 *  <code>allowGlobalConnections(true)</code> must be called.
 */
public class HTTPProxy extends DavServer {
    public static boolean dbg = false;
    private static void p(String s) { System.out.println("HTTPProxy: "+s); }

    protected IndexedPool pool;
    /** Public pool, where published blocks go to */
    protected IndexedPool pubpool;
    protected String addr;
    protected String ROOTURL;
    protected String REWRITE;
    protected String URNPAC;
    protected String BACKLINKS;
    protected String PUBLISH = "publish";
    protected String HISTORY = "history?doc=";
    protected HTTPServer server;

    protected boolean allowGlobalConnections = false;
    protected boolean acceptPut = false;
    protected boolean showBlocks = false;
    protected PointerSigner signer;
    
    public HTTPProxy(IndexedPool pool, int port) throws IOException {
	super(new SimpleDirectory(), port);

	this.pool = pool;
	this.addr = InetAddress.getLocalHost().getHostName();
	this.ROOTURL = "http://"+addr+":"+port+"/";
	this.REWRITE = "rewrite";
	this.BACKLINKS = "backlinks";
	this.URNPAC = "urn-proxy.pac";
    }

    /** Construct a gateway that allows PUT to pointer/block URIs.
     *  Needs to be passed the PointerSigner to sign the pointer records.
     */
    public HTTPProxy(IndexedPool pool, int port, 
		     PointerSigner signer) throws IOException {
	this(pool, port);
	this.acceptPut = true;
	this.signer = signer;
    }
    
    public void allowGlobalConnections(boolean allowGlobalConnections) {
	this.allowGlobalConnections = allowGlobalConnections;
    }

    public void acceptPut(boolean acceptPut) {
	this.acceptPut = acceptPut;
    }

    public HTTPConnection newConnection(Socket s) throws IOException {
	if(!allowGlobalConnections && 
	   !s.getInetAddress().getHostAddress().equals("127.0.0.1"))
	    return super.newConnection(s);
	return new StormConnection(s);
    }

    public void run() {
	p("Starting Storm URI server @ "+ROOTURL);
	super.run();
    }

    public void close() throws InterruptedException {
        server.interrupt();
        server.join();
    }

    public String getURL() {
	return ROOTURL;
    }
    
    protected class StormConnection extends DavConnection {
	protected StormConnection(Socket s) throws IOException { 
	    super(s); 
	}

        protected HTTPResponse doDelete(HTTPRequest req, 
				     HTTPResponse.Factory resf) throws IOException {
	    if(!acceptPut)
		return resf.makeError(404, "No delete access!");

            try {
		String uri = req.getRequestURI();
		pool.delete(pool.get(new BlockId(uri)));
		return resf.makeError(200, "Ok");
            } catch (IOException e) {
                return resf.makeError(404, e.toString());
            }
        }

	protected HTTPResponse doGet(HTTPRequest req, 
				     HTTPResponse.Factory resf) throws IOException {
	    try {

		boolean rewrite = false, backlinks = false;
		String uri = req.getRequestURI();
		if(HTTPProxy.dbg) 
		    p("<"+port+"> GET: "+uri + " (" + this.getRemoteIPAddress() + ")");
		
		if(!uri.startsWith("/"))
		    /* URN proxy requests don't start with slash */
		    return serveBlock(uri, resf);
		else
		    uri = uri.substring(1);

		if(uri.equals(URNPAC))
		    return makePAC(req, resf);
		if(uri.startsWith(HISTORY))
		    return history(req, resf, uri);

		String element;
		while(!uri.startsWith("vnd-storm-")) {
		    int slash = uri.indexOf('/');
		    if(slash != -1) {
			element = uri.substring(0, slash);
			uri = uri.substring(slash+1);
		    } else {
			element = uri;
			uri = "";
		    }
		    if(element.equals(REWRITE))
			rewrite = true;
		    else if(element.equals(BACKLINKS))
			backlinks = true;
		    else if(element.equals("ids"))
			return getIdsList(resf);
		    else if(element.startsWith("mappings?"))
			return getMappings(element, resf);
		    else if(element.equals(""))
			return serveHomePage(rewrite, backlinks, resf);
		    //else if(element.equals("pointers"))
		    //return servePointerList(resf);
		    else
			throw new FileNotFoundException("Unknown: "+uri);
		}

		/* At this point, only the Storm URI is left of URL */

		uri = URLDecoder.decode(uri);

		Object backlinksInformation = null;
		if(backlinks)
		    backlinksInformation = searchBacklinks(uri);

		BlockId id = getBlockId(uri);

		if((!rewrite && !backlinks) || 
		   !id.getContentType().startsWith("text/html"))
		    return serveBlock(uri, resf);


		if(HTTPProxy.dbg) p("Start rewrite");

		Block block = pool.get(id);
		
		HTTPResponse resp = resf.makeResponse(200, "Ok");
		resp.setField("Content-Type", id.getContentType());

		String s = CopyUtil.readString(block.getInputStream(), "UTF-8");

		String prefix = "";
		if(rewrite) {
		    prefix = ROOTURL+REWRITE+"/";
		    if(backlinks) prefix += BACKLINKS+"/";
		}

		if(rewrite)
		    s = rewriteURIs(s, prefix);

		if(backlinks)
		    s = insertBacklinks(s, prefix, uri, backlinksInformation);
			
		byte[] bytes = s.getBytes("UTF-8");
		resp.getOutputStream().write(bytes);
		resp.close();
		return resp;
	    } catch(FileNotFoundException e) {
		return resf.makeError(404, "Not found");
	    } catch(IllegalArgumentException e) {
		HTTPResponse resp = resf.makeResponse(404, "Not found");
		resp.setField("Content-Type", "text/plain");
		Writer w = new OutputStreamWriter(resp.getOutputStream(), "US-ASCII");
		w.write("404 - Not found\n\n");
		w.write("The URN in request is invalid: "+e.getMessage()+"\n");
		w.write("Requested URI: "+req.getRequestURI()+"\n");
		w.close();
		return resp;
	    } catch(Exception e) {
		e.printStackTrace();
		return resf.makeError(500, "Internal error");
	    }
	}

	protected void writeRewriteLinks(Writer w, boolean rewrite)
	    throws IOException {
	    if(!rewrite) {
		w.write("<p>If your browser <em>is not</em> configured ");
		w.write("to use this gateway as a proxy for URNs, ");
		w.write("you can use ");
		w.write("<a href=\"/rewrite/\">URI-rewriting version</a> ");
		w.write("instead.</p>\n");
	    } else {
		w.write("<p>If your browser <em>is</em> configured ");
		w.write("to use this gateway as a proxy for URNs, ");
		w.write("you can use ");
		w.write("<a href=\"/\">Non-URI-rewriting version</a> ");
		w.write("instead.</p>\n");
	    }
	    w.write("<p>");
	    w.write("To configure URN proxy in Netscape Navigator 4, ");
	    w.write("Automatic Proxy Configuration location can be set to ");
	    w.write("<a href=\""+ROOTURL+URNPAC+"\">"
		    +ROOTURL+URNPAC+"</a></p>\n");
        }

	protected HTTPResponse serveHomePage(boolean rewrite, 
					     boolean backlinks,
					     HTTPResponse.Factory resf) 
	    throws IOException {

	    String base = ROOTURL;
	    if(rewrite) base += REWRITE+"/";
	    if(backlinks) base += BACKLINKS+"/";

	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Content-Type", "text/html");
	    Writer w = new OutputStreamWriter(resp.getOutputStream(), "US-ASCII");
	    w.write("<html><head><title>Storm gateway</title>" +
		    "</head><body>\n");
	    w.write("<h1>Storm gateway</h1>\n");
	    
	    writeRewriteLinks(w, rewrite);

	    w.write("<form action=\""+(rewrite ? "/rewrite" : "")+"/search\" method=\"get\">Find pointer: <input type=\"text\" name=\"q\"><input type=\"submit\" value=\"Find\"></form>");

	    if(acceptPut) {
		w.write("<h2>New pointer</h2>\n\n");

		w.write("<FORM action=\""+(rewrite ? "/rewrite" : "")+"/new-pointer\" method=\"post\">\n");
		w.write("<P>\n");
		w.write("Title: <INPUT type=\"text\" name=\"title\">");
		w.write("<SELECT name=\"target\">\n");
		w.write("<OPTION selected value=\"vnd-storm-hash:text/prs.fallenstein.rst,3i42h3s6nnfq2msvx7xzkyayscx5qbyj.lwpnacqdbzryxw3vhjvcj64qbznghohhhzwclnq\">ReStructuredText file</OPTION>\n");
		w.write("<OPTION value=\"vnd-storm-hash:text/plain,3i42h3s6nnfq2msvx7xzkyayscx5qbyj.lwpnacqdbzryxw3vhjvcj64qbznghohhhzwclnq\">Text file</OPTION>\n");
		w.write("<OPTION value=\"vnd-storm-hash:text/html,3i42h3s6nnfq2msvx7xzkyayscx5qbyj.lwpnacqdbzryxw3vhjvcj64qbznghohhhzwclnq\">HTML page</OPTION>\n");
		w.write("<OPTION value=\"vnd-storm-hash:application/vnd.sun.xml.writer,b3xaowxhn3gg2hmz4e7rofjgfuesytja.jjdrwdheghbhr5qzc5we2giul22s6ljipavhj3q\">OpenOffice.org Writer document</OPTION>\n");
		w.write("<OPTION value=\"vnd-storm-hash:application/vnd.kde.kword,ly3m5evqznmuxnuxyduv43ikvor2dkze.gxeygfionykrrizvwkoyevkdysdnrksw4t5i2yi\">KWord file</OPTION>\n");
		w.write("</SELECT>\n");
		w.write("<INPUT type=\"submit\" value=\"Create\">\n");
		w.write("</P>\n");
		w.write("</FORM>\n\n");
	    }

	    // Index for finding all pointers in a pool
	    // not implemented for new pointers
	    /**w.write("<h2>Pointers</h2>\n\n");

	    PointerIndex pIndex = 
		(PointerIndex)pool.getIndex(PointerIndex.uri);
                
	    Collector pIds = pIndex.getIds();
	    **/

	    /**
	    synchronized(pIds) {
                for(Iterator i=pIds.iterator(); i.hasNext();) {
		    PointerId id = (PointerId)i.next();
                    try {
                        PointerBlock pb = pIndex.getPointerBlock(id); 
                        String n = pb.getName();
                        String s = id.getURI();
                        if(rewrite) 
                            s = base + s;
			if(n != null) {
			    w.write("\"<b><a href=\""+s+"\">"+n+"</a></b>\"\n");
			    w.write("<small>("+id+")</small><br />\n");
			} else {
			    w.write("<a href=\""+s+"\">"+id+"</a><br />\n");
			}
                    } 
                    catch(GeneralSecurityException _) { _.printStackTrace(); w.write("<p> error X"); }
                }
	    }
	    */

	    if(showBlocks) {
		w.write("<h2>Blocks</h2>\n\n");

		SetCollector ids = pool.getIds();
		
		try {
		    // wait for DHT to receive information from network
		    Thread.sleep(2000);
		} catch(InterruptedException _) {}

		synchronized(ids) {
		    for(Iterator i=ids.iterator(); i.hasNext();) {
			BlockId id = (BlockId)i.next();
			String s = id.getURI();
			if(rewrite) 
			    s = base + s;
			w.write("<a href=\""+s+"\">"+id+"</a><br />\n");
		    }
		}
	    }

	    w.write("</body></html>\n");
	    w.close();
	    return resp;
	}

        protected HTTPResponse getMappings(String uri, 
					   HTTPResponse.Factory resf) 
	    throws IOException {
	    
	    //p("MAPPINGS: "+uri);

	    if(!uri.startsWith("mappings?typeURI="))
		return resf.makeError(404, "Not found");
	    int p = uri.indexOf('&');
	    if(p < 0) return resf.makeError(404, "Not found");
	    String typeURI = 
		URLDecoder.decode(uri.substring("mappings?typeURI=".length(),
						p));
	    String rest = uri.substring(p+1);
	    if(!rest.startsWith("key="))
		return resf.makeError(404, "Not found");
	    String key = URLDecoder.decode(rest.substring("key=".length()));

	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Content-Type", "text/plain");
            resp.commit();

            Writer w = new OutputStreamWriter(
                resp.getOutputStream(), "UTF-8");

	    Collector mappings = pool.getMappings(typeURI, key);
	    for(Iterator i=mappings.blockingIterator(); i.hasNext();) {
		IndexedPool.Mapping m = (IndexedPool.Mapping)i.next();
		w.write(m.block.getURI());
		w.write(' ');
		w.write(m.value);
		w.write('\n');
	    }

	    w.close();
	    return resp;
	}
	
        protected HTTPResponse getIdsList(HTTPResponse.Factory resf) 
	    throws IOException {

	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Content-Type", "text/plain");
            resp.commit();

            Set set = pool.getIds();
            Writer w = new OutputStreamWriter(
                resp.getOutputStream(), "US-ASCII");
	    synchronized(set) {
                for(Iterator i=set.iterator(); i.hasNext();) {
		    BlockId id = (BlockId)i.next();
                    w.write(id.getURI());
                    w.write('\n');
                }
	    }
            w.close();
            return resp;
        }

        protected HTTPResponse getIndexTypesList(HTTPResponse.Factory resf) 
	    throws IOException {

	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Content-Type", "text/plain");
            resp.commit();

            Set set = pool.getIndexTypes();
            Writer w = new OutputStreamWriter(
                resp.getOutputStream(), "US-ASCII");
	    synchronized(set) {
                for(Iterator i=set.iterator(); i.hasNext();) {
		    IndexedPool.IndexType idx =
                        (IndexedPool.IndexType)i.next();
                    w.write(idx.getIndexTypeURI());
                    w.write('\n');
                }
	    }
            w.close();
            return resp;
        }

        protected HTTPResponse getIndicesList(HTTPResponse.Factory resf) 
	    throws IOException {

	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Content-Type", "text/plain");
            resp.commit();

            Map map = pool.getIndices();
            Writer w = new OutputStreamWriter(
                resp.getOutputStream(), "US-ASCII");
	    synchronized(map) {
                for(Iterator i=map.keySet().iterator(); i.hasNext();) {
		    String id = (String)i.next();
                    w.write(id);
                    w.write('\n');
                    for (Iterator j=((Collection)
                                     map.get(id)).iterator(); 
                         j.hasNext();) 
                    {
                        w.write(j.next().toString());
                        w.write('\n');
                    }
                    w.write('\n');
                }
	    }
            w.close();
            return resp;
        }

        protected HTTPResponse addBlock(BlockId id, HTTPRequest request, 
                                        HTTPResponse.Factory resf)
            throws IOException 
            {
                BlockOutputStream out = 
		    pool.getBlockOutputStream(id.getContentType());
                InputStream in = request.getInputStream();

                CopyUtil.copy(in, out);
                return resf.makeError(200, "Ok "+out.getBlockId()); // not really error...
            }


        
	protected HTTPResponse serveBlock(String uri, 
					  HTTPResponse.Factory resf) 
	    throws Exception {
	        BlockId id = getBlockId(uri);
                HTTPResponse resp = null;
                try {
                    Block block = pool.get(id);
                    
                    resp = resf.makeResponse(200, "Ok");
                    resp.setField("Content-Type", id.getContentType());

                    int blocksize = CopyUtil.copy(block.getInputStream(),
                                                  resp.getOutputStream());
                } catch (IOException e) {
                    resp = resf.makeResponse(404, "File not found! "+uri);
                }
                return resp;
                    
	}


	// Index for finding all pointers in a pool
	// not implemented for new pointers
	/**
	protected HTTPResponse servePointerList(HTTPResponse.Factory resf) 
	    throws IOException {
	    
	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Content-Type", "text/plain");
	    Writer w = new OutputStreamWriter(resp.getOutputStream(), "US-ASCII");
	    PointerIndex pIndex = 
		(PointerIndex)pool.getIndex(PointerIndex.uri);
                
	    Collector pIds = pIndex.getIds();

	**//*
	    try {
		// wait for DHT to receive information from network
		Thread.sleep(2000);
	    } catch(InterruptedException _) {}
	   *//**

	    synchronized(pIds) {
                for(Iterator i=pIds.iterator(); i.hasNext();) {
		    PointerId id = (PointerId)i.next();
                    try {
                        PointerBlock pb = pIndex.getPointerBlock(id); 
                        String n = pb.getName();
                        String s = id.getURI();
			w.write(id.getURI()); w.write('\n');
			w.write(pb.getName()); w.write('\n');
			w.write('\n');
                    } 
                    catch(GeneralSecurityException _) { _.printStackTrace(); }
                }
	    }

	    w.close();
	    return resp;
	}
	     **/

	protected String rewriteURIs(String s, String prefix) {
	    // Block urns can be in upper or lower or mixed case; 
	    // matching on the lower-case version makes
	    // our life much easier.
	    String l = s.toLowerCase(); 
	    int i = -1;
	    while((i=l.indexOf("vnd-storm-", i)) >= 0) {
		s = s.substring(0, i) + prefix + s.substring(i);
		l = s.toLowerCase(); 
		i += prefix.length() + 1;
	    }
	    return s;
	}

	/** The reverse of rewriteURIs(s, prefix).
	 */
	protected String unrewriteURIs(String s, String prefix) {
	    // Block urns can be in upper or lower or mixed case; 
	    // matching on the lower-case version makes
	    // our life much easier.
	    String l = s.toLowerCase(); 
	    int i = -1;
	    while((i=l.indexOf(prefix+"vnd-storm-", i)) >= 0) {
		s = s.substring(0, i) + s.substring(i+prefix.length());
		l = s.toLowerCase();
		i++;
	    }
	    return s;
	}
	
	protected Object searchBacklinks(String uri) 
	    throws IOException, GeneralSecurityException {

	    if(HTTPProxy.dbg) p("Getting HtmlLinkIndex.");
		    
	    HtmlLinkIndex idx = null;
	    try {
	    	idx = (HtmlLinkIndex)pool.getIndex(HtmlLinkIndex.uri);
	    } catch(NoSuchElementException _) {}
		   
	    if(HTTPProxy.dbg) p("HtmlLinkIndex = "+idx);
		
	    if(idx != null) {
		if(HTTPProxy.dbg) p("Looking for links");
		return idx.getLinksTo(uri);
	    }

	    return null;
	}

	protected String insertBacklinks(String s, String prefix, String uri,
					 Object info) 
	    throws IOException, GeneralSecurityException {

	    PointerIndex pIndex = 
		(PointerIndex)pool.getIndex(PointerIndex.uri);

	    String t = "";

	    if(info != null) {
		SetCollector links = (SetCollector)info;

		PointerIndex.FreshnessChecker chk = 
		    pIndex.new FreshnessChecker(Pointers.VERSION_PROPERTIES);
		
		if(HTTPProxy.dbg) p("Iter thru links");

		synchronized(links) {
		    int n = 0;
		    for(Iterator iter=links.block().iterator(); 
			iter.hasNext();) {
			n++;
			HtmlLinkIndex.Link link = 
			    (HtmlLinkIndex.Link)iter.next();
			PointerSignature sig = chk.getIfCurrent(link);
			if(sig != null) {
			    Reference record = 
				new Reference(pool, sig.getRecord());
			    Block b = Pointers.get(record, pool);
			    String content = 
				CopyUtil.readString(b.getInputStream());
			    int i = content.indexOf("<title>") + 7,
				j = content.indexOf("</title>", i);
			    String name = "???";
			    if(i >= 0 && j >= 0) name = content.substring(i,j);
			    t += "[<a href=\""+prefix+sig.getPointer()+
				"\">"+name+"</a>]<br />";
			}
		    }
		    if(HTTPProxy.dbg) p(n+" links processed.");
		}
	    }

	    if(!t.equals(""))
		t = "\n<p align=\"right\"><small>This page is linked from:<br />"
		    + t
		    + "</small></p>\n";

	    String top = "";
	    p("Backlinks URI: "+uri);
	    if(uri.toLowerCase().startsWith("vnd-storm-ptr:")) {
		top = "<a href=\""+ROOTURL+HISTORY+uri+"\">History</a>";
		if(pubpool != null)
		    top = "\n\n" +
			"<form action=\""+ROOTURL+PUBLISH+"\" "+
			"method=\"post\"><small>\n" +
			top + " | " +
			"<input type=\"submit\" value=\"Publish\" />\n" +
			"<input type=\"hidden\" name=\"uri\" "+
			"value=\""+uri+"\" />\n" +
			"</small></form>\n" +
			"\n";
		top = "\n<div align=\"right\"><small>"+top+"</small></div>";

	    }

	    int i = s.indexOf("<body");
	    i = s.indexOf(">", i);
	    if(i < 0) i = 0;
	    else i++;
	    s = s.substring(0, i) 
		+ "\n" + top
		+ t
		+ s.substring(i);
	    //}
	    return s;
	}

	    
	protected HTTPResponse history(HTTPRequest req, 
				       HTTPResponse.Factory resf, String uri) 
	    throws IOException, GeneralSecurityException {
	    
	    PointerId pid = new PointerId(uri.substring(HISTORY.length()));
	    PointerIndex pIndex = 
		(PointerIndex)pool.getIndex(PointerIndex.uri);
	    SortedSet history = 
		pIndex.getHistory(pid, Pointers.VERSION_PROPERTIES);

	    String prefix = ROOTURL+REWRITE+"/"+BACKLINKS+"/";
	    
	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Content-Type", "text/html;charset=UTF-8");

	    Writer w = new OutputStreamWriter(resp.getOutputStream(), 
					      "UTF-8");
	    String title = "History of \""+pid/*XXX show title somehow*/+"\"";
	    w.write("<html><head><title>"+title+"</title></head><body>\n");
	    w.write("<h1>"+title+"</h1>\n");
	    w.write("<p><a href=\""+prefix+pid+"\">"+pid+"</a></p>\n\n");

	    if(history.isEmpty()) w.write("<p>No versions available.</p>\n\n");

	    for(Iterator i=history.iterator(); i.hasNext();) {
		PointerSignature sig = (PointerSignature)i.next();
		Reference record = new Reference(pool, sig.getRecord());
		String target = record.get("_:this", Pointers.version);
		w.write("<p><b><a href=\""+prefix+target+"\">");
		w.write(new Date(sig.getTimestamp()).toLocaleString());
		w.write("</a></b><br />\n");
		w.write("<small>"+target+"</small></p>\n\n");
	    }

	    w.write("</body></html>\n");
	    w.close();
	    return resp;
	}


	protected HTTPResponse makePAC(HTTPRequest req, 
				       HTTPResponse.Factory resf) 
	    throws IOException {
	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Content-Type", 
			  "application/x-ns-proxy-autoconfig");
	    Writer w = new OutputStreamWriter(resp.getOutputStream(), 
					      "US-ASCII");
	    w.write("function FindProxyForURL(url,host) {\n");
	    w.write("if (url.substring(0, 4) == \"urn:\") {\n");
	    w.write("return \"PROXY "+addr+":"+port+"\";\n");
	    w.write("} else {\n");
	    w.write("return \"DIRECT\";\n");
	    w.write("}}\n");
	    w.close();
	    return resp;
	}



	/** PUTting to a pointer creates a new block
	 *  and points the pointer to it.
	 */
	protected HTTPResponse doPut(HTTPRequest req, HTTPResponse.Factory resf) 
	    throws IOException {

	    String uri = URLDecoder.decode(req.getRequestURI());
	    System.out.println("PUT: "+uri);

	    if(!acceptPut)
		return doUnknown(req, resf);

	    if(uri.startsWith("/"))
		uri = uri.substring(1);
	    else if(uri.startsWith("x-storm:"))
		/* Work around Amaya bug */
		uri = REWRITE + "/urn:" + uri;

            if(uri.startsWith("vnd-storm-hash:"))
                return addBlock(new BlockId(uri), req, resf);



	    boolean rewrite = false;
	    if(uri.startsWith(REWRITE+"/")) {
		uri = uri.substring(REWRITE.length()+1);
		rewrite = true;
	    }
	    if(uri.startsWith(BACKLINKS+"/")) {
		uri = uri.substring(BACKLINKS.length()+1);
	    }

	    if(dbg) p("PUT accepted");
	    PointerId pointerId = new PointerId(uri);
	    PointerIndex idx = 
		(PointerIndex)pool.getIndex(PointerIndex.uri);

	    String contentType = req.getField("Content-Type");

	    if(contentType == null) {
		// because we don't get a content-type
		// assume it's the same as before
		try {
		    contentType = pool.get(pointerId).getId().getContentType();
		} catch(Exception _) {
		    _.printStackTrace();
		    throw new Error("Exception while getting content type XXX");
		}
	    }

	    if(dbg) p("Got old pointer value");
	    
	    BlockOutputStream bos = 
		pool.getBlockOutputStream(contentType);

	    if(!rewrite || !contentType.equals("text/html")) {
		CopyUtil.copy(req.getInputStream(), bos);
	    } else {
		String s = CopyUtil.readString(req.getInputStream());
		String prefix = ROOTURL+REWRITE+"/";
		s = unrewriteURIs(s, prefix);
		bos.write(s.getBytes("US-ASCII"));
		bos.close();
	    }

	    if(dbg) p("Created new block: "+bos.getBlockId());
	    
	    // If it may be an RST file, try to compile it.
	    p("showct");
	    p("CT: <"+contentType+">");
	    if(contentType.startsWith("text/plain") ||
	       contentType.startsWith("text/prs.fallenstein.rst")) {
		String charset = "iso-8859-1";

		String prefix = "text/plain;charset=";
		if(contentType.startsWith(prefix))
		    charset = contentType.substring(prefix.length());

		prefix = "text/prs.fallenstein.rst;charset=";
		if(contentType.startsWith(prefix))
		    charset = contentType.substring(prefix.length());

		new Thread(new CompileRST(pointerId, bos.getBlock())).start();
		p("Other thread continues");
	    }

	    // Now set RST pointer.
	    try {
		signer.updateNewest(pointerId, bos.getBlockId().toString());
	    } catch(Exception _) {
		_.printStackTrace();
		throw new Error("Exception while getting key XXX");
	    }

	    if(dbg) p("Set pointer, making response.");
	    
	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Content-Type", "text/html");
	    Writer w = new OutputStreamWriter(resp.getOutputStream(), 
					      "US-ASCII");
	    w.write("<html><head><title>Ok</title></head><body>");
	    w.write("PUT was successful, new block is: "+bos.getBlockId());
	    w.write("</body></html>");
	    w.close();

	    if(dbg) p("Return response");
	    return resp;
	}

	/** POSTing to /new-pointer creates a new pointer.
	 *  Option is <code>target</code>, the URI
	 *  of the block the new pointer will point to first.
	 */
	protected HTTPResponse doPost(HTTPRequest req, 
				      HTTPResponse.Factory resf) 
	    throws IOException {

	    String uri = req.getRequestURI();
	    System.out.println("POST: "+uri);

	    if(!acceptPut)
		return doUnknown(req, resf);

	    if(uri.equals("/"+PUBLISH)) {
		String formdata =
		    CopyUtil.readString(req.getInputStream());
		if(!formdata.startsWith("uri="))
		    return resf.makeError(400, "Bad formdata: "+formdata);
		return publish(URLDecoder.decode(formdata.substring(4)),
			       resf);
	    }

	    if(!uri.equals("/new-pointer") &&
	       !uri.equals("/new-pointer-plain") &&
	       !uri.equals("/rewrite/new-pointer"))
		return resf.makeError(404, "Not found");

	    String formdata =
		CopyUtil.readString(req.getInputStream());
	    if(dbg) p("Form data: "+formdata);

	    int i = formdata.indexOf("target=") + "target=".length();
	    int j = formdata.indexOf('&', i);
	    if(j < 0) j = formdata.length();
	    String targetURI = 
		java.net.URLDecoder.decode(formdata.substring(i,j));
	    p(targetURI);
	    BlockId target = new BlockId(targetURI);

	    PointerId id;
	    PointerIndex idx = 
		(PointerIndex)pool.getIndex(PointerIndex.uri);

	    try {
		id = signer.newPointer();
		signer.initialize(id, Pointers.hasInstanceRecord, 
				  target.toString());
	    } catch(Exception _) {
		_.printStackTrace();
		throw new Error("Exception while creating pointer XXX");
	    }

	    HTTPResponse resp = resf.makeResponse(203, "Created");
	    resp.setField("Location", target.getURI());

	    if(uri.equals("/new-pointer-plain")) {
		resp.setField("Content-Type", "text/plain");
		Writer w = new OutputStreamWriter(resp.getOutputStream(), 
						  "US-ASCII");
		w.write(id.getURI());
		w.close();
	    } else {
		boolean rewrite = uri.equals("/rewrite/new-pointer");
		resp.setField("Content-Type", "text/html");
		Writer w = new OutputStreamWriter(resp.getOutputStream(), 
						  "US-ASCII");
		w.write("<html><head><title>Created</title></head><body>");
		w.write("New pointer created at: \n");
		w.write("<a href=\""+(rewrite ? REWRITE : "")+id.getURI()+"\">"+id.getURI()+"</a>\n");
		w.write("<p><a href=\"/\">Back to the home page.</a>\n");
		w.write("</body></html>");
		w.close();
	    }

	    if(dbg) p("Return response");
	    return resp;
	}	

	protected HTTPResponse publish(String uri, HTTPResponse.Factory resf) 
	    throws IOException {

	    if(pubpool == null)
		return resf.makeError(400, 
				      "Server does not support publishing");

	    p("Publish URI: <"+uri+">");

	    PointerId id = new PointerId(uri);
	    publish(id);
	    if(uri.endsWith("/0:html")) {
		// publish RST source 
		publish(new PointerId(uri.substring(0, uri.length()-7)));

		// publish stylesheet
		publish(new PointerId(id.getRoot(), "/0:stylesheet"));
	    }

	    return resf.makeError(200, "Ok"); // XXX not "error" =)
	}


	protected void publish(PointerId id) 
	    throws IOException {

	    PointerIndex idx = (PointerIndex)pool.getIndex(PointerIndex.uri);
	    SortedSet history = idx.getHistory(id, 
					       Pointers.VERSION_PROPERTIES);
	    Iterator iter = history.iterator();
	    PointerSignature sig = (PointerSignature)iter.next();
	    Reference record = new Reference(pool, sig.getRecord());
	    
	    pubpool.add(pool.get(sig.getRecord().getGraphId()));
	    pubpool.add(pool.get(sig.getPointer().getGraphId()));
	    pubpool.add(Pointers.get(record, pool));
	    
	    BlockOutputStream bos = pubpool.getBlockOutputStream("application/prs.fallenstein.pointersignature");
	    sig.write(bos);
	    bos.close();

	}

	/**
	protected HTTPResponse doPropfind(HTTPRequest req, HTTPResponse.Factory resf)
	    throws IOException {
	    // XXX absolutely dummy implementation

	    String r = CopyUtil.readString(req.getInputStream());

	    p("PROPFIND request:");
	    p(r);
	    p("Sending dummy response.");

	    HTTPResponse resp = resf.makeResponse(207, "Multi-Status");
	    resp.setField("Content-Type", "text/xml");
	    Writer w = new OutputStreamWriter(resp.getOutputStream(), 
					      "UTF-8");
	    w.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
	    w.write("<D:multistatus xmlns:D=\"DAV:\">\n");
	    w.write("<D:response>\n");
	    w.write("<D:href>"+ROOTURL+req.getRequestURI()+"</D:href>\n");
	    w.write("<D:propstat>\n");
	    w.write("<D:prop>\n");
	    //w.write("<D:creationdate/>\n");
	    //w.write("<D:displayname/>\n");
	    w.write("<D:resourcetype>\n");
	    if(req.getRequestURI().endsWith("/"))
		w.write("<D:resourcetype/>\n");
	    w.write("</D:resourcetype>\n");
	    //w.write("<D:supportedlock/>\n");
	    w.write("</D:prop>\n");
	    w.write("<D:status>HTTP/1.1 200 OK</D:status>");
	    w.write("</D:propstat>\n");
	    w.write("</D:response>\n");
	    w.write("<D:responsedescription>\n");
	    w.write("A faked empty response has been sent.\n");
	    w.write("WebDAV isn't really implemented yet.\n");
	    w.write("</D:responsedescription>\n");
	    w.write("</D:multistatus>\n");
	    w.close();

	    return resp;
	}

	protected HTTPResponse dispatch(HTTPRequest req, HTTPResponse.Factory resf) 
	    throws IOException {
	    p("Dispatch: "+req);
	    return super.dispatch(req, resf);
	}	
	**/
    }    

    protected BlockId getBlockId(String uri) throws IOException {
	uri = uri.toLowerCase();
	if(uri.startsWith("vnd-storm-hash:")) {
	    return new BlockId(uri);
	} else if(uri.startsWith("vnd-storm-ref:")) {
	    return pool.get(new ReferenceId(uri)).getId();
	} else if(uri.startsWith("vnd-storm-ptr:")) {
	    return pool.get(new PointerId(uri)).getId();
	} else {
	    throw new Error("Malformed Storm URN: "+uri);
	}
    }

    static PythonInterpreter interp;
    protected class CompileRST implements Runnable {
	PointerId pointer_rst, pointer_html;
	Block src;
	String stylesheet;

	protected CompileRST(PointerId pointer_rst, Block src) 
	    throws IOException {

	    this.pointer_rst = pointer_rst;
	    this.src = src;

	    this.pointer_html = new PointerId(pointer_rst, "/0:html");

	    PointerId root = 
		new PointerId(signer.getOwner().getId().getGraphId());
	    PointerId pointer_stylesheet = 
		new PointerId(root, "/0:stylesheet");

	    this.stylesheet = pointer_stylesheet.getURI();
	}

	public void run() {
	    p("Start compiling RST...");
	    if(interp == null) {
		interp = new PythonInterpreter();
		interp.exec("import docutils");
	    }

	    try {
		String contentType = src.getId().getContentType().toLowerCase();
		String encoding = "us-ascii";
		int pos = contentType.indexOf("charset=");
		if(pos >= 0) {
		    encoding = contentType.substring(pos+"charset=".length());
		    pos = encoding.indexOf(';');
		    if(pos >= 0) encoding = encoding.substring(0, pos);
		    if(encoding.startsWith("\"") || encoding.startsWith("'"))
			encoding = encoding.substring(1);
		    if(encoding.endsWith("\"") || encoding.endsWith("'"))
			encoding = encoding.substring(0, encoding.length()-1);
		} else if(contentType.startsWith("text/prs.fallenstein.rst"))
		    encoding = "utf-8"; // default for this media type
		
		Random r = new Random();
		String filename = "RST-SOURCE-"+r.nextLong();
		File file = new File("/tmp/"+filename);
		CopyUtil.copy(src.getInputStream(),
			      new FileOutputStream(file));

		p("Start docutils.");
		interp.exec("docutils.core.publish_cmdline(writer_name='html', argv=[\""+file+"\", \""+file+".gen.html\", '--input-encoding="+encoding+"', '--output-encoding=utf-8', '--generator', '--source-link', '--stylesheet="+stylesheet+"'])");
		p("Docutils finished.");

		String html = 
		    CopyUtil.readString(new FileInputStream(file+".gen.html"), 
					"UTF-8");
		int i = html.lastIndexOf(filename);
		html = html.substring(0, i) + pointer_rst + 
		       html.substring(i+filename.length());

		BlockOutputStream bos = 
		    pool.getBlockOutputStream("text/html;charset=utf-8");
		Writer w = new OutputStreamWriter(bos, "UTF-8");
		w.write(html);
		w.close();

		p("HTML block created: "+bos.getBlockId());
	    
		try {
		    signer.updateNewest(pointer_html, 
					bos.getBlockId().toString());
		} catch(Exception _) {
		    _.printStackTrace();
		    throw new Error("Exception while putting key XXX");
		}

		p("Pointer set: "+pointer_html.getURI());
	    } catch(IOException _) {
		_.printStackTrace();
		throw new Error("Error while compiling RST: "+_);
	    }
	    p("RST compiled.");
	}
    }

    public static IndexedPool getPool(String name, Set indexTypes) 
	throws Exception {

	if(name.startsWith("BDB::"))
	    return new BerkeleyDBPool(new File(name.substring(5)), indexTypes);
	else if(name.startsWith("DIR::"))
	    return new DirPool(new File(name.substring(5)), indexTypes);
	else
	    throw new Error("Unknown pool type -- "+name);
    }

    public static void main(String[] args) throws Exception {
	IndexedPool pool, pubpool = null;
	Set indexTypes = new HashSet();
	indexTypes.add(HtmlLinkIndex.type);
	indexTypes.add(PointerIndex.type);
	pool = getPool(args[0], indexTypes);

	int port = 5555;

	if(args.length == 1)
	    new HTTPProxy(pool, port).run();
	else {
	    if(args.length > 2 && !args[2].equals("--"))
		pubpool = getPool(args[2], indexTypes);

	    if(args.length > 3 && !args[3].equals("--"))
		port = Integer.parseInt(args[3]);

	    if(args.length > 4) {
		String uri = args[4];
		int i = uri.indexOf(':');
		String host = i<0 ? uri : uri.substring(0, i);
		int hport = i<0 ? 5555 : Integer.parseInt(uri.substring(i+1));
		IndexedPool httppool = new HTTPPool(host, hport, indexTypes);
		// auto-sync every 15 minutes
		pubpool = new StormSync.AutoSyncPool(pubpool, httppool,
						     15 * 60 * 1000);
	    }

	    PointerSigner signer = null;

	    if(!args[1].equals("--")) {
		InputStream in = new FileInputStream(args[1]);
		signer = new PointerSigner(pool, in);
	    }
	    HTTPProxy proxy = new HTTPProxy(pool, port, signer);
	    proxy.pubpool = pubpool;
	    proxy.run();
	}
    }
}
