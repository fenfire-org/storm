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
package org.nongnu.storm.bench;
import cryptix.jce.provider.*; // assumes Cryptix JCE is present (for Tiger)
import java.security.*;

/** Benchmark the speed of computing Tiger hashes of small
 *  byte arrays (24 bytes).
 */
public class BenchTiger {
    static final int N = 1000000; // one million

    public static void main(String[] argv) throws Exception {
	System.out.println("Start benchmarking Tiger...");

	java.security.Security.addProvider(new cryptix.jce.provider.
					   CryptixCrypto());
	MessageDigest dig = 
	    MessageDigest.getInstance("Tiger", "CryptixCrypto");

	byte[] bytes = new byte[24];
	long start = System.currentTimeMillis();

	for(int i=0; i<N; i++) {
	    dig.update(bytes);
	    dig.digest(bytes, 0, 24);
	}

	long stop = System.currentTimeMillis();

	double each = (stop-start)*1.0/N;
	System.out.println("Milliseconds per Tiger operation: "+each);
    }
}
