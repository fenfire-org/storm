/*
HtmlLinkIndex.java
 *    
 *    Copyright (c) 2003, Benja Fallenstein
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
import org.nongnu.storm.impl.AsyncSetCollector;
import nu.xom.*;
import nu.xom.xslt.XSLTransform;
import nu.xom.xslt.XSLException;
import java.io.*;
import java.net.*;
import java.util.*;

/** An IndexType of indices which
 *  
 *
 */
public class HtmlLinkIndex { 
    public static boolean dbg = false;
    private static void p(String s) { System.out.println("HtmlLinkIndex:: "+s); }

    public static final String uri =
	"http://fenfire.org/2004/02/backlinks";

    public static final IndexType type = new IndexType();

    protected IndexedPool pool;

    protected static final org.w3c.tidy.Tidy tidy = new org.w3c.tidy.Tidy();
    static {
	tidy.setXmlOut(true);
    }


    protected static class IndexType implements IndexedPool.IndexType {
	public Set getMappings(Block block) throws IOException {
	    if(!block.getId().getContentType().equals("application/prs.fallenstein.pointersignature"))
		return Collections.EMPTY_SET;
	    if(dbg) p("index "+block.getId());

	    IndexedPool pool = (IndexedPool)block.getPool();

	    PointerSignature sig;
	    try {
		sig = PointerIndex.loadSignature(block);
	    } catch(Throwable _) {
		if(dbg) _.printStackTrace();
		return Collections.EMPTY_SET;
	    }
	    Reference record = new Reference(pool, sig.getRecord());
	    sig.checkTimestamp(record);

	    Block target = Pointers.get(record, pool);
	    if(dbg) p("target: "+target.getId());

	    String type = target.getId().getContentType();
	    if(type.indexOf(';') >= 0)
		type = type.substring(0, type.indexOf(';'));
	    type = type.trim().toLowerCase();

	    Document document;

	    if(type.equals("text/html")) {

		/*
		org.w3c.dom.Document domDocument =
		    tidy.parseDOM(target.getInputStream(), null);
		document = nu.xom.converters.DOMConverter.convert(domDocument);
		*/

		// AAARGH -- XOM can't read from Tidy's DOM properly

		ByteArrayOutputStream o = new ByteArrayOutputStream();
		tidy.parse(target.getInputStream(), o);
		byte[] b = o.toByteArray();
		
		Builder builder = new Builder();
		ByteArrayInputStream i = new ByteArrayInputStream(b);
		try {
		    document = builder.build(i);
		} catch(ParsingException e) {
		    e.printStackTrace();
		    // This is Tidy's output -- it should be parsable,
		    // if not, it's Tidy that's wrong, not the input file
		    throw new Error(e);
		}

	    } else if(type.equals("text/xml") || 
		      type.equals("application/xml") ||
		      type.equals("application/xhtml+xml")) {

		try {
		    Builder builder = new Builder();
		    document = builder.build(target.getInputStream());
		} catch(ParsingException e) {
		    e.printStackTrace();
		    return Collections.EMPTY_SET;
		}

		for(int i=0; i<document.getChildCount(); i++) {
		    if(document.getChild(i) instanceof ProcessingInstruction) {
			ProcessingInstruction pi = 
			    (ProcessingInstruction)document.getChild(i);
			if(dbg) p("processing instruction "+pi);
			if(pi.getTarget().equals("xml-stylesheet")) {
			    String href = parsePseudoAtts(pi.getValue());
			    if(dbg) p("href: "+href);
			    if(href == null) continue;
			    try {
				URL url = new URL(href);
				URLConnection conn = url.openConnection();
				InputStream in = conn.getInputStream();
				Builder builder = new Builder();
				Document styledocument = builder.build(in, href);
				XSLTransform stylesheet = new XSLTransform(styledocument);
				Nodes n = stylesheet.transform(document);
				if(n.size() != 1)
				    throw new Error("???");
				document = new Document((Element)n.get(0));
			    } catch(ParsingException e) {
				e.printStackTrace();
				return Collections.EMPTY_SET;
			    } catch(XSLException e) {
				e.printStackTrace();
				return Collections.EMPTY_SET;
			    }
			    break;
			}
		    }
		}

		if(!type.equals("application/xhtml+xml")) {
		    if(dbg) p("check root");
		    Element root = document.getRootElement();
		    if(!root.getLocalName().equals("html"))
			return Collections.EMPTY_SET;
			
		    if(!root.getNamespaceURI().equals("") &&
		       !root.getNamespaceURI().equals("http://www.w3.org/1999/xhtml"))
			return Collections.EMPTY_SET;
		    if(dbg) p("root checked, is HTML");
		}

	    } else {
		return Collections.EMPTY_SET;
	    }

	    Set mappings = new HashSet();
	    addMappings(document, block.getId(), 
			sig.getPointer().getURI(), mappings);
	    return mappings;
	}

