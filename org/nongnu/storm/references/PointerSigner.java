/*
PointerSigner.java
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
import org.nongnu.storm.util.URN5Namespace;
import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

/** A class that knows how to sign pointer records.
 *  This class stores the private key of a certain pointer owner.
 */
public class PointerSigner {
    private Reference owner;
    private KeyPair keyPair;
    private IndexedPool pool;
    private PointerIndex idx;

    public Reference getOwner() { return owner; }
    public IndexedPool getPool() { return pool; }

    public KeyPair getKeyPair() { return keyPair; }

    public PointerSigner(IndexedPool pool, InputStream readFrom) 
	throws IOException, GeneralSecurityException, ClassNotFoundException {

	this.pool = pool;
	idx = (PointerIndex)pool.getIndex(PointerIndex.uri);
	ObjectInputStream in = new ObjectInputStream(readFrom);
	this.owner = new Reference(pool, new ReferenceId(in.readUTF()));
	this.keyPair = (KeyPair)in.readObject();
	in.close();
    }
	
    public PointerSigner(Reference owner, KeyPair keyPair, IndexedPool pool) {
	this.owner = owner; this.keyPair = keyPair; this.pool = pool;
	idx = (PointerIndex)pool.getIndex(PointerIndex.uri);
	// XXX check pubkey
    }

    public static PointerSigner createOwner(IndexedPool pool,
					    BlockId identificationInfo) 
	throws IOException, InvalidKeyException, InvalidKeySpecException {

	KeyPairGenerator gen;
	try {
	    gen = KeyPairGenerator.getInstance("DSA");
	} catch(NoSuchAlgorithmException _) {
	    _.printStackTrace();
	    throw new Error("Pointers need DSA keys. "+_);
	}
	KeyPair keyPair = gen.generateKeyPair();

	return createOwner(pool, keyPair, identificationInfo);
    }

    public static PointerSigner createOwner(IndexedPool pool, KeyPair keyPair,
					    BlockId identificationInfo) 
	throws IOException, InvalidKeyException, InvalidKeySpecException {
	Graph.Maker g = new Graph.Maker();
	g.add("_:this", Pointers.identificationInfo, 
	      identificationInfo.getURI());
	g.add("_:this", "http://purl.oclc.org/NET/storm/vocab/ref-uri/resolutionMethod", "http://purl.oclc.org/NET/storm/vocab/ref-uri/ReferenceGraph");
	g.addBase64("_:this", Pointers.initialPublicKeySpec,
		    getKeyBytes(keyPair.getPublic()));
	Reference owner = Reference.create(g, pool);

	return new PointerSigner(owner, keyPair, pool);
    }

    public void writeKeys(OutputStream writeTo) throws IOException {
	ObjectOutputStream out = new ObjectOutputStream(writeTo);
	out.writeUTF(owner.getId().getURI());
	out.writeObject(keyPair);
    }

    private static SecureRandom rand = new SecureRandom();
    /** Create a new pointer with a random id.
     */
    public PointerId newPointer() {
	byte[] randomBytes = new byte[8];
	rand.nextBytes(randomBytes);
	String random = 
	    com.bitzi.util.Base32.encode(randomBytes).toLowerCase();
	PointerId root = new PointerId(owner.getId().getGraphId());
	return new PointerId(root, "/owns,id='" + random + "'");
    }

    /** Sign an existing pointer record for this pointer.
     */
    public void sign(Reference record) throws IOException, 
					      GeneralSecurityException {
	PointerId pointer = Pointers.getPointer(record);
	String property = Pointers.getProperty(record);
	long millis = record.getDate("_:this", Pointers.timestamp).getTime();

	PointerSignature sig = 
	    new PointerSignature(pointer, property, record.getId(),
				 millis, keyPair);
	BlockOutputStream bos = pool.getBlockOutputStream("application/prs.fallenstein.pointersignature");
	sig.write(bos);
	bos.close();
    }

    public Reference initialize(PointerId pointer, String property,
				String target)
	throws IOException, GeneralSecurityException {
	return addVersionRecord(pointer, property, target,
				Collections.EMPTY_SET);
    }

    /** Update pointer, obsoleting the record with the most current timestamp.
     *  If there is no record yet, obsolete nothing.
     *  <p>
     *  <strong>Avoid using this method if at all possible</strong>;
     *  it uses a heuristic for determining which blocks to obsolete,
     *  and this heuristic may easily fail in the <em>interesting</em> cases
     *  (i.e., when the obsoletion information is actually needed).
     *  So, if possible, use <code>initialize(...)</code> and
     *  <code>update(...)</code>.
     */
    public Reference updateNewest(PointerId pointer, String target) 
	throws IOException, GeneralSecurityException {

	Reference newest = null;
	try {
	    newest = idx.getMostCurrent(pointer, Pointers.VERSION_PROPERTIES);
	} catch(FileNotFoundException e) {
	    return initialize(pointer, Pointers.hasInstanceRecord, target);
	}
	return update(newest, target);
    }

    public Reference update(Reference lastRecord, String target) 
	throws IOException, GeneralSecurityException {
	String property = Pointers.getProperty(lastRecord);
	return update(lastRecord, property, target);
    }

    public Reference update(Reference lastRecord, 
			    String property, String target)
	throws IOException, GeneralSecurityException {
	PointerId pointerId = Pointers.getPointer(lastRecord);
	return addVersionRecord(pointerId, property, target, 
				Collections.singleton(lastRecord.getId()));
    }
    
    public Reference addVersionRecord(PointerId pointer, String property, 
				      String target, Set obsoletes) 
	throws IOException, GeneralSecurityException {

	return addVersionRecord(pointer, property, target, obsoletes,
				System.currentTimeMillis());
    }

    public Reference addVersionRecord(PointerId pointer, String property, 
				      String target, Set obsoletes,
				      long timestamp) 
	throws IOException, GeneralSecurityException {

	if(!Pointers.VERSION_PROPERTIES.contains(property))
	    throw new IllegalArgumentException("Not a pointer version "+
					       "property: "+property);

	Graph.Maker g = new Graph.Maker(); // for pointer record graph
	g.add(pointer.getURI(), property, "_:this");
	g.add(property, Pointers.RDFS_SUBPROPERTY_OF, 
	      Pointers.hasPointerRecord);
	g.add("_:this", Pointers.version, target);
	for(Iterator i=obsoletes.iterator(); i.hasNext();) {
	    ReferenceId id = (ReferenceId)i.next();
	    g.add(id.getURI(), Pointers.obsoletedBy, "_:this");
	}
	g.addDate("_:this", Pointers.timestamp, new Date(timestamp));
	g.add("_:this", "http://purl.oclc.org/NET/storm/vocab/ref-uri/resolutionMethod", "http://purl.oclc.org/NET/storm/vocab/ref-uri/ReferenceGraph");
	Reference record = Reference.create(g, pool);

	sign(record);
	return record;
    }


    public static byte[] getKeyBytes(PublicKey key) 	
	throws InvalidKeyException, InvalidKeySpecException {
	if(Pointers.keyFactory == null) 
	    throw new Error("PointerSigner needs DSA algorithm");
	key = (PublicKey)Pointers.keyFactory.translateKey(key);
	EncodedKeySpec keySpec = 
	    (EncodedKeySpec)Pointers.keyFactory.getKeySpec(key, X509EncodedKeySpec.class);
	return keySpec.getEncoded();
    }
}
