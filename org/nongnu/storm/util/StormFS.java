/*
StormFS.java
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
import org.nongnu.storm.impl.DirPool;
import org.nongnu.storm.references.*;
import java.io.*;
import java.util.*;

/** Utility classes for Storm file system emulation layer.
 */
public class StormFS {
    public static boolean dbg = false;
    private static void p(String s) { System.out.println("DavServer:: "+s); }

    public static final String
	rdf = "XXX",
	stormfs = "XXX",
	rdf_type = rdf+"type",
	rdf_first = rdf+"first",
	rdf_next = rdf+"next",
	rdf_List = rdf+"List",
	rdf_nil = rdf+"nil",
	stormfs_hasDirectoryEntries = stormfs+"hasDirectoryEntries",
	stormfs_filename = stormfs+"filename",
	stormfs_resource = stormfs+"resource";

    public static boolean isDirectory(Graph graph) {
	try {
	    graph.get("_:this", stormfs_hasDirectoryEntries);
	    return true;
	} catch(NoSuchElementException _) {
	    return false;
	}
    }

    protected PointerSigner signer;
    protected IndexedPool pool;

    public StormFS(PointerSigner signer) {
	this.signer = signer;
	this.pool = signer.getPool();
    }

    public class DavResource implements DavServer.Resource {
	protected String uri;

	public DavResource(String uri) { this.uri = uri; }

	public String getURI() { return uri; }

	public String getContentType() throws IOException {
	    try {
		return Pointers.get(uri, pool).getId().getContentType();
	    } catch(FileNotFoundException e) {
		return null;
	    }
	}

	public int getContentLength() { return 0; }

	public InputStream getInputStream() throws IOException {
	    return Pointers.get(uri, pool).getInputStream();
	}

	public OutputStream getOutputStream(String ct) throws IOException {
	    final PointerId ptr;
	    try {
		ptr = new PointerId(uri);
	    } catch(IllegalArgumentException e) {
		throw new IOException("Cannot write to non-pointer: "+uri);
	    }

	    final BlockOutputStream bos = pool.getBlockOutputStream(ct);

	    return new FilterOutputStream(bos) {
		    public void close() throws IOException {
			super.close();
			try {
			    BlockId id = bos.getBlockId();
			    signer.updateNewest(ptr, id.getURI());
			} catch(java.security.GeneralSecurityException e) {
			    e.printStackTrace();
			    throw new IOException(""+e);
			}
		    }
		};
	}

	public Map getProperties() { return Collections.EMPTY_MAP; }

	public DavResource copy() throws IOException {
	    //throw new UnsupportedOperationException();	    
	    PointerId ptr = signer.newPointer();
	    
	    PointerId thisPtr = null;
	    try {
		thisPtr = new PointerId(uri);
	    } catch(IllegalArgumentException _) {}

	    String target;
	    if(thisPtr != null) {
		try {
		    target = Pointers.getTarget(thisPtr, pool);
		} catch(FileNotFoundException e) {
		    // copied pointer not initialized
		    return new DavResource(ptr.getURI());
		}
	    } else {
		target = uri;
	    }

	    try {
		signer.initialize(ptr, Pointers.hasInstanceRecord, target);
	    } catch(java.security.GeneralSecurityException e) {
		e.printStackTrace();
		throw new IOException(""+e);
	    }

	    return new DavResource(ptr.getURI());
	}
    }

