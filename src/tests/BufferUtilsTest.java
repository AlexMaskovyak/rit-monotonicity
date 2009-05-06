package tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import raids.PartIndicator;

import junit.framework.TestCase;

import util.BufferUtils;

/**
 * TestSuite for the BufferUtils Class
 *
 * @author Joseph Pecoraro
 */
public class BufferUtilsTest extends TestCase {

// Constants

    /** Path to the directory (this) where the test files are stored. */
    private static final String TEST_PATH = "src/tests/util/";

    /** Text File */
    private static final String TEXT_FILENAME = "hello_world.txt";

    /** Temporary File */
    private static final String TEXT_FILE_NAME_DUP = "hello_world_dup.txt";

    /** String Contents */
    private static final String TEXT_CONTENTS = "Hello world!";

    /** SHA */
    private static final String TEXT_SHA = "d12fa9ab27473e663908ed6b68038ac1e59a25f1"; // 40 char SHA1


// Test Cases

    /**
     * Test creating a ByteBuffer for a file
     */
    public void testBufFile() {

        // Setup
        ByteBuffer expected = ByteBuffer.wrap( TEXT_CONTENTS.getBytes() );
        ByteBuffer actual = BufferUtils.getBufferForFile(TEST_PATH + TEXT_FILENAME, TEXT_CONTENTS.length());
        actual.flip(); // Position is set to 0, limit is set to where the position _was_

        // Looking at the high level
        assertEquals( expected.position(), actual.position() );
        assertEquals( expected.limit(), actual.limit() );
        assertEquals( expected.capacity(), actual.capacity() );
        // assertEquals( expected, actual );

        // Looking at the innards
        byte[] arr1 = expected.array();
        byte[] arr2 = actual.array();
        for (int i = 0; i < arr1.length; i++) {
            assertEquals( arr1[i], arr2[i] );
        }

    }


    /**
     * Test creating a ByteBuffer for a file and a string prefix
     */
    public void testBufFileAndPrefix() {

        // Setup
    	PartIndicator pi = new PartIndicator(TEXT_SHA, 4);
        String total = TEXT_SHA + "0004" + TEXT_CONTENTS;
        byte[] bytes = total.getBytes();
        bytes[40] = (byte)0x00; // int high byte
        bytes[41] = (byte)0x00;
        bytes[42] = (byte)0x00;
        bytes[43] = (byte)0x04; // int low byte
        ByteBuffer expected = ByteBuffer.wrap( bytes );
        ByteBuffer actual = BufferUtils.getBufferForFile(TEST_PATH + TEXT_FILENAME, total.length(), pi);
        actual.flip(); // Position is set to 0, limit is set to where the position _was_

        // Looking at the high level
        assertEquals( expected.position(), actual.position() );
        assertEquals( expected.limit(), actual.limit() );
        assertEquals( expected.capacity(), actual.capacity() );
        assertEquals( expected, actual );

        // Looking at the innards
        byte[] arr1 = expected.array();
        byte[] arr2 = actual.array();
        for (int i = 0; i < arr1.length; i++) {
        	System.out.println(i + ": " + arr1[i] + " " + arr2[i]);
            assertEquals( arr1[i], arr2[i] );
        }

    }


    /**
     * Test writing a File from a Buffer
     */
    public void testWritingFile() {
        ByteBuffer buf = ByteBuffer.wrap( TEXT_CONTENTS.getBytes() );
        File f = new File( TEST_PATH + TEXT_FILE_NAME_DUP );
        BufferUtils.writeBufferToFile(buf, f.getAbsolutePath(), false);
        assertTrue( compareFiles(TEST_PATH + TEXT_FILENAME, TEST_PATH + TEXT_FILE_NAME_DUP) );
        f.delete();
    }


// Private Helpers

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

}
