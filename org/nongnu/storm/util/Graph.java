/*
Graph.java
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

import java.io.*;
import java.util.*;

/** An RDF graph with methods for conversion 
 *  from and to an N-Triples representation.
 */
public class Graph {

    protected final SortedSet triples;

    protected static final String
	INT_TYPE = "http://www.w3.org/2001/XMLSchema#int",
	SHORT_TYPE = "http://www.w3.org/2001/XMLSchema#short",
	BYTE_TYPE = "http://www.w3.org/2001/XMLSchema#byte",
	BASE64_TYPE = "http://www.w3.org/2001/XMLSchema#base64Binary",
	DATETIME_TYPE = "http://www.w3.org/2001/XMLSchema#dateTime",
	STRING_TYPE = "http://www.w3.org/2001/XMLSchema#string";

    protected static final char SPACE = ' ', TAB = '\t';

    public Graph(Set triples) {
	this.triples = 
	    Collections.unmodifiableSortedSet(new TreeSet(triples));

	for(Iterator i=this.triples.iterator(); i.hasNext();) {
	    Object t = i.next();
	    if(!(t instanceof Triple))
		throw new ClassCastException("Element of triples set "+
					     "is not a triple: "+t);
	}
    }
    
    public Set getTriples() {
	return triples;
    }

    public static Token token(String s) {
	if(!s.startsWith("_:"))
	    return new URIToken(s);
	else
	    return new BlankNodeToken(s.substring(2));
    }

    public String get(String subject, String predicate) {
	return get(token(subject), token(predicate)).untoken();
    }
    public Token get(Token subject, Token predicate) {
	return get(subject, predicate, 1);
    }

    public String get(String subjectOrObject, String predicate, int dir) {
	return get(token(subjectOrObject), token(predicate), dir).untoken();
    }
    public Token get(Token subjectOrObject, Token predicate, int dir) {
	Set s = getAll(subjectOrObject, predicate, dir);
	if(s.isEmpty()) throw new NoSuchElementException("Graph.get("+subjectOrObject+", "+predicate+", "+dir+")");
	if(s.size() > 1)
	    throw new NotUniqueException(subjectOrObject, predicate, dir);
	return (Token)s.iterator().next();
    }

    public Set getAll(String subject, String predicate) {
	Set s = getAll(token(subject), token(predicate));
	Set result = new HashSet();
	for(Iterator i=s.iterator(); i.hasNext();) {
	    Token t = (Token)i.next();
	    result.add(t.untoken());
	}
	return result;
    }
    public Set getAll(Token subject, Token predicate) {
	Set result = new HashSet();
	for(Iterator i=triples.iterator(); i.hasNext();) {
	    Triple t = (Triple)i.next();
	    if(t.getSubject().equals(subject) &&
	       t.getPredicate().equals(predicate))
		result.add(t.getObject());
	}
	return result;
    }

    public Set getAll(String subjectOrObject, String predicate, int dir) {
	Set s = getAll(token(subjectOrObject), token(predicate), dir);
	Set result = new HashSet();
	for(Iterator i=s.iterator(); i.hasNext();) {
	    Token t = (Token)i.next();
	    result.add(t.untoken());
	}
	return result;
    }
    public Set getAll(Token subjectOrObject, Token predicate, int dir) {
	if(dir > 0)
	    return getAll(subjectOrObject, predicate);
	Set result = new HashSet();
	Token object = subjectOrObject;
	for(Iterator i=triples.iterator(); i.hasNext();) {
	    Triple t = (Triple)i.next();
	    if(t.getPredicate().equals(predicate) &&
	       t.getObject().equals(object))
		result.add(t.getSubject());
	}
	return result;
    }

    public String getString(String subject, String predicate) {
	return getString(token(subject), token(predicate));
    }
    public String getString(Token subject, Token predicate) {
	Token t = get(subject, predicate);
	// We don't check datatypes or distinguish between
	// typed and plain literals here
	return t.getLexicalForm();
    }

