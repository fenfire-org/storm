/*
AbstractLocalPool.java
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
import java.io.*;
import java.util.*;

/** An abstract implementation of a non-network <code>StormPool</code>.
 *  This provides default implementations of the 
 *  <code>request()</code> methods.
 */
public abstract class AbstractLocalPool extends AbstractPool {

    public AbstractLocalPool(Set indexTypes) throws IOException {
	super(indexTypes);
    }

    public Block request(BlockId id) throws IOException {
	return get(id);
    }

    public Block request(BlockId id, BlockListener listener) 
                                                   throws IOException {
	return get(id);
    }
}
