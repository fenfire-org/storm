===========================================================
``raw_blocks--benja``: Storm blocks without a normal header
===========================================================

:Author:	Benja Fallenstein
:Date:		2003-01-12
:Revision:	$Revision: 1.2 $
:Last-Modified:	$Date: 2003/07/04 13:08:30 $
:Type:		Architecture
:Scope:		Minor
:Status:	Implemented


An increasing number of systems identifies data by an SHA-1 hash
of its content. This provides a chance for interoperability,
which we want to exploit as much as possible.

Retrieving Storm blocks through such a system is already trivial,
since we simply need to pass the SHA-1 from the block id
to the external system. However, we would also like to make use of
existing files which are stored in such systems but, naturally,
do not have a Storm header.


Alternatives
============

There are two possibilities how we could make existing,
SHA-1 identified files into Storm blocks:

1. In the header, include an SHA-1 hash of the block's body.
   In the block id, include only the SHA-1 hash of the header.
   (To check the id, you have to hash the header, take the
   expected body hash from the header, and hash the body.)
2. Not all blocks have explicit headers; some just have
   the SHA-1 of their content, plus a content type
   (encoded in the id). There is a deterministic algorithm
   for constructing the bytes in the header,
   given the content type.

Both of these schemes need the introduction of a new type
of block id.

The first alternative's advantage is that we can put arbitrary
metadata into the header, not just the content type.
The second alternative's advantage is that the content's SHA-1
is immediately visible from the id, and the id is easily constructed
from the content's SHA-1 (provided the content type is known).
The id is always a deterministic function of the content and
its content type (whereas in the first alternative, it depends
on our choice of what to put into the header).

Knowing the content's SHA-1 is important for interoperability
with systems like `Bitzi <http://bitzi.com>`__, which provide
ratings for digital data identified by its content hash:
We want to retrieve the Bitzi rating for the content of
some Storm block. Forming a Storm id from the content's hash
is important so that we can construct a Storm id given a Bitzi
rating of some content (including the content's SHA-1 hash
and content type), for example to retrieve it from a Storm pool
or to link to it.

The first alternative's disadvantages can be addressed as follows:

- We can easily retrieve the content's SHA-1 provided that
  the header is available on the network.
- We can define a mapping from content types to headers
  as follows::

    Content-Type: <content-type-in-lowercase><CRLF>
    Content-Transfer-Encoding: binary<CRLF>
    Storm-Content-Hash: <sha-1-of-body><CRLF>
    <CRLF>

  Then the block's id, again, is a deterministic function
  of the content type and the body's SHA-1 hash.
- We can create an index of Storm blocks by body hashes,
  easily constructed by looking at the headers, which contain
  the hashes after all. Thus we can easily ask, 
  "which non-canonical blocks exist in the system whose body
  has a given SHA-1?"

The second alternative's disadvantage could only be addressed
by putting all the metadata into the id, which would
quickly become bloated.

Therefore, we choose the first route.


Changes
=======

A new id format is introduced: Ids starting with ``02``.
All new blocks should use this id format.

Headers of all new blocks must at least contain:

- A ``Content-Type`` header field.
- A ``Content-Transfer-Encoding`` header field
  whose value is ``binary``.
- A ``Storm-Content-Hash`` header field
  whose value is the SHA-1 hash of the block's body,
  encoded in hexadecimal form.

(The first two requirements are not new.)

Block ids are the byte ``0x02`` plus the SHA-1 hash
of the header, including the two CRLFs at the end.

Canonical headers can be constructed from a body's SHA-1
and content type as follows:

- Take the string "Content-Type: ".
- Append the content type, in lower case. No additional
  rules how to canonicalize a content type are given
  at this time. (Note that the rule given here means
  you must use "text/plain; charset=utf-8" instead of
  "text/plain; charset=UTF-8".)
- Append CRLF, "Content-Type: binary", CRLF, and
  "Storm-Content-Hash: ".
- Append the SHA-1 of the body, encoded in hexadecimal,
  using uppercase A-F digits.
- Append CRLF CRLF.

\- Benja
