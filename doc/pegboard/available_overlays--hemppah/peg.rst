==========================================================================
PEG available_overlays--hemppah: Available overlays
==========================================================================

:Authors:  Hermanni Hyytiälä
:Date-Created: 2003-06-18
:Last-Modified: $Date: 2003/08/01 09:14:03 $
:Revision: $Revision: 1.41 $
:Status:   Current
:Scope:    Major
:Type:     Implementation
:Stakeholders: tjl, benja


The current implementation of GISP seems to have (too) many obvious security 
exploits. Therefore we need to examine other available structured overlays which 
have an open source implementation in the hope to find a more mature P2P platform. 
After reviewing available open source overlays we propose that we shall use Tapestry 
as a P2P platform with Storm/Fenfire. 

The list of implemented overlays is as of July 2003.

Issues
======

None.

Terminology
===========

This section briefly covers the terminology used in this document. 

Abstractions
    The following text for the abstraction definitions is taken from 
    `Towards a Common API for Structured P2P Overlays`_ by Frank Dabek et al.

.. _DHT: 

    DHT
        The DHT abstraction provides the same functionality as a 
        traditional hashtable, by storing the mapping between a
        key and a value. This interface implements a simple store
        and retrieve functionality, where the value is always stored
        at the live overlay node(s) to which the key is mapped by
        the KBR layer. Values can be objects of any type. For example, the DHT 
        implemented as part of the DHash interface in CFS stores 
        and retrieves single disk blocks by their content hashed keys.

.. _DOLR:

    DOLR
        The DOLR abstraction provides a decentralized directory
        service. Each object replica (or endpoint) has an
        objectID and may be placed anywhere within the system.
        Applications announce the presence of endpoints by publishing
        their locations. A client message addressed with
        a particular objectID will be delivered to a nearby endpoint
        with this name. Note that the underlying distributed
        directory can be implemented by annotating trees associated
        with each objectID; other implementations are possible.
        One might ask why DOLR is not implemented on
        top of a DHT, with data pointers stored as values; this is
        not possible because a DOLR routes messages to the nearest
        available endpoint providing a locality property not
        supported by DHTs. An integral part of this process is the
        maintenance of the distributed directory during changes
        to the underlying nodes or links.
	
	
Note that DHT and DOLR can be seen as "equal" abstractions, i.e., either 
one doesn't rely on other: DHT can be implemented without relying DOLR and vice 
versa.

    

Activity of development
    How actively a software is being developed.

Developer
    Who is developing a software.

Language
    What programming language is used to program a software + additional
    software packages which are required.

License
    Under what license a software is distributed.

Other notes
    Other miscellaneous notes.
    
    
Additionally, we will mention the community that is developing a software under a
"Developer" section. THe two choices are:

1) "software engineering community", i.e., a regular free software project.

2) "research community, i.e., a research group that consist one or more researcher 
   who developes a software

Implemented overlays
====================

In this section we list the structured P2P overlays which have an open source
implementation. Please note that the description of each implementation's features 
is deliberately kept as short as possible. For more in-depth information about the 
overlays we suggest reading the original publications.

Chord
-----

Homepage
http://www.pdos.lcs.mit.edu/chord/

Abstraction
    DHT_/DOLR_

Redundancy
    Replication, backup links.

Fault tolerance against hostile nodes
    Not known.
    
License
    BSD-like
    
Language
    C++ (GCC 2.95, not 2.96, although 3.x should work)

Activity of development
    Quite high.

    According to the Chord website:

        At this point no official release for 
        Chord is available, but you are welcome to check out the latest version 
        from the CVS repository. This version is experimental, under active 
        development, and may frequently be broken.

Developer
    MIT (research community)

