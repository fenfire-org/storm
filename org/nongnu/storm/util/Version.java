/*
Version.java
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
package org.nongnu.storm.util;
import java.util.*;

/** A version of something that can be diffed.
 *  This is an abstract representation of a version. It provides a method
 *  for creating diffs. It can be used to implement saving in an abstract
 *  way (for example, saving versions to and loading versions from
 *  a Mediaserver using diffs).
 *  <p>
 *  Implementations must be immutable.
 *  @see SliceVersion.Diff
 */
public interface Version {
String rcsid = "$Id: Version.java,v 1.1 2003/04/03 14:38:14 benja Exp $";

    /** Return the diff from a previous to this version.
     *  <code>previous</code> must be a <code>Version</code>
     *  of the same kind as this one.
     */
    Diff getDiffFrom(Version previous);

    /** A diff between two <code>Version</code>s.
     *  It must hold that <code>w.getDiffFrom(v).applyTo(v).equals(w)
     *  for all <code>Version</code>s <code>v</code> and <code>w</code>
     *  of the same kind.
     */
    interface Diff {
        /** Return the inverse of this diff.
         */
        Diff inverse();

	/** Apply the diff to a <code>Version</code> and return
         *  the result. The <code>Version</code> must be of the same
	 *  kind as the <code>Version</code>s this diff was
	 *  generated from.
         */
	Version applyTo(Version old);

	/** Whether this is the empty diff.
	 *  It must hold that <code>v.equals(diff.applyTo(v))</code>
	 *  for all <code>Version</code>s <code>v</code> and
	 *  <code>Version.Diff</code>s <code>diff</code> of the
	 *  matching kind, iff <code>diff.isEmpty()</code>.
	 *  <p>
	 *  We need this method because we never want to save
	 *  empty diffs...
	 */
	boolean isEmpty();
    }
}

