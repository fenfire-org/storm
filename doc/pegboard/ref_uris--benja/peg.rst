==========================================================================
PEG ref_uris--benja: Storm blocks with metadata
==========================================================================

:Author:  Benja Fallenstein
:Created: 2003-08-18
:Changed: $Date: 2003/08/19 06:00:18 $
:Status:  Current
:Scope:   Major
:Type:    Architecture


Storm blocks are very simple, just a sequence of octets paired with a
MIME media type. There are good reasons for this design, explained
elsewhere.

However, sometimes you want to include more information with a document,
for example the author; a title; copyright information; the creation date;
the natural language; licensing information (e.g. copyleft or micropayment)
that can be automatically processed by your computer; other files that
should be downloaded if you download this one, e.g. images used on a
Web page; for audio data, the artist and album; for an image,
where it was taken, what is shown on it, and an alternative description
for the blind. The list goes on.

Also, you may want to create something more complex than a document
represented by a single octet stream. For example, a Web page may be
available in different languages, and an image may be available as both
``image/png`` and ``image/svg+xml``.

This PEG proposes an extensible architecture that allows for all this.


Issues
======

.. None yet.


Reference URIs
==============

So far, we have used one type of URI in Storm: Block URIs,
preliminarily of the form::

    urn:x-storm:1.0:<media-type>,<hash>

This PEG introduces a new kind of URI, for now of the form::

    urn:x-storm:ref:1.0:<hash>

where ``<hash>`` is the hash of a Storm block in `NTriples format`_.
This block contains *metadata about*
the resource identified by the ``ref`` URI.

.. _NTriples format: http://www.w3.org/TR/2003/PR-rdf-testcases-20031215/#ntriples

It follows that the metadata graph itself has a URI in Storm, namely::

    urn:x-storm:1.0:text/plain,<hash>

This graph defines authoritative metadata for the ``ref`` URI.


How to make statements about the ``ref`` URI
============================================

Even though metadata blocks are in NTriples format, there's
one part about it that we interpret specially. The problem is
how to make statements about the resource identified by the ``ref`` URI,
because that URI contains the hash of the graph-- and without
breaking a hash function, it is not possible to create a file
that contains its own hash.

The solution is simple. In the NTriples file, we use
a special anonymous node, ``_:this``, instead of the ``ref`` URI.
When reading the graph in, ``_:this`` is replaced by that URI.
So, for example, we could state the triples ::

    _:this  dc:author   <http://example.org/~alice>.
    _:this  cc:license  <http://www.gnu.org/licenses/gpl.html>.

which would be read in as::

    <urn:x-storm:ref:1.0:<hash>> dc:author  <http://example.org/~alice>.
    <urn:x-storm:ref:1.0:<hash>> cc:license <http://www.gnu.org/licenses/gpl.html>.

(``cc`` is Creative Commons, and ``dc`` is Dublin Core.)


Documents
=========

Now we have a way to store arbitrary metadata *about* our document--
but how do we tell Storm what the *content* of our document is?

For this, we use a special RDF vocabulary:

    _:this  refuri:resolutionMethod  refuri:StaticRepresentation
    _:this  repr:instance            <urn:x-storm:1.0:<type2>,<hash2>>

The first triple tells Storm the method to use
to find a representation of this document.
``refuri:StaticRepresentation`` means that the representation
is specified through a vocabulary introduced in this PEG.
More on resolution methods below.

The second triple tells Storm that when the user requests ``_:this``
(i.e., the resource denoted by the ``ref`` URI), then
Storm can serve ``urn:x-storm:1.0:<type>,<hash2>``.

The ``repr`` is for "representation."

In the Web architecture, there are *resources*, denoted by URIs;
for example, "The home page of Amazon, Inc.," or "An image of
Sandro Hawke's dog, Taiko."

These resources can have multiple *representations*, octet streams
with media types and other metadata. For example, the home page
can have versions in English and French; the image can be available
in JPEG or PNG.

