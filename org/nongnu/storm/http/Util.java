/*   
Util.java
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
 * Written by Antti-Juhani Kaijanaho
 */

package org.nongnu.storm.http;
import java.io.*;
import java.net.*;
import java.util.*;

/** An aggregation of utility methods. */
public class Util {

    public static String getLine(InputStream is) throws IOException {
        StringBuffer sb = new StringBuffer();
        do {
            int b = is.read();
            if (b == -1) {
                if (sb.length() == 0) throw new EOFException();
                break;
            }
            sb.append((char)b);
        } while (!(sb.length() >= 1
                   && sb.charAt(sb.length()-1) == 10));
        if (sb.length() >= 2 && sb.charAt(sb.length()-2) == 13) {
            sb.setLength(sb.length() - 2);
        } else {
            sb.setLength(sb.length() - 1);
        }
        return new String(sb);
    }

}