    public int getInt(String subject, String predicate) {
	return getInt(token(subject), token(predicate));
    }
    public int getInt(Token subject, Token predicate) {
	Token t = get(subject, predicate);
	// According to XML Schema, short and byte are derived types
	// (or whatever XML Schema calls that) of int, and therefore
	// a 'short' or 'byte' literal is parseable as an int.
	if(!t.getDatatype().equals(INT_TYPE) &&
	   !t.getDatatype().equals(SHORT_TYPE) &&
	   !t.getDatatype().equals(BYTE_TYPE))
	    throw new DatatypeException("Not an int literal: "+t);
	return Integer.parseInt(t.getLexicalForm());
    }

    public byte[] getBase64(String subject, String predicate) {
	return getBase64(token(subject), token(predicate));
    }
    public byte[] getBase64(Token subject, Token predicate) {
	Token t = get(subject, predicate);
	if(!t.getDatatype().equals(BASE64_TYPE))
	    throw new DatatypeException("Not a base64Binary literal: "+t);
	return Base64.decode(t.getLexicalForm().toCharArray());
    }

    public Date getDate(String subject, String predicate) {
	return getDate(token(subject), token(predicate));
    }
    public Date getDate(Token subject, Token predicate) {
	Token t = get(subject, predicate);
	if(!t.getDatatype().equals(DATETIME_TYPE))
	    throw new DatatypeException("Not a dateTime literal: "+t);
	return DateParser.parse(t.getLexicalForm());
    }


    /** Return a copy of this graph in which all occurrences of
     *  'current' have been replaced with 'replacement'.
     */
    public Graph replace(Token current, Token replacement) {
	Set s = new HashSet();
	for(Iterator i = triples.iterator(); i.hasNext();) {
	    Triple t = (Triple)i.next();
	    s.add(t.replace(current, replacement));
	}
	return new Graph(s);
    }
    public Graph replace(String current, String replacement) {
	return replace(token(current), token(replacement));
    }


    

    public void write(OutputStream out) throws IOException {
	write(new OutputStreamWriter(out, "US-ASCII"));
    }

    public void write(Writer w) throws IOException {
	write(new PrintWriter(w));
    }

    public void write(PrintWriter w) throws IOException {
	for(Iterator i=triples.iterator(); i.hasNext();) {
	    w.println(i.next());
	}
	w.flush();
    }

    public static Graph read(InputStream in) throws IOException {
	return new Graph(readTriples(in));
    }

    public static Graph read(Reader r) throws IOException {
	return new Graph(readTriples(r));
    }

    public static Graph read(BufferedReader r) throws IOException {
	return new Graph(readTriples(r));
    }




    public static final class Triple implements Comparable {
	public final Token subject, predicate, object;
	
	public Triple(Token subject, Token predicate, Token object) {
	    // first set the instance variables, before checking,
	    // so that we can print out even malformed triples
	    // (e.g., with a literal as the predicate)
	    this.subject = subject;
	    this.predicate = predicate;
	    this.object = object;
	    
	    check(subject instanceof NodeToken, 
		  "Subject must be URI or blank node, not literal: "+this);
	    check(predicate instanceof URIToken,
		  "Predicate must be URI, not literal or blank node: "+this);
	}
	
	public Token getSubject() { return subject; }
	public Token getPredicate() { return predicate; }
	public Token getObject() { return object; }

	/** Return a triple in which all occurrences of 'current'
	 *  have been replaced by 'replacement'.
	 */
	public Triple replace(Token current, Token replacement) {
	    Token s = subject.replace(current, replacement);
	    Token p = predicate.replace(current, replacement);
	    Token o = object.replace(current, replacement);
	    if(s == subject && p == predicate && o == object)
		return this;
	    else
		return new Triple(s, p, o);
	}
	
	public String toString() {
	    return subject + " " + predicate + " " + object + ".";
	}
	
	public boolean equals(Object o) {
	    if(!(o instanceof Triple)) return false;
	    Triple t = (Triple)o;
	    return subject.equals(t.subject) && predicate.equals(t.predicate) &&
	    object.equals(t.object);
	}

