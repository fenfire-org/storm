/*
FileBlock.java
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
import java.io.*;
import java.util.*;

/** A <code>Block</code> which is stored as a file on the disk.
 */
public interface FileBlock extends Block {

    /** Get the file on the harddisk that contains the data
     *  for this block.
     *  This method will verify the data in the file; it may
     *  throw <code>BlockId.WrongIdException</code> if
     *  the data is corrupted.
     */
    File getFile() throws IOException, BlockId.WrongIdException;
}
