/*
MockP2PMap.java
 *    
 *    Copyright (c) 2003, Benja Fallenstein
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
package org.nongnu.storm.impl.p2p;
import org.nongnu.storm.*;
import org.nongnu.storm.impl.*;
import org.nongnu.storm.util.ByteArrayKey;
import java.io.*;
import java.util.*;

public class MockP2PMap implements P2PMap {
    protected Map map = new HashMap();

    protected Collection getSet(String key) {
	Collection s = (Collection)map.get(key);
	if(s == null) {
	    s = new ArrayList();
	    map.put(key, s);
	}
	return s;
    }

    public Collector get(String key) {
	return new SimpleSetCollector(new HashSet(getSet(key)));
    }

    public int put(String key, String value, int timeout) {
	getSet(key).add(value);
	// We don't care about timeouts, we never remove items
	return timeout;
    }

    public void remove(String key, String value) {
	getSet(key).remove(value);
    }
}