	public int compareTo(Object o) {
	    if(!(o instanceof Triple))
		throw new ClassCastException(this+" not comparable to "+o);
	    return toString().compareTo(o.toString());
	}

	public int hashCode() {
	    return subject.hashCode() ^ (predicate.hashCode() * 343) ^
		(object.hashCode() * 28345);
	}
    }

    
    
    /** Create a new token from its N3 serialization.
     */
    public static Token parseToken(String serialization) {
	String s = serialization;
	if(s.startsWith("<")) {
	    check(s.endsWith(">"), "Malformed URI token: "+s);
	    return new URIToken(unescape(s.substring(1, s.length()-1)));
	} else if(s.startsWith("_:")) {
	    return new BlankNodeToken(unescape(s.substring(2)));
	} else if(s.startsWith("\"")) {
	    int i = s.lastIndexOf("\"");
	    check(i > 0, "Malformed literal token: "+s);
	    String lexicalForm = unescape(s.substring(1, i));

	    if(i == s.length()-1) {
		return new PlainLiteralToken(lexicalForm);
	    } else if(s.charAt(i+1) == '@') {
		String languageTag = s.substring(i+2);
		return new PlainLiteralToken(lexicalForm, languageTag);
	    } else if(i<s.length()-3 && s.substring(i+1, i+4).equals("^^<")) {
		check(s.endsWith(">"), 
		      "Malformed data type part in literal token: "+s);
		String datatype = unescape(s.substring(i+4, s.length()-1));
		return new TypedLiteralToken(lexicalForm, datatype);
	    }
	}
	throw new ParseError("Malformed token: "+s);
    }
    
    private static void check(boolean condition, String errorMessage) {
	if(!condition) throw new ParseError(errorMessage);
    }
    
    public static abstract class Token {
	private Token() {}

	public abstract String getSerialization();
	
	public String getURI() {
	    throw new UnsupportedOperationException("Not a URI token: "+this);
	}
	public String getID() {
	    throw new UnsupportedOperationException("Not a blank node "+
						    "token: "+this);
	}
	public String getLexicalForm() {
	    throw new UnsupportedOperationException("Not a literal token: "+
						    this);
	}
	public String getDatatype() {
	    throw new UnsupportedOperationException("Not a literal token: "+
						    this);
	}
	public String getLanguageTag() {
	    throw new UnsupportedOperationException("Not a literal token: "+
						    this);
	}
	public String untoken() {
	    throw new UnsupportedOperationException("Not a URI or blank node "+
						    "token: "+this);
	}

	public Token replace(Token current, Token replacement) {
	    return equals(current) ? replacement : this;
	}

	public abstract boolean equals(Object o);
	public int hashCode() { return getSerialization().hashCode(); }
	public String toString() { return getSerialization(); }
    }

    public static abstract class NodeToken extends Token {}
    
    public static final class URIToken extends NodeToken {
	private final String uri;
	public URIToken(String uri) {
	    this.uri = uri;
	}
	public String getURI() { return uri; }
	public String getSerialization() { return "<" + escape(uri) + ">"; }
	public String untoken() { return uri; }
	public boolean equals(Object o) {
	    if(!(o instanceof URIToken)) return false;
	    URIToken t = (URIToken)o;
	    return uri.equals(t.uri);
	}
    }
    
    public static final class BlankNodeToken extends NodeToken {
	private final String id;
	public BlankNodeToken(String id) {
	    this.id = id;
	}
	public String getID() { return id; }
	public String getSerialization() { return "_:" + id; }
	public String untoken() { return "_:" + id; }
	public boolean equals(Object o) {
	    if(!(o instanceof BlankNodeToken)) return false;
	    BlankNodeToken t = (BlankNodeToken)o;
	    return id.equals(t.id);
	}
    }
    
    public static abstract class LiteralToken extends Token {
	protected final String lexicalForm;
	private LiteralToken(String lexicalForm) {
	    this.lexicalForm = lexicalForm;
	}
	public String getLexicalForm() { return lexicalForm; }
	public String getDatatype() {
	    throw new UnsupportedOperationException("Not a typed literal token: "+
						    this);
	}
	public String getLanguageTag() {
	    throw new UnsupportedOperationException("Not a plain literal token: "+
						    this);
	}
    }
    
