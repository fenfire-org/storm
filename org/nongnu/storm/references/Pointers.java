/*
Pointers.java
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
import java.io.IOException;
import java.security.*;
import java.util.*;

public class Pointers {
    public static final String initialPublicKeySpec = 
	"http://purl.oclc.org/NET/storm/vocab/pointers/initialPublicKeySpec";

    public static final String identificationInfo = 
	"http://purl.oclc.org/NET/storm/vocab/pointers/identificationInfo";

    public static final String timestamp = 
	"http://purl.oclc.org/NET/storm/vocab/pointers/timestamp";

    public static final String hasPointerRecord = 
	"http://purl.oclc.org/NET/storm/vocab/pointers/hasPointerRecord";

    public static final String version = 
	"http://purl.oclc.org/NET/storm/vocab/pointers/version";

    public static final String obsoletedBy = 
	"http://purl.oclc.org/NET/storm/vocab/pointers/obsoletedBy";

    public static final String hasRepresentationRecord = 
	"http://purl.oclc.org/NET/storm/vocab/pointers/hasRepresentationRecord";

    public static final String hasInstanceRecord = 
	"http://purl.oclc.org/NET/storm/vocab/pointers/hasInstanceRecord";

    public static final String hasDescriptionRecord = 
	"http://purl.oclc.org/NET/storm/vocab/pointers/hasDescriptionRecord";


    public static final Set VERSION_PROPERTIES =
	Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {hasRepresentationRecord, hasInstanceRecord, hasDescriptionRecord})));



    public static final KeyFactory keyFactory;
    static {
	KeyFactory f = null;
	try {
	    f = KeyFactory.getInstance("DSA");
	} catch(NoSuchAlgorithmException e) {
	    e.printStackTrace();
	    //throw new Error("Pointers need DSA algorithm");
	}
	keyFactory = f;
    }



    public static Reference getRecord(PointerId id, IndexedPool pool) 
	throws IOException {

	PointerIndex idx = (PointerIndex)pool.getIndex(PointerIndex.uri);
	Reference record = idx.getMostCurrent(id, Pointers.VERSION_PROPERTIES);
	return record;
    }

    public static String getTarget(Reference record) throws IOException {
	return record.get("_:this", Pointers.version);
    }

    public static String getTarget(PointerId id, IndexedPool pool) 
	throws IOException {

	return getTarget(getRecord(id, pool));
    }

    public static Block get(PointerId id, IndexedPool pool) 
	throws IOException {

	return get(getTarget(id, pool), pool);
    }

    public static Block get(Reference record, IndexedPool pool) 
	throws IOException {

	return get(getTarget(record), pool);
    }

    public static Reference getReference(String uri, IndexedPool pool)
	throws IOException {

	if(uri.startsWith(ReferenceId.PREFIX))
	    return new Reference(pool, new ReferenceId(uri));
	else if(uri.startsWith(PointerId.PREFIX))
	    return getReference(getTarget(new PointerId(uri), pool), pool);
	else
	    throw new IllegalArgumentException("URI scheme cannot be resolved to reference: "+uri);
    }

    public static Block get(String uri, IndexedPool pool) throws IOException {
	if(uri.startsWith(BlockId.PREFIX))
	    return pool.get(new BlockId(uri));
	else if(uri.startsWith(ReferenceId.PREFIX))
	    return pool.get(new ReferenceId(uri));
	else if(uri.startsWith(PointerId.PREFIX))
	    return pool.get(new PointerId(uri));
	else
	    throw new IOException("URI scheme not understood: "+uri);
    }



    /** Get the pointer a pointer record is associated with.
     */
    public static PointerId getPointer(Reference record) {
	// XXX can a pointer record be associated with a pointer
	// on more than one property, and maybe even associated
	// with more than one pointer?
	String property = getProperty(record);
	return new PointerId(record.get("_:this", property, -1));
    }

    public static final String RDFS_SUBPROPERTY_OF = 
	"http://www.w3.org/2000/01/rdf-schema#subPropertyOf";

    public static String getProperty(Reference record) {
	return record.get(Pointers.hasPointerRecord, RDFS_SUBPROPERTY_OF, -1);
    }

}
