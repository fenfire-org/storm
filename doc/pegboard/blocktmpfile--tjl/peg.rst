=============================================================
PEG blocktmpfile--tjl: 
=============================================================

:Author:   Tuomas J. Lukka
:Last-Modified: $Date: 2003/07/04 13:07:04 $
:Revision: $Revision: 1.2 $
:Status:   Implemented

Java.awt can create a new image from a file, but not from an inputstream.
Same for libvob. 

This PEG proposes a new class, BlockTmpFile, which will make it easy
to obtain a temporarily used file with the block data.

Issues
======

- Should BlockTmpFiles be cached for a given block?

    RESOLVED: Not user-visibly. It is important to have the explicit
    close() method for efficiency. There may be internal cached
    objects which do reference counting, but this optional.

Changes
=======

New class, BlockTmpFile, with the following interface::

    package org.nongnu.storm.util;

    public abstract class BlockTmpFile {
	/** Create a new blocktmpfile for the given block.
	 * This ensures that there is, on the disk, a physical
	 * file with the checked contents of the given block.
	 * This method will throw whatever errors the InputStream
	 * can give.
	 */
	static public BlockTmpFile get(Block block)
	    throws ...;

	/** Return the file object pointing to the file
	 * which contains the data.
	 */
	public File getFile();

	/** Return the file which contains the data.
	 */
	public String getFilename();

	/** The file will no longer be used.
	 */
	public void close();
    }

Initially, there will be two different implementations of BlockTmpFile
internally, one for FileBlocks, and another for non-File Blocks.
The FileBlock implementation is trivial, close() does nothing and getFile()
returns block.getFile(). The other implementation will use getInputStream()
and read the block to the disk.

We shall have to trust the user code not to modify the file.
