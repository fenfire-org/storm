/*   
HTTPResponse.java
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

package org.nongnu.storm.http.client;
import org.nongnu.storm.http.*;
import org.nongnu.storm.util.*;
import java.io.*;
import java.util.*;

/** A HTTP response.  
 */
public class HTTPResponse extends HTTPReceivedMessage {

    public HTTPResponse(InputStream is) throws ParseException, IOException {
        super(is);
        int sp1 = startLine.indexOf(' ');
        if (sp1 == -1 || sp1 + 1 >= startLine.length())
            throw new ParseException("malformed status line: "+startLine);
        int sp2 = startLine.indexOf(' ', sp1 + 1);
        if (sp1 == -1) sp2 = startLine.length();
        httpVersion = startLine.substring(0, sp1);
        status = Integer.parseInt(startLine.substring(sp1 + 1, sp2));
        reason = startLine.substring(sp2 + 1);
    }

    public final int status;
    public final String httpVersion;
    public final String reason;

}
