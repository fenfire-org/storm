/*
Collector.java
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
import java.util.*;

/** A <code>Collection</code> representing the results
 *  of a lookup that may be continuing on another thread.
 *  This is an unmodifiable collection, but until
 *  the lookup has been finished, additional elements
 *  may appear in it. 
 *  <p>
 *  Note: All methods in this interface are guaranteed
 *  to be synchronized so that they can be safely used
 *  from a single thread without worrying about
 *  the other thread that possibly pours additional
 *  objects into this collection. However, if you want to
 *  use this object from more than one of your threads,
 *  you need to do your own synchronization (this permits
 *  implementations that do <em>not</em> read
 *  from the network not to be synchronized). Additionally,
 *  you need to synchronize if your code relies on the collection 
 *  not to change between two function calls, or if you 
 *  iterate over the collection. For example, you might write:
 *  <pre>
 *      Collector c = ...;
 *      synchronized(c) {
 *          Iterator i = c.iterator();
 *          for(; i.hasNext();) {
 *              ...
 *          }
 *      }
 *  </pre>
 *  Note that the <code>iterator()</code> call must be inside
 *  the synchronized block.
 */
public interface Collector extends Collection {
    
    /** Get the number of milliseconds since this request was started.
     *  This may be useful in deciding whether to re-lookup the data.
     */
    long getAge();

    /** Return whether the lookup is completed.
     *  If true, this <code>Collection</code> is immutable.
     */
    boolean isReady();

    /**
     *  Returns false if the lookup was terminated by a timeout
     *  or error, rather than completed regularly.
     *  @throws IllegalStateException if <code>isReady() == false</code>.
     */
    boolean isComplete() throws IllegalStateException;

    /** Block this thread until this <code>Collection</code>
     *  has been fully loaded.
     *  This method does nothing but waiting for the collection
     *  to become complete, i.e. until <code>isReady()</code> is true.
     *  It returns this object as a convenience, so that
     *  you can, for example, write:
     *  <pre>
     *  for(Iterator i=pool.getIds().block().iterator(); i.hasNext();) {
     *      BlockId id = (BlockId)i.next();
     *      // do something
     *  }
     *  </pre>
     *  In other words, given a method that returns a <code>Collector</code>,
     *  you can write <code>method().block()</code> to get the synchronous
     *  behavior of first completing a lookup and the returning
     *  the complete results.
     *  <p>
     *  Obviously, if the lookup has been completed already,
     *  this returns immediately.
     *  @return This object.
     */
    Collector block();

    /** Return an iterator that blocks when it has gone through all
     *  elements currently known, but the request has not completed.
     *  <p>
     *  XXX(benja): I'm not sure whether this should require
     *  synchronization or not...
     */
    Iterator blockingIterator();

    /** Send the elements in this collection to a callback interface
     *  as they arrive.
     *  All elements already in the collection are also sent
     *  to the listener-- this is really more like a reverse iterator,
     *  where the iterating code doesn't call the iterator,
     *  but is called through a callback interface (XXX explain better,
     *  possibly rename).
     */
    void addCollectionListener(CollectionListener listener);
}
