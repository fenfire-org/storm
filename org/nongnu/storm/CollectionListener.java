/*
CollectionListener.java
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

public interface CollectionListener {

    /**
     *  @return Whether this listener wants to
     *          receive further events
     *          from this <code>Collector</code>.
     */
    boolean item(Object item);

    /** No more items will be received.
     *  @param timeout Whether the operation finished
     *         because of a timeout. If false,
     *         we know the operation has completed
     *         successfully.
     */
    void finish(boolean timeout);
}
