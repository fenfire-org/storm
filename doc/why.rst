==================================================
What's wrong about the current, client/server Web?
==================================================

:Author: Benja <b.fallenstein@gmx.de>

Several things, in my opinion.


Non-permanence
==============

The Web is a comprehensive 
digital library of everything published... last month. 
Going back only a few years, when you look at e-mail 
archives from 2000, many of the HTTP links have 
ceased to work.

How are we supposed to "stand on the shoulders of 
giants" if more of these shoulders slip out of existence 
every day?

Point in case: I'm working on a project that has had two 
trademark-induced name changes since then. Our old web 
page, http://gzigzag.org, has in the meantime been 
acquired by a domain grabber and turned into an 
advertisement for "teen porn." Sure, the homepage is 
`accessible through the Internet Archive`__,
but even that `only takes you so far`__.

__ http://web.archive.org/web/20001204160000/http://www.gzigzag.org/
__ http://web.archive.org/web/*/http://www.gzigzag.org/faq.html

The problem can be alleviated inside the existing 
infrastructure, as `Tim BL points out`__, but this means 
that pages will only be preserved if there's some 
big organization interested enough in them to put up 
the servers, do the paperwork, acquire the domains, etc. 
That interesting personal home page with notes about 
taking the train to Morocco which I linked to in my 
private diary *will* have gone "out of print" when I 
look at the diary again in five years.

__ http://www.w3.org/DesignIssues/PersistentDomains.html

My stance is that a web page should remain accessible 
as long as there is somebody who think it's worth 
keeping a copy around on their own hard disk, and not 
keeping it private. Links to it should work without 
using some special "archive" command or web site. 
With p2p and location-independent identifiers, 
that's possible.

Namespace integration with documents on the desktop
===================================================

Yes, there's file:, but there's 
no way to make a link to a file that I have and 
that you have-- but in different places of 
our respective file systems.

I want to be able to make links between desktop documents 
that continue to work when the documents are--

- moved between directories
- sent by mail
- published on the Web
- moved between Web servers
- downloaded
- edited
- sent back to the original authors in modified form

Web linking is used on the Web, but not on the desktop. 
I believe that one reason is the above-- when you make 
a link, it only works on your own desktop, not if you 
publish the document or send it by mail.

When I send a mail with a link, the link should work 
if the receiver can find a copy of the linked to document, 
no matter whether it's--

- a file on their desktop
- an attachment to another e-mail I sent to them
- published on a shared intranet
- published on the public Web.

A related issue is linking to e-mail; I should be able 
to link from one e-mail to another so that the receiver 
can follow the link if they either have the target 
e-mail in their own inbox, or if it is archived 
somewhere on the Web.

This needs location-independent URIs; P2P makes these 
resolvable on the public Web.


Building more advanced hypermedia structures
============================================

We need something better than the web to build more
advanced hypermedia structures on top of it.

As a simple example, why doesn't the Web show which 
other pages link *to* the page you are viewing? In the 
client/server architecture, there is no way to do this 
globally. For some thoughts about this from the dawntime 
of the Web, see `Building back-links`_ by TBL.

.. _Building back-links: http://www.w3.org/DesignIssues/BuildingBackLinks

More ambitiously, we are implementing `Xanalogical storage`_, 
hypertext as envisioned by the group of Ted Nelson, who 
introduced the word in 1965. In Xanalogical storage, each 
*character* you type (or pixel you photograph, etc.) has 
a global identity number. When you copy some characters to 
a different document, they retain their identities. Links 
aren't between documents, they are externally attached to 
characters (and thus to the documents where these characters 
appear). This allows you to--

- see which documents cite from a document, see where 
  a quotation is from;
- see which versions there are of a document (as long as 
  they have at least some text in common), 
  compare these documents;
- make an external comment on a document written by 
  someone else, which can be seen by the document's 
  readers (if they choose to show "all comments 
  made by anybody");
- make a link to one version of a document, see it in a 
  different (only slightly altered) version;
- make links to any portion of a document, without the link 
  breaking when the document changes;
- and others.

.. _Xanalogical storage: http://www.xanadu.net/xuTheModel/

This system needs two lookup primitives:

- Which documents quote this character (given a global ID)?
- Which links link to this character (given a global ID)?

To do this in a decentral, scalable, and reliable way, 
a p2p lookup scheme is necessary.


Conclusions
===========

Not everybody may agree that the above are reason enough 
to replace the current Web, but they are concerns 
that are important enough in my own use of the Web that I 
would be happy about having a replacement. 

\- Benja