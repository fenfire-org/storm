/*
DavServer.java
 *    
 *    Copyright (c) 2004, Benja Fallenstein
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
import org.nongnu.storm.http.*;
import org.nongnu.storm.http.server.*;
import nu.xom.*;
import java.io.*;
import java.net.*;
import java.util.*;

/** An HTTP server that implements WebDAV.
 */
public class DavServer extends HTTPConnection.Factory implements Runnable {
    public static boolean dbg = true;
    private static void p(String s) { System.out.println("DavServer:: "+s); }

    public static final String DAV = "DAV:";

    public static final String key(Element e) {
	return e.getNamespaceURI() + " " + e.getLocalName();
    }

    /** A resource in the DAV sense -- think of it as a file or directory.
     */
    public interface Resource {
	InputStream getInputStream() throws IOException;
	OutputStream getOutputStream(String contentType) throws IOException;
	String getContentType() throws IOException;
	int getContentLength() throws IOException;

	/** Map from property names to XOM <code>Element</code> objects.
	 *  Property names are strings of the form
	 *  <code>namespaceURI+SPACE+localName</code>.
	 *  The <code>Element</code> that is the value must have that
	 *  property name as its tag. For example, this element:
	 *  <pre>
	 *      &lt;x:foo xmlns:x="http://example.org/namespace/"&gt;
	 *          foo bar baz
	 *      &lt;/x:foo&gt;
	 *  </pre>
	 *  as a value would have as its key this string:
	 *  <pre>
	 *      "http://example.org/namespace/ foo"
	 *  </pre>
	 *  <p>
	 *  The map is <em>live</em>, i.e., changes in the map
	 *  must be reflected in the backend and vice versa.
	 */
	Map getProperties() throws IOException;
    }

    /** A resource that is a directory which can 
     */
    public interface Directory extends Resource {
	/** The contents of this directory, as a map from file names 
	 *  to Resource objects.
	 *  In URIs, all characters from a file name that 
	 *  cannot appear in a path segment, as well as the '%' character, 
	 *  are URI-escaped. (The % character can appear in path segments
	 *  but only for the purpose of escaping, so it needs to be
	 *  escaped itself.)
	 *  <p>
	 *  This map is not live, i.e., changes in the underlying model
	 *  are <em>not</em> reflected in the map.
	 */
	Map getEntries() throws IOException;

	/** Create a new "file" in this directory.
	 */
	Resource newFile(String filename) throws IOException;

	/** Create a subdirectory.
	 */
	Directory newDirectory(String filename) throws IOException;

	void delete(String filename) throws IOException;

	/** Copy an entry in this directory to a different directory.
	 */
	void copy(String sourceName, Directory destination, 
		  String destName, boolean overwrite) throws IOException;

	/** Move an entry in this directory to a different directory.
	 */
	void move(String sourceName, Directory destination, 
		  String destName, boolean overwrite) throws IOException;
    }

    protected Resource root;
    protected HTTPServer server;
    protected int port;

    public DavServer(Resource root, int port) throws IOException {
	this.root = root;
	this.server = new HTTPServer(this, port);
	this.port = port;
    }
    public DavServer(Resource root, int port, int backlog) throws IOException {
	this.root = root;
	this.server = new HTTPServer(this, port, backlog);
	this.port = port;
    }
    public DavServer(Resource root, int port, int backlog,
		     InetAddress bindAddr) throws IOException {
	
	this.root = root;
	this.server = new HTTPServer(this, port, backlog, bindAddr);
	this.port = port;
    }

    public void run() {
	server.run();
    }

    protected Resource getResource(String path) throws IOException {
	String[] segments;
	try {
	    segments = splitPath(path);
	} catch(IllegalArgumentException _) {
	    throw new FileNotFoundException();
	}
	//p("segments.length = "+segments.length);
	return getResource(segments);
    }

    protected Resource getResource(String[] segments) throws IOException {
	Resource r = root;
	for(int i=0; i<segments.length; i++) {
	    //p("...segment '"+segments[i]+"'");
	    if(!(r instanceof Directory))
		throw new FileNotFoundException();
	    
	    Directory d = (Directory)r;
	    r = (Resource)d.getEntries().get(segments[i]);
	    if(r == null)
		throw new FileNotFoundException();
	}

	return r;
    }


