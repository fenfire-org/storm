===========================================
Digital archives without patented techology
===========================================

:Author:    Benja Fallenstein <b.fallenstein@gmx.de>
:Created:   2003-07-17
:Modified:  $Date: 2003/07/17 17:17:31 $

The Web isn't made to last. If you browse a mail archive
from 1999, many links have already ceased to work.
How can we stand on the shoulders on giants if those
shoulders are rotting away?

The Internet Archive is a good thing, but it would
be even better if links to documents would continue
to work normally as long as there is *anybody*
on the network who still has a copy. With P2P lookup
services, we can do that-- if we can authenticate
old documents. We want to know what *really* was at
``foo.com`` in 2003, not what somebody we don't even know
only claims was there. We need a new addressing scheme
for Web pages that continues to work after the publisher
has stopped publishing the page.

We could use digital signatures: Any page signed
by the original publisher would be acceptable.
But digital signatures are only valid as long as
the key that gave them hasn't expired-- two years
are often recommended. To solve this problem, timestamping
is used. But digital timestamping technology is
heavily `patented`_.

.. _patented: http://www.surety.com/patents.php

This idea describes a lasting digital verification
system which provides a service similar to digital
signatures, but (AFAIK) doesn't use patented techology.



Introduction
============

First off, I should probably mention that my main
motivation for this writeup is not to get people
excited about it, but to prevent this from being
patented, even though I'm certainly trying to make
it interesting to read. ;-) 

I'm also not a
professional cryptologist; I'll be gladful to hear
of all security holes, fundamental or in details,
that people discover in this system.

With that out of the way, I'll first briefly
describe the "classic" way of archiving digital
signatures, based on patented technology. 

This discussion
is useful for two reasons; firstly because it allows
me to show how my system appears to be just as secure
(because possible attacks on my system have equivalents
in the "classic" system), and secondly because
it's good to know how close my system is technically
to a timestamping system, even though it doesn't do
timestamping (and thus doesn't infringe on the
patents!).

But if you want, you can `skip over`__ this part.

__ #digital-signatures-without-timestamping


Public-key infrastructure
-------------------------

First off, cryptographic keys don't last forever,
for two reasons. First, your private key can be
stolen, e.g. by someone hacking into your computer.
Second, with most signature algorithms, if you give
a lot of signatures with the same private key,
it becomes less expensive for an attacker to compute
the private key from the signatures.

Therefore, there must be a way for every participant
in the system to *revoke* an old key (say that it's
not valid any more) and to acquire a new key.

Because of this, it's not a good idea to put
public keys in document identifiers; we want
the identifiers to remain valid even after the
public keys have been revoked.

Instead, we have *certificate authorities* (CAs).
A CA binds a public key to a name that is unique
inside the CA's domain. 

For example, we could have
a document identifier ``addr:foo/bar/baz``, 
indicating a hierarchy. First we would look
for a certificate for ``foo``, signed by the
root CA. Then we would look for a certificate
for ``bar``, signed by the public key
indicated in the previous certificate. Then
we would look for a version of ``addr:foo/bar/baz``
signed by the key from the ``bar`` certificate.

(A CA also publishes a Certificate Revocation
List (CRL), which is a list of certificates
for which the certified keys have been revoked.
A verifier can be sure that 

The root CA's public key needs to be transmitted
out-of-bounds; for SSL (``https:`` URIs), it's
often embedded in the code of your browser.

When a key is revoked, the key holder can ask
their CA to issue a new certificate with a new
public key. The key holder can then continue
to sign messages. However, signatures given
with the old key will no longer be valid.


Timestamping
------------

To solve this problem, digital timestamping
is used. A timestamping service (TSS) is an entity
you can submit documents to, and get back a
certificate that the given document existed
at a given time. If a signature is timestamped,
you can prove that it existed before the key
that gave it was revoked.

There are a number of different ways to
timestamp a digital document. Here is a simple
way, which isn't patented (there was a patent,
but it has been overturned):

    There is a Trusted Third Party (TTP) which is
    trusted by everybody. To obtain a timestamp,
    submit a cryptographic hash of your document to the TTP.
    The TTP will sign a statement like, "Hash H
    was submitted on 2003-07-10." Showing the
    TTP's signature proves that the document
    really existed on that date.

Of course, you're really putting a lot of trust
in the TTP, here: It can arbitrarily back-date
or forward-date documents.

