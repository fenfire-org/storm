/*
Reference.java
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
package org.nongnu.storm.references;
import org.nongnu.storm.*;
import org.nongnu.storm.util.Graph;
import java.io.IOException;
import java.util.*;

public final class Reference extends Graph {
    private ReferenceId id;

    private static Map resolvers = new HashMap();

    static { 
	resolvers.put("http://purl.oclc.org/NET/storm/vocab/ref-uri/StaticRepresentation", new StaticReferenceResolver()); 
	resolvers.put("http://purl.oclc.org/NET/storm/vocab/ref-uri/ReferenceGraph", new GraphReferenceResolver()); 
    }


    /** Get the parsed URI of this reference as a <code>ReferenceId</code> 
     *  object.
     */
    public ReferenceId getId() { return id; }


    public Reference(StormPool pool, BlockId graphId) throws IOException {
	this(new ReferenceId(graphId), pool.get(graphId));
    }

    public Reference(StormPool pool, ReferenceId id) throws IOException {
	this(id, pool.get(id.getGraphId()));
    }

    public Reference(Block graphBlock) throws IOException {
	this(new ReferenceId(graphBlock.getId()), graphBlock);
    }

    public Reference(ReferenceId id, Block graphBlock) throws IOException {
	super(Graph.readTriples(graphBlock.getInputStream()));
	this.id = id;
    }

    public static Reference create(Set triples, 
				   StormPool pool) throws IOException {
	return create(new Graph(triples), pool);
    }

    public static Reference create(Graph.Maker graphMaker,
				   StormPool pool) throws IOException {
	return create(graphMaker.make(), pool);
    }

    public static Reference create(Graph graph, 
				   StormPool pool) throws IOException {
	BlockOutputStream bos = pool.getBlockOutputStream("text/plain");
	graph.write(bos);
	bos.close();
	return new Reference(bos.getBlock());
    }

    public Block resolve(StormPool pool) throws IOException {
	String method = this.get("_:this", "http://purl.oclc.org/NET/storm/vocab/ref-uri/resolutionMethod");
	ReferenceResolver resolver = (ReferenceResolver)resolvers.get(method);
	return resolver.resolve(this, pool);
    }
}