    public HTTPConnection newConnection(Socket s) throws IOException {
	return new DavConnection(s);
    }

    protected class DavConnection extends HTTPConnection {
	public DavConnection(Socket s) throws IOException {
	    super(s);
	}

	protected HTTPResponse doOptions(HTTPRequest req,
					 HTTPResponse.Factory resf)
	    throws IOException {

	    if(DavServer.dbg) DavServer.p("OPTIONS "+req);

	    if(!req.getRequestURI().trim().equals("*")) {
		// check that the resource exists
		try {
		    getResource(req.getRequestURL().getPath());
		} catch(FileNotFoundException e) {
		    return resf.makeError(404, "File not found");
		} catch(IOException e) {
		    e.printStackTrace();
		    return resf.makeError(500, "Internal server error");
		}
	    }

	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Dav", "1");
	    resp.commit();
	    resp.close();
	    return resp;
	}

	protected HTTPResponse doGet(HTTPRequest req, 
				     HTTPResponse.Factory resf)
	    throws IOException {

	    if(DavServer.dbg) DavServer.p("GET "+req);

	    Resource res;

	    try {
		res = getResource(req.getRequestURL().getPath());
	    } catch(FileNotFoundException e) {
		return resf.makeError(404, "File not found");
	    } catch(IOException e) {
		e.printStackTrace();
		return resf.makeError(500, "Internal server error");
	    }

	    HTTPResponse resp = resf.makeResponse(200, "Ok");
	    resp.setField("Content-Type", res.getContentType());
	    resp.commit();
	    CopyUtil.copy(res.getInputStream(), resp.getOutputStream());
	    return resp;
	}

	protected HTTPResponse doPut(HTTPRequest req, 
				     HTTPResponse.Factory resf)
	    throws IOException {

	    if(DavServer.dbg) DavServer.p("PUT "+req);
	    
	    Resource res;
	    String[] path;
	    try {
		path = splitPath(req.getRequestURL().getPath());
	    } catch(IllegalArgumentException _) {
		return resf.makeError(404, "Not found (unparseable path)");
	    }

	    try {
		res = getResource(path);
	    } catch(FileNotFoundException e) {
		if(path.length == 0) {
		    throw new Error("Root doesn't exist?!?");
		} else {
		    // try to find parent
		    String[] ppath = parent(path);
		    
		    Resource parent;

		    try {
			parent = getResource(ppath);
		    } catch(FileNotFoundException _) {
			// parent doesn't exist
			return resf.makeError(409, "Conflict "+
					      "(parent doesn't exist)");
		    } catch(IOException _) {
			return resf.makeError(500, "Internal server error");
		    }

		    if(!(parent instanceof Directory))
			return resf.makeError(409, "Conflict (parent exists "+
					      "but isn't collection)");

		    res = ((Directory)parent).newFile(path[path.length-1]);
		}
	    } catch(IOException e) {
		e.printStackTrace();
		return resf.makeError(500, "Internal server error");
	    }
	    
	    try {
		String ct = req.getField("Content-Type");
		if(DavServer.dbg) DavServer.p("PUT content type: "+ct);
		if(ct == null) ct = res.getContentType();

		CopyUtil.copy(req.getInputStream(), res.getOutputStream(ct));
	    } catch(IOException e) {
		e.printStackTrace();
		throw e;
	    }

	    HTTPResponse resp = resf.makeResponse(204, "No content");
	    DavServer.p("resp "+resp);
	    resp.commit();
	    resp.close();
	    return resp;
	}

