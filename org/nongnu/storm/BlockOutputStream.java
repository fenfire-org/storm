/*
BlockOutputStream.java
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
import java.io.*;
import java.security.*;

/** An <code>OutputStream</code> for creating a new Storm block.
 *  The block is not actually created until <code>close()</code>
 *  is called on this stream. At that time, the block is added
 *  to the pool, and its data can be retrieved through the
 *  <code>getBlock()</code> and <code>getBlockId</code> methods.
 *  <p>
 *  While data is written to the stream, it is also sent
 *  to a <code>MessageDigest</code> object that is used
 *  to detemine the block's id.
 *  <p>
 *  This class works in a way similar to what
 *  <code>java.security.DigestOutputStream</code> does. However,
 *  we cannot subclass <code>DigestOutputStream</code> here,
 *  because it provides public methods that could be used to
 *  change the id generated (it is possible to write data
 *  to the stream and not send it to the digest, for example).
 */
public abstract class BlockOutputStream extends FilterOutputStream {
    boolean dbg = false;
    private static void p(String s) { System.out.println(s); }
    
    protected MessageDigest dig_sha1, dig_tt;
    protected String contentType;

    protected BlockOutputStream(OutputStream out, String contentType) {
        super(out);
	this.contentType = contentType;

	dig_sha1 = BlockId.makeSHA1Digest(); dig_sha1.reset();
	dig_tt = BlockId.makeTigerTreeDigest(); dig_tt.reset();
    }

    public void write(byte[] b, int off, int len) throws IOException {
	dig_sha1.update(b, off, len);
	dig_tt.update(b, off, len);
	if(dbg) p("<"+new String(b)+">");
	out.write(b, off, len);
    }

    public void write(int b) throws IOException {
	dig_sha1.update((byte)b);
	dig_tt.update((byte)b);
	if(dbg) p("<"+new String(new byte[] {(byte)b})+">");
	out.write(b);
    }

    /** Construct a <code>BlockId</code> from the current state
     *  of the internal message digest. This is usefully called
     *  from the <code>close()</code> method.
     */
    protected BlockId makeIdFromDigest() throws IOException {
	return new BlockId(contentType,
			   dig_sha1.digest(), dig_tt.digest());
    }

    /** Get the content type of the block created by this stream.
     */
    public String getContentType() {
	return contentType;
    }

    /** Close this stream and create the new block.
     *  The block is automatically added to the pool this stream
     *  came from. Additionally, the block and its id can be
     *  retrieved from this stream through the <code>getBlock()</code>
     *  and <code>getBlockId()</code> methods.
     */
    abstract public void close() throws IOException;

    /** Get the <code>Block</code> created by this stream.
     *  This may only be called after the stream has been closed.
     *  @throws IllegalStateException if the stream isn't closed yet.
     */
    abstract public Block getBlock() throws IOException, IllegalStateException;

    /** Get the id of the block created by this stream.
     *  This may only be called after the stream has been closed.
     *  @throws IllegalStateException if the stream isn't closed yet.
     */
    public BlockId getBlockId() throws IOException, IllegalStateException {
        return getBlock().getId();
    }
}
