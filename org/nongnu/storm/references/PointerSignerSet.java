/*
PointerSignerSet.java
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
import org.nongnu.storm.util.URN5Namespace;
import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

/** A class that knows a couple of pointer signers for different owners.
 *  The point of this class is that you may know the private keys
 *  for a couple of different public keys (pointer owners), for example
 *  yourself, your working group, etc. It dispatches calls
 *  to the individual pointer owners responsible for certain ids.
 */
public class PointerSignerSet {
    /** Pointer signers by owners' ReferenceIds.
     */
    private Map signers;

    public PointerSignerSet(Set signers) {
	for(Iterator i=signers.iterator(); i.hasNext();)
	    addSigner((PointerSigner)i.next());
    }

    public void addSigner(PointerSigner signer) {
	this.signers.put(signer.getOwner().getId(), signer);
    }


    protected PointerSigner get(ReferenceId owner) {
	try {
	    return (PointerSigner)signers.get(owner);
	} catch(NoSuchElementException e) {
	    throw new NoSuchElementException("No pointer signer known for " +
					     owner);
	}
    }

    protected PointerSigner get(PointerId pointer) {
	return get(pointer.getReferenceId());
    }

    protected PointerSigner get(Reference record) {
	return get(Pointers.getPointer(record));
    }


    public PointerId newPointer(ReferenceId owner) {
	return get(owner).newPointer();
    }
	
    /** Sign an existing pointer record.
     */
    public void sign(Reference record) throws IOException, 
					      GeneralSecurityException {
	get(record).sign(record);
    }

    public Reference initialize(PointerId pointer, String property,
				String target)
	throws IOException, GeneralSecurityException {

	return get(pointer).initialize(pointer, property, target);
    }

    /** Update pointer, obsoleting the record with the most current timestamp.
     *  If there is no record yet, obsolete nothing.
     *  <p>
     *  <strong>Avoid using this method if at all possible</strong>;
     *  it uses a heuristic for determining which blocks to obsolete,
     *  and this heuristic may easily fail in the <em>interesting</em> cases
     *  (i.e., when the obsoletion information is actually needed).
     *  So, if possible, use <code>initialize(...)</code> and
     *  <code>update(...)</code>.
     */
    public Reference updateNewest(PointerId pointer, String target) 
	throws IOException, GeneralSecurityException {

	return get(pointer).updateNewest(pointer, target);
    }

    public Reference update(Reference lastRecord, String target) 
	throws IOException, GeneralSecurityException {

	return get(lastRecord).update(lastRecord, target);
    }

    public Reference update(Reference lastRecord, 
			    String property, String target)
	throws IOException, GeneralSecurityException {

	return get(lastRecord).update(lastRecord, property, target);
    }
    
    public Reference addVersionRecord(PointerId pointer, String property, 
				      String target, Set obsoletes) 
	throws IOException, GeneralSecurityException {

	return get(pointer).addVersionRecord(pointer, property, target,
					     obsoletes);
    }

    public Reference addVersionRecord(PointerId pointer, String property, 
				      String target, Set obsoletes,
				      long timestamp) 
	throws IOException, GeneralSecurityException {

	return get(pointer).addVersionRecord(pointer, property, target,
					     obsoletes, timestamp);
    }
}
