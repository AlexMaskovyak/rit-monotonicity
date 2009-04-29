package tests;

import java.io.File;

import util.SHA1;
import junit.framework.TestCase;

/**
 * Setup SHA1 and ensure that the hashes are correct.
 *
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class TestSHA1 extends TestCase {

	/*
	 * NOTE: Expected Results were generated from a trusted 3rd party:
	 * http://www.whatsmyip.org/hash_generator/
	 */

	/** Path to the directory (this) where the test files are stored. */
	private static final String TEST_PATH = "src/tests/util/";

	/** Text File */
	private static final String TEXT_FILENAME = "test.txt";

	/**
	 * Simple test for a string
	 */
	public void testChunkString() {
		String testString = "Hello World";
		String expectedResult = "0a4d55a8d778e5022fab701977c5d840bbc486d0";
		String ourResult = SHA1.getInstance().hash(testString);
		assertEquals(ourResult, expectedResult);
	}

	/**
	 * Simple test for a File
	 */
	public void testChunkFile() {
		File f = new File( TEST_PATH + TEXT_FILENAME );
		String expectedResult = "92bb11ed62182f47afdf9868d1a2401b2d590773";
		String ourResult = SHA1.getInstance().hash(f);
		assertEquals(ourResult, expectedResult);
	}

}
