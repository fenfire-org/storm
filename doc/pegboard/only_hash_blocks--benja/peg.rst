
==========================================================================
Wherever you need a cryptographic hash, use a Storm block
==========================================================================

:Authors:  Benja Fallenstein
:Date-Created: 2003-07-14
:Last-Modified: $Date: 2003/07/28 13:49:36 $
:Revision: $Revision: 1.2 $
:Status:   Current
:Scope:    Major
:Type:     Architecture


When designing cryptographic protocols, you often need to
use cryptographic hashes in one way or another.

Cryptographic hash functions may be broken. As a precaution,
current Storm block URIs include two hashes: a SHA-1 and
a TigerTree hash. 

If one but not both of these is broken, we can select
a new, third hash function and `extend the lifetime of
our URIs through timestamping`__. (That's `patented`__,
but maybe we get lucky and the patent will have expired
by the time our first hash function breaks...)

__ http://www.math.columbia.edu/~bayer/papers/Timestamp_BHS93.pdfp
__ http://patft.uspto.gov/netacgi/nph-Parser?Sect1=PTO1&Sect2=HITOFF&d=PALL&p=1&u=/netahtml/srchnum.htm&r=1&f=G&l=50&s1=5373561.WKU.&OS=PN/5373561&RS=PN/5373561

Now, in such a situation, *all* the (hash, hashed data)
pairs *must* be timestamped in order to remain valid.

If different protocols built on top of Storm store
hashed data in different, application-specific ways
each, all these protocols must be known in order to
reliably do the timestamping. If, however, Storm blocks
are always used then a whole pool can be secured
by simply timestamping each block in the pool through
a simple algorithm.

Therefore, whenever you need a cryptographic hash 
to be verified, it is best to put the hashed data
into its own Storm block.

(An additional benefit is that then, Storm will do
the hash checking for you; you don't need to
worry about it.)


Issues
======

None.


Changes
=======

It is best current practice that a protocol which is
part of or based on Storm will, if it needs to verify
the cryptographic hash of some piece of data,

- put that data in its own Storm block;
- refer to it using a Storm URI which contains
  the hash;
- use the Storm interfaces to retrieve the block,
  which ensures that the hash has been checked.

\- Benja