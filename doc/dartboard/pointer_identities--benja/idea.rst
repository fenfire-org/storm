======================================
The problem with pointers and identity
======================================

:Author:  Benja Fallenstein <b.fallenstein@gmx.de>
:Created: 2003-07-10
:Changed: $Date: 2003/09/10 13:20:27 $

.. contents::

Introduction / Problem statement
================================

So I've been doing a *lot* of thinking over
the last few days regarding Storm pointers.

The basic idea was, and the current implementation
is, that a pointer URI contains a cryptographic
public key. Pointer blocks signed with the
corresponding private key would be valid.

This doesn't work for two reasons:

- Given enough time, it's feasible to obtain a
  private key from the public key; the recommendation
  is to change private keys after about two years.
- Private keys can be stolen.

This isn't really acceptable for either publishing
or storage on your own computer: Once your key has
been compromised somehow, no pointer URI using it
will work reliably any more. There's no way to
repair the problem, and there cannot be: You now
have exactly the same information as the adversary
who has obtained your key, and thus, they can do
any (cryptographic) thing that you can do.


Using a public key infrastructure (PKI)?
----------------------------------------

Okay, I have to admit that I don't know a hell of
a lot about PKIs. Here's a reference I found
which tries to explain X.509 certification 
in some depth:

    http://mcg.org.br/certover.pdf

The purpose of a PKI is normally to allow
a signature verifier to put trust into the signer:
they bind a key X to a person Y, so that the
user can say with confidence, a message signed
with key X was signed by person Y.

We don't actually need that. We simply need a way
to ascertain that a pointer block was signed
by the entity that "owns" the pointer (originally
the entity that created the pointer).

It seems that we need to use a non-cryptographic id
for the entity that created a pointer, and use
a non-cryptographic means to obtain a key that
is currently associated with that id.

A certificate authority (CA) could do that job.
Certificates can associate any kind of information
with a key-- certainly they can associate an id with it.
In fact, CAs following X.509 apparently must
include a "Distinguished Name" in every certificate
that must be unique in that CA.

However, there is a deeper problem: Certificates
as used in e.g. X.509 become invalid when the key
that has signed them becomes invalid.

(Now, if we had a good timestamping algorithm...
oh well.)


Key-based identity; hierarchical identity; axiomatic identity
=============================================================

Allow me to go a little theoretical and introduce 
three "types of identity" now. I'll keep it short.

Key-based identity:
    The identity of an entity is its public key.
    Anything signed with the matching private key
    is taken to come from that entity.

Hierarchical identity:
    A known entity can create "child" entities
    by assigning them names. The identity of a
    child entity is the parent's identity plus
    the name assigned by the parent entity.
    To know who the child entity is, ask the
    parent entity.

    Example: DNS; to know which host is
    ``io.it.jyu.fi``, ask ``it.jyu.fi``.

Axiomatic identity:
    An entity's identity is not concluded from some
    other information, but specified through
    some out-of-bounds means.

    Examples: The DNS root servers; file names
    on a hard disk (the entity corresponding to
    ``/home/benja/foo.txt`` is "axiomatically" stated
    by the bits on my hard disk, not concluded
    from some other information).

(I have a hard time describing the third category
well, so maybe it doesn't really make sense to
lump the DNS root and files on your hard disk
into the same category, but somehow I have this
feeling that they're similar in a fundamental way.
Hm. Comments appreciated.)


A first cut at the problem
==========================

So, let's examine how the above work out for Storm.

- We cannot use key-based identity for pointers because
  any private key can get exposed-- that's the motivation
  for this document.
- Using axiomatic identity would mean that for every
  entity signing pointers, we would have to establish
  out-of-bounds (manually) who they are, *before we can
  read any documents from them*. Clearly infeasible.
- So we're left with hierarchical identity, which works
  for DNS. (Of course, we need a root for the hierarchy;
  since key-based is out, its identity must be asserted
  axiomatically, as in DNS.)

