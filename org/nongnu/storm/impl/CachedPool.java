// (c): Matti J. Katila

package org.nongnu.storm.impl;
import org.nongnu.storm.*;
import org.nongnu.storm.references.*;
import java.io.*;
import java.util.*;

/** Cached indexed storm pool which multiplexes 
 *  between a local cache and a remote, "real" pool.
 *  XXX remove blocks from local cache?
 */
public class CachedPool implements IndexedPool {

    protected IndexedPool remote, cache;

    protected Set indexTypes;
    protected Map indices = new HashMap();

    protected SetCollector cachedIds = null;
    protected Map cachedMappings = new HashMap();

    /** Value in milliseconds to retrieve new 
     *  setCollector from remote pool.
     */
    public long TOO_OLD_COLLECTOR = 2 * 60 * 1000; // 2 min

    public CachedPool(IndexedPool remote, IndexedPool cache) {
	this.remote = remote;
	this.cache = cache;
	this.indexTypes = remote.getIndexTypes();
	
	for(Iterator i=indexTypes.iterator(); i.hasNext();) {
	    IndexType type = (IndexType)i.next();
	    indices.put(type.getIndexTypeURI(), type.createIndex(this));
	}
    }

    protected void changed() {
	cachedIds = null;
	cachedMappings = new HashMap();
    }

    
    // ---------------------------------------------
    // implement  StormPool 
    // ---------------------------------------------

    public void add(Block b) throws IOException {
        cache.add(b);
        remote.add(b);
        changed();
    }

    public void delete(Block b) throws IOException {
        cache.delete(b);
        remote.delete(b);
        changed();
    }


    public Block get(BlockId id) throws IOException, FileNotFoundException {
        try {
            Block b = cache.get(id);
            return b;
        } catch (FileNotFoundException _) {
            Block b2 = remote.get(id);
            cache.add(b2);
            return cache.get(id);
        }
    }

    protected class Request extends Thread {
	protected BlockId id;
	protected BlockListener listener;

	protected Request(BlockId id, BlockListener listener) {
	    this.id = id;
	    this.listener = listener;
	}

	public void run() {
	    try {
		Block b = remote.get(id);
		cache.add(b);
		listener.success(cache.get(id));
	    } catch(IOException e) {
		listener.failure(id, e);
	    }
	}
    }

    public Block request(BlockId id, BlockListener listener) 
        throws IOException {

        try {
            return cache.request(id);
        } catch (FileNotFoundException exp) {
	    new Request(id, listener).run();
	    return null;
        }
    }

    public Block request(BlockId id) throws IOException {
        return request(id, null);
    }
    

    public SetCollector getIds() throws IOException {
        if (cachedIds == null ||
            cachedIds.getAge() > TOO_OLD_COLLECTOR) {
            
            cachedIds = remote.getIds();
        }
        return cachedIds;
    }

    public Block get(ReferenceId id) throws IOException {
	return new Reference(this, id).resolve(this);
    }


    // ---------------------------------------------
    // implement  IndexedPool 
    // ---------------------------------------------

    public Object getIndex(String typeURI) {
	return indices.get(typeURI);
    }
    public Set getIndexTypes() {
	return Collections.unmodifiableSet(indexTypes);
    }
    public Map getIndices() {
	return Collections.unmodifiableMap(indices);
    }

    public Collector getMappings(String typeURI, String key) 
	throws IOException {

	Collector mappings = (Collector)cachedMappings.get(typeURI+" "+key);

        if (mappings == null ||
            mappings.getAge() > TOO_OLD_COLLECTOR) {
            
            mappings = remote.getMappings(typeURI, key);
	    cachedMappings.put(typeURI+" "+key, mappings);
        }
        return mappings;
    }

    public Block get(PointerId id) throws IOException {
	return Pointers.get(id, this);
    }






    public BlockOutputStream getBlockOutputStream(String contentType) throws IOException {
        changed();
        final BlockOutputStream out = 
            cache.getBlockOutputStream(contentType);
        return new BlockOutputStream(out, contentType) {

                boolean closed = false;
                
                public void close() throws IOException {
                    if (closed) return;
                    closed = true;

                    out.close();
                    remote.add(((BlockOutputStream)out).getBlock());
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


}
