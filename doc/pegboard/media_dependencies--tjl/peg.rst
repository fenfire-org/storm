=========================================================================
PEG ``media_dependencies--tjl``: Remove AWT dependencies of ``gzz.media``
=========================================================================

:Authors:  Tuomas Lukka
:Last-Modified: $Date: 2003/03/31 09:25:01 $
:Revision: $Revision: 1.1 $
:Date-Created: 2002-09-05
:Status:   Implemented

Subject: Remove AWT dependencies of ``gzz.media``
Author: Tuomas J. Lukka

All uses of the class Image in ``gzz.media.*`` should be removed.
The methods to get the image for a given span should be in 
``gzz.client``.

Changes
-------

Remove AWT dependencies of gzz.media; 
move into gzz.client.GraphicsAPI
implementations where they belong.






