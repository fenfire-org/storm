/*   
HTTPRequest.java
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
import java.io.*;
import java.net.*;
import java.util.*;

/** A HTTP request. */
public class HTTPRequest extends HTTPSendableMessage {

    static public boolean dbg = false;
    static private void p(String s) { if(dbg) System.out.println(s); }

    /** Create a HTTP request writing to os
     * @param os The input stream where the request is to be sent
     * @throws ParseException Indicates a syntax error in the request
     * @throws IOException Indicates an IO problem
    */
    public HTTPRequest(OutputStream os, String method,
                       String uri, String httpvers, boolean withBody)
        throws IOException {
        super(os, true, method + " " + uri + " " + httpvers, !withBody);
        this.method = method;
        this.uri = uri;
        this.httpVersion = httpvers;
    }

    public final String method;
    public final String uri;
    public final String httpVersion;

    protected void commitMessageHeaders() throws IOException {
        commit("Accept");
        commit("Accept-Charset");
        commit("Accept-Encoding");
        commit("Accept-Language");
        commit("Authorization");
        commit("Expect");
        commit("From");
        commit("Host");
        commit("If-Match");
        commit("If-Modified-Since");
        commit("If-None-Match");
        commit("If-Range");
        commit("If-Unmodified-Since");
        commit("Max-Forwards");
        commit("Proxy-Authentication");
        commit("Range");
        commit("Referer");
        commit("TE");
        commit("User-Agent");
    }  
    
    
}