A first cut:

- We have a root entity whose public key is specified
  through out-of-bounds means (e.g., "download from
  http://himalia.it.jyu.fi/pubkey").
- The root entity gives names to other entities and
  signs ``(name,pubkey)`` pairs with its own key.
- The other entities can do the same.
- Then, given a path like ``foo/bar/baz``, we can
  find out who ``foo`` is, according to the root;
  who ``bar`` is, according to ``foo``; and who
  ``baz`` is, according to ``foo/bar``.

When any of the keys is revoked, the corresponding
entity can ask its parent identity to sign a new key.
The root identity will need out-of-bounds means.

Clearly, a parent entity can misrepresent a child
entity-- ``foo`` assigns a key of its own to ``bar``
and can further on sign messages in ``bar``'s name.

It isn't the Storm-using Web surfer who needs
to put trust in the parent entity, then.
It's the child entity-- because the parent
entity can misrepresent the child entity.
In fact, an entity needs to trust *all* its
ancestor entities.

(Just like your web hosting company can take your
pages online and replace them by something else;
it's the web page author who needs to trust the
hosting company, not the person reading the page.)

In fact, I believe the problem *cannot* be solved
without having to trust somebody: Because you cannot
guarantee that your private key will not be exposed,
you need to give somebody else the right to assign
you a new key; which means you need to trust them
not to assign your key to someone else.


Non-repudiability
=================

Now, an important consideration here is 
*non-repudiability*: the inability to sign a message
today and say tomorrow, "No, it wasn't me!"

Repudiability is a problem in public-key cryptography
when keys expire oor are revoked. If anybody could
have a copy of the corresponding private key,
a signature given with it isn't worth anything-- even
if it was given *before* the key expired, because
we cannot prove that.

In our context, repudiability results in two problems:

- Web page authors can claim they never published
  a version of their page which they really *did*
  publish.
- Parent authorities deny the history of their
  child authorities. I.e., a parent authority changes
  the child authority's key to one that the parent
  authority controls; it doesn't sign the blocks
  that the child authority had signed, so all the
  blocks signed by the child authority are invalidated
  and all history of the data is lost.

Normally, timestamping is used to proof the validity
of signatures after the key was revoked.


How timestamping works
----------------------

The trick with timestamping is,
if signatures are timestamped, you can verify
that a signature was created *before* the key
was revoked.

Unfortunately, the good methods for timestamping
have been heavily patented (by `Surety, Inc.`__).

__ http://www.surety.com/

Here's a simple, patentless timestamping 
technology (there was a patent, but it's been
overturned):

    There is a Trusted Third Party (TTP) which is
    trusted by everybody. To obtain a timestamp,
    submit a hash of your document to the TTP.
    The TTP will sign a statement like, "Hash H
    was submitted on 2003-07-10." Showing the
    TTP's signature proves that the document
    really existed on that date.

Of course, the TTP could be fraudulent.

Here's a much better system:

    There is a central timestamping service (TS).
    To obtain a timestamp, submit its hash to the TS.
    The TS operates in a sequence of "rounds," say,
    one minute long. It builds a hash tree of all
    documents submitted in one round. Showing the chain
    of hashes required to authenticate your document
    as part of the hash proves that your document
    existed during that timestamping round.

    Each round includes the hash of the previous round,
    so each round certifies the previous round. Once
    a week or so, the TS publishes the current round's
    hash in a widely-witnessed manner (e.g., in an
    important newspaper). Thus, even if one doesn't trust
    the TS or anybody on the network at all, the age
    of a document can be proven (well enough that it
    should stand up in court) with a resolution of
    one week (in the example).

Unfortunately, this one's patented.


So what can we do?
------------------

Well, first of all, we need some way to keep signatures
valid even after the key that originally signed them
has expired (or been revoked) and been replaced by
a new key, without having to take every signature given
with the old key and giving it again with the new key.

The key, here, is to 