	protected HTTPResponse doMkcol(HTTPRequest req, 
				       HTTPResponse.Factory resf)
	    throws IOException {

	    if(DavServer.dbg) DavServer.p("MKCOL "+req);

	    if(req.getField("Content-Type") != null)
		resf.makeError(415, "Unsupported media type");
	    
	    String[] path;
	    try {
		path = splitPath(req.getRequestURL().getPath());
	    } catch(IllegalArgumentException _) {
		return resf.makeError(404, "Not found (unparseable path)");
	    }

	    boolean found = false;
	    try {
		getResource(path);
		found = true;
	    } catch(FileNotFoundException e) {
	    } catch(IOException e) {
	    }

	    if(found) {
		return resf.makeError(405, "Resource exists");
	    }

	    if(path.length == 0)
		throw new Error("Root doesn't exist?!?");

	    // try to find parent
	    String[] ppath = new String[path.length-1];
	    for(int i=0; i<ppath.length; i++) ppath[i] = path[i];
	    
	    Resource parent;
	    
	    try {
		parent = getResource(ppath);
	    } catch(FileNotFoundException _) {
		// parent doesn't exist
		return resf.makeError(409, "Conflict "+
				      "(parent doesn't exist)");
	    } catch(IOException _) {
		return resf.makeError(500, "Internal server error");
	    }
	    
	    if(!(parent instanceof Directory))
		return resf.makeError(409, "Conflict (parent exists "+
				      "but isn't collection)");
	    
	    Directory d = 
		((Directory)parent).newDirectory(path[path.length-1]);
	    
	    HTTPResponse resp = resf.makeResponse(201, "Created");
	    resp.commit();
	    resp.close();
	    return resp;
	}

	protected HTTPResponse doCopy(HTTPRequest req, 
				      HTTPResponse.Factory resf)
	    throws IOException {
	    
	    return doCopyOrMove(req, resf, true);
	}

	protected HTTPResponse doMove(HTTPRequest req, 
				      HTTPResponse.Factory resf)
	    throws IOException {
	    
	    return doCopyOrMove(req, resf, false);
	}

	protected HTTPResponse doCopyOrMove(HTTPRequest req, 
					    HTTPResponse.Factory resf,
					    boolean copy)
	    throws IOException {

	    if(DavServer.dbg) DavServer.p("COPY/MOVE "+req);

	    String s = req.getField("Destination");
	    if(s.toLowerCase().startsWith("http://"))
		// remove 'http://' and host name
		s = s.substring(s.indexOf('/', 7));

	    if(DavServer.dbg) DavServer.p("Destination: "+s);

	    boolean overwrite = true;
	    String field = req.getField("Overwrite");
	    if(field != null && field.toUpperCase().equals("F"))
		overwrite = false;

	    String[] srcPath = splitPath(req.getRequestURL().getPath());
	    String[] dstPath = splitPath(s);

	    Directory srcDir, dstDir;

	    try {
		srcDir = (Directory)getResource(parent(srcPath));
		dstDir = (Directory)getResource(parent(dstPath));
	    } catch(FileNotFoundException _) {
		return resf.makeError(409, "Conflict");
	    } catch(IllegalArgumentException _) {
		return resf.makeError(409, "Conflict");
	    } catch(ClassCastException _) {
		return resf.makeError(409, "Conflict");
	    }

	    try {
		if(copy) 
		    srcDir.copy(srcPath[srcPath.length-1], dstDir,
				dstPath[dstPath.length-1], overwrite);
		else
		    srcDir.move(srcPath[srcPath.length-1], dstDir,
				dstPath[dstPath.length-1], overwrite);
	    } catch(FileNotFoundException e) {
		if(DavServer.dbg) e.printStackTrace();
		return resf.makeError(404, "Not found");
	    } catch(FileExistsException e) {
		return resf.makeError(412, "File exists");
	    }

	    HTTPResponse resp = resf.makeResponse(204, "No content");
	    resp.commit();
	    resp.close();
	    return resp;
	}

