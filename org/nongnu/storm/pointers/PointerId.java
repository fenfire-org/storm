/*
PointerId.java
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
package org.nongnu.storm.pointers;
import org.nongnu.storm.*;
import com.bitzi.util.*;
import java.util.*;
import java.io.*;
import java.security.*;
import java.security.spec.*;

/** The URN of a Storm 0.1 pointer, urn:x-storm:pointer-0.1.
 */
public final class PointerId {
    public static String PREFIX = "urn:x-storm:pointer-0.1:";
    public static int PREFIX_LEN = PREFIX.length();

    static KeyFactory keyFactory;
    static {
	try {
	    keyFactory = KeyFactory.getInstance("DSA");
	} catch(NoSuchAlgorithmException e) {
	    e.printStackTrace();
	    //throw new Error("PointerId needs DSA algorithm");
	}
    }
    private static SecureRandom random = new SecureRandom();



    private String uri;
    private byte[] bytes;
    private String randomPart;

    public PointerId(String uri) throws IllegalArgumentException {
	uri = uri.toLowerCase().intern();
	this.uri = uri;
	
	int colon = uri.indexOf(':', PREFIX_LEN+1);

	if(!uri.startsWith(PREFIX))
	    throw new IllegalArgumentException("Storm URN must start "+PREFIX+" [[ was "+uri+" ]]");
	if(colon < PREFIX_LEN)
	    throw new IllegalArgumentException("Illegal pointer URN (colon missing)");
	bytes = Base32.decode(uri.substring(PREFIX_LEN, colon));
	randomPart = uri.substring(colon+1);
    }

    public PointerId(PublicKey key, String randomPart) 
	throws InvalidKeyException, InvalidKeySpecException {

	MessageDigest d;
	try {
	    d = MessageDigest.getInstance("SHA-1");
	} catch(NoSuchAlgorithmException _) {
	    throw new Error("Need SHA-1 algorithm support in Storm");
	}

	d.update(getKeyBytes(key));

	this.bytes = d.digest();
	this.randomPart = randomPart;
	String uri = PREFIX + Base32.encode(bytes) + ":" + randomPart;
	this.uri = uri.toLowerCase().intern();
    }

    /** Create a new PointerId with a random randomPart. */
    public PointerId(PublicKey key)
	throws InvalidKeyException, InvalidKeySpecException {
	this(key, Base32.encode(randomBytes()));
    }

    public String getURI() { return uri; }
    public String toString() { return uri; }

    public String getRandomPart() { return randomPart; }

    public boolean equals(Object o) {
        if(!(o instanceof PointerId)) return false;
        return ((PointerId)o).uri == uri;
    }

    public int hashCode() { return uri.hashCode(); }

    public void verify(byte[] keyBytes) {
	MessageDigest d;
	try {
	    d = MessageDigest.getInstance("SHA-1");
	} catch(NoSuchAlgorithmException _) {
	    throw new Error("Need SHA-1 algorithm support in Storm");
	}

	d.update(keyBytes);

	if(!d.isEqual(d.digest(), this.bytes))
	    throw new IllegalArgumentException("Pointer doesn't match: "+this);
    }

    private static byte[] randomBytes() { 
	byte[] b = new byte[20];
	random.nextBytes(b);
	return b;
    }

    public static byte[] getKeyBytes(PublicKey key) 	
	throws InvalidKeyException, InvalidKeySpecException {
	if(keyFactory == null) throw new Error("PointerId needs DSA algorithm");
	key = (PublicKey)keyFactory.translateKey(key);
	EncodedKeySpec keySpec = 
	    (EncodedKeySpec)keyFactory.getKeySpec(key, X509EncodedKeySpec.class);
	return keySpec.getEncoded();
    }
}
