/*   
TestUtil.java
 *    
 *    Copyright (c) 2001, Ted Nelson and Tuomas Lukka
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
 * Written by Tuomas Lukka
 */
package org.nongnu.storm.util;
import java.io.*;

/** Utility for creating temporary files.
 */
public class TempFileUtil {
public static final String rcsid = "$Id: TempFileUtil.java,v 1.1 2003/04/08 08:04:46 benja Exp $";

    static public File tmpFile(File dir) {
	while(true) {
	    String name = "tmp"+System.currentTimeMillis()+"."+
				(int)(Math.random()*10000);
	    File t = new File(dir, name);
	    if(!t.exists())
		return t;
	}
    }

    static public void deltree(File f) {
	if(f.isDirectory()) {
	    String[] s = f.list();
	    for(int i=0; i<s.length; i++)
		deltree(new File(f, s[i]));
	}
	f.delete();
    }

}
