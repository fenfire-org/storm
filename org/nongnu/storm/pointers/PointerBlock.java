/*
PointerBlock.java
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

public final class PointerBlock {
    public static final String COOKIE = 
	"File format: <http://fenfire.org/2003/05/pointer-block-0.1>";

    private BlockId blockId;
    private PointerId pointer;
    private long timestamp;
    private BlockId target;
    private String name;

    public BlockId getBlockId() { return blockId; }
    public PointerId getPointer() { return pointer; }
    public long getTimestamp() { return timestamp; }
    public BlockId getTarget() { return target; }
    public String getName() { return name; }

    public PointerBlock(Block block) 
	throws IOException, GeneralSecurityException {
	if(!block.getId().getContentType().equals("text/plain"))
	    throw new IllegalArgumentException("Not a pointer block");

	blockId = block.getId();

	InputStream is = block.getInputStream();
	Reader isr = new InputStreamReader(is, "US-ASCII");
	BufferedReader r = new BufferedReader(isr);


	String cookie = r.readLine();
	if(!cookie.equals(COOKIE))
	    throw new IllegalArgumentException("Not a pointer block");

	byte[] signature = Base32.decode(r.readLine());

	pointer = new PointerId(r.readLine());

	byte[] keyBytes = Base32.decode(r.readLine());
	X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
	PublicKey key = PointerId.keyFactory.generatePublic(keySpec);

	timestamp = Long.parseLong(r.readLine());
	target = new BlockId(r.readLine());

	name = r.readLine(); // may be null -> no name given

	if(r.readLine() != null) 
	    throw new IOException("Pointer block too long");

	r.close();

	// Next, verify that the pubkey matches the pointer id.

	pointer.verify(keyBytes);

	// Now, verify signature.
	// Everything *after* the signature itself is signed.
	is = block.getInputStream();

	int linenr = 0;
	while(linenr < 2) {
	    int b = is.read();
	    if(b == '\n') linenr++;
	    else if(b < 0)
		throw new IOException("Unexpected EOF");
	}

	Signature s = Signature.getInstance("SHA1withDSA");
	s.initVerify(key);

	int b;
	while((b = is.read()) >= 0)
	    s.update((byte)b);
	
	is.close();
	if(!s.verify(signature))
	    throw new IOException("Wrong signature in block: "+block.getId());
    }

    public boolean equals(Object o) {
	if(!(o instanceof PointerBlock)) return false;
	return ((PointerBlock)o).blockId.equals(blockId);
    }
}