    public class DavDirectory 
	extends DavResource implements DavServer.Directory {

	protected PointerId ptr;
	protected Reference lastRecord;

	public DavDirectory(PointerId ptr) {
	    super(ptr.getURI());
	    this.ptr = ptr;
	}

	public DavDirectory(String uri) {
	    super(uri);
	    this.ptr = new PointerId(uri);
	}


	protected DirectoryVersion getVersion() throws IOException {
	    String target = Pointers.getTarget(ptr, pool);
	    ReferenceId refid = new ReferenceId(target);
	    Reference ref = new Reference(pool, refid);
	    return new DirectoryVersion(ref);
	}

	protected void setVersion(DirectoryVersion v) throws IOException {
	    ReferenceId id = v.write(pool);
	    try {
		if(lastRecord != null)
		    lastRecord = signer.update(lastRecord, id.getURI());
		else
		    lastRecord = signer.initialize(ptr, 
						   Pointers.hasInstanceRecord,
						   id.getURI());
	    } catch(java.security.GeneralSecurityException e) {
		e.printStackTrace();
		throw new IOException(""+e);
	    }
	}

	public Map getEntries() throws IOException {
	    try {
		Map entries = new HashMap();

		DirectoryVersion v = getVersion();
		for(Iterator i=v.getEntries().keySet().iterator(); i.hasNext();) {
		    String filename = (String)i.next();
		    String uri = (String)v.getEntries().get(filename);

		    DavResource resource;
		    
		    Reference ref;
		    try {
			ref = Pointers.getReference(uri, pool);
		    } catch(FileNotFoundException _) {
			ref = null;
		    } catch(IllegalArgumentException _) {
			ref = null;
		    }

		    if(ref != null && isDirectory(ref))
			resource = new DavDirectory(new PointerId(uri));
		    else
			resource = new DavResource(uri);

		    entries.put(filename, resource);
		}

		return entries;
	    } catch(IOException e) {
		e.printStackTrace();
		throw e;
	    }
	}

	public DavServer.Resource newFile(String filename) throws IOException {
	    String uri = signer.newPointer().getURI();
	    setVersion(getVersion().put(filename, uri));
	    return new DavResource(uri);
	}

	public DavServer.Directory newDirectory(String filename) 
	    throws IOException {

	    DavDirectory d = makeDirectory();
	    setVersion(getVersion().put(filename, d.getURI()));
	    return d;
	}

	public void delete(String filename) throws IOException {
	    setVersion(getVersion().remove(filename));
	}


	public void copy(String sourceName, DavServer.Directory destination, 
			 String destName, boolean overwrite) 
	    throws IOException {

	    copyOrMove(sourceName, destination, destName, overwrite, true);
	}

	public void move(String sourceName, DavServer.Directory destination, 
			 String destName, boolean overwrite) 
	    throws IOException {

	    copyOrMove(sourceName, destination, destName, overwrite, false);
	}

	protected void copyOrMove(String sourceName, 
				  DavServer.Directory destination, 
				  String destName, boolean overwrite, 
				  boolean copy) throws IOException {

	    DavDirectory dst = (DavDirectory)destination;

	    Map srcEntries = new HashMap(getEntries());
	    DavResource r = (DavResource)srcEntries.get(sourceName);
	    if(r == null)
		throw new FileNotFoundException(sourceName);
	    
	    Map entryMap = new HashMap(dst.getVersion().getEntries());
	    if(!overwrite && entryMap.keySet().contains(destName))
		throw new DavServer.FileExistsException(destName);

	    if(copy) {
		entryMap.put(destName, r.copy().getURI());
		dst.setVersion(new DirectoryVersion(entryMap));
	    } else {
		entryMap.put(destName, r.getURI());
		srcEntries.remove(sourceName);

		dst.setVersion(new DirectoryVersion(entryMap));
		setVersion(new DirectoryVersion(srcEntries));
	    }
	}
    }

    public DavDirectory makeDirectory() throws IOException {
	DirectoryVersion v = new DirectoryVersion(Collections.EMPTY_MAP);
	ReferenceId ref = v.write(pool);
	
	PointerId ptr = signer.newPointer();
	try {
	    signer.initialize(ptr, Pointers.hasInstanceRecord, 
			      ref.getURI());
	} catch(java.security.GeneralSecurityException e) {
	    e.printStackTrace();
	    throw new IOException(""+e);
	}
	
	return new DavDirectory(ptr.getURI());
    }

    public static class DirectoryVersion {
	protected Graph graph;
	protected Map entries;

	public DirectoryVersion(Graph graph) {
	    Map entries = new TreeMap();

	    this.graph = graph;
	    this.entries = Collections.unmodifiableMap(entries);

	    String list = graph.get("_:this", stormfs_hasDirectoryEntries);
	    while(!list.equals(rdf_nil)) {
		String entry = graph.get(list, rdf_first);
		String filename = graph.getString(entry, stormfs_filename);
		String resource = graph.get(entry, stormfs_resource);

		entries.put(filename, resource);

		list = graph.get(list, rdf_next);
	    }
	}

