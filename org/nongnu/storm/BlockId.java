/*
BlockId.java
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
package org.nongnu.storm;
import com.bitzi.util.*;
import java.io.*;
import java.security.*;

/** The URI of a Storm block.
 */
public final class BlockId {
    public static String PREFIX = "vnd-storm-hash:";
    public static String OLD_PREFIX = "urn:x-storm:1.0:";
    public static int PREFIX_LEN = PREFIX.length();

    public static class WrongIdException extends IOException {
	public WrongIdException(String s) { super(s); }
    }

    private byte[] sha1;
    private byte[] tigertree;
    private String contentType;

    private String uri;

    public BlockId(String uri) throws IllegalArgumentException {
	uri = uri.toLowerCase();
	if(uri.startsWith(OLD_PREFIX))
	    uri = PREFIX + uri.substring(OLD_PREFIX.length());
	uri = uri.intern();
	this.uri = uri;
	
	int dot = uri.lastIndexOf('.');
	int comma = uri.lastIndexOf(',');

	if(!uri.startsWith(PREFIX))
	    throw new IllegalArgumentException("Block URI must start "+PREFIX+ " -- was "+uri);
	if(dot < 0 || comma < 0)
            throw new IllegalArgumentException("URN must contain . and ,");
	if(dot - comma != 32 + 1)
	    throw new IllegalArgumentException("1st hash must be 32 chars");
	if(uri.length() - dot != 39 + 1) 
	    throw new IllegalArgumentException("2nd hash must be 39 chars");
	    
	contentType = uri.substring(PREFIX_LEN, comma);
	checkContentType(contentType);

	sha1 = Base32.decode(uri.substring(comma+1, dot));
	tigertree = Base32.decode(uri.substring(dot+1));
    }

    public BlockId(String contentType, 
		   byte[] sha1, byte[] tigertree) {
	checkContentType(contentType);
	
	this.sha1 = sha1;
	this.tigertree = tigertree;
	this.contentType = contentType;

	String uri = PREFIX + contentType + "," +
	    Base32.encode(sha1) + "." + Base32.encode(tigertree);

	this.uri = uri.toLowerCase().intern();
    }

    public byte[] getSha1() { return sha1; }
    public byte[] getTigerTree() { return tigertree; }
    public String getContentType() { return contentType; }

    /** Get the hash part with the dot, 
     *  i.e. everything after the comma.
     */
    public String getBitprint() {
	return uri.substring(uri.lastIndexOf(',')+1);
    }

    public String getURI() { return uri; }
    public String toString() { return uri; }

    /** Check that the given data bytes match this id.
     */
    public void check(byte[] data) throws WrongIdException {
	if(!equals(getIdForData(contentType, data)))
	    throw new WrongIdException("ID doesn't match");
    }

    /** Get an InputStream that checks whether the data read from
     *  <code>in</code> matches this id. The returned input stream
     *  is a filter input stream that returns the same data as the
     *  underlying input stream, but at the same time puts the data
     *  into a <code>MessageDigest</code> object to generate its
     *  hash. When <code>close()</code> is called, the stream checks
     *  whether the hash matches this id, and throws a 
     *  <code>WrongIdException</code> if it doesn't.
     *  <p>
     *  <strong>Do not forget to call <code>close()</code>!</strong>
     */
    public InputStream getCheckedInputStream(InputStream in)
                                                throws IOException {
        final MessageDigest dig_tt = makeTigerTreeDigest();
        final MessageDigest dig_sha1 = makeSHA1Digest();

	in = new DigestInputStream(in, dig_tt);
	in = new DigestInputStream(in, dig_sha1);

	return new FilterInputStream(in) {
		public void close() throws IOException {
		    /** Read all that hasn't been read yet.
		     */
		    while(read() >= 0);

		    super.close();
		    byte[] dig; 

		    dig = dig_sha1.digest();
		    for(int i=0; i<dig.length; i++)
			if(dig[i] != sha1[i])
			    throw new WrongIdException("SHA-1 hash doesn't match");

		    dig = dig_tt.digest();
		    for(int i=0; i<dig.length; i++)
			if(dig[i] != tigertree[i])
			    throw new WrongIdException("TigerTree hash doesn't match");
		}
	    };
    }

    public boolean equals(Object o) {
        if(!(o instanceof BlockId)) return false;
        return ((BlockId)o).uri == uri;
    }

    public int hashCode() { return uri.hashCode(); }

    /** Get the id for a given array of bytes.
     *  The byte array must contain the bytes in a block.
     */
    public static BlockId getIdForData(String contentType, 
				       byte[] bytes) {
        MessageDigest dig_tt = makeTigerTreeDigest();
        MessageDigest dig_sha1 = makeSHA1Digest();

	dig_tt.update(bytes);
	dig_sha1.update(bytes);

	return new BlockId(contentType, dig_sha1.digest(),
			   dig_tt.digest());
    }


    private void checkContentType(String s) {
	if(s.equals("")) return;
	
	s = s.toLowerCase();
	int slash = s.indexOf('/'); 
	if(slash == -1 ||               // There must be a slash,
	   s.lastIndexOf('/') != slash) // and only one slash
	    throw new IllegalArgumentException("Content type must have "+
					       "exactly 1 slash: "+s);
	if(slash == 0 || slash == s.length()-1)
	    throw new IllegalArgumentException("Content type must have two "+
					       "non-empty parts: "+s);
	if(s.substring(slash+1).startsWith("x-") ||
	   s.substring(slash+1).startsWith("x."))
	    throw new IllegalArgumentException("x- and x. content types "
					       + "not allowed in "
					       + "Storm URNs: "+s);

	if(s.indexOf(' ') >= 0)
	    throw new IllegalArgumentException("URIs cannot contain spaces; space was in: '"+s+"'");
    }

    /** Create a new SHA-1 message digest; throw an error
     *  if this algorithm isn't available. 
     */
    public static MessageDigest makeSHA1Digest() {
	try {
	    return MessageDigest.getInstance("SHA");
	} catch(NoSuchAlgorithmException e) {
	    throw new Error("Fatal error: The SHA-1 algorithm "+
			    "is not supported by this version "+
			    "of the Java libraries. "+
			    "Storm cannot operate without "+
			    "an SHA-1 implementation.");
	}
    }

    /** Create a new TigerTree message digest; throw an error
     *  if this algorithm isn't available. 
     */
    static MessageDigest makeTigerTreeDigest() {
	try {
	    return new TreeTiger();
	} catch(NoSuchAlgorithmException e) {
	    throw new Error("Fatal error: There was a problem " +
			    "initializing the TigerTree " +
			    "message digest (maybe the Cryptix " +
			    "JCE is missing). " +
			    "Storm cannot operate without "+
			    "a TigerTree implementation.");
	}
    }
}