	protected HTTPResponse doPropfind(HTTPRequest req, 
					  HTTPResponse.Factory resf)
	    throws IOException {

	    if(DavServer.dbg) DavServer.p("PROPFIND "+req);

	    // 1. find out which resources are queried

	    Resource res;

	    try {
		res = getResource(req.getRequestURL().getPath());
	    } catch(FileNotFoundException e) {
		return resf.makeError(404, "File not found");
	    } catch(IOException e) {
		e.printStackTrace();
		return resf.makeError(500, "Internal server error");
	    }

	    int depth; // 0, 1, or -1, which stands for infinity
	    
	    String s = req.getField("Depth");
	    if(s == null) depth = -1;
	    else {
		s = s.trim().toLowerCase();
		if(s.equals("0")) depth = 0;
		else if(s.equals("1")) depth = 1;
		else if(s.equals("infinity")) depth = -1;
		else
		    return resf.makeError(400, "Bad Depth header: "+s);
	    }

	    String base = req.getRequestURL().getPath();
	    if(base.endsWith("/")) base = base.substring(0, base.length()-1);
	    Set paths = new HashSet();
	    paths.add(base);
	    if(depth > 0)
		addChildren(res, base, paths, false);
	    else if (depth < 0)
		addChildren(res, base, paths, true);

	    // 2. parse request body

	    // Set of properties to be returned, as strings
	    // (namespaceURI + " " + localName).
	    // null = all properties.
	    Set properties = null;

	    // The request may have no body; then there shouldn't be
	    // a Content-Type header. If there is one, we assume a body.
	    if(req.getField("Content-Type") != null) {
		Builder builder = new Builder();
		Document request;
		try {
		    request = builder.build(req.getInputStream());
		} catch(nu.xom.ParsingException _) {
		    return resf.makeError(400, "Bad request " +
					  "(XML does not parse)");
		}
		if(DavServer.dbg) {
		    DavServer.p("Propfind: "+request.toXML());
		}

		Element e = request.getRootElement();
		if(!key(e).equals("DAV: propfind"))
		    return resf.makeError(400, "Bad request (root element "+
					  "is not <DAV:propfind>)");

		Elements es = e.getChildElements();
		if(es.size() == 0)
		    return resf.makeError(400, "Bad request (no content "+
					  "in propfind element)");
		else if(es.size() > 1)
		    return resf.makeError(400, "propfind element has more "+
					  "than one child");

		e = es.get(0);
		es = e.getChildElements();

		if(key(e).equals("DAV: allprop"))
		    properties = null;
		else {
		    properties = new HashSet();
		    for(int i=0; i<es.size(); i++)
			properties.add(key(es.get(i)));
		}
	    }

	    if(DavServer.dbg)
		DavServer.p("Requested properties: "+properties);

	    // 3. prepare result

	    Element eRoot = new Element("multistatus", DAV);
	    Document document = new Document(eRoot);

	    for(Iterator i=paths.iterator(); i.hasNext();) {
		String path = (String)i.next();
		Resource r = getResource(path);


		Element eResourceType = new Element("resourcetype", DAV);

		if(r instanceof Directory) {
		    path += "/";
		    eResourceType.appendChild(new Element("collection", DAV));
		}

		String ct = r.getContentType();
		if(ct == null) ct = "application/octet-stream";
		if(r instanceof Directory)
		    ct = "httpd/unix-directory";

		Element eContentType = new Element("getcontenttype", DAV);
		eContentType.appendChild(ct);
		Element eContentLength = new Element("getcontentlength", DAV);
		eContentLength.appendChild(""+r.getContentLength());
		Element eCreationDate = new Element("creationdate", DAV);
		eCreationDate.appendChild("2004-03-06T19:07:15Z");
		Element eLastModified = new Element("getlastmodified", DAV);
		eLastModified.appendChild("Sat, 06 Mar 2004 19:07:15 GMT");
		Element eLockDiscovery = new Element("lockdiscovery", DAV);
		Element eSupportedLock = new Element("supportedlock", DAV);

		Map found = new HashMap(r.getProperties());
		Set notFound;

		found.put("DAV: resourcetype", eResourceType);
		found.put("DAV: getcontenttype", eContentType);
		found.put("DAV: getcontentlength", eContentLength);
		found.put("DAV: creationdate", eCreationDate);
		found.put("DAV: getlastmodified", eLastModified);
		found.put("DAV: lockdiscovery", eLockDiscovery);
		found.put("DAV: supportedlock", eSupportedLock);

		if(DavServer.dbg) DavServer.p("existing properties: "+found);

		if(properties == null) {
		    notFound = Collections.EMPTY_SET;
		} else {
		    found.keySet().retainAll(properties);

		    notFound = new HashSet(properties);
		    notFound.removeAll(found.keySet());
		}

		if(DavServer.dbg) {
		    DavServer.p("found: "+found);
		    DavServer.p("not found: "+notFound);
		}


		Element eResponse = new Element("response", DAV);
		eRoot.appendChild(eResponse);
		
		Element eHref = new Element("href", DAV);
		eResponse.appendChild(eHref);
		eHref.appendChild(path);


		if(found.size() > 0) {
		    Element ePropstat = new Element("propstat", DAV);
		    eResponse.appendChild(ePropstat);

		    Element eStatus = new Element("status", DAV);
		    eResponse.appendChild(eStatus);
		    eStatus.appendChild("HTTP/1.1 200 Ok");
		    
		    Element eProp = new Element("prop", DAV);
		    ePropstat.appendChild(eProp);

		    for(Iterator j=found.values().iterator(); j.hasNext();) {
			Element e = new Element((Element)j.next());
			eProp.appendChild(e.copy());
		    }
		}

		if(notFound.size() > 0) {
		    Element ePropstat = new Element("propstat", DAV);
		    eResponse.appendChild(ePropstat);

		    Element eStatus = new Element("status", DAV);
		    eResponse.appendChild(eStatus);
		    eStatus.appendChild("HTTP/1.1 404 Not Found");
		    
		    Element eProp = new Element("prop", DAV);
		    ePropstat.appendChild(eProp);

		    for(Iterator j=notFound.iterator(); j.hasNext();) {
			String key = (String)j.next();
			int sp = key.indexOf(' ');
			Element e = new Element(key.substring(sp+1),
						key.substring(0, sp));
			eProp.appendChild(e);
		    }
		}
	    }

	    // 4. send result

	    HTTPResponse resp = resf.makeResponse(207, "Multi-Status");
	    resp.setField("Content-Type", "text/xml; charset=\"utf-8\"");
	    resp.commit();

	    OutputStream outputStream = resp.getOutputStream();
	    Serializer serializer = new Serializer(outputStream);
	    serializer.setIndent(4);
	    serializer.setLineSeparator("\n");
	    serializer.setMaxLength(80);
	    serializer.write(document);

	    if(DavServer.dbg) {
		serializer.setOutputStream(System.out);
		serializer.write(document);
	    }

	    outputStream.close();
	    resp.close();

	    return resp;
	}	    
    }

