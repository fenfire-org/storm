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
import java.security.*;

/** Benchmark the speed of checking DSA signatures of small
 *  byte arrays (20 bytes).
 */
public class BenchDSA {
    static final int N = 1000; // one thousand-- still takes a while

    public static void main(String[] argv) throws Exception {
	System.out.println("Setting up DSA benchmark...");

	KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
	KeyPair keyPair = gen.generateKeyPair();
	
	// sign 20 byte messages: one SHA-1 hash
	byte[] bytes = new byte[] { 42, 43, 44, 45, 26, 27, 28, 29, 80, 81,
				    -7, -8, -9, 50, 51, 52, 93, 94, -5, -6 };

	Signature s = Signature.getInstance("SHA1withDSA");
	s.initSign(keyPair.getPrivate());
	s.update(bytes);
	byte[] signature = s.sign();
	
	System.gc();

	System.out.println("Start benchmarking DSA...");
	long start = System.currentTimeMillis();

	for(int i=0; i<N; i++) {
	    s.initVerify(keyPair.getPublic());
	    s.update(bytes);
	    s.verify(signature);
	}

	long stop = System.currentTimeMillis();

	double each = (stop-start)*1.0/N;
	System.out.println("Milliseconds per DSA verification: "+each);
    }
}