Here's a much better system:

    There is a central timestamping service (TSS).
    To obtain a timestamp, submit its hash to the TSS.
    The TSS operates in a sequence of "rounds," say,
    one second long. It builds a Merkle hash tree of all
    documents submitted in one round. Showing the chain
    of hashes required to authenticate your document
    as part of the hash proves that your document
    existed during that timestamping round.

    Each round includes the hash of the previous round,
    so each round certifies the previous round. Once
    a week or so, the TSS publishes the current round's
    hash in a widely-witnessed manner (e.g., in an
    important newspaper). Thus, even if one doesn't trust
    the TSS or anybody on the network at all, the age
    of a document can be proven (well enough that it
    should stand up in court) with a resolution of
    one week (in the example).

Unfortunately, this one's patented.


Key archival
------------

The final component of the system is a Key
Archival Service (KAS) which stores the public key
of the root CA (or root CAs, if there is more than
one root) at any given time. Using the KAS, it is
possible to authenticate a past signature by
checking that all the necessary certificates
existed and were signed by keys that were correct
at the time.

I'll omit detailed discussion about how a KAS
could work. The notion of a KAS has been
`introduced relatively recently`__, in a
`FAST 2002`__ paper.

__ http://mosquitonet.stanford.edu/publications/FAST2002/
__ http://www.usenix.org/publications/library/proceedings/fast02/



Digital "signatures" without timestamping
=========================================

It's important to note that in a conventional
public key system, as described above, your
Certificate Authority can, if it acts maliciously,
sign arbitrary messages in your name:
It simply issues a certificate which binds your
name to one of its own keys, and uses this key
to sign messages in your stead.

In other words, you have to trust your CA
not to misrepresent you.

Based on this, I suggest a system in which you
appoint a *deputy*, an entity somewhat like a CA,
who tells others which messages you have
signed. Let's first examine a simplistic
implementation, similar to the simple timestamping
system above:

    There is a single deputy, a third party trusted
    by everybody in the system. To sign a document,
    the signer authenticates themselves to the TTP
    and submits the document. To verify, the
    verifier submits the document to the TTP and asks
    whether it was earlier submitted by the signer.

Again, clearly, the trust this system places in the
TTP is too high. Therefore, we extend it in a way
similar to the second timestamping system, above,
to ensure that the deputy cannot take back anything
that it has said before.

As with the timestamping service, we let the deputy
operate in rounds. In every round, the deputy 'signs'
statements of the form, "Signer S has signed message M."
The deputy builds a hash tree over these statements,
and publishes the root as its current round value.

A signature consists of all information necessary
to prove that the statement "S signed M" is part
of the hash tree of any round published by the deputy.
Verification consists of retrieving the appropriate
published round value and checking that "S signed M"
is part of that hash tree.

Additionally, every round can include the
hash of the previous round. Verification then
consists of retrieving the current round value;
retrieving the previous round records
(not necessarily from the deputy itself)
and verifying them against the current round value;
and verifying the signature against the relevant
previous round value.

This way, the deputy cannot "undo" a previous round
without also undoing all rounds after it,
because all later rounds include the earlier round
by reference.

