// (c) Tuomas J. Lukka

package org.nongnu.storm.util;
import org.nongnu.storm.*;
import java.io.*;

/** A (possibly) temporarily available physical file with block contents.
 */
public abstract class BlockTmpFile {

    private static class FileBlockTmpFile extends BlockTmpFile {
	File f;
	public FileBlockTmpFile(FileBlock block) 
		throws IOException, BlockId.WrongIdException {
	    f = block.getFile();
	}
	public File getFile() { return f; }
    }

    private static class RealBlockTmpFile extends BlockTmpFile {
	File f;
	public RealBlockTmpFile(Block block) 
		throws IOException, BlockId.WrongIdException {
	    f = File.createTempFile("blocktmp", "",
		    new File("."));
	    f.deleteOnExit();
	    InputStream i = block.getInputStream();
	    int count;
	    byte[] data = new byte[1024 * 32];
	    FileOutputStream out = new FileOutputStream(f);
	    while((count = i.read(data)) > 0) {
		out.write(data, 0, count);
	    }
	    i.close();
	    out.close();
	}
	public File getFile() { return f; }
	public void close() { 
	    f.delete();
	}
	public void finalize() {
	    f.delete();
	}
    }

    /** Create a new blocktmpfile for the given block.
     * This ensures that there is, on the disk, a physical
     * file with the checked contents of the given block.
     * This method will throw whatever errors the InputStream
     * can give.
     * <p>
     * This method will ensure that the data is in the file,
     * so the WrongIdException will be thrown from here
     * if there is one.
     */
    static public BlockTmpFile get(Block block) throws
	    IOException, BlockId.WrongIdException {
	if(block instanceof FileBlock) {
	    return new FileBlockTmpFile((FileBlock)block);
	} else {
	    return new RealBlockTmpFile(block);
	}
    }
	

    /** Return the file object pointing to the file
     * which contains the data.
     */
    abstract public File getFile();

    /** Return the file which contains the data.
     */
    public String getFilename() {
	return getFile().getPath();
    }

    /** The file will no longer be used.
     */
    public void close() { }
}
