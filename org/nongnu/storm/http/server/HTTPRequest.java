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

package org.nongnu.storm.http.server;
import org.nongnu.storm.http.*;
import java.io.*;
import java.net.*;
import java.util.*;

/** A HTTP request. */
public class HTTPRequest extends HTTPReceivedMessage {

    static public boolean dbg = false;
    static private void p(String s) { if(dbg) System.out.println(s); }

    /** Create a HTTP request reading from is
     * @param is The input stream where the request is to be read
     * @throws ParseException Indicates a syntax error in the request
     * @throws IOException Indicates an IO problem
    */
    public HTTPRequest(InputStream is) throws ParseException, IOException {
        super(is);
        parseStartLine();

        // See RFC 2616 section 14.10
        if (getHTTPVersion().equals("HTTP/1.0")
            || getHTTPVersion().equals("HTTP/0.9")) {
            for (Enumeration e = tokenizeCSL(getField("Connection"));
                 e.hasMoreElements();) {
                String fn = (String)e.nextElement();
                clearField(fn.toLowerCase());
            }
        }
    }

    /** Get the request method.
        @return The request method (eg. "GET")
    */
    public String getMethod() {
        return method;
    }

    /** Get the request URI.
        @return The request URI
     */
    public String getRequestURI() {
        return reqURI;
    }

    /** Get the request URI parsed as an URL.
        @return A parsed representation of the URI as an URL
        @throws MalformedURLException Indicates that the URI is not an
        URL or it is syntactically broken.
     */
    public HTTPURL getRequestURL() throws MalformedURLException {
        return new HTTPURL(getRequestURI());
    }

    /** Get the request HTTP version.
        @return The request HTTP version (eg. "HTTP/1.1")
    */
    public String getHTTPVersion() {
        return theHTTPVersion;
    }

    public String toString() {
	return "HTTPRequest("+startLine+")";
    }

    private String method;
    private String reqURI;
    private String theHTTPVersion;

    private static String getLine(InputStream is) throws IOException {
        return Util.getLine(is);
    }

    private void parseStartLine()
        throws ParseException, IOException {
        String line = startLine;
        int sp1 = line.indexOf(' ');
        int sp2 = line.indexOf(' ', sp1+1);
        if (sp1 == -1 || sp2 == -1) throw new ParseException("");
        method = line.substring(0, sp1);
        reqURI = line.substring(sp1 + 1, sp2);
        theHTTPVersion = line.substring(sp2 + 1);        
    }
}
