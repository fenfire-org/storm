
==========================================================================
PEG storm_with_tapestry--hemppah:
==========================================================================

:Authors:  Hermanni Hyytiälä
:Date-Created: 2003-07-03
:Last-Modified: $Date: 2003/08/05 07:01:27 $
:Revision: $Revision: 1.31 $
:Status:   Incomplete

.. :Stakeholders:
.. :Scope:    Major|Minor|Trivial|Cosmetic
.. :Type:     META|Policy|Architecture|Interface|Implementation

.. Affect-PEGs:

This document describes the use of Tapestry overlay with Storm. We start by 
proviving a short introduction to Tapestry, to the SEDA model and to the Tapestry 
API. Then, we propose new classes to be implemented in order to transfer
Storm-related messages in a Tapestry overlay, and a pseudo code which would 
provide Tapestry's basic services to Storm; pseudo code describes a simple 
StormTapestryManager class which supports Storm block query and insert operations
in a Tapestry overlay.

This document is based on Tapestry 2.0 release.

Issues
======

ISSUE:  Do we need any code from the Oceanstore codebase, and if needed, what
        are the parts which are required ?
       
SOLVED: Yes, we need code from Oceanstore codebase. There are *many* classes
        and they are also fundamental to Tapestry's functionality. The names of
        the classes are not listed here.
	
ISSUE:  What would be the best way to embed Tapestry/Oceanstore source tree 
	into Storm source tree (Tapestry/Oceanstore source tree includes
	.jar, .java, .so, .h and .c files)?
	
SOLVED: Create a "tapestry" sub directory under Storm's root directory. Resulting 
	"tapestry/src" sub directory must be included to CLASSPATH variable.
	This directory will be the root directory from which all 
	Tapestry related java class names will reference.
	       

ISSUE:  Does Storm use native Java threads in the current implementation? The 
	issue with this may be that Sandstorm Tutorial (included with the SEDA release)
	says "Don't allocate or manage threads in your application".
	
SOLVED:	The current implementation of Storm uses native Java threads in the 
	following classes:
	
	org/nongnu/storm/util/HTTPProxy.java
	org/nongnu/storm/http/server/HTTPServer.java
	org/nongnu/storm/http/server/HTTPConnection.java
	org/nongnu/storm/impl/p2p/Peer.java
	org/nongnu/storm/modules/gispmap/GispP2PMap.java
	org/nongnu/storm/modules/gispmap/GispPeer.java
	
	The first three classes are used with HTTP-Kit and the three 
	latter ones with GISP. Obviously, GISP related classes are not a problem
	as they are obsolete but HTTP-Kit related may be a problem, i.e., classes 
	have to re-engineer to SEDA compliant classes. UPDATE: This is not
	necessary; see the next ISSUE.	
	
ISSUE: If a software uses SEDA in one component, is it mandatory to use SEDA's
       thread-event model in all other components also (i.e. native Java threads
       are not allowed)?
       
SOLVED: According to Matt Welsh, the author of SEDA/Sandstorm framework, we are
        able to use native (Java) threads while using SEDA in a software:
	
	    It is true that SEDA intends to manage all of the threads in the system
	    itself. However, it should be fine to use your own threads as long as
            you yield or sleep often enough for SEDA's threads to get a chance to
            run. Let me know how it goes!
	    
ISSUE: Is it mandatory to run *always* the federation/static client bootstrap
       process (i.e., one federation peer and few static peers) before any dynamic 
       peers are able join into a network?
       
       [Yes. However, we are able to run the bootstrap process by running a 
       federation peer and a static peer on a single computer. After a local
       stabilization (that is performed rapidly), other computers (peers) are able
       join the network.
       
       The federation/static client bootstrap process is due to 
       historical reasons and it should be fixed very soon.]
       
       UPDATE: No. We can bootstrap a dynamic node and set its gateway to
       itself -- this create a new single node network and other dynamic nodes
       can use the bootrapping node as the gateway.
                     

Introduction to Tapestry
========================