    public static final class PlainLiteralToken extends LiteralToken {
	private final String languageTag;
	public PlainLiteralToken(String lexicalForm, String languageTag) {
	    super(lexicalForm);
	    this.languageTag = languageTag;
	}
	public PlainLiteralToken(String lexicalForm) {
	    super(lexicalForm);
	    this.languageTag = null;
	}
	public String getLanguageTag() { return languageTag; }
	public String getSerialization() { 
	    if(languageTag == null)
		return '"' + escape(lexicalForm) + '"';
	    else
		return '"' + escape(lexicalForm) + '"' + 
		    '@' + escape(languageTag);
	}
	public boolean equals(Object o) {
	    if(!(o instanceof PlainLiteralToken)) return false;
	    PlainLiteralToken t = (PlainLiteralToken)o;
	    if(languageTag == null)
		return lexicalForm.equals(t.lexicalForm) &&
		    t.languageTag == null;
	    else
		return lexicalForm.equals(t.lexicalForm) &&
		    languageTag.equals(t.languageTag);
	}
    }
    
    public static final class TypedLiteralToken extends LiteralToken {
	private final String datatype;
	public TypedLiteralToken(String lexicalForm, String datatype) {
	    super(lexicalForm);
	    this.datatype = datatype;
	}
	public String getDatatype() { return datatype; }
	public String getSerialization() { 
	    return '"' + escape(lexicalForm) + '"' + 
		"^^<" + escape(datatype) + ">";
	}
	public boolean equals(Object o) {
	    if(!(o instanceof TypedLiteralToken)) return false;
	    TypedLiteralToken t = (TypedLiteralToken)o;
	    return lexicalForm.equals(t.lexicalForm) &&
		datatype.equals(t.datatype);
	}
    }




    protected static Set readTriples(InputStream in) throws IOException {
	return readTriples(new InputStreamReader(in, "US-ASCII"));
    }

    protected static Set readTriples(Reader r) throws IOException {
	return readTriples(new BufferedReader(r));
    }

    protected static Set readTriples(BufferedReader r) throws IOException {
	Set s = new HashSet();
	while(true) {
	    String line = r.readLine();
	    if(line == null) break;
	    Triple t = readTriple(line);
	    if(t != null) s.add(t);
	}
	return s;
    }

    protected static Triple readTriple(String line) {
	String s = line.trim();
	if(s.equals("") || s.startsWith("#"))
	    // This line is a blank line or a comment: ignore
	    return null;

	if(!s.endsWith("."))
	    throw new ParseError("Triple line must end in '.': '"+line+"'");
	s = s.substring(0, s.length()-1).trim();
	
	int i = tokenEnd(s, line);
	Token subject = parseToken(s.substring(0, i));
	s = s.substring(i).trim();

	i = tokenEnd(s, line);
	Token predicate = parseToken(s.substring(0, i));
	s = s.substring(i).trim();

	Token object = parseToken(s);

	return new Triple(subject, predicate, object);
    }
    
    protected static int tokenEnd(String s, String line) {
	int i = s.indexOf(SPACE), j = s.indexOf(TAB);
	if(i < 0) {
	    if(j >= 0) 
		return j;
	    else
		throw new ParseError("Not enough tokens on line: "+line);
	} else {
	    if(j < 0)
		return i;
	    else
		return (i<j) ? i : j;
	}
    }



    protected static String escape(String s) {
	StringBuffer buf = new StringBuffer();
	for(int i=0; i<s.length(); i++) {
	    char c = s.charAt(i);
	    if(c <= 0x08)
		buf.append(escape(c));
	    else if(c == 0x09)
		buf.append("\\t");
	    else if(c == 0x0A)
		buf.append("\\n");
	    else if(c <= 0x0C)
		buf.append(escape(c));
	    else if(c == 0x0D)
		buf.append("\\r");
	    else if(c <= 0x1F)
		buf.append(escape(c));
	    else if(c <= 0x21)
		buf.append(c);
	    else if(c == 0x22)
		buf.append("\\\"");
	    else if(c <= 0x5B)
		buf.append(c);
	    else if(c == 0x5C)
		buf.append("\\\\");
	    else if(c <= 0x7E)
		buf.append(c);
	    else if(Character.getType(c) == Character.SURROGATE)
		throw new Error("XXX surrogates not handled");
	    else
		buf.append(escape(c));
	}
	return buf.toString();
    }

