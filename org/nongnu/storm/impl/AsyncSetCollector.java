/*
AsyncSetCollector.java
 *    
 *    Copyright (c) 2003, Benja Fallenstein
 *    
 *    This file is part of Fenfire.
 *    
 *    Fenfire is free software; you can redistribute it and/or modify it under
 *    the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *    
 *    Fenfire is distributed in the hope that it will be useful, but WITHOUT
 *    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 *    Public License for more details.
 *    
 *    You should have received a copy of the GNU General
 *    Public License along with Fenfire; if not, write to the Free
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

/** XXX
 */
public class AsyncSetCollector implements SetCollector {
    static private void p(String s) { System.out.println(s); }

    /** The set of elements that have been read so far.
     *  Must always be synchronized...
     */
    protected final Set set = new HashSet();

    /** The time this object was created.
     *  Used by <code>getAge()</code>.
     */
    protected final long creationTime;

    /** Zero for working, >0 for complete, <0 for timeout.
     */
    protected int state;

    protected Set listeners = new HashSet();

    public AsyncSetCollector() {
	this.creationTime = System.currentTimeMillis();
	this.state = 0;
    }

    public void receive(Object o) {
	if(state != 0)
	    throw new IllegalStateException("Cannot receive more elements");
	
	synchronized(set) {
	    if(!set.add(o))
		return; // object already received
	}

	for(Iterator i=listeners.iterator(); i.hasNext();) {
	    CollectionListener l = (CollectionListener)i.next();
	    if(!l.item(o))
		i.remove();
	}
    }

    public synchronized void finish(boolean timeout) {
	state = timeout ? -1 : 1;

	for(Iterator i=listeners.iterator(); i.hasNext();) {
	    CollectionListener l = (CollectionListener)i.next();
	    l.finish(timeout);
	}
	notifyAll();
    }

    

    // Implementation of SetCollector

    public long getAge() {
	return System.currentTimeMillis() - creationTime;
    }

    public boolean isReady() {
	return state != 0;
    }

    public boolean isComplete() {
	if(state > 0) return true;
	else if(state < 0) return false;
	else throw new IllegalStateException("Collector not ready");
    }

    public Collector block() {
	return blockSet();
    }

    public synchronized SetCollector blockSet() {
	if(state != 0) return this;

	try {
	    wait();
	} catch(InterruptedException e) {
	}
	return this;
    }

    public void addCollectionListener(CollectionListener l) {
	if(state == 0) listeners.add(l);
	synchronized(set) {
	    for(Iterator i=set.iterator(); i.hasNext();) {
		if(!l.item(i.next())) return;
	    }
	}
	if(state != 0) l.finish(state < 0);
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
	final List queue;
	queue = Collections.synchronizedList(new ArrayList());
	addCollectionListener(new CollectionListener() {
		public boolean item(Object item) {
		    synchronized(queue) {
			queue.add(item);
			queue.notifyAll();
		    }
		    return true;
		}
		public void finish(boolean timeout) {
		    synchronized(queue) {
			queue.notifyAll();
		    }
		}
	    });
	return new Iterator() {
		public boolean hasNext() {
		    if(!queue.isEmpty())
			return true;
		    else if(isReady())
			return false;

		    synchronized(queue) {
			try {
			    queue.wait();
			} catch(InterruptedException e) {}
		    }
		    return !queue.isEmpty();
		}
		public Object next() {
		    if(!queue.isEmpty())
			return queue.remove(0);
		    else if(isReady())
			throw new NoSuchElementException();

		    synchronized(queue) {
			try {
			    queue.wait();
			} catch(InterruptedException e) {}
		    }
		    // throws NoSuchElementException 
		    // in the correct case
		    return queue.remove(0);
		}
		public void remove() {
		    throw new UnsupportedOperationException("modification");
		}
	    };
    }

    public Object[] toArray() {
	return set.toArray();
    }

    public Object[] toArray(Object[] a) {
	return set.toArray(a);
    }

    public boolean add(Object o) {
	throw new UnsupportedOperationException("Unmodifiable collector");
    }

    public boolean remove(Object o) {
	throw new UnsupportedOperationException("Unmodifiable collector");
    }

    public boolean containsAll(Collection c) {
	return set.containsAll(c);
    }

    public boolean addAll(Collection c) {
	throw new UnsupportedOperationException("Unmodifiable collector");
    }

    public boolean removeAll(Collection c) {
	throw new UnsupportedOperationException("Unmodifiable collector");
    }

    public boolean retainAll(Collection c) {
	throw new UnsupportedOperationException("Unmodifiable collector");
    }

    public void clear() {
	throw new UnsupportedOperationException("Unmodifiable collector");
    }
    
    public boolean equals(Object o) {
	return set.equals(o);
    }

    public int hashCode() {
	return set.hashCode();
    }

    public String toString() {
	return "<<"+super.toString()+" / "+state+">>";
    }
}
