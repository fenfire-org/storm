===========================================================
``simple_storm--benja``: Simplify Storm by dropping headers
===========================================================

:Author:	Benja Fallenstein
:Date:		2003-02-16
:Revision:	$Revision: 1.5 $
:Last-Modified: $Date: 2003/04/10 07:45:18 $
:Type:		Architecture
:Scope:		Major
:Status:	Implemented


Storm is quite complex with its MIME headers, and prone to become
more complex if we choose to separate hashing of headers and bodies
(``raw_blocks--benja``). If we break backward compatibility
a single time, as Tuomas suggests, we should take the opportunity to
get rid of our mistakes from the past, in order to make
the future simpler.

By analogy with the ``data`` URL scheme [RFC2397], this PEG
proposes a URN namespace to be registered whose URIs would
contain a MIME type and the content hash of a block of data.
"data" URLs contain a MIME type and a sequence of bytes,
either literally or encoded as base64. The analogy runs deep;
"data" URLs are a MIME type plus an immutable byte sequence,
and so are URIs in this URN namespace. The MIME type is included
with "data" URLs because it is considered the one absolutely
essential piece of metadata necessary to interpret
the byte sequence; for this URN namespace, the same thing holds.


Issues
======

- Won't dropping headers make it harder to include metadata?

   RESOLVED: MIME headers are a non-extensible form of metadata
   anyway; if we allow ``X-`` headers, we have problems with
   permanence. We can still put metadata into another block
   refering to this one; alternatively, many file formats
   allow inclusion of metadata in the file itself (e.g. PNG).
   We could also devise a MIME type or something that is
   some RDF metadata plus a reference to the actual body block--
   so we might simulate the old system when we need it.

   Content types are now included in the block id (different
   content type -> different block).

   The benefits outweigh the problems by far.

- How about metadata that would be included in an HTTP
  response, such as alternative representations of a
  resource (different languages etc.)? How about Creative
  Commons licenses? Wouldn't it be better to have an
  RDF "header" block containing this data?

   RESOLVED: The idea about alternative representations
   is that a single "header" block would refer to
   different "body" blocks, each of which could be used.
   However, it is also necessary to be able to refer
   to each of these representations by itself; if we
   don't want to have an *additional* header block
   for each of these representations, we still need
   something like this proposal to refer to the
   individual alternatives.

   While it would be nice if a CC or other license would
   travel with every block in a computer-readable format,
   this is not by itself enough reason to require
   header blocks, making for a much more complex system
   and separating namespaces in the Storm world.

   I think the best route is to have the simple system
   specified here for now, and possibly extend it later
   by another kind of reference which points to
   a metadata block that then points to the actual body.

- What about the hash tree vulnerabilities mentioned in
  <http://zgp.org/pipermail/p2p-hackers/2002-November/000993.html> /
  <http://zgp.org/pipermail/p2p-hackers/2002-November/000998.html>?
  
   RESOLVED: They've settled on a new convention, prepending a
   zero byte to tree leaves and a one to tree branches
   (concatenated hashes of tree leaves) before hashing.
   Their software is being updated; there's a Java implementation.
   We'll be using that (and we'll fully specify it when
   writing the informal URN namespace registration).

- Why bitzi bitprint? What is it? Why not SHA-1?

   RESOLVED: Bitprints are a combination of a SHA-1 hash with a
   Merkle hash tree based on the Tiger hash algorithm.
   Hash algorithms get broken; when one of the above
   is broken, you have a transitional period before
   the other is, too, in which you can e.g. sign blocks,
   ensuring you can still use them when the other
   is broken too.

   Having a hash tree allows you to download pieces
   of a block from different sources, verifying each
   piece individually. This can be of great help
   in speeding up download times.

- Are bitprints too long for short blocks like ours?
  (How long are the IDs going to be and whether 
  this will be a problem.)

   RESOLVED: Here's an example URI, 106 characters long:

     urn:urn-?:1.0:application/rdf+xml,QLFYWY2RI5WZCTEP6MJ
     KR5CAFGP7FQ5X.VEKXTRSJPTZJLY2IKG5FQ2TCXK26SECFPP4DX7I
  
   This is long, but IMO not 'too long.'

- Why the ``1.0``?

   RESOLVED: To have some kind of versioning information,
   e.g. if we have to change the hash functions because
   something is broken.

- Are the rules for escaping too complex? What's with all this
  escape this, don't escape that, quote this, don't quote that?

   RESOLVED: The important things to notice are that
   the common cases are simple (just a type, type plus charset),
   and that canonicalization is *really* easy. The other
   rules aren't that difficult, either, and they only
   apply in uncommon cases. It should be ok.

- Why this syntax? Why not another?

   RESOLVED: For similarity to ``data`` URLs.


Changes
=======

Storm blocks do not have headers any more; the hash in their URN
is only of the body. Block URNs have the following form::

    blockurn   := namespace "1.0:" [ mediatype ] "," bitprint
    mediatype  := [ type "/" subtype ] *( ";" parameter )
    parameter  := attribute "=" value

``namespace`` is an informal URN namespace to be registered,
like ``urn:urn-5``. Before it is registered, ``urn:storm:`` 
is used. ``bitprint`` is a Bitzi bitprint as defined
by <http://bitzi.com/developer/bitprint>; this means it's
32 characters, a dot, plus 39 more characters. 

The ``type``, ``subtype``, ``attribute`` and ``value``
tokens are specified by [RFC2045]. All characters not
in ``<URN chars>`` as defined by [RFC2141] MUST be
percent escaped [RFC1630], with one special exception:
The slash separating type from subtype MUST NOT be escaped.
This is for easier readability, and is consistent with
the use in ``data`` URLs [RFC2397] (it's also the thing
most likely to be struck down in the namespace
application process... but we can see whether it
gets through or not).

Block URNs are completely case-insensitive; they are
canonicalized by lower-casing them, character by character. 
Two block URNs are thus considered equal when compared
ignoring case.

To make this work, in case-sensitive ``values``, upper-case
characters MUST be percent escaped, since they are not allowed
in the canonical form. This is admittedly ugly, but 
case-sensitive ``values`` are rare. For parameters whose ``value``
is always a ``token`` as defined by [RFC2045] (for example
``charset``), ``value`` SHOULD NOT be enclosed in quotation marks
(prior to percent escaping). For parameters whose value may
contain characters not allowed in ``token``, ``value`` SHOULD 
be enclosed in quotation marks. Quoting [RFC2045], ::

     token := 1*<any (US-ASCII) CHAR except SPACE, CTLs,
                 or tspecials>

"X-" types aren't allowed, as they work against the persistence
of Storm blocks; ``application/octet-stream`` or similar
must be used instead. There is an internet-draft 
[draft-eastlake-cturi-04] on the use of URIs as MIME types; 
if this becomes standard, it should be used for extension.

Unlike in [RFC2397], if no ``<mediatype>`` is given,
``application/octet-stream`` is assumed (not ``text/plain``).

There is a public domain Java implementation of bitprints at
<http://cvs.sourceforge.net/cgi-bin/viewcvs.cgi/bitcollider/jbitprint/>.
Bitprints may be registered as a URN namespace in the future,
according to Bitzi. However, they will not include a
content type.

\- Benja
