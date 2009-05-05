package util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Helper Functions for interactions between
 * ByteBuffers and Files.
 *
 * @author Joseph Pecoraro
 */
public class BufferUtils {

	/** The Size of a SHA1 Hash String as Bytes */
	public static int HASH_STRING_SIZE = 40;


	/**
	 * Build a ByteBuffer solely for a file
	 * @param filename the path to the file
	 * @param maxSize the maximum size the buffer could be
	 * @return a ByteBuffer containing that files data with the position at the end of the data load
	 */
	public static ByteBuffer getBufferForFile(String filename, int maxSize) {
		return getBufferForFile(filename, maxSize, "");
	}


	/**
	 * Build a ByteBuffer for a file with a prefixed String
	 * Reference: Source: http://nadeausoftware.com/articles/2008/02/java_tip_how_read_files_quickly
	 * @param filename the path to the file
	 * @param maxSize the maximum size the buffer could be
	 * @param prefixData a String to prefix onto the buffer, ignored if the empty string ""
	 * @return a ByteBuffer containing that files data with the position at the end of the data load
	 */
	public static ByteBuffer getBufferForFile(String filename, int maxSize, String prefixData) {
		try {

			// Setup
			FileInputStream f = new FileInputStream( filename );
			FileChannel channel = f.getChannel();
			ByteBuffer buf = ByteBuffer.allocate( maxSize );

			// Prefix Data
			if ( prefixData.length() != 0 ) {
				buf.put( prefixData.getBytes() );
			}

			// File Data
			int nRead;
			while ( (nRead=channel.read(buf)) != -1 ) {
				if ( nRead == 0 ) {
					break;
				}
			}

			// Cleanup
			channel.close();
			f.close();
			return buf;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}


	/**
	 * Write the given byte buffer to a file
	 * @param buf the buffer assumed to be in the desired position
	 * @param filename the path of the file to write to
	 * @param append set to true if you wish to append to the file, false if you want to overwrite the file
	 */
	public static void writeBufferToFile(ByteBuffer buf, String filename, boolean append) {
		try {
			FileOutputStream f = new FileOutputStream( filename, append );
			FileChannel channel = f.getChannel();
			channel.write(buf);
			while ( buf.hasRemaining() ) {
				buf.compact();
				channel.write(buf);
			}
			channel.close();
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
