==========================================================================
PEG attacking_gisp--hemppah:
==========================================================================

:Authors:  Hermanni Hyytiälä
:Date-Created: 2003-06-05
:Last-Modified: $Date: 2003/08/01 12:01:07 $
:Revision: $Revision: 1.41 $
:Status:   Incomplete

.. :Stakeholders:
.. :Scope:    Major|Minor|Trivial|Cosmetic
.. :Type:     META|Policy|Architecture|Interface|Implementation

.. Affect-PEGs:

This is the first version of PEG document which briefly describes the attack 
methods used by a "killer" program. The program is intended to be used to 
test GISP_ P2P software's robustness against hostile attacks. 

In this document we mean with "hostile peer" as an entity which is able to do a 
(limited/simplified/modified) number of regular GISP peer's functionalies
in a way which may be harmful for the GISP network w.r.t. performance
and redundancy. The harmfulness of a peer is a consequence of the fact
that a peer is wilfully malicious. 

.. Once this PEG is accepted we will start the experiments. 

Disclaimer
==========
This program is only used for research purposes and
the goal is to improve GISP's resilience against hostile attacks.

Additionally, the author of GISP has stated that he will address 
attacks as they become a problem. This inclines us to think 
that writing an attack program will get the author to address 
that attacks.


Issues
======

ISSUE: When should we discuss this with the author of GISP?

ISSUE: Does GISP support "free-choice" during lookups?

       RESOLVED: No, the current Java implementation does not support
       "peer-choice". However, the protocol specification of GISP-3.4 allows 
       the "free choice".

