/*   
IPUtil.java
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
import java.net.*;

/** Utility methods for dealing with IP addresses.
 */
public class IPUtil {

    private static final String[] localNets = new String[] {
	"0.",
	"10.",
	"127.",
	"169.254.",
	"172.16.", "172.17.", "172.18.", "172.19.", "172.20.", "172.21.", 
	"172.22.", "172.23.", "172.24.", "172.25.", "172.26.", "172.27.", 
	"172.28.", "172.29.", "172.30.", "172.31.",
	"192.0.2.",
	"192.168."
    };

    /** Return false if the given address is known not to be
     *  an address on the global Internet.
     *  Used to catch 127.0.0.1, 192.168.0.5 etc.
     */
    public static boolean isGlobal(InetAddress ip) {
	String str = ip.getHostAddress();

	for(int i=0; i<localNets.length; i++)
	    if(str.startsWith(localNets[i]))
		return false;

	return true;
    }

    public static boolean isGlobal(String str) throws UnknownHostException {
	return isGlobal(InetAddress.getByName(str));
    }


    public static void main(String argv[]) throws Exception {
	if(argv.length > 0 && argv[0].equals("-fix"))
	    System.setProperty("sun.net.spi.nameservice.provider.1", 
			       "dns,sun");

	InetAddress ip = InetAddress.getLocalHost();
	System.out.println("Host address: "+ip);
	if(isGlobal(ip))
	    System.out.println("This is a global address.");
	else
	    System.out.println("This is NOT a global address.");
    }
}
