package tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import util.SHA1;

import chunker.ChunkedFileInfo;
import chunker.Chunker;
import junit.framework.TestCase;

/**
 * TestSuite for our chunker package.
 *
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class TestChunker extends TestCase {

	/** Configuration Option */
	private static boolean DELETE_COMPONENTS = true;

	/** Path to the directory (this) where the test files are stored. */
	private static final String TEST_PATH = "src/tests/chunker/";

	/** Text File */
	private static final String TEXT_FILENAME = "test.txt";

	/** Image File */
	private static final String IMAGE_FILENAME = "monalisa.jpg";

	/** Reassembled Suffix */
	private static final String REASSEMBLED_PREFIX = "reassembled-";


	/**
	 * Simple test for a text file.
	 */
	public void testChunkTextFile() {

		// Debug
		System.out.println("Chunking File: " + TEST_PATH + TEXT_FILENAME);

		// Test Variable
		int numChunks = 3;

		// Chunk
		ChunkedFileInfo cfi = Chunker.chunk(TEST_PATH, TEXT_FILENAME, numChunks);

		// Verify there are the correct number of files (chunks)
	    File pwd = new File(TEST_PATH);
	    String[] fileChunks = pwd.list(new FilenameFilter() {
	        public boolean accept(File dir, String name) {
	            return name.endsWith("_" + TEXT_FILENAME);
	        }
	    });

	    // Sort the Filenames (for correct assembly)
	    // NOTE: Only for 0-9 chunks, maybe sort incorrectly for >10 chunks
	    Arrays.sort(fileChunks);

		// Debug
	    debugList(fileChunks);

	    // Assert Correct Number of Chunks
	    assertEquals(numChunks, fileChunks.length);
	    assertEquals(numChunks, cfi.getChunkPaths().length);

		// Reassemble (assume fileChunks are in sorted order)
	    Chunker.reassemble(TEST_PATH, fileChunks, TEST_PATH, REASSEMBLED_PREFIX + TEXT_FILENAME);

		// Verify the two files are equivalent
	    String orig = TEST_PATH + TEXT_FILENAME;
	    String reassembled = TEST_PATH + REASSEMBLED_PREFIX + TEXT_FILENAME;
	    assertTrue( compareFiles(orig, reassembled) );
	    assertTrue( compareFilesByHash( orig, reassembled ) );
	    
	    // Debug
	    debugHashes( orig, reassembled );

	    // Delete Chunks (not final reassembled part)
	    if ( DELETE_COMPONENTS ) {
		    for (int i = 0; i < fileChunks.length; i++) {
				File f = new File(TEST_PATH + fileChunks[i]).getAbsoluteFile();
				f.delete();
		    }
	    }
	}


	/**
	 * More advanced test for an image file
	 */
	public void testChunkImageFile() {

		// Debug
		System.out.println("\nChunking File: " + TEST_PATH + IMAGE_FILENAME);

		// Test Variable
		int numChunks = 5;

		// Chunk
		Chunker.chunk(TEST_PATH, IMAGE_FILENAME, numChunks);

		// Verify there are the correct number of files (chunks)
	    File pwd = new File(TEST_PATH);
	    String[] fileChunks = pwd.list(new FilenameFilter() {
	        public boolean accept(File dir, String name) {
	            return name.endsWith("_" + IMAGE_FILENAME);
	        }
	    });

	    // Sort the Filenames (for correct assembly)
	    // NOTE: Only for 0-9 chunks, maybe sort incorrectly for >10 chunks
	    Arrays.sort(fileChunks);

	    // Debug
	    debugList(fileChunks);

	    // Assert Correct Number of Chunks
	    assertEquals(numChunks, fileChunks.length);

		// Reassemble (assume fileChunks are in sorted order)
	    Chunker.reassemble(TEST_PATH, fileChunks, TEST_PATH, REASSEMBLED_PREFIX + IMAGE_FILENAME);

		// Verify the two files are equivalent
	    String orig = TEST_PATH + IMAGE_FILENAME;
	    String reassembled = TEST_PATH + REASSEMBLED_PREFIX + IMAGE_FILENAME;
	    assertTrue( compareFiles(orig, reassembled) );
	    assertTrue( compareFilesByHash( orig, reassembled ) );

	    // Debug
	    debugHashes( orig, reassembled );

	    // Delete Chunks (not final reassembled part)
	    if ( DELETE_COMPONENTS ) {
		    for (int i = 0; i < fileChunks.length; i++) {
				File f = new File(TEST_PATH + fileChunks[i]).getAbsoluteFile();
				f.delete();
			}
	    }
	}

	/**
	 * More advanced test for an image file
	 * - Deleting a Part
	 */
	public void testChunkImageWithMissingSection() {

		// Debug
		System.out.println("\nChunking File: " + TEST_PATH + IMAGE_FILENAME);

		// Test Variable
		int numChunks = 5;

		// Chunk
		Chunker.chunk(TEST_PATH, IMAGE_FILENAME, numChunks);

		// Verify there are the correct number of files (chunks)
	    File pwd = new File(TEST_PATH);
	    String[] fileChunks = pwd.list(new FilenameFilter() {
	        public boolean accept(File dir, String name) {
	            return name.endsWith("_" + IMAGE_FILENAME);
	        }
	    });

	    // Sort the Filenames (for correct assembly)
	    // NOTE: Only for 0-9 chunks, maybe sort incorrectly for >10 chunks
	    Arrays.sort(fileChunks);

	    // Debug
	    debugList(fileChunks);

	    // Assert Correct Number of Chunks
	    assertEquals(numChunks, fileChunks.length);

	    // Delete Part 3 of 5 (0, 1, x, 3, 4)
	    File partTwo = new File(TEST_PATH + fileChunks[2]);
	    partTwo.delete();

	    // New Listing
	    String[] fileChunksNew = pwd.list(new FilenameFilter() {
	        public boolean accept(File dir, String name) {
	            return name.endsWith("_" + IMAGE_FILENAME);
	        }
	    });

	    // Debug
	    debugList(fileChunksNew);

	    // Assert Correct Number of Chunks (1 less then before)
	    assertEquals(numChunks-1, fileChunksNew.length);

		// Reassemble (assume fileChunks are in sorted order)
	    Chunker.reassemble(TEST_PATH, fileChunks, TEST_PATH, REASSEMBLED_PREFIX + IMAGE_FILENAME);

		// Verify the two files are equivalent
	    String orig = TEST_PATH + IMAGE_FILENAME;
	    String reassembled = TEST_PATH + REASSEMBLED_PREFIX + IMAGE_FILENAME;
	    assertTrue( compareFiles(orig, reassembled) );
	    assertTrue( compareFilesByHash( orig, reassembled ) );

	    // Debug
	    debugHashes( orig, reassembled );
	    
	    // Delete Chunks (not final reassembled part)
	    if ( DELETE_COMPONENTS ) {
		    for (int i = 0; i < fileChunks.length; i++) {
				File f = new File(TEST_PATH + fileChunks[i]);
				f.delete();
			}
	    }
	    
	    

	}


	// Private Helpers

	/**
	 * Debug a file list
	 * @list List of Strings to print out
	 */
	private void debugList(String[] list) {
		System.out.println( "Files: (" + list.length + ")" );
	    for (int i = 0; i < list.length; i++) {
			System.out.println( list[i] );
		}
	}
	
	/**
	 * Debug display of hash comparison.
	 * @param filenames names of file to demonstrate hash comparison.
	 */
	private void debugHashes(String... filenames) {
		for( int i = 0; i < filenames.length; ++i ) { 
			
			System.out.printf( 
				"Hash%d: %s File%d: %s\n",
					i,
					SHA1.getInstance().hash( new File( filenames[ i ] ) ),
					i,					
					filenames[ i ] );
		}
	}
	
	/**
	 * Quick Comparison of two files
	 * @param filename1 name of the first file
	 * @param filename2 name of the second file
	 * @return true if their stream contents were equal, false otherwise
	 */
	private boolean compareFiles(String filename1, String filename2) {
		try {
			InputStream in1 = new FileInputStream(filename1);
			InputStream in2 = new FileInputStream(filename2);
			while (true) {
				int read1 = in1.read();
				int read2 = in2.read();
				if ( read1 != read2 ) { return false; }
				if ( read1 == -1 ) { return true; }
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * SHA1-based comparison of two files.
	 * @param filename1 name of the first file.
	 * @param filename2 name of the second file.
	 * @return true if their hashes are equal, false otherwise.
	 */
	private boolean compareFilesByHash( String filename1, String filename2 ) {
		String hash1 = SHA1.getInstance().hash( new File( filename1 ) );
		String hash2 = SHA1.getInstance().hash( new File( filename2 ) );
		return hash1.equals( hash2 );
	}
}