    protected void addChildren(Resource parent, String path,
			       Set into, boolean recurse) throws IOException {
	if(!(parent instanceof Directory)) return;
	Directory d = (Directory)parent;
	for(Iterator i=d.getEntries().keySet().iterator(); i.hasNext();) {
	    String child = encode((String)i.next());
	    into.add(path+"/"+child);
	    if(recurse) addChildren((Resource)d.getEntries().get(child),
				    path+"/"+child, into, true);
	}
    }


    protected String[] parent(String[] path) {
	if(path.length == 0)
	    throw new IllegalArgumentException("empty path");
	String[] parent = new String[path.length-1];
	for(int i=0; i<parent.length; i++) parent[i] = path[i];
	return parent;
    }

    protected String[] splitPath(String path) throws IllegalArgumentException {
	List segments = new ArrayList();

	String rest = path;

	if(rest.startsWith("/"))
	    rest = rest.substring(1);
	if(rest.endsWith("/"))
	    rest = rest.substring(0, path.length()-1);

	while(true) {
	    int pos = rest.indexOf('/');
	    //DavServer.p("pos "+pos+", rest '"+rest+"'");
	    if(pos < 0) break;
	    if(pos == 0)
		throw new IllegalArgumentException("Malformed path: "+path);

	    String part = rest.substring(0, pos);
	    rest = rest.substring(pos+1);

	    segments.add(decode(part));
	}
	if(!rest.equals(""))
	    segments.add(rest);

	String[] result = new String[segments.size()];
	for(int i=0; i<result.length; i++) 
	    result[i] = (String)segments.get(i);
	return result;
    }

