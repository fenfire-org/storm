
.. !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   !!! THIS PEG DOCUMENT IS CURRENTLY POSTPONED !!!
   !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

=================================================================================
PEG storm_gisp_simulation--hemppah: Storm P2P simulations using the GISP protocol
=================================================================================

:Authors:  Hermanni Hyytiälä
:Date-Created: 2003-06-02
:Last-Modified: $Date: 2003/06/05 10:51:39 $
:Revision: $Revision: 1.17 $
:Status:   Incomplete [POSTPONED]

.. :Stakeholders:
.. :Scope:    Major|Minor|Trivial|Cosmetic
.. :Type:     META|Policy|Architecture|Interface|Implementation

.. Affect-PEGs:


For determining whether Storm with unmodified GISP_ P2P protocol is practical and
useful, we want increase our understanding GISP's scalability properties. Also, we 
want to know how GISP performs against different threats such as network 
partition or security attacks. 

This PEG discusses research problems, hypotheses, the theoretical 
knowledge we have about the hypotheses, and possible simulations to 
validate hypotheses related to the GISP P2P protocol.

If not separately mentioned, in this context we mean with 
"Storm" as an entity which is able to do a (limited/simplified) number 
of Storm's functionalies.  

Issues
======

ISSUE:

    In the future, do we want to perform simulations in a LAN cluster (or 
    relevant) ?
  
    RESOLVED: Yes, if we want to simulate huge virtual networks and/or
    memory requirements are too massive for a single desktop.

