/*
P2PMap.java
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
import java.io.IOException;

/** A distributed search network.
 *  Can be a DHT; can also be a Gnutella-like network
 *  if desired. Abstracts over time-to-live in DHTs:
 *  <p>
 *  Keys and values may not contain any newline
 *  characters (CR, LF).
 *  <p>
 *  Note (2004-01-24): May s/String/byte[]/ in the near future.
 */
public interface P2PMap {
    Collector get(String key) throws IOException;

    /** Return the actual expected timeout time.
     *  I.e., if this says timeout = 100 days, but the map
     *  can only keep the item published for 30 min, this
     *  will return 30 min (it's in millis, so 30*60*1000).
     *  @param timeout The requested timeout.
     */
    int put(String key, String value, int timeout) throws IOException;

    /** Stop keeping the given key/value pair published.
     *  This doesn't necessarily remove the pair from the map,
     *  it just tells the map that it doesn't any longer
     *  need to keep it published.
     */
    void remove(String key, String value) throws IOException;
}

