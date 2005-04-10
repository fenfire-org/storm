/*
OutputStream2BlockId.java
 *
 *    Copyright (c) 2002, Benja Fallenstein
 *                  2005, Matti J. Katila
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
 * Written by Matti J. Katila and Benja Fallenstein
 */
package org.nongnu.storm.util;
import org.nongnu.storm.*;
import java.io.*;
import java.security.*;
import com.bitzi.util.*;

/** 
 */
public class InputStream2BlockId { 


    static public BlockId slurp(String contentType, InputStream in) {
	return slurp(contentType, in, 4096, true);
    }
    static public BlockId slurp(String contentType, InputStream in,
			       int blockSize, boolean close) {
	try {
	    MessageDigest dig_sha1, dig_tt;
	    dig_sha1 = BlockId.makeSHA1Digest(); dig_sha1.reset();
	    dig_tt = new TreeTiger(); //BlockId.makeTigerTreeDigest(); 
	    dig_tt.reset();
	    
	    byte[] buf = new byte[blockSize];
	    while(true) {
		int r = in.read(buf);
		if(r == -1) break;
		dig_sha1.update(buf, 0, r);
		dig_tt.update(buf, 0, r);
	    }
	    if(close) {
		in.close();
	    }
	    return new BlockId(contentType,
			       dig_sha1.digest(), dig_tt.digest());
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new Error("NOU");
	}
    }

}