ISSUE:
    
    For now, do we need "real" Storm servers or blocks during simulations 
    or not ?
 
    RESOLVED: No, since we want to make our simulator environment 
    as lightweight as possible (See issue #1). 
    
    
ISSUE: 

    Why GISP? Why are we using it versus some other systems?
    What properties does it share with others, to such a degree
    that its performance might be deduced from theirs?

    RESOLVED:
    
    Why GISP ? Why are we using it versus some other systems?
    
    - According to Benja, "...since it's written in Java" :)
    - Easy implementation (and it's already implemented)
    - (belief that) GISP is a Kademlia_ implementation (which
      is not true - only distance function is similar)
     
    It is good to mention that there is also a Python 
    implemenation of Kademlia called Kashmir_.
     
    What properties does it share with others, to such a degree
    that its performance might be deduced from theirs?
    
    GISP's routing table is based on Chord_'s routing 
    table -- O(log^2 n) messages are required to join/leave 
    operations and O(log n) for lookup efficiency (according to 
    the original Chord publication). GISP extends Chord's routing
    table to have more space for cached data (both the protocol
    specification and the original publication talks about 
    "peer information"). Chord's routing table maintains 
    information about O(log n) peers. 
    
    GISP publication does not (nor the protocol specification) 
    describe how much more space a GISP peer maintain compared 
    to Chord's O(log n). Instead, the protocol specification 
    states that "A peer should keep as much other
    peer information as possible for cache". Neither 
    the original GISP publication nor the GISP
    protocol specification do not provide any formal
    properties of the GISP lookup protocol. However,    
    in the publication the author states that GISP is more efficient
    than a "broadcasting system".    
       

ISSUE:
        
    What experiments / simulations / methods are used in the literature?
    
    RESOLVED:
    
    - mainly theoretical/best case experiments/simulations
    - small amount of uncontrolled (real life), great amount of
      controlled (simulation environment)
    - simulations have been rather static and are often favored the 
      proposed algorithm    
    - in "real life" experiments, there have been very fast connections 
      between peers
    - some form of formal analyses ("proofs") are used, 
      e.g., with Chord and Kademlia
    - distribution of number of peers used in simulations: from 2^7 to 2^20
    
    
ISSUE: 
   
    What is known or easily deducible about the effect of a single
    hostile node? Following variants:

    - Node just wants to make the network work slower

    - Node wants to make searches for some particular informations difficult

    - Node wants to make life difficult for some particular nodes,
      slowing down their queries

    - Node wants to make search for particular information by particular nodes
      difficult

    - Node wants to spam a particular identifier or xu block
    
    RESOLVED:
    
    Directly related to DHTs, there are two publications about this topic:

    "Security Considerations for Peer-to-Peer Distributed Hash Tables"
    by Emil Sit and Robert Morris

    "Security for structured peer-to-peer overlay networks"
    by M. Castro, P. Druschel, A. Ganesh, A. Rowstron and and D. Wallach

    Both papers discusses scenarios mentioned above, e.g., Sit's
    paper describes:

    - incorrect lookup routing
    - incorrect routing updates
    - partition
    - storage and retrieval attacks
    - inconsistent behavior
    - overload of targeted nodes
    - rapid joins and leaves
    - unsolicited messages    

    Castro's paper discusses:

    - secure nodeId assignment methods
    - secure routing table maintenance
    - secure message forwarding
    - self-certifying data

    Castro et al. first describe briefly different kinds of attacks
    ans possible solutions to them.

    "Controlling the Cost of Reliability in Peer-to-Peer
    Overlays" focuses DHTs' capability to self-organise in highly
    adverse conditions (e.g., a network partition). The paper
    is written by Ratul Mahajan, Miguel Castro and Antony Rowston.

    Additionally, Moni Naor and Udi Wieder have published a paper, "A Simple 
    Fault Tolerant Distributed Hash Table" which is designed to be resilient
    against the spam attack.     
    
ISSUE: 
 
   Do we want to keep GISP or move to Kademlia or some other?
   
   RESOLVED:
   
   I think we can use GISP as long as we do not deploy Storm
   into a "production use", i.e., we use GISP more like a P2P 
   testing platform for Storm rather than "the best and the final"
   choice for Storm. 
   
   However, the problem is that how soon the deployment
   will happen (I don't know anything about the milestones :)).
   
   Currently, I see Kademlia as the best alternative to us, but
   as we just got finished a working Storm/GISP 
   implementation, do we really want to move an another 
   algorithm/protocol immediately ?
   
   OTOH, it's obvious that better and better P2P algorithms
   are invented in the future. The question is that do we
   want to wait those algorithms, or do we want to use
   the algorithms which are currently available ?   
   
ISSUE:
   
   Do we want to implement currently available P2P lookup 
   algorithm ourselves, or do we want to develop a novel 
   P2P lookup algorithm ourselves ?      
   
   
Plan
====

First of all, we will create a PEG document (this document) which 
discusses general and theoretical aspects of the research problems. Then, 
if necessary, we program (rather short) test cases which will test 
the GISP/Storm P2P properties, as discussed in this document. Finally, we will 
collect and analyse test cases' information and publish any interesting 
discoveries. 

The GISP protocol
=================

According to Daishi Kato, the author of GISP, GISP ''...intends leverage 
those (DHT) algoritms and make a practical protocol.''. GISP's distance 
(unsigned integer of XOR). The GISP protocol specification paper describes the 
XOR-metric but the original GISP publication describes only numerical 
metric (which is little confusing since Chord uses numerical distance
metric and it's evident that a numerical metric is different from a XOR-metric).

GISP extends Chord's routing table to have more peer information as a cache.
Thus, log^2 messages are required to join/leave operations (according to the
original Chord publication). Additionally, Chord's routing table is asymmetric 
(requires stabilization protocol) and lookup process is unidirectional 
(in a virtual overlay ring). Original Kademlia publication states that 
Chord's routing table is rigid compared to Kademlia's routing table.

Since GISP lacks of Kademlia's binary-tree-like abstraction of the routing 
table, it is clear that the benefits of Kademlia's lookup properties (over 
other DHTs) are *not available* in the GISP protocol, e.g., no concurrent and
asynchronous lookups (no free of choice) and when a peer leaves the system
no messages are required. 

Original GISP publication describes "peer strength" as a measure for peer 
heteregeneity but do not describe what properties are used and how this
value is calculated.

For network communication the GISP protocol uses XML-RPC.

Research problems
=================
 
For determining whether Storm with unmodified GISP is practical, we want the 
answers to the following questions.

- How well GISP can scale if there are lot of concurrent peer joins and
  leaves in the system ? 
  
- What about GISP's lookup effieciency when the network grows ?

- GISP does not use Kademlia's binary-tree abstraction - does it have negative 
  influences and if it does what are the (bad) influences ? 
  
- How much better/worse pure Kademlia implementation (e.g. Kashmir) is over the GISP 
  protocol in face of performance and fault-tolerance ?
         
- How well GISP is able to perform in adverse conditions, e.g., a
  network partition occurs ?
   
- How well GISP is able to perform against different kind of
  security attacks and what are the impacts ?
   

Simulations
===========

If needed, using a simulation process we try solve research problems. Also, 
with a simulation process we are able test the GISP protocol without having 
to deploy real life experiments.

During simulation process, we assume that simulation network is rather ideal, 
e.g., there are no network latencies in the simulation network. In the future,
however, we plan to perform simulations in a non-ideal network.

   
Hypothesis
==========

In this section we introduce few hypothesis related to research problems.
Hypothesis' claims (goodness/badness) are based on the features of other DHTs 
and their simulation results (which can be found from the literature).
Also, values presented here (e.g., 1000-10000) are widely used in DHTs' 
simulation processes. 


- GISP's overlay can scale rather well when peers join and leave the system at a 
  constant rate or constant fraction (both are used) for a given time period 
  and cost of joining/leaving is logarithmic (e.g. Start with 1000 blocks and 
  1000 Storm-servers, 10 peer(s) joins/leaves every 5 seconds).
   
- GISP's overlay can scale well and is adaptable if the cost of join/leave is 
  logarithmic when peers join and leave the system constantly
  and the variable rate for joining/leaving changes greatly (e.g., Start with 1000 
  blocks and 1000 Storm-servers. 1-10 peer(s) joins/leaves every 1-10 second(s), 
  at a given time suddenly 100-900 peers joins/leaves randomly).
   
- GISP's data lookup is efficient and can scale well if lookup length grows with a 
  logarithmic growth inspite that the number of Storm-servers increases 
  linearly (e.g. 10-10000 Storm-servers, 10000 Storm blocks, with 10-10000
  Storm-servers perform 10000 lookups randomly)
     
- A there can be query/routing hotspots in the system and load balancing properties 
  may not scalable/tolerance against a hostile attack if a GISP peer is not able
  to handle all request (a peer is responsible for a given key) (e.g., 1000 
  Storm-server system, each server hosting 1-10 Storm block(s), 1-900 peers 
  (randomly chosen) queries a single key every 1-10 second(s); calculate average 
  block request failure, average lookup length, number of timed-out lookups and
  the distribution of lookup messages processed per peer).
     
- We can say that GISP is rather fault-tolerant if 80% of lookups are succesful 
  when 20% of peers die (this is Chord's simulation result) (e.g., 1000 Storm blocks are 
  insterted into a 1000 Storm-server system. After insertions, 1-99% of 
  servers die randomly or in a controlled way. Before GISP starts rebuilding 
  routing tables, perform 1000 Storm block fetches; calculate average block 
  request failure, average lookup length and number of timed-out lookups).
 
- A hostile entity is able to reroute a data lookup to a incorrect 
  destination peer during a data lookup process (e.g., e.g., 1000 Storm 
  blocks are insterted into a 1000 Storm-server system in which a a fraction
  of peers are hostile. Perform data lookups 1000 lookups randomly so that 
  in every lookup process, one forwarding request is rerouted incorrectly towards
  randomly chosen destionation peer; calculate average block request failure, 
  average lookup length, number of timed-out lookups and the distribution of 
  lookup messages processed per peer).  


Changes
=======

If we decide to perform simulations, we will program simulation test cases 
into the Storm CVS module. No changes are required to the Storm implementation
codebase.


.. _GISP: http://gisp.jxta.org
.. _Kademlia: http://kademlia.scs.cs.nyu.edu
.. _Chord: http://www.pdos.lcs.mit.edu/chord/
.. _Kashmir: http://khashmir.sourceforge.net/
