/*   
HTTPServer.java
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

/** A HTTP server class.  An application wishing to use this HTTP
 * server should instantiate this class and then start the listener
 * thread by calling the start method.  It should also create its own
 * derivative HTTPConnection that handles the serving of content as
 * the application sees fit.
 * @see HTTPConnection
 */
public class HTTPServer extends Thread {

    /** Instantiate this HTTP server.
        @param connf A factory of application-specific HTTP connection
        objects
        @param port The TCP port to bind to
        @throws IOException Indicates a failure to bind to the port
    */
    public HTTPServer(HTTPConnection.Factory connf, int port) 
        throws IOException {
        this.connf = connf;
        lsock = new ServerSocket(port);
    }

    /** Instantiate this HTTP server.
        @param connf A factory of application-specific HTTP connection
        objects
        @param port The TCP port to bind to
        @param backlog XXX see java.net.ServerSocket
        @throws IOException Indicates a failure to bind to the port
    */
     public HTTPServer(HTTPConnection.Factory connf, int port, int backlog)
        throws IOException {
        this.connf = connf;
        lsock = new ServerSocket(port, backlog);
    }

    /** Instantiate this HTTP server.
        @param connf A factory of application-specific HTTP connection
        objects
        @param port The TCP port to bind to
        @param backlog XXX see java.net.ServerSocket
        @param bindAddr XXX see java.net.ServerSocket
        @throws IOException Indicates a failure to bind to the port
    */
    public HTTPServer(HTTPConnection.Factory connf, int port, int backlog,
                      InetAddress bindAddr) throws IOException {
        this.connf = connf;
        lsock = new ServerSocket(port, backlog, bindAddr);
    }
   
   
    public void run() {
        try {
            while (!isInterrupted()) {
                Socket csock = lsock.accept();
                HTTPConnection sess =
                    connf.newConnection(csock);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private ServerSocket lsock;
    private HTTPConnection.Factory connf;

    public static void main(String[] argv) {
        try {
            HTTPServer hs = new HTTPServer(new HTTPConnection.Factory(), 5555);
            hs.run();
        } catch (IOException e) { e.printStackTrace(); }
    }

}