A triple with property ``repr:instance`` says that the subject
is some sort of "document"-- both the home page and the image are
documents, but the city of Hameln or the Fenfire project are not--
and that the object is one representation of this document.

Or, maybe more precisely, as the object is also a resource, not a
representation: The subject is some sort of document, and
all representations of the object are also representations of the subject.

The object may be a Storm URI or any other kind of URI; a Storm
implementation is not obligated to support anything else but
Storm URIs, though. (In fact, it might warn the user when a ``ref`` URI
is used to refer to e.g. an HTTP page.)


Alternative representations
===========================

A document may have multiple, alternative representations::

    _:this   refuri:resolutionMethod  refuri:StaticRepresentation
    _:this   repr:instance            <urn:x-storm:1.0:<type1>,<hash1>>
    _:this   repr:instance            <urn:x-storm:1.0:<type2>,<hash2>>

A Storm implementation can then serve either of these as the document.

Additional triples can be used to describe these representations further::

    <urn:x-storm:1.0:<type1>,<hash1>>  mime:mimeType  "image/png"
    <urn:x-storm:1.0:<type1>,<hash1>>  img:height "100"
    <urn:x-storm:1.0:<type1>,<hash1>>  img:width  "200"

    <urn:x-storm:1.0:<type2>,<hash2>>  mime:mimeType  "image/png"
    <urn:x-storm:1.0:<type2>,<hash2>>  img:height "500"
    <urn:x-storm:1.0:<type2>,<hash2>>  img:width  "1000"

    <urn:x-storm:1.0:<type3>,<hash3>>  mime:mimeType  "image/svg"

Given this, a Storm implementation which understands the ``img``
and ``mime`` properties could pick either the low or the high resolution
version of the image, or the scaleable SVG version, if supported
by the client.

An HTTP gateway can use this kind of information to perform
content negotiation, selecting one of the alternative versions
depending on the client's ``Language`` and ``Accept`` headers.


Abstract concepts
=================

While ``block`` URIs always identify an octet stream with a media type,
a ``ref`` URI can be used to identify dogs, cars, houses, an RDF class
or the theory of relativity: *Anything*.

Of course you can also use ``urn-5`` for that, but sometimes it is useful
to be able to get some authoritative information about a resource--
the ability for a human to put a URI into a browser and get documentation
about what it identifies, and the ability for a machine to resolve a URI
and get some machine-readable information about it. For example, the
``ref`` block for an RDF class could include a human-readable label
for the class as well as its superclasses, and refer to some human-readable
documentation.

(Fenfire could then, when the class is used in some graph, download
its authoritative description and use the human-readable label from that
description to show the class.)

In order to be able to put an abstract concept ``ref`` URI in a browser
and have it resolve to some documentation about the concept, we have
to associate it with a representation. For this, we do not use
``repr:instance``, because a description of a concept is not an
*instance*, a *version* of that concept. Instead, we use ::

    _:this   refuri:resolutionMethod  refuri:StaticRepresentation
    _:this   repr:description         <urn:x-storm:1.0:<type>,<hash>>

In general, there should only be one ``repr:description`` associated
with a resource, although the implementation should treat
``repr:description`` the same as ``repr:instance``. If the description
needs to be available in different languages or something like that,
it should have a ``ref`` URI itself.

This is because on the Web, important resources should have their own
URIs so that you can link to them and make statements about them--
you want to be able to make statements about both the theory of relativity
and the Web page that describes this concept.


Resolution methods
==================

In all of the examples above, we have used the resolution method
``StaticRepresentation``:

    _:this   refuri:resolutionMethod  refuri:StaticRepresentation

This triple tells Storm to employ the resolution method described above,
using the ``repr:`` properties. A resource can only have one
resolution method. 

