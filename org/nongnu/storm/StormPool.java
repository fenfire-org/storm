/*
StormPool.java
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
import org.nongnu.storm.references.ReferenceId;

import java.io.*;
import java.util.*;

/** A Storm pool of blocks.
 */
public interface StormPool {

    // BASIC BLOCK HANDLING

    /** Get a block from this pool.
     *  This does not necessarily load the data (and thus doesn't
     *  necessarily detect spoofing). Depending on the implementation
     *  of <code>Block</code>, it is possible that spoofing isn't
     *  detected until an InputStream retrieved from the Block
     *  is closed.
     *  <p>
     *  Note: If the block has to be requested from the network,
     *  this method blocks until it has been loaded!
     *  For asynchronous loading, see <code>request()</code>.
     *  <p>
     *  Calling <code>getPool()</code> on the returned block may return
     *  a different pool than this: If the block is retrieved from
     *  a sub-pool of this pool, <code>getPool()</code> will return
     *  that sub-pool. This is intentional.
     *  @throws FileNotFoundException if the block is not found
     *                                in the pool.
     */
    Block get(BlockId id) 
	throws FileNotFoundException, IOException;

    /** XXX document!!!
     */
    Block get(ReferenceId id)
	throws FileNotFoundException, IOException;

    /** Load a block from the network, if it is not available locally.
     *  If <code>listener</code> is given, it is informed when
     *  the block has either been loaded, or the pool
     *  has given up on loading it.
     *  <p>
     *  If the block is already available locally, it will be returned.
     *  The <code>BlockListener</code> will not be called at all.
     *  If the block must be loaded, <code>null</code> is returned
     *  (no exception is thrown). If the block is known not to be
     *  available, a <code>FileNotFoundException</code> is thrown.
     *  <p>
     *  After the block has been loaded, it will be kept locally
     *  for some time, but no guarantees are made as to how long.
     *  It is reasonable to <code>request()</code> a block,
     *  doing nothing if <code>null</code> is returned,
     *  and when notification comes in that the block
     *  has been loaded, repeat the whole procedure.
     *  If the block has already been uncached at the time
     *  of the second <code>request()</code> and we get
     *  another chance.
     *  <p>
     *  We could make it so that the <code>BlockListener</code>
     *  is always called, even when we can return the block
     *  immediately. However it is easier to simulate that
     *  behavior on top of the current one than it would be
     *  to simulate the current behavior on top of that one,
     *  and both are expected to be needed.
     *  @throws FileNotFoundException if the block is not found
     *                                in the pool.
     */
    Block request(BlockId id, BlockListener listener) throws IOException;

    /** Load a block from the network, if it is not available locally.
     *  Equivalent to <code>request(id, null)</code>.
     *  @throws FileNotFoundException if the block is not found
     *                                in the pool.
     */
    Block request(BlockId id) throws IOException;

    /** Add a block to this pool.
     *  The data in the block is checked by this class to assure
     *  that the block's id really matches the data in the block.
     *  If this weren't the case, spoofing could occur by simply
     *  creating a non-checking implementation of <code>Block</code>.
     */
    void add(Block b) throws IOException;

    /** Remove a block from this pool.
     *  There are two reasons this is not a method of <code>Block</code>:
     *  <ul>
     *  <li>All actions that actually modify a pool are a responsibility
     *      of this class. This allows <code>Block</code> implementations
     *      not to depend on specific <code>StormPool</code> implementations.
     *  </li>
     *  <li>This method will remove a block from all sub-pools. As a block
     *      may be in more than one sub-pool, and <code>delete()</code>
     *      method in the <code>Block</code> would only delete the block
     *      from the single pool it was loaded from.
     *  </li>
     *  </ul>
     *  @throws FileNotFoundException if the block is not found
     *                                in the pool.
     */
    void delete(Block b) throws IOException;


    // GETTING THE SET OF IDS

    /** Get the set of all <em>known</em> ids in this pool.
     *  There is no guarantee that all ids which can be loaded
     *  from this block are in this set. For example, a pool
     *  that represents a p2p network may not know the set of ids
     *  it is able to load.
     *  <p>
     *  This returns a <code>SetCollector</code> so that
     *  implementations that need to do a network lookup
     *  can spawn a thread to do that. If you want to
     *  block your thread until all ids have been read
     *  from the network, you can write:
     *  <pre>
     *      Set ids = pool.getIds().blockSet();
     *  </pre>
     *  <p>
     *  Never returns <code>null</code>.
     */
    SetCollector getIds() throws IOException;


    // CREATING NEW BLOCKS

    /** Get an <code>OutputStream</code> for adding a new block to this pool.
     *  @param contentType The content type of the new block.
     *         Must already be URI-escaped: All non-URN characters
     *         (except the first slash, as in image/jpeg) and significant
     *         upper-case characters must be percent-escaped.
     *         There must be no spaces in the contentType string.
     *  @see BlockOutputStream
     */
    BlockOutputStream getBlockOutputStream(String contentType) throws IOException;
}
