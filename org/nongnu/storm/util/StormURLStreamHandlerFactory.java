/*
StormURLStreamHandlerFactory.java
 *
 *    Copyright (c) 2002, Benja Fallenstein
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
import org.nongnu.storm.*;
import org.nongnu.storm.references.*;
import java.io.*;
import java.net.*;

/** 
 */
public class StormURLStreamHandlerFactory implements URLStreamHandlerFactory {
    /** The factory to use for URI schemes not handled
     *  by this factory (i.e., non-Storm URI schemes).
     */
    protected URLStreamHandlerFactory subFactory;

    /** The pool to retrieve data from.
     */
    protected IndexedPool pool;

    private static StormURLStreamHandlerFactory installed = null;

    public static void install(IndexedPool pool) {
	if(installed == null) {
	    installed = new StormURLStreamHandlerFactory(pool, null);
	    URL.setURLStreamHandlerFactory(installed);
	} else {
	    installed.pool = pool;
	}
    }

    public static void install(IndexedPool pool,
			       URLStreamHandlerFactory subFactory) {
	if(installed == null) {
	    installed = new StormURLStreamHandlerFactory(pool, subFactory);
	    URL.setURLStreamHandlerFactory(installed);
	} else {
	    installed.pool = pool;
	    installed.subFactory = subFactory;
	}
    }

    /**
     *  @param pool The pool to retrieve data from.
     */
    public StormURLStreamHandlerFactory(IndexedPool pool) {
	this.pool = pool;
	this.subFactory = null;
    }

    /**
     *  @param pool The pool to retrieve data from.
     *  @param subFactory The factory to use for URI schemes not handled
     *                    by this factory (i.e., non-Storm URI schemes).
     */
    public StormURLStreamHandlerFactory(IndexedPool pool,
					URLStreamHandlerFactory subFactory) {
	this.pool = pool;
	this.subFactory = subFactory;
    }

    public URLStreamHandler createURLStreamHandler(String _protocol) {
	String protocol = _protocol.toLowerCase();
	if(protocol.equals("vnd-storm-hash"))
	    return handler;
	else if(protocol.equals("vnd-storm-ref"))
	    return handler;
	else if(protocol.equals("vnd-storm-ptr"))
	    return handler;
	else if(subFactory != null)
	    return subFactory.createURLStreamHandler(_protocol);
	else
	    return null;
    }


    public final URLStreamHandler handler = new URLStreamHandler() {
	    public URLConnection openConnection(URL u) {
		return new StormURLConnection(u);
	    };
	};

    public class StormURLConnection extends URLConnection {
	protected Block block;

	public StormURLConnection(URL url) {
	    super(url);
	}

	public void connect() throws IOException {
	    if(pool == null)
		throw new IllegalStateException("StormURLStreamHandlerFactory"+
						".pool not set");
	    block = Pointers.get(url.toExternalForm(), pool);
	}

	public InputStream getInputStream() throws IOException {
	    if(!connected) connect();
	    return block.getInputStream();
	}

	public String getContentType() {
	    return block.getId().getContentType();
	}
    }
}
