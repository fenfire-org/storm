/*
URIUtil.java
 *    
 *    Copyright (c) 2003, Benja Fallenstein
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
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/** URI-syntax related utility methods.
 */
public class URIUtil {
    /** Escape a UTF-8 string into URI syntax */
    public static String escapeUTF8(String s) {
	StringBuffer buf = new StringBuffer();
	
	byte[] bytes;
	try {
	    bytes = s.getBytes("UTF-8");
	    //System.out.println(new String(bytes, "UTF-8"));
	} catch(UnsupportedEncodingException e) {
	    throw new Error("JVM does not support UTF-8 encoding (!)");
	}

	/*
	for(int i=0; i<bytes.length; i++)
	    System.out.print(bytes[i]+" ");
	System.out.println();
	*/
	
	for(int i=0; i<bytes.length; i++) {
	    char c;
	    if(bytes[i] >= 0) c = (char)bytes[i];
	    else c = (char)(128 + (char)(bytes[i] & 127));

	    if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
	       (c >= '0' && c <= '9') || c=='(' || c==')' ||
	       c=='+' || c==',' || c=='-' || c=='.' || c==':' ||
	       c=='=' || c=='@' || c==';' || c=='$' || c=='_' ||
	       c=='!' || c=='*' || c=='\'')
		buf.append((char)c);
	    else {
		buf.append("%");
		char b1 = (char)(c >> 4), b2 = (char)(c % 16);
		//System.out.println((int)c+" "+(int)b1+" "+(int)b2);
		if(b1 < 10) buf.append((char)('0'+b1));
		else buf.append((char)('a'+b1-10));
		if(b2 < 10) buf.append((char)('0'+b2));
		else buf.append((char)('a'+b2-10));
	    }
	}
	return buf.toString();
    }

    /** Unescape a UTF-8 string from URI syntax */
    public static String unescapeUTF8(String s) {
	ByteArrayOutputStream buf = new ByteArrayOutputStream();

	for(int k=0; k<s.length(); k++) {
	    char c = s.charAt(k);
	    if(c != '%') {
		buf.write((byte)c);
		//System.out.println("w: "+c);
	    } else {
		char d1 = s.charAt(k+1),
		     d2 = s.charAt(k+2);
		k += 2;
		
		int v = (fromHex(d1)*16) + fromHex(d2);
		//System.out.println("w: "+v);
		buf.write((byte)v);
	    }
	}
	
	byte[] arr = buf.toByteArray();

	/*
	for(int i=0; i<arr.length; i++)
	    System.out.print(arr[i]+" ");
	System.out.println();
	*/
	
	try {
	    return new String(arr, "UTF-8");
	} catch(UnsupportedEncodingException e) {
	    throw new Error("JVM does not support UTF-8 encoding (!)");
	}
    }

    public static byte fromHex(char hex) {
	if('0' <= hex && hex <= '9')
	    return (byte)(hex - '0');
	    
	if('a' <= hex && hex <= 'h')
	    return (byte)(hex - 'a' + 10);

	if('A' <= hex && hex <= 'H')
	    return (byte)(hex - 'A' + 10);

	throw new NumberFormatException("Not a hex digit: "+hex);
    }
}
