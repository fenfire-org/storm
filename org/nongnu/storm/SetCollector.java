/*
SetCollector.java
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

/** A <code>Collector</code> which is also a <code>Set</code>.
 */
public interface SetCollector extends Set, Collector {

    /** Same as <code>block()</code>, but with a <code>SetCollector</code>
     *  return value. Since <code>block()</code> always returns
     *  this object, and this object is a <code>SetCollector</code>,
     *  it necessarily returns a <code>SetCollector</code> always.
     *  This method saves us an unnecessary class cast
     *  in some places, resulting in cleaner code.
     */
    SetCollector blockSet();
}
