/*
IndexedPool.java
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

/** A pool that, in addition to storing blocks, keeps some indexes for them.
 *  For many purposes, you not only need to retrieve blocks for block ids
 *  from a pool, but need some kind of "reverse lookup"-- for example,
 *  "get me all blocks that transclude this xanalogical span." This interface
 *  provides a general way to implement such indexes.
 *  <p>
 *  Indices aren't created on the fly: They are handed
 *  to the pool's constructor. This is not only for false simplicity ;-),
 *  but also for safety: whoever has a pointer to a StormPool
 *  is allowed to read, add and delete blocks, but not to
 *  mess up the pool by bringing it into an inconsistent state.
 *  An evil index implementation would be capable of that
 *  (even though we try to limit damage).
 *  <p>
 *  The internal model for every index is as follows:
 *  <ul>
 *  <li>Every block is deterministically mapped to a set of
 *      <code>Mapping</code>s; a <code>Mapping</code>
 *      has a key and a value, both of which are <code>String</code>s,
 *      and is associated with a <code>BlockId</code>.</li>
 *  <li><code>Mapping</code>s for blocks currently in the pool
 *      can be looked up using the <code>getMappings()</code> method,
 *      given the <code>Mapping</code>'s key and the type URI
 *      of the index type that created the mapping.</li>
 *  </ul>
 *  An <code>IndexType</code> object provides a method
 *  for retrieving the <code>Mapping</code>s associated
 *  with a block, as well as a method for creating
 *  a public interface representing an index (an index type-specific
 *  interface that provides the lookups an index implements;
 *  for example, the pointer index type provides
 *  a <code>getPointer(String pointerURI)</code> method).
 *  The block processor and the front-end are completely decoupled;
 *  their only way to communicate is through the
 *  <code>getMappings()</code> methods of <code>IndexType</code>
 *  and <code>IndexedPool</code>.
 *  <p>
 *  An <code>IndexType</code> has a unique URI, which is used
 *  to tell the different index types' sets of mappings apart.
 *  One <code>IndexType</code> URI should always be bound
 *  to one single interpretation of <code>Mapping</code> keys
 *  and values, to ensure interoperability on a network:
 *  all clients on a network should ideally agree what
 *  a given key means for a given index type, so that they
 *  will only produce relevant mappings for that key.
 *  (Of course, all <code>IndexType</code>s must be robust
 *  in the face of illegal data returned by 
 *  <code>IndexedPool.getMappings()</code>, since there can always be 
 *  malfunctioning or malicious nodes on the network.)
 *  <p>
 *  Implementation node: Keys for distributed hashtables
 *  can be simple concatenations of <code>IndexType</code> URIs
 *  with a space and <code>Mapping</code> keys.
 */
public interface IndexedPool extends StormPool {

    final class Mapping {
	public final BlockId block;
	public final String key, value;
	public Mapping(BlockId b, String k, String v) { 
	    block=b; key=k; value=v; 
	}
	public boolean equals(Object o) {
	    if(!(o instanceof Mapping)) return false;
	    Mapping m = (Mapping)o;
	    return block.equals(m.block) && key.equals(m.key) &&
		value.equals(m.value);
	}
	public int hashCode() {

	    return block.hashCode() ^ 3*key.hashCode() ^ -value.hashCode();
	}
    }

    interface IndexType {
	/** 
	 *  @return The set of <code>IndexedPool.Mapping</code>s
	 *          this index wants to extract from this block.
	 *          The <code>block</code> field of each
	 *          returned <code>Mapping</code> must contain the id of
	 *          the block passed to this method; if this
	 *          isn't satisfied, an exception will be thrown.
	 *          (XXX specify which and where...)
	 */
	Set getMappings(Block b) throws IOException;

	/** A factory method for the public interface
	 *  of this type of index.
	 *  This is the object that <code>IndexedPool.getIndex()</code>
	 *  returns, i.e. the object that clients of the pool
	 *  use to retrieve the data in the index. 
	 *  The actual interface isn't in any way constrained
	 *  by Storm, because different indexes will want to
	 *  provide very different interfaces; therefore, we simply
	 *  return an <code>Object</code> that has to be cast
	 *  to the desired interface first.
	 *  @param pool The <code>IndexedPool</code> this index
	 *              will belong to.
	 */
	Object createIndex(IndexedPool pool);

	/** Return a unique URI identifying this index type.
	 */
	String getIndexTypeURI();

	/** Return a human-readable name for this kind of index.
	 */
	String getHumanReadableName();
    }

    /** Collect <code>IndexedPool.Mappings</code>s
     *  with the given key, generated by the index type
     *  with the given type URI.
     *  The data may be requested from untrusted network sources,
     *  so code using this method must be robust
     *  in the face of corrupted data.
     */
    Collector getMappings(String typeURI, String key) throws IOException;

    /** Get the Index object for a given IndexType.
     *  Equivalent to <code>getIndices().get(type)</code>,
     *  except that it must throw NoSuchElementException
     *  when no index with that typeURI exists.
     */
    Object getIndex(String typeURI) throws NoSuchElementException;

    /** Return a mapping from index type URIs
     *  to <code>Index</code> objects. This map
     *  cannot be modified.
     */
    Map getIndices();

    /** Get a set of all index types this pool supports.
     *  The returned set cannot be modified.
     */
    Set getIndexTypes();

    Block get(org.nongnu.storm.references.PointerId id) throws IOException;
}
