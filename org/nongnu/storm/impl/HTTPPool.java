// (c): Matti J. Katila


/*

10:01 < benja> they need to implement IndexedPool
10:01 < mudyc> thank you :)
10:01 < benja> HTTPPool is supposed to interface through the network with 
               HTTPProxy
10:01 < benja> i.e., the HTTPPool accesses the HTTPProxy and makes its contents 
               available through the IndexedPool interface
10:02 < benja> (so far, HTTPProxy doesn't serve index entries from IndexedPool, 
               so you also have to add that)


18:57 < mudyc> i tried to look HTTPProxy, but it was quite unclear how it 
               should be a pool :/
18:57 < mudyc> s/look/look at/
18:57 < benja> HTTPProxy is the "server," HTTPPool is supposed to be the 
               "client"


17:48 <@mudyc> how should HTTPPool use the server? i am willing to use the 
               HTTPRequest like  request.setField("StormPool operation", "Add");
17:49 <@benja> no, no, no
17:49 <@benja> add uses a PUT http message
17:50 <@benja> get uses a GET message
17:51 <@mudyc> and others, like delete?
17:51 <@benja> delete uses a DELETE message
17:51 <@benja> which others?
17:52 <@mudyc> getIds?
17:52 <@benja> a GET to a specially designated URI
17:52 <@benja> say ./ids



*/


package org.nongnu.storm.impl;
import org.nongnu.storm.http.client.*;
import org.nongnu.storm.util.*;
import org.nongnu.storm.*;
import org.nongnu.storm.impl.TransientPool;
import java.util.*;
import java.io.*;
import java.net.URLEncoder;

/** Transient Storm pool which operates trough net via HTTP protocol. 
 *  The server should allow only localhost connection for security.
 */
public class HTTPPool extends AbstractLocalPool {

    static public boolean dbg = false;
    static private void p(String s) { System.out.println("HTTPPool:: "+s); }


    protected IndexedPool pool = new TransientPool(new HashSet());
    protected String httpVersion = "HTTP/1.1";
    protected HTTPConnection conn;
    public HTTPPool(String host, int port, Set indexTypes) throws IOException {
	super(indexTypes);
        conn = new HTTPConnection(host, port);
    }


    protected class HTTPBlock extends AbstractBlock {
	protected HTTPBlock(BlockId id) { super(id); }

	public InputStream getInputStream() throws IOException {
	    HTTPRequest request = conn.newRequest("GET", 
						  id.getURI(),
						  httpVersion, false);
	    request.commit();
	    HTTPResponse resp = conn.send(request);
	    if (resp.status == 200)
		return id.getCheckedInputStream(resp.getInputStream());
	    else 
		throw new IOException("Server error: "+resp.status);
	}
    }

    protected class HTTPBlockOutputStream extends AbstractBlockOutputStream {
	protected HTTPBlockOutputStream(String contentType)
	    throws IOException {

            super(new ByteArrayOutputStream(), contentType);
        }

	public Block makeBlock() throws IOException {
	    ByteArrayOutputStream baos = (ByteArrayOutputStream)out;
	    BlockId id = makeIdFromDigest();

	    InputStream in = new ByteArrayInputStream(baos.toByteArray());
	    putBlock(id, in);

	    return new HTTPBlock(id);
	}
    }

    
    /*
     * **********************************************************
     *   Implement StormPool
     * **********************************************************
     */

    // connect to server and add the block
    public void add(Block block) throws IOException {

	putBlock(block.getId(), block.getInputStream());
    }

    public void putBlock(BlockId id, InputStream data) throws IOException {
        // new add request
        HTTPRequest request = conn.newRequest("PUT", 
					      id.getURI(),
					      httpVersion, true);
	
        request.setField("Content-Type", id.getContentType());
        request.commit();

        // write the block into request
        CopyUtil.copy(data, request.getOutputStream());

        // send the request away
        HTTPResponse response = conn.send(request);
	while(response.getInputStream().read() >= 0);
	response.getInputStream().close();
        if (response.status != 200) 
            throw new IOException("exception: "+response);
    }