    protected String decode(String uriSegment) {
	// XXX handle UTF-8 encoded strings
	// i.e., %aa%bb to represent a character in UTF-8

	return java.net.URLDecoder.decode(uriSegment);
    }

    protected String encode(String uriSegment) {
	// XXX handle UTF-8 encoded strings
	// i.e., %aa%bb to represent a character in UTF-8

	return java.net.URLEncoder.encode(uriSegment);
    }

    

    public static class SimpleResource implements Resource {
	protected ByteArrayOutputStream bos = new ByteArrayOutputStream();
	protected String contentType = null;
	protected Map properties = new HashMap();

	public String getContentType() { return contentType; }
	public int getContentLength() { return bos.toByteArray().length; }

	public Map getProperties() { return properties; }
	public OutputStream getOutputStream(String contentType) 
	    throws IOException {

	    bos = new ByteArrayOutputStream();
	    this.contentType = contentType;
	    return bos;
	}
	public InputStream getInputStream() throws IOException {
	    return new ByteArrayInputStream(bos.toByteArray());
	}

	public SimpleResource copy() throws IOException {
	    SimpleResource n = new SimpleResource();
	    n.contentType = contentType;
	    n.properties.putAll(properties);
	    n.bos.write(bos.toByteArray());
	    return n;
	}
    }

    public static class SimpleDirectory 
	extends SimpleResource implements Directory {

	protected Map entries = new HashMap();

	public Map getEntries() { 
	    return new HashMap(entries); 
	}
	public SimpleResource copy() throws IOException {
	    SimpleDirectory n = new SimpleDirectory();
	    n.contentType = contentType;
	    n.properties.putAll(properties);
	    n.bos.write(bos.toByteArray());
	    for(Iterator i=entries.keySet().iterator(); i.hasNext();) {
		String filename = (String)i.next();
		SimpleResource res = (SimpleResource)entries.get(filename);
		n.entries.put(filename, res.copy());
	    }
	    return n;
	}

	public Resource newFile(String filename) {
	    Resource r = new SimpleResource();
	    entries.put(filename, r);
	    return r;
	}
	public Directory newDirectory(String filename) {
	    Directory d = new SimpleDirectory();
	    entries.put(filename, d);
	    return d;
	}
	public void delete(String filename) throws FileNotFoundException {
	    if(entries.remove(filename) == null)
		throw new FileNotFoundException(filename);
	}
	public void move(String filename, Directory dst, String newname,
			 boolean overwrite) throws IOException {
	    SimpleDirectory d = (SimpleDirectory)dst;
	    SimpleResource r = (SimpleResource)entries.get(filename);
	    if(r == null)
		throw new FileNotFoundException(filename);
	    if(!overwrite && d.entries.keySet().contains(newname))
		throw new FileExistsException("File exists: "+newname);

	    entries.remove(filename);
	    d.entries.put(newname, r);
	}
	public void copy(String filename, Directory dst, String newname,
			 boolean overwrite) throws IOException {
	    SimpleDirectory d = (SimpleDirectory)dst;
	    SimpleResource r = (SimpleResource)entries.get(filename);
	    if(r == null)
		throw new FileNotFoundException(filename);
	    if(!overwrite && d.entries.keySet().contains(newname))
		throw new FileExistsException("File exists: "+newname);

	    d.entries.put(newname, r.copy());
	}
    }


    public static class FileExistsException extends IOException {
	public FileExistsException(String s) { super(s); }
    }


    public static void main(String[] argv) throws Exception {
	int port = 5000;
	if(argv.length > 0)
	    port = Integer.parseInt(argv[0]);

	Directory root = new SimpleDirectory();
	CopyUtil.writeString("(root)\n", root.getOutputStream("text/plain"));

	Directory a = root.newDirectory("a");
	CopyUtil.writeString("/a\n", a.getOutputStream("text/plain"));

	Resource b = a.newFile("b");
	CopyUtil.writeString("/a/b\n", b.getOutputStream("text/plain"));

	DavServer server = new DavServer(root, port);
	p("Starting example server");
	if(dbg) p("dbg is on");
	server.run();
    }
}