ISSUE: Is GISP peer able to determine if an another peer is
       not "useful" or not (not just PING scenario)  ("a peer can discard
       information of unreachable peers")
       
       RESOLVED: Yes, there are directions in the protocol specification
       how GISP peer should handle undesirable messages and peers:
       
       "3.6. Security Behavior

       For the security of the entire system,
       each peer SHOULD deal with possible attacks.
       A peer SHOULD detect undesirable messages such as:
       A message that is not valid.
       A message with <peer> or <insert> elements
       whose millisecond to live (ttl) is too big.
       A message or its elements are
       already received in a short period of time.
       A whole or a part of an undesirable message
       SHOULD be discarded.
       A peer MAY record the undesirable message or its information
       for the future security purpose.

       A peer SHOULD also detect undesirable peers such as:
       A peer which never respond.
       A peer which rarely responds.
       A peer which sends undesirable messages.
       A peer which sends too many messages.
       An undesirable peer SHOULD be removed from the peer routing table
       and SHOULD NOT be included in outgoing messages.
       A peer MAY record the undesirable peer information
       for the future security purpose."
       
       Please notice, however, the word "SHOULD".
       
ISSUE: Related to the previous issue, how (non) "uselessness" is 
       determined by a GISP peer and is it a reliable method ?
       
       RESOLVED: No, the current GISP implementation does not address 
       "uselessness" properly, since GISP peer only observes reply 
       timeouts for the "seen" messages. Thus, we can implement a "dumb" 
       peer which answers to the "seen" messages normally but does not
       perform requested operations, e.g., reply to "query" message. This 
       behaviour allows other peers to maintain "dumb" peers in their 
       routing tables while still "dumb" peers are able to cause 
       problems for the network's operation.


Research problems
=================

GISP does not appear to address the attacks against DHT systems
described in ref1_ and ref2_. Using a simulation process as a 
research method we want the answers to the following questions:

- Does GISP have *obvious* exploits ?

- If there are exploits how easily they can be used by an 
  hostile peer ? 
  
- How severe implications attacks may cause to GISP network ?

- In general, is GISP resilient against (hostile) attacks or not ?

- How well GISP is able to re-organise after a (hostile) attack ?

Simulation method
==================

We will perform simulations on a single desktop computer. We intend
to use GISP's native code as much as possible. We start our 
simulation process by creating simple scenarios in which:

- A hostile peer(s) doesn't reply to queries (a "dumb" peer)*

.. Next you should describe the exact scenarios you want to try,
   so start with the dumb peer and specify it exactly. Then run 
   the test.
   
Scenario #1 (static "dumb"):

Description:
In this scenario, we use "dumb" peers to test GISP's fault tolerance.
In this context, with "dumb" peers we mean peers which do not reply or
forward queries at all. There are two variations of this scenario. The first
variation is the static scenario in which peers do not join or leave the system while
the test is running. In the second variation, peers can join and leave the
system while the test is running. 

We expect that GISP's fault tolerance is at least at the same level as Chord_'s 
fault tolerace since GISP's routing table is based on Chord's routing table.

Chord's general properties:

- O(log^2 n) messages are required to join/leave operations
 
- O(log n) lookup efficiency

- Routing table maintains information about O(log n) peers

- Routing table requires information about O(log n) of other peers
  of efficient routing, but performance degrades gracefully
  when that information is out of date
  
- Only one piece of information per peer need to be corect in 
  order to guarantee correct (though slow) routing queries
  
- Requires active stabilization protocol to aggressively maintain
  the routing tables of all peers
  
- "As long as the the time fo adjust incorrect routing table entries
  is less than the time it takes to the network to double in size,
  lookups should continue to take O(log n)"
  
- Has no specific mechanism to heal partitioned peer groups

- Additional redundancy can be achievied using a "successor-list",
  e.g., O(log n) successor peers
  
- Numerial metric: no "peer-choice" during lookups

Additionally, the current version of GISP (3.4) have following properties:

- Only uses the idea of XOR-metric in Kademlia

- The protocol specification of GISP-3.4 allows the "free choice", *but*
  the current Java implementation just selects fixed peers (like Chord)
  
- Compared to Chord routing table, GISP-3.4 protocol specification 
  suggests peers to cache as much peer information as possible
  (in order to reduce hops)
  
- The current implementation does not support "free choice"

- Future versions may include "peer strength" feature (a peer decides 
  whether to put it an another peer in its routing table or not)
  
- GISP maintains cache information about 10000 peers (max) whereas Chord
  caches information about 1000 peers (max)
  
The assumption for the results above is that the system is "in the steady state", 
according to the authors.

Note: The length of the path to resolve the query is O(log n) *with high 
probability*.  

Example Chord's path lengths in a static network (from figure):

~10 peers: 2 (average)
~20 peers: 2 (average)
~100 peers: 3 (average)
~1000 peers: 5 (average)
~10000 peers: 6 (average)
~16000 peers: 7 (average)

Chord's fault tolerance properties include (from figures):

Simultaneous peer failures: 
- Randomly selected fraction of nodes fail
- No peers join or leave the system
- Result: 20% of lookups fail, when 20% of peers are failed

Lookups during peers join and leave the system:
 
- The fraction of lookups fail as a function of the rate (over time)
  at which peers join and leave the system
  
- Only failures caused by Chord state inconsistency are included, not
  failures due to lost keys (text copied directly from the figure text)
  
- The authors

- Queries are not retried

- 500 peers

- Result: 6.5% of lookups fail, when peer join/leave rate per second
  is 0.1 (corresponds to peer joining and leaving every 10 seconds
  on average)
  

Simulation Process:
 
- Fraction of "dumb" peers is constant: create 9*10^k normal peers, 
  create 1*10^k "dumb" peers, where k = 1..3
  
- Fraction of "dumb" peers is dynamic: create n*10^k normal peers, 
  d*10^k "dumb" peers, where k = 1..3, n = 1..9 and d = 1..9
  
- Use both the constant and dynamic fraction scenarios, start with the
  constant
  
- Create 100*N key/value items in the network, where the N is the number of 
  all peers in the network

- Each peer queries a set of random keys

- Try to use same code as in GISP's implementation/simulation base

- For "dumb" peers we have to create own class 
  (extends GISPXML-class) which has "dumb" methods for query 
  forward and processing

During the simulation process we will use a single hostile  peer
or a group of hostile peers (fraction of all peers) in the test network.
We assume that hostile peer(s) takes part in forming of the network normally. 

By using above scenarios, we want to clarify GISP's properties with regard to research 
questions.

\*=We start the simulation process with these scenarios.


Hypothesis:

- GISP has a exploit, if a hostile peer is able to behave like "dumb"
  peer easily. More specifically, a hostile peer answers to "SEEN" messages in 
  the network, but no dot perform requested operations, e.g., do not provide
  a "RESULT" message for a "QUERY" message. This behaviour allows other peers 
  to maintain hostile peers in their routing tables while still hostile
  peers are able to cause problems to the network's operation. "SEEN", "RESULT"
  and "QUERY" messages are part of GISP-3.4 specification. Also, 
  a hostile peer is able to use this "dumb" behaviour with the other messages
  of GISP-3.4 specification. 
    
- In the GISP overlay, the average routing hop length is proportional to 
  O(log n).
  
- In a GISP network, when fraction f of the peers are "dumb" and a "dumb" peer 
  do not provide a "RESULT" message for a "QUERY" message, the probability of 
  routing successfully a "QUERY" message between two regular peers is 
  (1-f)^h-1, where h is the routing hops used in the overlay (i.e, O(log n)).
  
- It is expected that fraction f of all network traffic is "lost" in 
  a static GISP network since the current GISP implementation do not handle 
  "dumb" peers properly. More specifically, if there are n peers and fraction 
  f are "dumb" (i.e., do not process regular messages, replies only to "SEEN" 
  messages), the overlay has O(log n) average path length and all n
  peers perform n-1 lookups, then the estimated total number of lost packets is 
  [f^((log n)-1)]*[n((n-1)(log n))]. 
  
    
- - -

- It is expected that (popular) data items can be found with fewer hops in 
  *some cases* in GISP network w.r.t Chord network, since GISP extends Chord's 
  routing table to have more information as a cache
  
.. more specific, what effects cache has, distributions, formula ?
  

  
.. more specific, create a formula (?)
   
If fraction f of the peers are dumb in a static network with 10 - 10000
  peer and fraction f of the lookups, then fraction of the lookups will 
  fail at all network sizes
  
- GISP's lookup failure rate increases linearly in a static network with the 
  fraction of "dumb" peers when a "dumb" peer do not provide a "RESULT" 
  message for a "QUERY" message. More specifically, when randomly selected 
  fraction of f all peers become "dumb", same fraction f of all lookups will 
  fail.  

  Notes:
  
  n peers, O(log n) path length, all peers perform n-1 lookups. Then the 
  total number of packets is n((n-1)(log n))
  
  n peers, O(log n) path length, random fraction of peers, r, perform rl 
  lookups. Then the total number of packets is r((rl)(log n))

  GISP network:  n peers, O(log n) path length, all peers perform n-1 lookups. 
  Then the total number of packets is n((n-1)(log n))
  
  n peers where fraction f are hostile, O(log n) path length, all peers 
  perform n-1 lookups. Then the total number of lost packets is 
  [f^((log n)-1)]*[n((n-1)(log n))]
  
  
.. No, you should not say "expect similar" - it's far too vague.
   You should make explicit statements as to what the results will
   be.

   Stating

      If fraction f of the peers are dumb, then fraction f of the lookups
      will fail at all network sizes.

  would be ok. This is an accurate, measurable statement which we can then
  start seeing whether it's right or wrong.

  Whether it *is* right or wrong, depends of course on the exact behaviour
  of GISP which it's your job to find out about (and not emailing the author
  except as last recourse).

.. more to come
   "two way proof":
   1) The hypothesis holds true for the experiment
   2) The hypothesis does not hold true from a "random/pseudo experiment"
  

We will compare (and validate) our test results with the Chord's 
performance and fault tolerance properties.  
  

Changes
=======

No changes are required to the Storm implementation
codebase.


.. _ref1: http://www.cs.rice.edu/Conferences/IPTPS02/173.pdf    
.. _ref2: http://dcslab.snu.ac.kr/~buggy/p2p/paper/security/Pastry%20--%20Security%20for%20structured%20peer-to-peer%20overlay%20networks.pdf
.. _Chord: http://www.pdos.lcs.mit.edu/chord/
.. _GISP: http://gisp.jxta.org
