/*
VersionFormat.java
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
import java.io.*;

/** A serialization format for Versions and Version.Diffs.
 *  Each format will be able to serialize a *subset* of all
 *  versions and diffs, for example all Java-serializable
 *  ones.
 *  @see org.nongnu.storm.util.SerializedVersionFormat
 */
public interface VersionFormat {
    void writeVersion(OutputStream out, Version v) throws IOException;
    void writeDiff(OutputStream out, Version.Diff d) throws IOException;

    Version readVersion(InputStream in) throws IOException;
    Version.Diff readDiff(InputStream in) throws IOException;
}
