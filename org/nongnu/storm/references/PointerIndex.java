/*
PointerIndex.java
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
package org.nongnu.storm.references;
import org.nongnu.storm.*;
import org.nongnu.storm.util.Graph;
import org.nongnu.storm.util.Base64;
import org.nongnu.storm.util.HtmlLinkIndex;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.security.*;
import java.security.spec.*;
import java.util.*;

public class PointerIndex {
    public static boolean dbg = true;
    private static void p(String s) { System.out.println("PointerIndex:: "+s); }
    public static final String uri = 
	"http://fenfire.org/2004/01/pointer-index-v1";

    public static final IndexType type = new IndexType();

    protected IndexedPool pool;


    public Reference getMostCurrent(PointerId pointer, 
				    Set properties) throws IOException {
	Reference owner = new Reference(pool, pointer.getReferenceId());
	PublicKey pubkey = getPublicKey(owner, pool);
	Iterator iter = getUncheckedHistory(pointer, properties).iterator();
	for(; iter.hasNext();) {
	    PointerSignature sig = (PointerSignature)iter.next();
	    try {
		sig.check(pubkey);
	    } catch(IOException _) {
		continue;
	    } catch(GeneralSecurityException _) {
		continue;
	    }
	    return new Reference(pool, sig.getRecord());
	}
	throw new FileNotFoundException("pointer has no valid signatures");
    }

    public class FreshnessChecker {
	private Set properties;
	private Map current = new HashMap();

	public FreshnessChecker(Set properties) { 
	    this.properties = properties;
	}

	/** Return the PointerSignature if the link is current.
	 */
	public PointerSignature getIfCurrent(HtmlLinkIndex.Link link) 
	    throws IOException {

	    PointerSignature sig = (PointerSignature)current.get(link.pointer);
	    if(sig == null) {
		Reference owner = 
		    new Reference(pool, link.pointer.getReferenceId());
		PublicKey pubkey = getPublicKey(owner, pool);
		Iterator iter = 
		    getUncheckedHistory(link.pointer, properties).iterator();
		for(; iter.hasNext();) {
		    sig = (PointerSignature)iter.next();
		    try {
			sig.check(pubkey);
			break;
		    } catch(Exception _) {
			sig = null;
			continue;
		    }
		}
		current.put(link.pointer, sig);
	    }
	    if(sig.getBlockId().equals(link.signature))
		return sig;
	    else
		return null;
	}
    }

    public static final String key(String pointerURI, String propertyURI) {
	return pointerURI+" "+propertyURI;
    }
    
    /** Get a list of past pointer signatures of a pointer (newest first) */
    public SortedSet getHistory(PointerId pointer, 
				Set properties) throws IOException {
	SortedSet s = getUncheckedHistory(pointer, properties);
	Reference owner = new Reference(pool, pointer.getReferenceId());
	PublicKey pubkey = getPublicKey(owner, pool);

	for(Iterator i=s.iterator(); i.hasNext();) {
	    PointerSignature sig = (PointerSignature)i.next();
	    try {
		sig.check(pubkey);
	    } catch(Exception _) {
		i.remove();
	    }
	}

	return s;
    }

    public SortedSet getUncheckedHistory(PointerId pointer, 
					 Set properties) throws IOException {
	String pointerURI = pointer.getURI();
	Reference owner = new Reference(pool, pointer.getReferenceId());

	String[] prop = new String[properties.size()];
	Collector[] c = new Collector[properties.size()];
	int nth = 0;
	for(Iterator i=properties.iterator(); i.hasNext();) {
	    String propertyURI = (String)i.next();
	    prop[nth] = propertyURI;
	    c[nth] = pool.getMappings(uri, key(pointerURI, propertyURI));
	    nth++;
	}

	SortedSet s = new TreeSet(new Comparator() {
		public int compare(Object o1, Object o2) {
		    PointerSignature sig1 = (PointerSignature)o1;
		    PointerSignature sig2 = (PointerSignature)o2;

		    if(sig1.getTimestamp() == sig2.getTimestamp())
			return 0;
		    else if(sig1.getTimestamp() < sig2.getTimestamp())
			return +1;
		    else
			return -1;
		}
	    });

	PublicKey pubkey = getPublicKey(owner, pool);

	for(int k=0; k<c.length; k++) {
	    for(Iterator i=c[k].blockingIterator(); i.hasNext();) {
		IndexedPool.Mapping m = (IndexedPool.Mapping)i.next();
		int sp1 = m.value.indexOf(' '), 
		    sp2 = m.value.indexOf(' ', sp1+1);
		ReferenceId record = 
		    new ReferenceId(m.value.substring(0, sp1));
		long timestamp = Long.parseLong(m.value.substring(sp1+1, sp2));
		byte[] signature = 
		    Base64.decode(m.value.substring(sp2+1).toCharArray());
		try {
		    s.add(new PointerSignature(pointer, prop[k], 
					       record, timestamp, signature, 
					       pubkey, m.block));
		} catch(IOException e) {
		    if(dbg) p("Ignore mapping "+m+": "+e);
		} catch(GeneralSecurityException e) {
		    if(dbg) p("Ignore mapping "+m+": "+e);
		}
	    }
	}

	return s;
    }

    protected static PublicKey getPublicKey(Reference owner, 
					    StormPool pool) throws IOException {
	if(Pointers.keyFactory == null) 
	    throw new Error("PointerIndex needs DSA algorithm");

	byte[] keyBytes = 
	    owner.getBase64("_:this", Pointers.initialPublicKeySpec);
	X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
	try {
	    return Pointers.keyFactory.generatePublic(keySpec);
	} catch(InvalidKeySpecException e) {
	    throw new IOException("Key spec invalid: "+e+
				  " (owner: "+owner+")");
	}
    }

    protected static class IndexType implements IndexedPool.IndexType {

	public Set getMappings(Block block) throws IOException {
	    if(!block.getId().getContentType().equals("application/prs.fallenstein.pointersignature"))
		return Collections.EMPTY_SET;

	    PointerSignature sig;
	    try {
		sig = loadSignature(block);
	    } catch(Throwable _) {
		if(dbg) _.printStackTrace();
		return Collections.EMPTY_SET;
	    }

	    Set mappings = new HashSet();
	    
	    String encodedSig = new String(Base64.encode(sig.getSignature()));
	    mappings.add(new IndexedPool.Mapping(block.getId(),
						 key(sig.getPointer().getURI(),
						     sig.getProperty()),
						 sig.getRecord() + " " +
						 sig.getTimestamp() + " " +
						 encodedSig));


	    return mappings;
	}

	public Object createIndex(IndexedPool pool) {
	    return new PointerIndex(pool);
	}
	
	public String getIndexTypeURI() {
	    return uri;
	}
	
	public String getHumanReadableName() {
	    return ("An index of pointer records by pointer id and property.");
	}
    }

    public static PointerSignature loadSignature(Block block) throws IOException, GeneralSecurityException {
	PointerSignature sig;
	sig = new PointerSignature(block);

	StormPool pool = block.getPool();
	PointerId pointer = sig.getPointer();
	Reference owner = new Reference(pool, pointer.getReferenceId());
	
	PublicKey pubkey = getPublicKey(owner, pool);

	sig.check(pubkey);

	return sig;
    }
    

    public PointerIndex(IndexedPool pool) {
	this.pool = pool;
    }
}
