==========================================================================
PEG vnd_storm_uris--benja: ``vnd-storm-*`` URIs for Storm
==========================================================================

:Authors:  Benja Fallenstein
:Date-Created: 2004-01-30
:Last-Modified: $Date: 2003/07/28 13:53:51 $
:Revision: $Revision: 1.4 $
:Status:   Incomplete

:Stakeholders:
:Scope:    Minor
:Type:     Protocol


We currently use ``urn:x-storm:`` URIs. This is not good,
because the ``x-`` means they're experimental; we want to really 
start using Storm now, and we want to make the persistency commitment
sometime soon so we know that our data will be save, and because
these are defined to be experimental we can never register them,
so it would be really bad to have to support them for "forever."

Additionally, there has been some `discussion`__ lately about
what should be a URN and what should be not, casting doubt on
whether we can register Storm URIs as URNs.

__ http://lists.research.netsol.com/pipermail/urn-nid/2003-September/000389.html


Issues
======

- Should it be ``vnd-storm-hash`` or ``vnd-storm-block``?

  RESOLVED: ``hash`` is easier to understand for outsiders,
  and ties in with the `proposal for hash URIs`__, so there
  is good precedence for this.

  __ http://lists.research.netsol.com/pipermail/urn-nid/2003-August/000373.html


Changes
=======

I propose to switch to ``vnd-storm-hash:`` instead of ``urn:x-storm:1.0:``
(for blocks), and ``vnd-storm-ref:`` instead of ``urn:x-storm:ref:1.0:``
(for references).

The part of the URI after the prefixes remains unchanged.

Easy registration of URI schemes of the form ``vnd-*`` is proposed
by `draft-king-vnd-urlscheme-03`__. (It changes the ``vnd.`` syntax
of earlier drafts, which was also used by a previous version of this PEG,
to ``vnd-``.

__ http://larry.masinter.net/vndurl.html

