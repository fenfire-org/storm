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
import org.nongnu.storm.impl.DirPool;
import java.util.*;
import com.bitzi.util.*;
import java.io.*;
import java.security.*;

public class SetPointer {

    public static KeyPair readKeyPair(File keyFile) throws Exception {
	KeyPair keys;

	if(keyFile.exists()) {
	    ObjectInputStream in =
		new ObjectInputStream(new FileInputStream(keyFile));
	    keys = (KeyPair)in.readObject();
	    in.close();
	} else {
	    KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
	    keys = gen.generateKeyPair();
	    ObjectOutputStream out =
		new ObjectOutputStream(new FileOutputStream(keyFile));
	    out.writeObject(keys);
	    out.close();
	}

	return keys;
    }

    /** Usage is SetPointer keyfile [pool [pointer] target].
     *  If no pointer is given, a new one is generated.
     *  If the key file doesn't exist, it's created.
     */
    public static void main(String[] argv) throws Exception {
	System.out.println("Please wait, may need to initialize random number generator...");

	File keyFile = new File(argv[0]);
	KeyPair keys = readKeyPair(keyFile);

	if(argv.length == 1) {
	    PointerId pointer = new PointerId(keys.getPublic());
	    System.out.println("Pointer created:");
	    System.out.println(pointer);
	    System.exit(0);
	}

	Set indexTypes = Collections.singleton(PointerIndex.type);
	IndexedPool pool =
	    new DirPool(new File(argv[1]), indexTypes);

	PointerId pointer;
	BlockId target;
	
	if(argv.length == 3) {
	    pointer = new PointerId(keys.getPublic());
	    target = new BlockId(argv[2]);
	} else {
	    pointer = new PointerId(argv[2]);
	    target = new BlockId(argv[3]);
	}

	String name = System.getProperty("setpointer.name");
	if(argv.length > 4)
	    name = argv[4];

	PointerIndex idx = (PointerIndex)pool.getIndex(PointerIndex.uri);
	idx.set(pointer, target, keys, name);

	System.out.println("Set pointer");
	System.out.println(pointer);
	System.out.println("to target");
	System.out.println(target);
    }
}