Additional requirements
    * Self-certifying File System (http://fs.net)
    * autoconf, automake, etc.
    * Berkeley DB 3.X 

Other notes
    No support for network locality- does not take network latencies into account
    when building neighbor links.

    Includes a Chord simulator.
    
    
    

Tapestry
--------

Homepage
http://www.cs.berkeley.edu/~ravenben/tapestry/

Abstraction
    DOLR_

Redundancy
    According to `Tapestry: A Resilient Global-scale Overlay for Service
    Deployment`_:

        Tapestry is highly resilient under dynamic conditions,
        providing a near optimal success rate for requests
        under high churn rates, and quickly recovering from
        massive membership events in under a minute.
    
    According to the Tapestry website:

        Tapestry offers fault-resilient mechanisms for both object 
        location and point to point message delivery.  For object location, 
        Tapestry generates a small number of deterministic and independent 
        GUIDs for each object.  An object server publishes an object's 
        availability on all of its GUIDs, and a client issues Tapestry locate 
        requests for all of the object GUIDs in parallel.  This greatly 
        increases availability under fault conditions, while improving 
        general performance and reducing performance variability.  For point 
        to point message delivery, Tapestry provides pre-calculated backup 
        routes at each overlay hop.  UDP soft-state beacons measure up-to-date 
        link conditions. Tapestry uses such information and simple fault-avoidance 
        protocols to route messages around failures, providing successful 
        delivery with high probability if a path between the endpoints exists.

    
Fault tolerance against hostile nodes
    * PKI is used while creating node identifiers (to prevent `Sybil attacks`_)
    * MACs are used to maintain integrity of overlay traffic (to maintain integrity 
      of overlay traffic)
    * Monitoring system for maintaining neighbor links (reduce packet loss/improve 
      message delivery in the overlay)
   
License
    BSD-like

Language
    Java (Sun JDK 1.3 or a compatible Java Development and Runtime environment).
    The Java interface libraries for the BerkeleyDB database
    
Activity of development
    Active.

    Tapestry  1.0 (April 2002)

    According to the Tapestry website::

        Tapestry Version 1.0 contains the following functionality:

        * Basic Tapestry Java code running on the SEDA stage-driven event model
        * Tapestry node insertion
              o Using a static configuration built from configuration files
              o Using the dynamic iterative insertion of single nodes to existing 
                Tapestry networks
        * Support for multiple backup links per route entry
        * Object location
              o Object publication with optional tags
              o Object location with optional tags
              o TapestryFailure messages returned upon the failure of a  
                TapestryLocateMsg
        * Message routing
              o Routing messages to an exact GUID match
              o Routing messages to the node closest in ID to a given GUID
        * An initial self-optimizing componentthat monitors link conditions to 
          decide when to use backup routes   
      
      
     According to the Tapestry website, Tapestry Version 2.0 contains the 
     following new functionality::

        * Algorithms for adapting to changing network conditions
              o API messages allowing the application to tell the local node 
                to enter or leave a Tapestry overlay
              o Recovery algorithms to large scale failures in the network
              o A resilient parallel insertion algorithm that supported large flash 
               crowds adding themselves to the overlay
        * Patchwork: a soft-state network monitoring component that uses 
          soft-state beacons to monitor link quality to a node's neighbors. 
          As link qualities change, nodes can be replaced by backups in the 
          routing table in order to reduce packet loss in overlay routing.
        * A distributed testing framework: nodes can set themselves to be 
          configured as FullTestMember nodes, and await explicit control 
          instructions from a central FullTestDriver node. Instructions are 
          sent via FullTestMsg messages, and include join, leave, publish, 
          routeToObject and routeToNode operations. The results and resulting 
          latencies are returned to the FullTestDriver.
        * Interweave: a simple file sharing application
        * Shuttle: a decentralized instant messaging protocol 

Developer
    University of Berkeley (research community)

Additional requirements
    * The Cryptix JCE library (included with the 2.0 release)
    * UNIX make program
    * The Java interface libraries for the BerkeleyDB database 
      (included with the 2.0 release) 

Other notes
    Support for network locality when building neighbor links.

    Why Oceanstore_ uses Tapestry ? 
    See http://www.oceanstore.org/info/whytapestry.html 

Kademlia
--------

Homepage
http://kademlia.scs.cs.nyu.edu

Abstraction
    DHT_
    
Redundancy
    No simulation or test results published (not even in the original publication).
    In *theory*, however, the "free-choice" feature
    gives peers freedom to adapt different conditions. However, the
    author of SharkyPy says:
    
        Kademlia has (in my taste, that's why I decided to drop it) a bad
	hole which makes it's use in remote querying for packets pretty useless:
	When you have key->value mappings, which have an equal key, in even in
	semi-large networks it gets very unprobable that you get all mappings,
	the more hosts there are, the less probable it is, and the more mappings
	there are, the less probable it is too. I've tried to remedy this in a
	later implementation, at the cost of lookup speed, but have never
	managed to get all entries when the network had over 100 nodes. (the
	later implementation is based on my own server-framework and uses no
	threads, btw.) This made it unusable for me, as basically I basically
	had to change the lookup-algorithm to query all nodes (back to gnutella,
	then...), to get all answers. And that's what is important in the
	network I designed it for.


Fault tolerance against hostile nodes
    Nothing said, expect the "free-choice" feature.
    
License
    GPL (Java), "Free for non-commercial use" (C++)
    
Language
    Java (Sun JDK 1.3 or a compatible Java Development and Runtime environment (?))

Activity of development
    Java development discontinued, C++ version
    is under development.

Developer
    New York University (research community).

Other notes
    The implementation of the Java version is discontinued. 

Pastry
------

Homepage
http://research.microsoft.com/~antr/Pastry/

Abstraction
    DHT_
    
Redundancy
   Backup links.

Fault tolerance against hostile nodes
   According to the release notes of 1.2, "Security support does not exist in 
    this release. Therefore, the  software should only be run in trusted 
    environments. Future releases will include security."

License
    BSD-like license (Java), MSR-EULA (C#)
    
Language
    Java (requires a Java runtime version 1.4), C# (not known)

Activity of development
    Active
    
    Current release is 1.3 (July 23, 2003)    

Developer
    Microsoft Research and Rice University (research community)

Other notes
    Support for network locality - Pastry actively replicates the objects and 
    places them at random locations in the network. Result: When locating
    nearby object it might require the client to route to a distant replica of 
    the object.
    
    According to the Pastry website:

        Future releases will address efficiency, 
        security, interoperability and will provide additional 
        application components like a generic distributed 
        hash table implementation, replica management, 
        caching, etc.

GISP
----

Homepage
http://gisp.jxta.org/

Abstraction
    DHT_/DOLR_

Redundancy
    Chord-like (since GISP uses similar routing tables as Chord)

Fault tolerance against hostile nodes
    Based on our own initial experiments: the fault tolerance
    is relatively weak - no specific techiques used.

License
    Sun Project JXTA License Version 1.1
    
Language
    Java (requires a Java runtime version 1.4)

Activity of development
    Quite active.

Developer
    Daishi Kato (software engineering community)

Other notes
    Uses 10x more cache as Chord for routing table.

    Includes a GISP simulator.

Circle
------

Homepage
http://thecircle.org.au/

Abstraction
    DHT_

Redundancy
    Not-known

Fault tolerance against hostile nodes
    According to Info-Anarchy Wiki:

        Problems are: The DHT implementation is 
        vulnerable to denial of service attacks.

License
    GPL

Language
    Python (version 2.0 or higher, 2.2 preferred, GTK+-2 and PyGTK)

Activity of development
    Active, the current version is 0.35 (30 May 2003)

Developer
    Paul Harrison (software engineering community)
 
Other notes
    Uses MD5 hashes for generating IDs.
    

Khashmir
--------

Homepage
http://khashmir.sourceforge.net/

Abstraction
    DHT_ (Kademlia algorithm)

Redundancy
    Not known.

Fault tolerance against hostile nodes
    According to the authors:

        Note that Khashmir currently isn't very 
        attack resistant.

License
    MIT License
    
Language
    Python
    
Activity of development
    Not active
    
    The current version is "3 - Alpha" (2002-09-02)

Developer
    Four developers (software engineering community)

Other notes
    (none)

MLDonkey
--------

Homepage
http://www.nongnu.org/mldonkey/

Abstraction
    DHT_ (Kademlia algorithm)

    (MLDonkey is compatible with Overnet_ (non-open source implementation of Kademlia
    developed by a company), and Overnet claims that it does Kademlia and multisource 
    downloading).

Redundancy
    Not known (we can imagine that redundancy is relatively high since MLDonkey
    is widely deplyed)

Fault tolerance against hostile nodes
    Not "officially" known (we can imagine that fault tolerance is relatively 
    high since MLDonkey is widely deplyed).
 
License
    GPL
    
Language
    Objective-Caml (a language that compiler compiling)
    
Activity of development
    Very active

    The current version is 2.5-3 (May 26th 2003) 

Developer
    12 developers (according to Savannah's project page, software engineering 
    community)

Other notes
    Supported P2P networks include eDonkey, Overnet, Bittorrent,
    Gnutella (Bearshare, Limewire,etc),  Gnutella2  (Shareaza),  
    Fasttrack  (Kazaa, Imesh, Grobster), Soulseek  (beta),  
    Direct-Connect  (alpha), and  Opennap  (alpha).
 
    Networks can be enabled/disabled.

    Widely deployed in real life.

    Overnet is not a free specification ,i.e., change control is in the Overnet 
    company's hands.

    According to the `MLDonkey CVS source server`_ (check this_ too), MLDonkey  
    uses MD4 hashes for Overnet/EDonkey2K IDs::
     
        peer: an overnet peer in the following format:
        md4: the peer md4
        ip:  the peer ip
        int16: the peer port 
        int8: the peer kind/uptime/last seen ?
    
    Original Overnet uses MD5 (and 2^128 space) for nodeIDs and data items
    (http://www.overnet.com/documentation/how.html, 
    http://bitzi.com/help/locallinks)

SharkyPy
--------

Homepage
http://www.heim-d.uni-sb.de/~heikowu/SharkyPy/

Abstraction
    DHT_ (Kademlia algorithm)

Redundancy
    According to the author:

        "It is being used in real code located at Sprachenzentrum der Universität des 
        Saarlandes (http://www.szsb.uni-saarland.de/tandem), and has been running for 
        several months now. Stable."

Fault tolerance against hostile nodes
    No specific techniques used: "it should work as is"

Language
    Python

License
    LGPL
    
Activity of development
    The current version is 0.2b3 (16th February 2003)

    According to the post_ to the `python-list`_ by the author (posted 04 Feb 2003)::

        SharkyPy 0.2
        ------------

        SharkyPy is a library for creating a distributed hash table using
        Python. It uses the Kademlia-Protocol (http://kademlia.scs.cs.nyu.edu/)
        over XMLRPC.

        In constrast to alternatives like khashmir it does not need twisted or
        any other library, and can even run without a persistence backend (as
        long as the daemon is kept running). Persistence backends currently only
        exist for MySQL.

        SharkyPy has been coded to only run with Python 2.3 at the moment, as it
        uses some new features such as enumerate, etc. But it should only be a
        matter of time to make it backwards compatible.

        - easily integrated into any program.
        - uses a standard protocol which should also be able to run over a
          HTTP-proxy (this is still being worked on).
        - loosely based on the Kademlia Java reference implementation, taking
          features like concurrent node queries into account.
        - completely implemented without external dependencies.

        URL: http://www.heim-d.de/~heikowu/SharkyPy
        License: LGPL (the source doesn't state this yet)
        Categories: Networking

        Heiko Wundram (heikowu@ceosg.de or heikowu@heim-d.de)

        PS: Still looking for co-developers... :)
    
    and here's an another message_ (posted 17 Feb 2003)::

        The third public beta of SharkyPy has just now been released. This beta
        adds new functionality to SharkyPy in the following areas:

        1) Refactoring/Cleaning of most classes, and introduction of
           Borg-patterns to reduce overhead.
        2) Integration of (public-key) signatures into DHT-values, by using
           Andrew M. Kuchlings PyCrypto package.
        3) Protocol specification is transmitted with every RPC-call, so that
           future protocols won't break the program.
        4) Several other buxfixes/changes not listed here.

        This version should be considered even more beta than the previous
        versions, as most of the functionality has only been tested over a
        limited timespan (half a day). It works for me(tm). And it works solely
        when using a version of Python 2.3.

        The feature consolidation process will start, when several other
        amendments have been made to the source.

        A stable version 0.2 will be released in about two weeks. Anybody who is
        interested in trying out SharkyPy so far is encouraged to do so, and
        should send any bugreports to me.

Developer
    Sprachenzentrum der Universität des Saarlandes / Heiko Wundram (research community)

Other notes
    According to the author of SharkyPy:
    
        This implementation is heavily based on threads, which makes it
	pretty resource-intensive on the computer it is running on. I never
	intended it to serve more than 30-40 clients, so this didn't matter to
	me. When you use it as a P2P network backend, things will certainly look
	different.

Changes
=======

While reviewing the different features of open source implementations of 
structured P2P overlays we can conclude that Tapestry seems to be the most
mature currently available. Specifically, other implementations
lack of features w.r.t. redundancy and fault tolerance that the Tapestry
implementation currently supports. Other reasons for choosing Tapestry in order of 
importance:

- The license
- The activity of development
- The implementation language

As a result, we recommend using Tapestry's open source implementation for use in
Storm. For a detailed list of changes necessary to use Tapestry in Storm, see 
the `storm_with_tapestry--hemppah`_ (a pending PEG) PEG document.


.. _storm_with_tapestry--hemppah: ../storm_with_tapestry--hemppah/peg.gen.html
.. _Towards a Common API for Structured P2P Overlays: http://www.cs.berkeley.edu/~ravenben/publications/pdf/apis.pdf
.. _`Tapestry: A Resilient Global-scale Overlay for Service Deployment`: http://www.cs.berkeley.edu/~ravenben/publications/pdf/tapestry_jsac.pdf
.. _Overnet: http://www.overnet.com
.. _Oceanstore: http://www.oceanstore.org
.. _MLDonkey CVS source server: http://savannah.nongnu.org/cgi-bin/viewcvs/mldonkey/mldonkey/docs/overnet.txt?rev=1.1&content-type=text/vnd.viewcvs-markup
.. _this: http://savannah.nongnu.org/cgi-bin/viewcvs/mldonkey/mldonkey/src/networks/donkey/donkeyOvernet.ml?rev=1.4&content-type=text/vnd.viewcvs-markup
.. _post: http://mail.python.org/pipermail/python-list/2003-February/143876.html
.. _python-list: http://mail.python.org/mailman/listinfo/python-list
.. _message: http://mail.python.org/pipermail/python-list/2003-February/148394.html
.. _Sybil attacks: http://www.cs.rice.edu/Conferences/IPTPS02/101.pdf