    public void delete(Block block)  throws IOException {
        HTTPRequest request = conn.newRequest("DELETE", 
                                               block.getId().getURI(),
                                               httpVersion, false);
        HTTPResponse response = conn.send(request);
	InputStream in = response.getInputStream();
	while(in.read() >= 0);
	in.close();
        if (response.status != 200) 
            throw new IOException("exception: "+response);
    }

    public Block get(BlockId id)  throws IOException {
        HTTPRequest request = conn.newRequest("GET", 
					      id.getURI(),
					      httpVersion, false);
        request.commit();
        HTTPResponse resp = conn.send(request);
	InputStream in = resp.getInputStream();
	while(in.read() >= 0);
	in.close();
        if (resp.status == 200)  // OK
            return new HTTPBlock(id);
	else if(resp.status == 404)
            throw new FileNotFoundException(id+" not found on HTTP server");
	else
	    throw new IOException("Server error code: "+resp.status);
    }

    public BlockOutputStream getBlockOutputStream(String contentType)
	throws IOException {

        final BlockOutputStream out = 
            pool.getBlockOutputStream(contentType);
        return new BlockOutputStream(out, contentType) {

                boolean closed = false;
                
                public void close() throws IOException {
                    if (closed) return;
                    closed = true;

                    out.close();
                    add(((BlockOutputStream)out).getBlock());
                }
                public Block getBlock() throws IOException {
                    if (!closed)
                        throw new Error("not closed!");
                    return ((BlockOutputStream)out).getBlock();
                }

                public BlockId getBlockId() throws IOException {
                    if (!closed)
                        throw new Error("not closed!");
                    return ((BlockOutputStream)out).getBlockId();
                }
                public String getContentType() {
                    return ((BlockOutputStream)out).getContentType();
                }
                public void write(byte[] b, int off, int len) throws IOException {
                    out.write(b,off,len);
                }
                public void write(int b) throws IOException {
                    out.write(b);
                }
            };

    }

    public SetCollector getIds() throws IOException {
        HTTPRequest request = conn.newRequest("GET", "/ids",
                                               httpVersion, false);
        request.commit();
        HTTPResponse resp = conn.send(request);

	if(resp.status != 200) {
	    InputStream in = resp.getInputStream();
	    while(in.read() >= 0);
	    in.close();
	    throw new IOException("Server response: "+resp.status);
	}

        BufferedReader buffer = new BufferedReader(
            new InputStreamReader(resp.getInputStream(), "US-ASCII"));

        Set set = new HashSet();
        String id = buffer.readLine();
        while (id != null) {
	    if(id.trim().equals("")) continue;
            set.add(new BlockId(id));
            id = buffer.readLine();
        }        
        buffer.close();

	return new SimpleSetCollector(set);
    }


    /*
     * **********************************************************
     *   Implement IndexedPool
     * **********************************************************
     */


    public Collector getMappings(String typeURI, 
                                 String key) throws IOException {
        
        String uri = "/mappings?typeURI=" + URLEncoder.encode(typeURI) + 
            "&key=" + URLEncoder.encode(key);
        HTTPRequest request = conn.newRequest("GET", uri,
                                               httpVersion, false);
        request.commit();
        HTTPResponse resp = conn.send(request);

	if(resp.status != 200) {
	    InputStream in = resp.getInputStream();
	    while(in.read() >= 0);
	    in.close();
	    throw new IOException("Server response: "+resp.status);
	}

        BufferedReader buffer = new BufferedReader(
            new InputStreamReader(resp.getInputStream(), "UTF-8"));

        Set set = new HashSet();
        while(true) {
	    String line = buffer.readLine();

	    if(line == null) break;
	    if(line.trim().equals("")) continue;

	    int sp = line.indexOf(' ');
	    BlockId block = new BlockId(line.substring(0, sp));
	    String value = line.substring(sp+1);

            set.add(new Mapping(block, key, value));
        }        
        buffer.close();

	return new SimpleSetCollector(set);
    }
}
