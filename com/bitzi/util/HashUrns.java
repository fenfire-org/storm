/* (PD) 2001 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: HashUrns.java,v 1.1 2003/04/03 19:29:27 benja Exp $
 */
package com.bitzi.util;

import java.security.*;
import java.util.*;
import java.io.*;
import java.math.*;
import cryptix.jce.provider.*; // assumes Cryptix JCE is present (for Tiger)

/**
 *
 */
public class HashUrns {

    public static void  main(String[] args)
    throws IOException, NoSuchAlgorithmException
    {
        if(args.length<1) {
            System.out.println("You must supply a filename.");
            return;
        }
        MessageDigest tt = new TreeTiger();
        MessageDigest sha1 = MessageDigest.getInstance("SHA");
        FileInputStream fis;

        for(int i=0;i<args.length;i++) {
            fis = new FileInputStream(args[i]);
            int read;
            byte[] in = new byte[1024];
            while((read = fis.read(in)) > -1) {
                tt.update(in,0,read);
                sha1.update(in,0,read);
            }
            fis.close();
            byte[] ttdigest = tt.digest();
            byte[] sha1digest = sha1.digest();

            System.out.println("urn:sha1:"+(new Base32()).encode(sha1digest));
            System.out.println("urn:tree:tiger/:"+(new Base32()).encode(ttdigest));
            tt.reset();
        }
    }
}
