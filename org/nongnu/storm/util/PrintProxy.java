/*   
PrintProxy.java
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
import java.net.*;

/** A TCP proxy for debugging.
 *  Forwards bytes between a client and a server, and also prints out 
 *  what either the client or the server sends (a command line argument
 *  determines which).
 */
public class PrintProxy {
    public static void main(String[] argv) throws Exception {
	boolean printServer = false;
	if(argv[0].equals("-s"))
	    printServer = true;
	else if(argv[0].equals("-c"))
	    printServer = false;
	else {
	    System.out.println("First argument must be -s (print server) or -c (print client)");
	    System.exit(1);
	}
	
	int clientport = Integer.parseInt(argv[1]);
	String server = argv[2];
	int serverport = Integer.parseInt(argv[3]);

	ServerSocket _s = new ServerSocket(clientport);

	while(true) {
	    Socket clientsock = _s.accept();
	    Socket serversock = new Socket(server, serverport);

	    System.out.println("<NEW-CONNECTION>");

	    new Thread(new Copy(clientsock.getInputStream(),
				serversock.getOutputStream(),
				!printServer)).start();
	    new Thread(new Copy(serversock.getInputStream(),
				clientsock.getOutputStream(),
				printServer)).start();
	}
    }

    static class Copy implements Runnable {
	InputStream in;
	OutputStream out;
	boolean print;
	Copy(InputStream i, OutputStream o, boolean p) { 
	    in = i; out = o; print = p; 
	}

	public void run() {
	    try {
		while(true) {
		    int b = in.read();
		    if(b == -1) break;
		    if(print) System.out.write((char)b);
		    out.write((byte)b);
		    if(print) System.out.print("");
		}
	    } catch(IOException e) {
	    }

	    try {
		in.close();
	    } catch(IOException e) {}
	    
	    try {
		out.close();
	    } catch(IOException e) {}
	    
	    if(print) System.out.println("<END-TRANSMISSION>");
	}
    }
}