    protected static String escape(char c) {
	String num = ""+((long)c);
	while(num.length() < 4) num = "0"+num;
	return "\\u" + num;
    }

    protected static String unescape(String s) {
	StringBuffer buf = new StringBuffer();
	for(int i=0; i<s.length(); i++) {
	    if(s.charAt(i) != '\\') {
		buf.append(s.charAt(i));
		continue;
	    }

	    i++;
	    if(i >= s.length())
		throw new ParseError("String ends inside escape: "+s);

	    char d = s.charAt(i);
	    if(d == 't')
		buf.append('\t');
	    else if(d == 'n')
		buf.append('\n');
	    else if(d == 'r')
		buf.append('\r');
	    else if(d == '"')
		buf.append('"');
	    else if(d == '\\')
		buf.append('\\');
	    else if(d == 'U')
		throw new Error("XXX surrogates not handled");
	    else if(d != 'u')
		throw new ParseError("Illegal escape sequence \\"+d+" in "+
				     "string "+s);
	    else {
		if(i+4 >= s.length())
		    throw new ParseError("Incomplete escape sequence "+
					 "at end of string: "+s);
		String nstr = s.substring(i+1, i+5);
		buf.append((char)Integer.parseInt(nstr));
		i += 4;
	    }
	}
	return buf.toString();
    }

    



    public static class ParseError extends RuntimeException {
	public ParseError() { super(); }
	public ParseError(String s) { super(s); }
    }

    public static class DatatypeException extends RuntimeException {
	public DatatypeException() { super(); }
	public DatatypeException(String s) { super(s); }
    }

    public static class NotUniqueException extends RuntimeException {
	public final Token subjectOrObject;
	public final Token predicate;
	public final int dir;

	public NotUniqueException(Token subjectOrObject, Token predicate,
				  int dir) {
	    super("Not unique: "+subjectOrObject+" "+predicate+" "+dir);
	    this.subjectOrObject = subjectOrObject;
	    this.predicate = predicate;
	    this.dir = dir;
	}
    }

    public static class Maker {
	Set triples = new HashSet();
	int bnode = 1;

	public void add(Token subject, Token property, Token object) {
	    triples.add(new Triple(subject, property, object));
	}
	public void add(String subject, String property, String object) {
	    add(token(subject), token(property), token(object));
	}
	public void addPlain(String subject, String property, 
			     String content) {
	    add(token(subject), token(property),
		new PlainLiteralToken(content));
	}
	public void addPlain(String subject, String property, 
			     String content, String languageTag) {
	    add(token(subject), token(property),
		new PlainLiteralToken(content, languageTag));
	}
	public void addString(String subject, String property, 
			      String string) {
	    add(token(subject), token(property),
		new TypedLiteralToken(string, STRING_TYPE));
	}
	public void addInt(String subject, String property, int integer) {
	    add(token(subject), token(property),
		new TypedLiteralToken(""+integer, INT_TYPE));
	}
	public void addBase64(String subject, String property, byte[] data) {
	    String lexical = new String(Base64.encode(data));
	    add(token(subject), token(property),
		new TypedLiteralToken(lexical, BASE64_TYPE));
	}
	public void addDate(String subject, String property, Date date) {
	    String lexical = DateParser.getIsoDate(date);
	    add(token(subject), token(property),
		new TypedLiteralToken(lexical, DATETIME_TYPE));
	}

	public String bnode() {
	    String s = "_:n"+bnode;
	    bnode++;
	    return s;
	}

	public Set triples() { return triples; }
	public Graph make() { return new Graph(triples); }
    }
}
