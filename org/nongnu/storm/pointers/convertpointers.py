# -*-Python-*-
# 
# Copyright (c) 2004, Benja Fallenstein
# 
# This file is part of Storm.
# 
# Storm is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
# 
# Storm is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
# or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
# Public License for more details.
# 
# You should have received a copy of the GNU General
# Public License along with Storm; if not, write to the Free
# Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
# MA  02111-1307  USA
# 

"""
Convert old-style (0.1) pointers in a pool to new-style (1.0) pointers.
"""

import java, com, org, sys, re

from java.util import HashSet

from org.nongnu.storm import BlockId
from org.nongnu.storm.impl import DirPool
from org.nongnu.storm.util import Graph, CopyUtil

from org.nongnu.storm import pointers as old
from org.nongnu.storm import references as new
from org.nongnu.storm.references import Pointers

dirname = java.io.File(sys.argv[1])
oldkeyfile = java.io.File(sys.argv[2])
idinfo = BlockId(sys.argv[3])
newkeyfile = java.io.File(sys.argv[4])

indexTypes = HashSet()
indexTypes.add(old.PointerIndex.type)
indexTypes.add(new.PointerIndex.type)
pool = DirPool(dirname, indexTypes)

old_index = pool.getIndex(old.PointerIndex.uri)
new_index = pool.getIndex(new.PointerIndex.uri)

stream = java.io.ObjectInputStream(java.io.FileInputStream(oldkeyfile))
keypair = stream.readObject()
stream.close()

if not newkeyfile.exists():
    signer = new.PointerSigner.createOwner(pool, keypair, idinfo)
else:
    signer = new.PointerSigner(pool, java.io.FileInputStream(newkeyfile))

    if keypair != signer.getKeyPair():
        print "New key file doesn't use same keys as old key file:", newkeyfile
        print "Give a nonexisting filename to convert the old key file"
        sys.exit(1)

signer.writeKeys(java.io.FileOutputStream(newkeyfile))
owner = signer.getOwner().getId()
root = new.PointerId(owner.getGraphId())


def getNewId(oldId):
    """
    Get the new pointer id corresponding to an old one.
    """

    local = oldId.getRandomPart()
    if local == 'stylesheet':
        return new.PointerId(root, '/0:stylesheet')

    elif local.endswith(':html'):
        source = getNewId(old.PointerId(oldId.getURI()[:-5]))
        return new.PointerId(source, '/0:html')

    else:
        # random number pointer, convert deterministically

        # We use only the first 12 bytes of the old random path
        # for the new id. In base32, this means 60 bits of randomness--
        # which means that we don't have to worry about collisions
        # until we approach the order of 1 billion pointers.

        return new.PointerId(root, "/owns,id='%s'" % local[:12])


# Find all old pointers pertaining to this key pair

dig = java.security.MessageDigest.getInstance("SHA-1")
dig.update(old.PointerId.getKeyBytes(keypair.getPublic()))
digstr = com.bitzi.util.Base32.encode(dig.digest()).lower()

prefix = old.PointerId.PREFIX + digstr + ':'

print "Prefix:",prefix

mine = []
new_ids = {}

all = old_index.getIds().block()
nall = all.size(); i = 1
iter = all.iterator()
while iter.hasNext():
    old_id = iter.next()
    print "Old pointer", old_id, "(%s of %s)" % (i, nall)
    i = i + 1
    if old_id.getURI().startswith(prefix):
        print "...will convert"
        if old_id not in mine:
            print "...(not seen before)"
            mine.append(old_id)
            new_ids[old_id] = getNewId(old_id)

# Find all old pointer signatures ("pointer blocks")

i = 1
for old_id in mine:
    new_id = new_ids[old_id]
    print "Convert pointer", old_id, "to", new_id, \
          "(%s of %s)" % (i, len(mine))
    i = i + 1

    pblocks = []
    iter = old_index.getHistory(old_id).iterator()
    while iter.hasNext(): pblocks.append(iter.next())

    pblocks.reverse() # make chronological history

    last = None
    for pblock in pblocks:
        print "...process pointer block of", \
              java.util.Date(pblock.getTimestamp())
	g = Graph.Maker()
	g.add(new_id.getURI(), Pointers.hasInstanceRecord, "_:this");
	g.add(Pointers.hasInstanceRecord,
              Pointers.RDFS_SUBPROPERTY_OF, 
	      Pointers.hasPointerRecord);
	g.add("_:this", Pointers.version, pblock.getTarget().getURI());

        if last != None:
            g.add(last.getId().getURI(), Pointers.obsoletedBy, "_:this")

	g.addDate("_:this", Pointers.timestamp,
                  java.util.Date(pblock.getTimestamp()));

	g.add("_:this", "http://purl.oclc.org/NET/storm/vocab/ref-uri/resolutionMethod", "http://purl.oclc.org/NET/storm/vocab/ref-uri/ReferenceGraph")

	record = new.Reference.create(g, pool);
	signer.sign(record);
        last = record

    if last != None:
        uri = last.get("_:this", Pointers.version)
        block = pool.get(BlockId(uri))

        charset = None
        ct = block.getId().getContentType()
        pos = ct.find(';charset=')
        if pos >= 0:
            epos = ct.find('&', pos)
            if epos < 0: epos = len(ct)
            charset = ct[pos+len(';charset='):epos]
        elif ct == 'text/plain' or ct == 'text/html':
            charset = 'us-ascii'
        elif ct == 'text/prs.fallenstein.rst':
            charset = 'utf-8'

        if charset != None:
            print "...change URIs in newest version..."

            str = CopyUtil.readString(block.getInputStream(), charset)
            while 1:
                match = re.search(prefix+'[a-zA-Z0-9:]*', str)
                if match is None: break
                start, end = match.start(), match.end()
                old_id = old.PointerId(match.group(0))
                if not new_ids.has_key(old_id):
                    new_ids[old_id] = getNewId(old_id)
                str = str[:start] + new_ids[old_id].getURI() + str[end:]

            str = re.sub('urn:x-storm:1.0:', 'vnd-storm-hash:', str)

            bos = pool.getBlockOutputStream(ct)
            w = java.io.OutputStreamWriter(bos, charset)
            w.write(str)
            w.close()

            signer.update(last, bos.getBlockId().getURI())

            print "...updated."
                
    


        

        
        
        
