/*
SimpleSetCollector.java
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
package org.nongnu.storm.impl;
import org.nongnu.storm.*;
import java.util.*;

/** A trivial implementation of <code>SetCollector</code>
 *  atop of an existing <code>Set</code>.
 */
public class SimpleSetCollector implements SetCollector {

    /** The <code>Set</code> all lookups are proxied to.
     */
    protected final Set set;

    /** The time this object was created.
     *  Used by <code>getAge()</code>.
     */
    protected final long creationTime;

    /** What <code>isComplete()</code> should return for this object.
     */
    protected final boolean isComplete;

    /** Create a new <code>SimpleSetCollector</code> object.
     *  <code>isComplete()</code> on methods
     *  created with this constructor always returns <code>true</code>.
     *  @param set The <code>Set</code> requests are proxied to.
     *             For this object to meet its contract,
     *             <code>set</code> <em>must be immutable</em>.
     */
    public SimpleSetCollector(Set set) {
	this.set = set;
	this.creationTime = System.currentTimeMillis();
	this.isComplete = true;
    }

    /** Create a new <code>SimpleSetCollector</code> object.
     *  @param set The <code>Set</code> requests are proxied to.
     *             For this object to meet its contract,
     *             <code>set</code> <em>must be immutable</em>.
     *  @param isComplete
     *             What <code>isComplete()</code> should return 
     *             for this object.
     */
    public SimpleSetCollector(Set set, boolean isComplete) {
	this.set = set;
	this.creationTime = System.currentTimeMillis();
	this.isComplete = isComplete;
    }

    public long getAge() {
	return System.currentTimeMillis() - creationTime;
    }

    public boolean isReady() {
	return true;
    }

    public boolean isComplete() {
	return isComplete;
    }

    public Collector block() {
	return this;
    }

    public SetCollector blockSet() {
	return this;
    }

    public void addCollectionListener(CollectionListener l) {
	for(Iterator i=set.iterator(); i.hasNext();) {
	    if(!l.item(i.next())) break;
	}
	l.finish(false);
    }

    public int size() {
	return set.size();
    }

    public boolean isEmpty() {
	return set.isEmpty();
    }

    public boolean contains(Object o) {
	return set.contains(o);
    }

    public Iterator iterator() {
	return set.iterator();
    }

    public Iterator blockingIterator() {
	return set.iterator();
    }

    public Object[] toArray() {
	return set.toArray();
    }

    public Object[] toArray(Object[] a) {
	return set.toArray(a);
    }

    public boolean add(Object o) {
	throw new UnsupportedOperationException("Immutable collector");
    }

    public boolean remove(Object o) {
	throw new UnsupportedOperationException("Immutable collector");
    }

    public boolean containsAll(Collection c) {
	return set.containsAll(c);
    }

    public boolean addAll(Collection c) {
	throw new UnsupportedOperationException("Immutable collector");
    }

    public boolean removeAll(Collection c) {
	throw new UnsupportedOperationException("Immutable collector");
    }

    public boolean retainAll(Collection c) {
	throw new UnsupportedOperationException("Immutable collector");
    }

    public void clear() {
	throw new UnsupportedOperationException("Immutable collector");
    }
    
    public boolean equals(Object o) {
	return set.equals(o);
    }

    public int hashCode() {
	return set.hashCode();
    }
}
