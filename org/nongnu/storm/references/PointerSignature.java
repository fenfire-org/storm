/*
PointerSignature.java
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
import org.nongnu.storm.util.CopyUtil;
import java.io.*;
import java.security.*;
import java.util.Date;

public final class PointerSignature {
    /** The pointer's id */
    private PointerId pointer;
    /** Property that relates the pointer to the signed pointer record */
    private String property;
    /** Id of the signed pointer record */
    private ReferenceId record;
    /** Timestamp of the signature */
    private long timestamp;
    /** The signature itself */
    private byte[] signature;

    /** The block id of the signature block */
    private BlockId blockId;

    /** The four bytes representing 'psig' in ASCII, plus four
     *  random bytes, plus a format version byte,
     *  as a unique 'magic cookie' header identifying pointer signatures.
     */
    private static final byte[] header =
        new byte[] { (byte)0x70, (byte)0x73, (byte)0x69, (byte)0x67, // 'psig'
		     (byte)0x3F, (byte)0x7E, (byte)0x9A, (byte)0xB3, // random
		     (byte)0x00  // format version number
	};

    public PointerId getPointer() { return pointer; }
    public String getProperty() { return property; }
    public ReferenceId getRecord() { return record; }
    public long getTimestamp() { return timestamp; }
    public byte[] getSignature() { return copy(signature); }

    public static byte[] getHeader() { return copy(header); }
    
    public PointerSignature(Block b) throws IOException,
					    GeneralSecurityException {
	this(b.getInputStream());
	this.blockId = b.getId(); // we trust the block object...
    }

    public PointerSignature(InputStream _in) throws IOException,
						    GeneralSecurityException {
	DataInputStream in = new DataInputStream(_in);

	// check header
	for(int i=0; i<header.length; i++) {
	    int r = in.read();
	    if(r < 0 || ((byte)r) != header[i])
		throw new IOException("Not a pointer signature");
	}
	
	// read pointer id
	byte[] sha1 = new byte[20], tigertree = new byte[24];
	in.read(sha1); in.read(tigertree);
	BlockId graph = new BlockId("text/plain", sha1, tigertree);

	int len = in.readUnsignedShort();
	byte[] pathBytes = new byte[len];
	in.read(pathBytes);
	String path = new String(pathBytes, "UTF-8");

	pointer = new PointerId(graph, path);

	// read property
	len = in.readUnsignedShort();
	byte[] propertyBytes = new byte[len];
	in.read(propertyBytes);
	property = new String(propertyBytes, "UTF-8");

	// read record id
	in.read(sha1); in.read(tigertree);
	record = new ReferenceId(new BlockId("text/plain", sha1, tigertree));

	timestamp = in.readLong();

	signature = CopyUtil.readBytes(in); // also closes the stream
    }

    public PointerSignature(PointerId pointer, String property,
			    ReferenceId record, long timestamp, 
			    byte[] signature,
			    PublicKey pubkey) throws IOException, 
						     GeneralSecurityException {
	this.pointer = pointer;
	this.property = property;
	this.record = record;
	this.timestamp = timestamp;
	this.signature = signature;

	//check(pubkey);
    }

    public PointerSignature(PointerId pointer, String property,
			    ReferenceId record, long timestamp, 
			    byte[] signature,
			    PublicKey pubkey,
			    BlockId blockId) throws IOException, 
						    GeneralSecurityException {
	this(pointer, property, record, timestamp, signature, pubkey);
	this.blockId = blockId;
    }

    /** Write the part of the signature block that is signed
     *  (i.e., everything except the signature itself) to an output stream.
     */
    private void writeSigned(OutputStream out) throws IOException {
	byte[] pathBytes = pointer.getPath().getBytes("UTF-8");
	if(pathBytes.length > 65535)
	    throw new IOException("Path too long (UTF-8 serialization may be at most 65535 bytes): "+property);

	byte[] propertyBytes = property.getBytes("UTF-8");
	if(propertyBytes.length > 65535)
	    throw new IOException("Property URI too long (UTF-8 serialization may be at most 65535 bytes): "+property);

	out.write(header);
	out.write(hash(pointer.getGraphId()));
	out.write((byte)((pathBytes.length >> 8) & 0xff));
	out.write((byte)((pathBytes.length >> 0) & 0xff));
	out.write(pathBytes);
	out.write((byte)((propertyBytes.length >> 8) & 0xff));
	out.write((byte)((propertyBytes.length >> 0) & 0xff));
	out.write(propertyBytes);
	out.write(hash(record.getGraphId()));
	out.write(timestampBytes(timestamp));
    }

    public void write(OutputStream out) throws IOException {
	writeSigned(out);
	out.write(signature);
    }

    public BlockId getBlockId() {
	if(blockId == null) {
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    try {
		write(bos);
	    } catch(IOException _) {
		_.printStackTrace();
		throw new Error("unexpected ioexception -- writing to bytearrayoutputstream: "+_);
	    }
	    blockId = BlockId.getIdForData("application/prs.fallenstein.pointersignature", bos.toByteArray());
	}
	return blockId;
    }

    public void check(PublicKey pubkey) throws IOException, 
					       GeneralSecurityException {
	Signature s = Signature.getInstance("SHA1withDSA");
	s.initVerify(pubkey);
	s.update(getSignedBytes());
	if(!s.verify(signature))
	    throw new IOException("Wrong signature");
    }

    public void checkTimestamp(Reference ref) throws IOException {
	Date date = ref.getDate("_:this", Pointers.timestamp);
	if(date.getTime() != timestamp)
	    throw new IOException("Timestamp in signature doesn't match "+
				  "timestamp in signed pointer record");
    }

    /** Return the signed data as a byte array. */
    private byte[] getSignedBytes() {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	try {
	    writeSigned(bos); bos.close();
	} catch(IOException e) {
	    // we don't expect any errors to be possible,
	    // we're writing to a ByteArrayOutputStream!
	    e.printStackTrace();
	    throw new Error("Unexpected IOException: "+e);
	}
	return bos.toByteArray();
    }

    public PointerSignature(PointerId pointer, String property, 
			    ReferenceId record, long timestamp,
			    KeyPair keyPair) throws GeneralSecurityException {
	this.pointer = pointer;
	this.property = property;
	this.record = record;
	this.timestamp = timestamp;

	Signature s = Signature.getInstance("SHA1withDSA");
	s.initSign(keyPair.getPrivate());
	s.update(getSignedBytes());
	
	this.signature = s.sign();

	try {
	    check(keyPair.getPublic());
	} catch(Exception e) {
	    throw new Error("Implementation error -- the generated signature cannot be verified: "+e);
	}
    }



    private static byte[] timestampBytes(long timestamp) {
	byte[] b = new byte[8];

	b[0] = (byte)((timestamp >> 56) & 0xff);
	b[1] = (byte)((timestamp >> 48) & 0xff);
	b[2] = (byte)((timestamp >> 40) & 0xff);
	b[3] = (byte)((timestamp >> 32) & 0xff);
	b[4] = (byte)((timestamp >> 24) & 0xff);
	b[5] = (byte)((timestamp >> 16) & 0xff);
	b[6] = (byte)((timestamp >> 8) & 0xff);
	b[7] = (byte)((timestamp >> 0) & 0xff);

	return b;
    }

    private static byte[] hash(BlockId id) {
	byte[] hash = new byte[44];
	System.arraycopy(id.getSha1(), 0, hash, 0, 20);
	System.arraycopy(id.getTigerTree(), 0, hash, 20, 24);
	return hash;
    }

    /** Return a copy of the byte array passed.
     *  Used in getXXX() accessor functions to private byte arrays,
     *  so that the callers can't muck with *our* arrays.
     */
    private static byte[] copy(byte[] arr) {
	byte[] result = new byte[arr.length];
	System.arraycopy(arr, 0, result, 0, arr.length);
	return result;
    }
}