While timestamping with catenate certificates
(rounds including the previous round's hash)
is `patented`__, the claims on this patents are
on "method[s] of certifying the temporal sequence 
of digital documents in a series of such documents"
or "method[s] of time-stamping a digital document."
Since we don't use this technology to time-stamp
documents or certify their temporal sequence--
we use it to provide digital signatures--, I believe
that the patent does not apply (I'm not a lawyer, though).

__ http://patft.uspto.gov/netacgi/nph-Parser?Sect1=PTO1&Sect2=HITOFF&d=PALL&p=1&u=/netahtml/srchnum.htm&r=1&f=G&l=50&s1=5136646.WKU.&OS=PN/5136646&RS=PN/5136646

In regular intervals,
the deputy may publish the current round's hash value
in a widely witnessed way. For example, it could
distribute a notarized (non-digital) document
to an international group of libraries, plus anybody
else who is interested and pays a nominal fee.

Then, it becomes truly difficult for the deputy
to deny a signature it has authorized at an
earlier time.

The `literature`__ contains numerous linking schemes
for timestamping certificates that are more
advanced than the simple concept of linearly
linked rounds of Merkle hash trees (`example`__).
Many of these could be trivially applied to
the above "signature" scheme as well.

__ http://www.tcs.hut.fi/~helger/crypto/link/timestamping/
__ http://www.tcs.hut.fi/~helger/cuculus/bula98.html

On the part of the deputy, two types of attack
are obviously possible:

- The deputy can sign arbitrary messages
  in others' names.
- The deputy could neglect to include one round's
  value into the chain of rounds. Then, these
  signatures would be as good as never given.

In the CA/TSS/KAS approach, equivalent attacks
are possible: A signer's CA can give signatures
in the signer's name, and the timestamping service
can neglect to include one round's value into
its chain of rounds, making signatures timestamped
in that round unverifiable after the corresponding
public key has expired.

Of course, the CA/TSS/KAS approach allows there
to be a hierarchy of CAs, instead of having
just a single CA for everybody. Similarly, there
could be a hierarchy of deputies, where every
deputy submits its round values to the parent
deputy for authentication (except the root deputy,
which behaves as described above).



Using Byzantine agreement to construct a trusted root
=====================================================

A problem with the CA/TSS/KAS approach is that
a certain level of trust in the timestamping service
is required; yes, you can verify securely that a
signature is from 1999 by verifying the timestamp
against the hash value published in a 1999 newspaper,
but usually you will not want to dig up old newspapers
before you can visit a web site. But if you rely
on the "current round value" as provided online
by the TSS, the TSS can cheat.

To rectify this situation, a system has been
`proposed`__ in which a group of timestamping
services from different administrative domains
(e.g., different nations) collaborate. In
this system, Prokopius, each participating
TSS publishes their own round values in a
"master" timestamping service, collaboratively
maintained by all the participating TSSes.
This may replace the publication of grounding
values in e.g. a newspaper.

__ http://www.arxiv.org/ps/cs/0106058

In Prokopius, the master timestamping service
operates in rounds which are comparatively long,
in the order of a few days to a week. The
participating nodes use a Byzantine agreement
protocol to agree on the value of each round;
due to the relative long rounds, the high costs
of Byzantine agreement protocols seem justified.

Each participant timestamping service would then
give out timestamps in rounds of a second or so,
but would "ground" its timestamps in the
"master" service maybe once a week.

A very similar system could be employed in the deputy
approach. There would be a hierarchy of
deputies, but the root deputy would be collaboratively
managed by the first-level deputies below the root,
which may come from a number of different
administrative domains.

A signer could then appoint a first-level deputy
that they trust, and be confident that the
first-level deputy will in turn ensure that
the Prokopius-like root deputy acts correctly.



Promises
========

The Prokopius-like root introduces one additional
problem: the long intervals between its rounds,
because until a signature can be verified
against one of the root's rounds, it cannot be
assumed to be trusted by every verifier in the
system. Therefore, it would take days to obtain
a globally verifiable digital signature.

To solve this, we can use conventional key-based
signatures. Before a signature has been
included in one of the root deputy's rounds,
the signer can issue a *promise* that
the signature will be included in the next
root round, and sign this promise with
a private key.

When the next root round is being formed,
the verifier can then ask the signer for
proof that it has really submitted the
signature for inclusion into the root round.
If the signer is unable to provide such proof,
the verifier may submit information to that
effect to the first-level deputies; then,
these deputies will include the signature
into the root round, basically as if it
had been submitted by the original signer.

Thus, a verifier can be sure that the
promised signature will eventually end up
in the next root round, provided that
it "keeps an eye on it."

Of course, the verifier may not be online
during the formation of the next root round,
and thus unable to ascertain the signature's
inclusion into the round. However, they can
easily delegate the task to a trusted
system which *is* "always on" (maybe
the verifier's own deputy). This trusted system
could then, for example, send the whole
signature information to the verifier
by e-mail when the signature has entered
the chain of root values.


Key management
--------------

For the promise system to work, it must be
possible to associate public keys with signers.
To do this, we can let a signer's deputy
double as its CA.

(It should be noted that while we use
public-key cryptography, we only need
key-based signatures for the duration
of a single root round; because we don't
use them in archival storage, we don't
need a timestamping system or Key Archival Service.)

To function as a CA, a deputy must both
be able to publish both certificates and a
Certificate Revocation List. 

Both of these functions could well be handled
through the normal signature system, as
already described; the deputy would simply
create certificates and CRLs and sign them
like any other message.

The deputy could try an attack in which it
signs two different CRLs in the same round,
presents one CRL to the verifier in an attempt
to convince them that a certain key is valid,
and later present a second one to the
root deputy to convince it that the key wasn't
valid at all.

If the verifier keeps a copy of the CRL
for every promise it is checking, this isn't
a problem, though. The verifier can show its
version of the CRL to the root deputy, which
will conclude that the deputy made the
verifier believe the signature was valid,
and will therefore include it in the next
root round.

It should be noted that if a key is revoked
in one round, promises given with this key
in the same round should be considered valid.
Otherwise, a signer could simply give a promise
and revoke their key immediately after,
subverting the promise system.



Final remarks
=============

I'd like to repeat that while I believe this system
not to be covered by the Surety patents, I am not
a lawyer. I would like to hear others' opinions
about this matter.

I haven't done a thorough security analysis
on this system, at least not yet, because
I wanted to get a note about it out as soon as
possible in order to establish prior art
in case anybody tries to patent it.

I would be happy to hear about any problems with
my system that I have not considered.