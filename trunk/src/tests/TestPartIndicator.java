package tests;

import raids.PartIndicator;
import junit.framework.TestCase;

/**
 * Ensure we are properly turning the Object into bytes
 * when sending over the wire through the ByteBuffer.
 *
 * @author Joseph Pecoraro
 */
public class TestPartIndicator extends TestCase {

//	Constants

    /** SHA */
    private static final String TEXT_SHA = "d12fa9ab27473e663908ed6b68038ac1e59a25f1"; // 40 char SHA1

    /** Different SHA */
    private static final String TEXT_SHA_2 = "abcdefab27473e663908ed6b68038ac1e59a25f1"; // 40 char SHA1

    /** Part Number */
    private static final int PART_NUM = 2;


//	Test Cases

    /**
     * Ensure SIZE Constant is correct
     */
    public void testSize() {
    	PartIndicator pi = new PartIndicator(TEXT_SHA, PART_NUM);
    	byte[] actual = pi.toBytes();
    	assertEquals(actual.length, PartIndicator.SIZE);
    }


    /**
     * Test that toBytes gives us what we would expect
     */
    public void testToBytes() {

    	// Expected
    	byte[] expected = new byte[PartIndicator.SIZE];
    	System.arraycopy(TEXT_SHA.getBytes(), 0, expected, 0, TEXT_SHA.length());
    	expected[40] = (byte) 0x00;
    	expected[41] = (byte) 0x00;
    	expected[42] = (byte) 0x00;
    	expected[43] = (byte) PART_NUM;

    	// Actual
    	PartIndicator pi = new PartIndicator(TEXT_SHA, PART_NUM);
    	byte[] actual = pi.toBytes();

    	// Assert Equal
    	assertEquals(expected.length, actual.length);
    	for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], actual[i]);
		}

    }


    /**
     * Test that fromBytes gives us what we would expect
     */
    public void testFromBytes() {

    	// Desired Byte Representation
    	byte[] expected = new byte[PartIndicator.SIZE];
    	System.arraycopy(TEXT_SHA.getBytes(), 0, expected, 0, TEXT_SHA.length());
    	expected[40] = (byte) 0x00;
    	expected[41] = (byte) 0x00;
    	expected[42] = (byte) 0x00;
    	expected[43] = (byte) PART_NUM;

    	// Create From Bytes
    	PartIndicator pi = new PartIndicator(expected);

    	// Assert Correct Values
    	assertEquals(TEXT_SHA, pi.getOrigHash());
    	assertEquals(PART_NUM, pi.getPartNum());

    	// Take Two, Create From Bytes using an Existing To Bytes!
    	PartIndicator pi2 = new PartIndicator( pi.toBytes() );
    	assertEquals(pi.getOrigHash(), pi2.getOrigHash());
    	assertEquals(pi.getPartNum(), pi2.getPartNum());
    	assertEquals(pi, pi2);

    }


    /**
     * Test Equals
     */
    public void testEquals() {

    	// Really Equals
    	PartIndicator pi1 = new PartIndicator(TEXT_SHA, PART_NUM);
    	PartIndicator pi2 = new PartIndicator(TEXT_SHA, PART_NUM);
    	assertTrue( pi1.equals(pi2) );

    	// Not Equals Part Number
    	PartIndicator pi3 = new PartIndicator(TEXT_SHA, PART_NUM+1);
    	assertFalse( pi1.equals(pi3) );

    	// Not Equals String
    	PartIndicator pi4 = new PartIndicator(TEXT_SHA_2, PART_NUM);
    	assertFalse( pi1.equals(pi4) );

    }

}