	public DirectoryVersion(Map entries) {
	    entries = Collections.unmodifiableMap(new TreeMap(entries));
	    this.entries = entries;
	    
	    List filenames = new ArrayList(entries.keySet());
	    Collections.reverse(filenames);

	    Graph.Maker m = new Graph.Maker();
	    m.add("_:this", "http://purl.oclc.org/NET/storm/vocab/ref-uri/resolutionMethod", "http://purl.oclc.org/NET/storm/vocab/ref-uri/ReferenceGraph"); // XXX

	    String list = rdf_nil;

	    for(Iterator i=filenames.iterator(); i.hasNext();) {
		String filename = (String)i.next();
		String resource = (String)entries.get(filename);

		String lastlist = list;
		list = m.bnode();
		m.add(list, rdf_type, rdf_List);
		m.add(list, rdf_next, lastlist);

		String entry = m.bnode();
		m.add(list, rdf_first, entry);
		m.addString(entry, stormfs_filename, filename);
		m.add(entry, stormfs_resource, resource);
	    }

	    m.add("_:this", stormfs_hasDirectoryEntries, list);

	    this.graph = m.make();
	}

	public Map getEntries() { return entries; }
	public Graph getGraph() { return graph; }

	public DirectoryVersion put(String filename, String resource) {
	    HashMap m = new HashMap(entries);
	    m.put(filename, resource);
	    return new DirectoryVersion(m);
	}

	public DirectoryVersion remove(String filename) {
	    HashMap m = new HashMap(entries);
	    m.remove(filename);
	    return new DirectoryVersion(m);
	}

	public ReferenceId write(StormPool pool) throws IOException {
	    BlockOutputStream bos = pool.getBlockOutputStream("text/plain");
	    graph.write(bos);
	    bos.close();
	    return new ReferenceId(bos.getBlockId());
	}
    }

    public static void main(String[] argv) throws Exception {
	int port = 5000;

	Set indexTypes = new HashSet();
	indexTypes.add(PointerIndex.type);

	File dir = new File("/tmp/dav-test-pool");
	dir.mkdir();

	IndexedPool pool = new DirPool(dir, indexTypes);

	BlockOutputStream bos = pool.getBlockOutputStream("text/plain");
	bos.write(ownerReference.getBytes("US-ASCII"));
	bos.close();

	PointerSigner signer = 
	    new PointerSigner(pool, new ByteArrayInputStream(keyinfo));
	
	StormFS fs = new StormFS(signer);
	DavDirectory root = fs.makeDirectory();

	DavServer server = new DavServer(root, port);
	p("Starting StormFS example server");
	if(dbg) p("dbg is on");
	server.run();
    }


    private static final byte[] keyinfo = Base64.decode("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".toCharArray());
    private static final String ownerReference = "_:this <http://purl.oclc.org/NET/storm/vocab/pointers/identificationInfo> <vnd-storm-hash:text/plain,3i42h3s6nnfq2msvx7xzkyayscx5qbyj.lwpnacqdbzryxw3vhjvcj64qbznghohhhzwclnq>.\n_:this <http://purl.oclc.org/NET/storm/vocab/pointers/initialPublicKeySpec> \"MIIBtzCCASwGByqGSM44BAEwggEfAoGBAOe2J4Sux+Ro1wGeGtQuzdURFYDD5fNWCKV6wMvUXGRYdzY2W1wPnbsGyY8ZT370qz0KzIhA2fC8Oitlx5lDizMt6tLcoIZuvtZqxboxSK+AunjHrQTPJ2EvTn6A2uFCbhS05QehFL10zO2Y36OVZC64L2Px/G30p3h2EGqig3fpAhUA4NzkcG0W3CDywKy6EagsFloDQ78CgYEAoWfwEMysRHNdZxZyouixQYh4oYAgbICJjZrhyhaZHNm5Ka4vuZ2l0j4vkLkN9JxAPofHssCArwJgk7EZ3VpMGitUThhq5R6TUApSSFNw47+/KABCd90iqOvggnUdbrd/K3zJR7E5V4jbAUJ1//PXNXnQJlnLXkI2gyjImjxNlIoDgYQAAoGAbeOO00VimGX/cL9iAkR1C1iAfb6l5ewpuDm5Ie8XqNMnDJ2Yamh5+IHrjqKsRqHRL7rDv+iDRq0DpMkRflXCR5OoIpL1tq3zT6TjiSqo0uyfRLRoh9umlYVYn0WISJENZ7SsV+poW0zF2Xjx1jWqjFxq3z2wlg1c4DwRm2ak6FU=\"^^<http://www.w3.org/2001/XMLSchema#base64Binary>.\n_:this <http://purl.oclc.org/NET/storm/vocab/ref-uri/resolutionMethod> <http://purl.oclc.org/NET/storm/vocab/ref-uri/ReferenceGraph>.\n";
}
