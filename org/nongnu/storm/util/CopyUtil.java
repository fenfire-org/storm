/*
CopyUtil.java
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
import java.io.*;

/** Utility methods for copying data from input to output streams.
 */
public class CopyUtil {
    static private int BLOCKSIZE = 4096;
    static private void p(String s) { System.out.println("CopyUtil:: "+s); }

    static public int copy(InputStream from, OutputStream to)
    						throws IOException {
        return copy(from, to, BLOCKSIZE, true);
    }

    static public int copy(InputStream from, OutputStream to, 
			   int blocksize)
    						throws IOException {
        return copy(from, to, blocksize, true);
    }

    static public int copy(InputStream from, OutputStream to,
			   boolean close)
    						throws IOException {
        return copy(from, to, BLOCKSIZE, close);
    }

    /** Copy data from an input to an output stream in blocks of a given size.
     *  If <code>close</code> is true, both streams are closed
     *  when the copy operation is complete.
     */
    static public int copy(InputStream from, OutputStream to, 
			   int blockSize, boolean close)
    						throws IOException {
	try {
	    byte[] buf = new byte[blockSize];
	    int bytesCopied = 0;
	    //p("start copying");
	    while(true) {
                //if (from.available() < 0) break;
		//p("read ");
		int r = from.read(buf);
		//p("check("+r+") ");
		if(r == -1) break;
		//p("write ");
		to.write(buf, 0, r);
		bytesCopied += r;
	    }
	    //p("... all read.");
	    return bytesCopied;
	} finally {
	    if(close) {
		from.close();
		to.close();
	    }
	}
    }

    /** Read data from an input stream into a byte array by copying it into
     *  a ByteArrayOutputStream.
     */
    static public byte[] readBytes(InputStream in) throws IOException {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	copy(in, out);
	return out.toByteArray();
    }

    /** Read data from an input stream into a String.
     *  Most useful for debug output. Default encoding is US-ASCII.
     */
    static public String readString(InputStream in, String encoding) throws IOException {
	return new String(readBytes(in), encoding);
    }

    static public String readString(InputStream in) throws IOException {
	return readString(in, "US-ASCII");
    }

    static public void writeString(String s, OutputStream out, 
				   String encoding) throws IOException {
	out.write(s.getBytes(encoding));
	out.close();
    }

    static public void writeString(String s, OutputStream out) 
	throws IOException {

	writeString(s, out, "UTF-8");
    }
}
