=========================================================
``async_storm--benja``: Supporting aynchronicity in Storm
=========================================================

:Author:	Benja Fallenstein
:Date:		2002-11-24
:Revision:	$Revision: 1.2 $
:Last-Modified:	$Date: 2003/07/04 13:08:30 $
:Type:		Architecture
:Scope:		Major
:Status:	Implemented


The vision of Storm has always been to operate on top of
a peer-to-peer network, providing network transparency
to all user data. However, the current Storm interfaces
are woefully inadequate for a network-based implementation,
because they do not support asynchronicity: All operations
block until they are successfully completed. 

This PEG defines a pattern to support asynchronicity.
A ``JobListener`` is introduced that can be used
to inform clients when new data arrives. All Storm lookups
take an optional ``JobListener`` parameter.
If the requested data is available locally, they
simply return it. Otherwise, they return ``null`` and
start a new thread to retrieve the data. Once that job is done,
the ``JobListener`` is informed; it can at that time
re-request the data and expect it to be available.

.. contents::


Issues
======

- Should we keep variations of the Storm methods that do not
  take a ``JobListener``? If so, what should these do
  if data is not available locally: Implement blocking IO,
  spawn a lookup job that does not inform any listener
  when it finishes, or simply return ``null``
  and do nothing?

   RESOLVED: I'm leaning towards not providing these methods 
   for now, since it's not clear what they'd do.)


Analysis
========

In a network environment, blocking IO is simply unbearable-- 
firstly because blocking user input during IO operations
is not acceptable, and secondly because it is 
vital for performance to be able to perform several
network lookups in parallel.

The classic pattern to support network asynchronicity
is to use GUI events to get new data to a program that
has requested it (it works like this both `in GNOME`_ and
`in KDE`_).

.. _in GNOME: http://www.usenix.org/events/usenix2000/freenix/full_papers/perazzoli/perazzoli_html/node16.html
.. _in KDE: http://www.heise.de/ct/english/01/05/242/

In this pattern, the callbacks include information
about the newly arrived data, like this:

    dataArrived(Job job, byte[] bytes);

A browser application, for example, could support
progressive image rendering by updating an image
on the screen whenever ``dataArrived()`` is called.

However, this doesn't fit very well with the Gzz
view architecture, where the whole view is re-generated
each time there are changes (possibly re-using
some cached parts). For example, when an image block
has been loaded from the network (we can currently
only check the data when it has completely arrived,
so we don't need to worry about progressive rendering
at this time), we would re-generate the view, putting
in the real image instead of the placeholder we've
had before. When new xanalogical links to a page we're viewing
arrive from the network, we regenerate the view
to incorporate them.

This architecture is better served with something like
`the Obs interface`__, which only provides a single,
parameterless ``chg()`` method; the callback then
re-requests the data when ``chg()`` is called.

__ ../../javadoc/gzz/Obs.html



Changes
=======

I propose a similar interface for Storm, ``JobListener``::

    /** Called when new data has arrived. */
    void newDataAvailable();

    /** Called when the lookup finishes.
     *  @param timeout Whether the lookup was finished
     *                 because of a time-out. If this
     *                 is false, the lookup was finished
     *                 because the pool implementation
     *                 believes that all relevant data
     *                 has been retrieved now.
     */
    void finished(boolean timeout);

The two separate callbacks make it easy to use the same
interface for lookups like for xu links, where we update
the view each time new information arrives, and
lookups like for pointer blocks, where we can only
continue when all relevant data is known.

The lookup methods in ``StormPool`` would simply get an additional
``JobListener`` parameter::

    Block get(BlockId id, JobListener listener);
    Set getIds(JobListener listener);

(``IndexedPool`` would be similar, but since that class isn't
fully thought-out yet, I'm not giving its methods here.)


\- Benja
