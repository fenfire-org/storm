/*
Block.java
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
import java.util.*;

/** An immutable byte sequence with a globally unique identifier.
 */
public interface Block {
    /** Get the id of this block.
     */
    BlockId getId();

    /** Get the pool this block is from.
     *  This method can be used to create a new block inside the same
     *  pool as an existing block: use
     *  <code>existingBlock.getPool().getBlockOutputStream(mimetype);</code>.
     */
    StormPool getPool();

    /** Get an input stream for reading the body of this block. 
     *  The data is <em>not</em> guaranteed to be verified against
     *  the id of this block before you start reading it, i.e. 
     *  what you read may be spoofed, incorrect data. This is for 
     *  efficiency: the data may be checked <em>while you read it</em>.
     *  If the data is spoofed, a <code>BlockId.WrongIdException</code>
     *  will be thrown when you call <code>close()</code>
     *  on the <code>InputStream</code> (that's a subclass of
     *  <code>IOException</code>). Therefore, <strong>do not forget
     *  to call <code>close()</code></strong> before you act on
     *  the data returned by this <code>InputStream</code>!
     *  <p>
     *  Generally there will be no network delays when reading
     *  from the input stream, i.e. if the data is retrieved
     *  from the network, this method will generally read from
     *  a local cache. However, there may be network delays
     *  if explicitly specified by a pool's documentation.
     *  <p>
     *  As usual, you have to be prepared for <code>IOException</code>s
     *  while reading from the stream.
     *  @return An input stream for reading the body of this block.
     *          Will not return <code>null</code>.
     *  @throws IOException if an error occurs while opening the
     *          input stream; for example, if the block was in
     *          a cache but has been pruned since.
     */
    InputStream getInputStream() throws IOException;
}