When an implementation of Storm 
tries to resolve a ``ref`` URI to a representation,
what exactly it does is specified by the resolution method.
If an implementation doesn't understand a resolution method,
it must issue an error that indicates this.
This PEG specifies only the method ``refuri:StaticRepresentation``,
as specified above.

The point of having resolution methods is to be able to also use
``ref`` URIs to identify pointers, which will be resolved through
a different mechanism than the one described above,
and possibly experiment with things like having a resolution method
that generates a representation client-side, using an XSLT transformation.


Vocabulary defined in this PEG
==============================

This PEG defines the following URIs:

http://purl.oclc.org/NET/storm/vocab/ref-uri/resolutionMethod (``refuri:resolutionMethod``)
    A property. The subject of triples with this property is a
    resource that can be resolved to a representation, e.g. to show
    in a browser. The object is an abstract resource specifying
    how to find or generate this representation. If a Storm implementation
    does not understand the resolution method specified
    in the authoritative metadata of a resource,
    it must signal an error.

    See the section on resolution methods, above.

http://purl.oclc.org/NET/storm/vocab/ref-uri/StaticRepresentation (``refuri:StaticRepresentation``)
    A resolution method. To resolve a resource R which has this
    resolution method, an implementation:

    - looks at the graph of RDF triples that forms the authoritative
      metadata of R (in the case of a ``ref`` URI, the associated
      NTriples block);
    - computes the set of triples in that graph that have R as the subject
      and one of ``repr:representation``, ``repr:instance`` 
      and ``repr:description`` (all defined below) as the property;
    - selects one of the objects of these triples (the algorithm
      used for selecting is up to the implementation);
    - resolves that resource, and uses the representation
      thus obtained as the representation of R.

http://purl.oclc.org/NET/storm/vocab/representations/representation (``repr:representation``)
    A property. The subject is any resource, and the object is a
    representation of that resource; or more precisely, all representations
    of the object are also representations of the subject.

    If included in the authoritative metadata about the subject, a
    URI resolver that understands this property shall consider
    the object of this property as one possible document that can be
    served as a representation of the subject.

    In particular, when a ``ref`` URI is e.g. entered into a browser,
    a URI resolver shall look at the ``ref`` block for triples
    of the form::

        _:this  repr:representation  _:foo

    where _:this is the resource represented by the ``ref`` URI.

    The objects in these triples (``_:foo``) are the possible
    representations of the resource (``_:this``).


http://purl.oclc.org/NET/storm/vocab/representations/instance (``repr:instance``)
    A property. Both the subject and the object are some kind of
    "document," something which can be serialized to bits and bytes.
    The object is some kind of specialization of the subject.

    For example, the subject might be "The Bible," and the object
    might be "The Bible, King James' Version," which is more specific.
    Or, the subject may be "An image of Sandro Hawke's dog Taiko,"
    and the object may be a PNG or JPEG version of that image.

    This is a sub-property of ``repr:representation``. A URI resolver shall
    treat a triple with this property like a triple with property
    ``repr:representation``.

http://purl.oclc.org/NET/storm/vocab/representations/description (``repr:description``)
    A property. The subject is any resource; the object is
    some kind of "document" which describes the subject.

    For example, the subject may be an RDF class, and the object
    may be a Web page describing how this class is used.

    This is a sub-property of ``repr:representation``. A URI resolver shall
    treat a triple with this property like a triple with property
    ``repr:representation``.

No other properties besides the three above shall be treated
the same as ``repr:representation``, even if some graph states that
they are a subproperty of ``repr:representation``. This is to make
resolution of ``ref`` URIs easier.


What this PEG does not define
=============================

This PEG doesn't define any "standard" properties for use inside a
``ref`` graph, besides the four used above. Other PEGs may define
properties to specify e.g. the languages or media types of representations,
and dictate resolver behavior in the presence of these properties,
for example honoring the ``Language`` header in HTTP requests.
However, this is left for future specifications.

\- Benja 