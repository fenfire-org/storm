/* (PD) 2001 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: TreeTiger.java,v 1.1 2003/04/03 19:29:27 benja Exp $
 */
package com.bitzi.util;

import java.security.*;
import java.util.*;
import java.io.*;
import java.math.*;
import cryptix.jce.provider.*; // assumes Cryptix JCE is present (for Tiger)

/**
 * Implementation of THEX tree hash algorithm, with Tiger
 * as the internal algorithm (using the approach as revised
 * in December 2002, to add unique prefixes to leaf and node
 * operations)
 *
 * For simplicity, calculates one entire generation before
 * starting on the next. A more space-efficient approach
 * would use a stack, and calculate each node as soon as
 * its children ara available.
 */
public class TreeTiger extends MessageDigest {
    private static final int BLOCKSIZE = 1024;
    private static final int HASHSIZE = 24;

    /** 1024 byte buffer */
    private final byte[] buffer;

    /** Buffer offset */
    private int bufferOffset;

    /** Number of bytes hashed until now. */
    private long byteCount;

    /** Internal Tiger MD instance */
    private MessageDigest tiger;

    /** Interim tree node hash values */
    private Vector nodes;

    /**
     * Constructor
     */
    public TreeTiger()
    throws NoSuchAlgorithmException
    {
        super("TreeTiger");
        buffer = new byte[BLOCKSIZE];
        bufferOffset = 0;
        byteCount = 0;
        try
        {
            java.security.Security.addProvider(new cryptix.jce.provider.
                                                   CryptixCrypto());
            tiger = MessageDigest.getInstance("Tiger", "CryptixCrypto");
        }
        catch(NoSuchProviderException e)
        {
            System.out.println("Provider Cryptix not found");
            System.exit(0);
        }
        nodes = new Vector();
    }

    protected int engineGetDigestLength()
    {
        return HASHSIZE;
    }

    protected void engineUpdate(byte in)
    {
        byteCount += 1;
        buffer[bufferOffset++] = in;
        if( bufferOffset==BLOCKSIZE )
        {
            blockUpdate();
            bufferOffset = 0;
        }
    }


    protected void engineUpdate(byte[] in, int offset, int length)
    {
        byteCount += length;

        int remaining;
        while( length >= (remaining = BLOCKSIZE - bufferOffset) )
        {
            System.arraycopy(in, offset, buffer, bufferOffset, remaining);
            bufferOffset += remaining;
            blockUpdate();
            length -= remaining;
            offset += remaining;
            bufferOffset = 0;
        }

        System.arraycopy(in, offset, buffer, bufferOffset, length);
        bufferOffset += length;
    }


    protected byte[] engineDigest()
    {
        byte[] hash = new byte[HASHSIZE];
        try {
            engineDigest(hash, 0, HASHSIZE);
        } catch (DigestException e) {
            return null;
        }
        return hash;
    }


    protected int engineDigest(byte[] buf, int offset, int len)
    throws DigestException
    {
        if(len<HASHSIZE)
            throw new DigestException();

        // hash any remaining fragments
        blockUpdate();

        // composite neighboring nodes together up to top value
        while (nodes.size() > 1) {
            Vector newNodes = new Vector();
            Enumeration iter = nodes.elements();
            while (iter.hasMoreElements()) {
                byte[] left = (byte[])iter.nextElement();
                if(iter.hasMoreElements()) {
                    byte[] right = (byte[])iter.nextElement();
                    tiger.reset();
                    tiger.update((byte)1); // node prefix
                    tiger.update(left);
                    tiger.update(right);
                    newNodes.addElement((Object)tiger.digest());
                } else {
                    newNodes.addElement((Object)left);
                }
            }
            nodes = newNodes;
        }
        System.arraycopy(nodes.elementAt(0), 0, buf, offset, HASHSIZE);
        engineReset();
        return HASHSIZE;
    }


    protected void engineReset()
    {
        bufferOffset = 0;
        byteCount = 0;
        nodes = new Vector();
        tiger.reset();
    }

    public Object clone()
    throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * Update the internal state with a single block of size 1024
     * (or less, in final block) from the internal buffer.
     */
    protected void blockUpdate() {
        tiger.reset();
        tiger.update((byte)0); // leaf prefix
        tiger.update(buffer,0,bufferOffset);
        if((bufferOffset==0)&(nodes.size()>0))
            return; // don't remember a zero-size hash except at very beginning
        nodes.addElement((Object)tiger.digest());
    }

    public static void  main(String[] args)
    throws IOException, NoSuchAlgorithmException
    {
        if(args.length<1) {
            System.out.println("You must supply a filename.");
            return;
        }
        MessageDigest tt = new TreeTiger();
        FileInputStream fis;

        for(int i=0;i<args.length;i++) {
            fis = new FileInputStream(args[i]);
            int read;
            byte[] in = new byte[1024];
            while((read = fis.read(in)) > -1) {
                tt.update(in,0,read);
            }
            fis.close();
            byte[] digest = tt.digest();
            String hash = new BigInteger(1,digest).toString(16);
            while(hash.length()<48) {
                hash = "0"+hash;
            }
            System.out.println("hex:"+hash);
            System.out.println("b32:"+(new Base32()).encode(digest));
            tt.reset();
        }
    }
}