	protected void addMappings(ParentNode node, 
				   BlockId signature,
				   String pointer,
				   Set mappings) {
	    for(int i=0; i<node.getChildCount(); i++) {
		Node child = node.getChild(i);
		if(child instanceof ParentNode)
		    addMappings((ParentNode)child, 
				signature, pointer, mappings);
	    }

	    if(node instanceof Element) {
		Element elem = (Element)node;

		for(int i=0; i<elem.getAttributeCount(); i++) {
		    Attribute att = elem.getAttribute(i);
		    if(att.getNamespaceURI().equals("") &&
		       att.getLocalName().equals("href") &&
		       att.getValue().startsWith("vnd-storm-")) {
			if(dbg) p("add mapping: "+signature+" "+
				  att.getValue()+" "+pointer);
			mappings.add(new IndexedPool.Mapping(signature, 
							     att.getValue(), 
							     pointer));
		    }
		}
	    }
	}

	public Object createIndex(IndexedPool pool) {
	    return new HtmlLinkIndex(pool);
	}
	
	public String getIndexTypeURI() {
	    return uri;
	}
	
	public String getHumanReadableName() {
	    return ("An index of HTML blocks by block ids they link to.");
	}
    }

    public HtmlLinkIndex(IndexedPool pool) {
	this.pool = pool;
    }

    public final class Link {
	public Link(PointerId p, BlockId s) { pointer=p; signature=s; }

	public final PointerId pointer;
	public final BlockId signature;
	
	public boolean equals(Object o) {
	    if(!(o instanceof Link)) return false;
	    Link l = (Link)o;
	    return l.signature.equals(signature) && l.pointer.equals(pointer);
	}
	public int hashCode() { 
	    return pointer.hashCode() ^ 24535*signature.hashCode();
	}
    }

    /** Return a set of pointer signature <em>block ids</em>
     *  for versions (allegedly) linking to this target.
     */
    public SetCollector getLinksTo(String uri) throws IOException {
	Collector c = pool.getMappings(HtmlLinkIndex.uri, uri);
	final AsyncSetCollector result = new AsyncSetCollector();

	c.addCollectionListener(new CollectionListener() {
		public boolean item(Object o) {
		    IndexedPool.Mapping m = (IndexedPool.Mapping)o;
		    try {
			result.receive(new Link(new PointerId(m.value), 
						m.block));
		    } catch(IllegalArgumentException e) {
			e.printStackTrace();
		    }
		    return true;
		}
		public void finish(boolean timeout) {
		    result.finish(timeout);
		}
	    });

	return result;
    }

    protected static final int
	OUTSIDE_ATT = 0,
	IN_NAME = 1,
	AFTER_NAME = 2,
	AFTER_EQUALS = 3,
	IN_VALUE = 4;

    protected static boolean isSpace(char c) {
	return (c == 0x20 || c == 0x09 || c == 0x0D || c == 0x0A);
    }

    protected static String parsePseudoAtts(String s) {
	int state = OUTSIDE_ATT;
	String name = null, value = null;
	String href = null, type = null;
	char quote = 0;
	
	for(int i=0; i<s.length(); i++) {
	    char c = s.charAt(i);
	    if(state == OUTSIDE_ATT) {
		if(!isSpace(c)) {
		    name = ""+c;
		    state = IN_NAME;
		    //p("IN_NAME");
		}
	    } else if(state == IN_NAME) {
		if(c == '=') {
		    state = AFTER_EQUALS;
		    //p("AFTER_EQUALS");
		} else if(!isSpace(c)) {
		    name += c;
		} else {
		    state = AFTER_NAME;
		    //p("AFTER_NAME");
		}
	    } else if(state == AFTER_NAME) {
		if(c == '=') {
		    state = AFTER_EQUALS;
		    //p("AFTER_EQUALS");
		} else if(!isSpace(c)) {
		    // parse error
		    //p("bad char AFTER_NAME: "+c);
		    return null;
		}
	    } else if(state == AFTER_EQUALS) {
		if(c == '"' || c == '\'') {
		    quote = c;
		    value = "";
		    state = IN_VALUE;
		    //p("IN_VALUE");
		} else if(!isSpace(c)) {
		    // parse error
		    //p("bad char AFTER_EQUALS: "+c);
		    return null;
		}
	    } else if(state == IN_VALUE) {
		if(c != quote)
		    value += c;
		else {
		    //p("parsed name '"+name+"' value '"+value+"'");
		    if(name.equals("href"))
			href = value;
		    if(name.equals("type"))
			type = value;
		    state = OUTSIDE_ATT;
		    //p("OUTSIDE_ATT");
		}
	    }
	}

	if(state != OUTSIDE_ATT) {
	    // parse error
	    //p("end of processing instruction while not OUTSIDE_ATT");
	    return null;
	}

	if(href == null || type == null || 
	   !type.toLowerCase().equals("application/xslt+xml"))
	    return null;

	return href;
    }
}