Tapestry_ is an application level Peer-to-Peer overlay. Currently Tapestry 
supports the `DOLR abstraction`_ but other abstraction can be implemented 
(e.g., DHT_). Tapestry's DOLR interface has two primary functions: efficient 
point to point message delivery and effcient object location. 
In Tapestry, objects are *published* with object to location mappings (a.k.a. 
pointers). This operation differs from DHTs in that Tapestry always tries use 
the closest available object.

Tapestry's routing table consists of levels of neighbor links. Each level 
represents a matching prefix up to a digit position in the ID. Additionally,
for each level, neighbor links are selected based on closest in network 
*latency*. Along the publication process, the overlay deposits a pointer at 
every hop along the way. When an object is searched, a query is forwarded 
towards object's publication route, and when a query run into a first pointer, 
overlay directly forwards to the object.

Tapestry 2.0 release is written in Java and uses SEDA_ (Staged Event-Driven 
Architecture) framework for thread and I/O-operations. Tapestry's code base
has approximately 57000 lines of code and shares some code with Oceanstore's
code base.


Overview of SEDA
================

SEDA_ is an asynchronous I/O library (similar to JDK 1.4's java.nio package). In
the SEDA model, there are stages and they communicate with each other by sending 
events. A stage runs as a thread which starts by executing initialization routines and 
then enters an event loop.  Inside each JVM, a dispatcher monitors all messages 
and events, and delivers copies to each stage that subscribes to messages of 
that type.

Currently, each Tapestry component is implemented as a stage. For example, 
A (common) Tapestry node uses StaticTClient stage, DynamicTClient stage, Router 
stage and Tapestry application stage. For each stage, a config file is required 
to specify stage's properties (XML-like tag structure). During the initialization 
phase, a stage specifies which event and messages it wants to "listen" to. For 
Storm, the Tapestry application stage is the most important. 

SEDA version 2.0 is included within the Tapestry 2.0 release.

Overview of Tapestry API
========================

According to the `Tapestry website`_, there are four main API services in
Tapestry that are used by other applications:

    PublishObject(O_GUID): 
    publish, or make available, object O on the local node. This call is best 
    effort, and receives no confirmation.
  
    UnpublishObject(O_GUID): 
    Best-effort attempt to remove location mappings for O.
  
    RouteToObject(O_GUID): 
    Routes message to location of an object with GUID O_GUID
  
    RouteToNode(N_ID, Exact): 
    Route message to node N. "Exact" specifies whether destination ID needs to 
    be matched exactly to deliver payload.
   

However, the implemented API, according to the JavaDocs, offers services with 
the following names (and some additional services):

    TapestryRouteMsg(SecureHash peer) - analog to RouteToObject(O_GUID)
    
    TapestryLocateMsg(SecureHash guid, TapestryQuery query, TapestryQueryState 
    query_state, SecureHash id)  - locate a node storing an object with GUID 
    
    TapestryMacRouteMsg(SecureHash peer) - cryptographic (MAC) implementation
    of RouteToObject(O_GUID)
         
    TapestryPrefixRouteMsg(SecureHash peer, boolean inbound)  - routes to the 
    node with the GUID closest to a given GUID.  OceanStore uses this service 
    to find storage candidates for erasure coded fragments. See Javadocs for 
    more detailed documentation.
            
    TapestryPublishMsg(SecureHash guid, TapestryTag tag)  - analog to 
    PublishObject(O_GUID)
    
    TapestryUnpublishMsg(SecureHash guid, TapestryTag tag) - analog to 
    UnpublishObject(O_GUID)
    
In addition, Tapestry API offers a number of other services. Again, see
JavaDocs for more detailed information.


Using Tapestry API from other applications
==========================================

According to the `Tapestry programmer's guide`_, there are three steps to write
an application that uses Tapestry's services:

   - Write the necessary messages to interface with Tapestry
   - Write an event handler class that would serve as the application
   - Write configuration file(s) to define stages and specify initialization 
     arguments
     
Custom messages for Tapestry
----------------------------

Again, According to the `Tapestry programmer's guide`_:

1. An application must extend extending the abstract API message types, e.g.:

   public class StormLocateMsg extends TapestryLocateMsg {

2. Class must have own serialization methods in order transfer messages over a 
network:
   
   public void constructor(byte [] data, int [] offset)
   public void to_bytes(byte [] data, int [] offset)
   
3. Do not write a working type_code method

 
An event handler class
----------------------

1. Write an event handler class which extends SEDA's existing stages, e.g.,

  public class StormTapestryManager implements EventHandlerIF
  
2. In an event handler class, implement a hanleEvent method which will
   process registered messages
   
3. In an eventhandler class, implement a init method which will define
   the events and messages that are "listened" for this class

4. Register your custom message classes

Using Tapestry with Storm
=========================

In this section, we will outline a design which would allow Storm to use
Tapestry's routing services.

For finding Storm blocks in a Tapestry overlay, we must create custom message
classes:

   - BlockIDTag class (implements ostore.tapestry.api.TapestryTag) for 
     Storm blocks' IDs for storing query specific information:
     in our case, we store at least block's GUID and other related info 
     (if needed)

   - BlockIDQuery class (implements ostore.tapestry.api.TapestryQuery) to receive an 
     object identified by a BlockIDTag (and dispatch a 
     ostore.tapestry.api.TapestryLocateMsg that contains a BlockIDQuery)  
     
   - StormLocateMsg class (extends ostore.tapestry.api.TapestryLocateMsg, 
     implements ostore.util.QuickSerializable) for locating Storm blocks 
     identified by a BlockIDTag in a Tapestry overlay
   
   - StormResponseMsg class (extends ostore.tapestry.api.TapestryRouteMsg,
     implements ostore.util.QuickSerializable) to carry meta data for a 
     requested Storm block. This is an optional class, i.e., if we want
     to see query results, we could use this class
     
   - StormBlockRequestMsg class (extends ostore.tapestry.api.TapestryRouteMsg,
     implements ostore.util.QuickSerializable) to request a specific Storm
     block
   
   - StormErrorMsg class (extends ostore.tapestry.api.TapestryRouteMsg,
     implements ostore.util.QuickSerializable) for sending error messages
   
   - StormBlockMsg class (extends ostore.tapestry.api.TapestryRouteMsg,
     implements ostore.util.QuickSerializable) to carry a requested Storm
     block being sent to a original requester
     
     
Please notice that the above message classes provide only basic functionality in a
Tapestry overlay, i.e., a Storm peer is able to perform lookups based
on Storm Block ID, get lookup results (optional), get error messages
(optional) and get a a requested Storm block. If needed, other messages
classes can be implemented for more versatile functionality.
     
   
For interacting Storm's storage model with Tapestry we must create an event
handler class:

   - StormTapestryManager class (implements sandStorm.api.EventHandlerIF) for 
     listening standard Tapestry events, registering custom Storm messages, 
     dispatching custom Storm messages and interacting with a local Storm pool
     
     
Here, we propose a simple StormTapestryManager pseudo code. This is an event
handler class for Tapestry services. In addition to basic event handling 
functionality, this class supports Storm block query and insert operations 
in a Tapestry overlay. ::
 
 	class StormTapestryManager implements EventHandlerIF {
	
		// Dispatcher which dispatches all items
		// to a target peer. 
		// (Mandatory: Sandstorm requires this)
		method dispatch (QueueElementIF item) {
			// try do dispatch an item
			try {
				classifier.dispatch(item);
			} catch (expection) {
				error("Could not dispatch item!")
			}
		}

		// General initialization method
		// that handles subscribes this stage
		// to listen certain events and messages.
		// (Mandatory: Sandstorm requires this)
		method init (ConfigDataIF config) {
 
 		// find our NodeId 
		self_node_id = new NodeId 
		
		// Initialize the appropriate
		// instance of the Classifier. The Classifier handles the 
		// publish/subscribe mechanism used for
		// event dispatch between stages.
		
		classifier = Classifier.getClassifier(self_node_id = new NodeId)
		
		// Create peer ID based on peer's public key.
		 peer = new SHA1Hash (publicKey);
		 
		 // The *events* which we want to listen.
		 // (Mandatory: Sandstorm requires this)

		 array event_types = {
		 "seda.sandStorm.api.StagesInitializedSignal",
		 "ostore.tapestry.impl.TapestryReadyMsg",
		 "ostore.tapestry.api.TapestryDetachConfirm"	 
	    	}
		
		for (From i = 0 To numberOf(event_types)) {
		 	classifier.subscribe (event_type[i])
		 }
		
		// The *messages* we want receive through Tapestry.
		// (Mandatory: Sandstorm requires this)

		array message_types = {
		"org.nongnu.storm.p2p.tapestry.StormErrorMsg",
		"org.nongnu.storm.p2p.tapestry.StormLocateMsg",
		"org.nongnu.storm.p2p.tapestry.StormResponseMsg",
		"org.nongnu.storm.p2p.tapestry.StormBlockMsg",
		"org.nongnu.storm.p2p.tapestry.StormBlockRequestMsg",
		"ostore.tapestry.api.TapestryLocateFailure"
		}
		
		for (From i=0 To numberOf(message_types)) {
	    
	 	   // First, register messages we intend to
		   // receive.
		   
		   TypeTable.register_type(messages_types[i])
		   
		   // Second, require that inbound field is
		   // set to true.  Otherwise, we will see the messages that we
		   // send as well. 

		   classifier.subscribe (message_type[i], verifyInbound);
		}
		
		 
 		}
		
		// Handles *all* events which stage is listening.
		// (Mandatory: Sandstorm requires this)
		 method handleEvents(QueueElementIF array items) 
		 throws EventHandlerException {
		 
		 for (From i=0 to numberOf(items)
		 	handleEvent(items[i])			
		 }
		
		// Handles single event for this stage. handleEvents
		// method gives an item to this method as a parameter.
		// (Mandatory: Sandstorm requires this)
		method handleEvent(QueueElementIF item) {
		
		if (item instanceof StagesInitializedSignal) {
	        	// StagesInitializedSignal received
	    
	    	} else if (item instanceof TapestryReadyMsg) {
			// Connected to network.	    
			publishLocalStormPool()
			// do something else, if needed
	    	} else if (item instanceof TapestryDetachConfirm) {
			// Disconnected from network.
			unpublishLocalStormPool()
		} else if (item instanceof TapestryLocateFailure) {
			// Search failed.			
		} else if (item instanceof StormLocateMsg) {
			// Handle StormLocateMsg with a custom method.
			handleStormLocateMsg(item)
		} else if (item instanceof StormResponseMsg) {
			// Handle StormResponseMsg with a custom method.
			handleStormResponseMsg(item)			
		} else if ( item instanceof XXX ) {
			// Handle message XXX with a custom method.
			handleOtherMsg(item)
			...
		} else {			
			//  Unknown QueueElementIF item
		}		
				
		}
		
		// Handles a certain type of event (StormLocateMsg).
		method handleStormLocateMsg(item){
			results = performLocalSearch()
			dispatch(results)		
		}
		
		// Handles a certain type of event (StormResponseMsg).
		method handleStormResponseMsg(item){
			response = createResponse()
			dispatch(response)		
		}
		
		// Handles a certain type of event (XXX).
		method handleXXXMsg(item){
			// do something	
		}
			
		// Create a Storm query to a Tapestry network,
		// Query tag is Storm block's GUID.
		method createQuery(){
		 	blockTag = new BlockIDTag(blockGUID)
			q = new StormQuery()
			queryMsg=  new StormLocateMsg(blockTag, q)
			dispatch(queryMsg)
		}
		
		// Dispatch TapestryPublishMsg for all blocks in a 
		// local Storm pool.
		method publishLocalStormPool(){		
			for each block in localPool
				block = localPool.getBlock()
				tag = new BlockIDTag(block.getGUID())				
				publishMsg = new 
				TapestryPublishMsg(block.getGUID(), tag)
				dispatch(publishMsg)		
		}
		
		// Dispatch TapestryUnpublishMsg for all blocks in a 
		// local Storm pool.
		method unpublishLocalStormPool(){		
			for each block in localPool
				block = localPool.getBlock()
				tag = new BlockIDTag(block.getGUID())				
				unpublishMsg = new 
				TapestryUnpublishMsg(block.getGUID(), tag)
				dispatch(unpublishMsg)
		}
		
		// Dispatch TapestryPublishMsg for a single block in a 
		// local Storm pool.		
		method publishSingleStormBlock(blockGUID) {
			tag = new BlockIDTag(blockGUID)				
			publishMsg = new 
			TapestryPublishMsg(blockGUID, tag)
			dispatch(publishMsg)
		}
		
		// Dispatch TapestryUnpublishMsg for a single block in a 
		// local Storm pool.		
		method unpublishSingleStormBlock(blockGUID) {
			tag = new BlockIDTag(blockGUID)				
			unpublishMsg = new 
			TapestryUnpublishMsg(blockGUID, tag)
			dispatch(unpublishMsg)
		}
		
		 
		// Sandstorm calls this method when it cleanups a stage.		 		 
     		method destroy() {
		// We do not have to anything here since Sandstorm does not use 
		// this method.
		}
	}
	

Appendix A
==========

Sandstorm configuration files for Storm
---------------------------------------

According to the `Tapestry programmer's guide`_, we have to have
three different config files for a Storm peer:

1. A config file for a federation Storm peer:

The federation peer is responsible for synchonizing and stabilizing the 
initial Tapestry network.

2. A config file for a static Storm peer(s)

Number of static peers are required for bootstrapping a Tapestry network. A static
peer uses the TClient stage for routing table operations.

3. A config file for a dynamic Storm peer

This is a 'normal' Storm peer. These kind of peers can join and leave a network
dynamically. A dynamic peer uses DTClient stage for routing table operations.

Once the initial static network is stabilized, dynamic peers can join the network.

For more information how a Tapestry network is bootstrapped and stabilized, please
see the `Tapestry programmer's guide`_. Here's an example config file for a 
dynamic Storm peer as the config file includes the DTClient stage. ::

	# Example Sandstorm configuration file for Storm/Tapestry.
	#
	# !!! Please notice that the documentation of this file is based
	# on a config file distributed within the Tapestry/SEDA 2.0 release. Some
	# parts of this config file are edited to provide better idea which parts
	# 'Storm specific', mandatory/optional &c  !!!
	#
	# The '#' starts a comment which extends to the end of the line
	# This file uses an XML-like format consisting of nested sections.
	# 
	# Most of the sections of this file are optional. The complete set of
	# options is given here just to document their use, and leaving options 
	# unspecified causes them to use their default values. In general it is
	# a good idea to just use the defaults.

	# The outermost section in the file must be called 'sandstorm'.
	<sandstorm>

	# Global options
	<global>

	# The default thread manager for stages. 
	# Allowable values are 'TPSTM' (thread-per-stage) and 'TPPTM' 
	# (thread-per-CPU). 
	defaultThreadManager TPSTM   

	# Options for TPSTM thread manager
	<TPSTM>
	# Enable the thread governor. This resizes the thread pool for each
	# stage when the stage's incoming event queue reaches its threshold.
	governor false
	# The sampling delay (in milliseconds) for the thread governor.
	governorDelay 2000
	# The maximum number of threads allocated to each thread pool
	governorMaxThreads 10
	</TPSTM>

	# Options for TPPTM thread manager
	<TPPTM>
	# The number of CPUs in the system. Eventually this will be
	# determined automatically.
	numCpus 1
	# The maximum number of threads to allocate.
	# In general this value should be equal to numCpus.
	maxThreads 1
	</TPPTM>

	# The Sandstorm profiler is extremely valuable for understanding the 
	# performance of applications, and for identifiying bottlenecks
	<profile>
	# specify whether the Sandstorm system profiler should be enabled.
	enable false

	# specify the samplying delay(in milliseconds) for the profiler.
	# Default is 1000 ms.
	delay 1000

	# specify the filename that the profile will be written to.
	# default is ./sandstorm-profile.txt
	filename /tmp/sandstorm-profile.txt

	# specify the graphfilename that the graph profile will be written to.
	# default is ./sandstorm-graph.txt
	graphfilename /tmp/sandstorm-graph.txt

	# specify whether the profile should generate a graph of stage
	# connectivity during runtime.  Default is false.
	graph false
      
      	# specify whether the outgoing queue length for sockets should be
	# included in the profile.  Default value is false.
	sockets false
    
    	</profile>

	<initargs>
	# Global parameters are defined here, for instance, we could define
	# my_node_id ip-address:port
	</initargs>

	</global>

	# Each stage is defined by a <stage> section

	<stages>

	# The name of the stage as registered with the system. Mandatory.
	<Network>

	# The	fully-qualified classname of the stage's event handler. Mandatory.
	class ostore.network.Network

	# The size of the event queue threshold for this stage. Optional.
	# The default is -1, which indicates an infinite threshold.
	queueThreshold 1000

	# Initial arguments to pass to the event handler's init() method
	<initargs>
	# Some parameters for this stage
	</initargs>

	</Network>

	<Router>
	class ostore.tapestry.impl.Router
	queueThreshold 1000
	<initargs>
        # Some parameters for this stage
	dynamic_route dynamic
	</initargs>
	</Router>                                                                   

	# The name of the stage as registered with the system. Mandatory.
	<DTClient>
	# The fully-qualified classname of the stage's event handler. Mandatory.
	class ostore.tapestry.impl.DynamicTClient

	# The size of the event queue threshold for this stage. Optional.
	# The default is -1, which indicates an infinite threshold.
	queueThreshold 1000
	# Initial arguments to pass to the event handler's init() method
	<initargs>
	# An IP address of a gateway peer which bootsraps us into a Tapestry network
	# This is a mandatory for a dynamic Storm peer
	gateway ${GatewayID}
	# This parameter tells Tapestry to threat local routing tables dynamically
	dynamic_route dynamic
	</initargs>
	</DTClient>

	# This stage is not mandatory
	# Patchwork stage can be used to monitor neighbor links' conditions &c   
	<Patchwork>
	class ostore.network.patchwork.Patchwork
	<initargs>
	# list here some arguments which we want to define
	</initargs>		
	</Patchwork>

	# The name of the stage as registered with the system. Mandatory.
	<StormTapestry>
    	class org.nongnu.storm.p2p.tapestry.StormTapestryManager
	queueThreshold 1000

	# Initial arguments to pass to the event handler's init() method
	<initargs>
	# list here some arguments which we want to define
	</initargs>

	</StormTapestry>
	</stages>

	</sandstorm>  # End of the configuration file


Also, any parameter's value can be set using a command line arguments. For 
more information about Sandstorm's config files, please refer to the documentation
distributed with the SEDA/Sandstorm release (included with the Tapestry 2.0 release).

Running Storm with Tapestry
---------------------------

To run Tapestry with Storm, we must use the following syntax.

	sandstorm <storm-configfile-name>
   
where ``sandstorm`` is a script distributed within the Sandstorm package and
``<storm-configfile-name>`` is the Sandstorm configuration file for Storm.
	
Also, we can use Perl scripts to handle more complex usage scenarios. In the
Tapestry 2.0 release, Perl scripts are used to create dynamic configuration
scripts for regression tests.    

Changes
=======

Eight new classes will be implemented under org.nongnu.storm.p2p.tapestry package 
into the Storm code base, if this PEG document is accepted.  

.. _Tapestry programmer's guide: http://www.cs.berkeley.edu/%7Eravenben/tapestry/html/guide.html
.. _Tapestry: http://www.cs.berkeley.edu/~ravenben/publications/pdf/tapestry_jsac.pdf
.. _Tapestry website: http://www.cs.berkeley.edu/~ravenben/tapestry/
.. _SEDA: http://www.eecs.harvard.edu/~mdw/proj/seda
.. _DOLR abstraction: http://www.cs.berkeley.edu/~ravenben/publications/pdf/apis.pdf
.. _DHT: http://www.cs.berkeley.edu/~ravenben/publications/pdf/apis.pdf 
