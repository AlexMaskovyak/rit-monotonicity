package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Wrapper around Java's SHA1 Functionality
 * @author Joseph Pecoraro
 */
public class SHA1 {

	/** Used for getting the proper java.security.MessageDigest */
	private static final String SHA1_TYPE = "SHA-1";


	/**
	 * Quick hash of a byte array.
	 * @param bytes the byte array
	 * @return the SHA1 hash of the byte array as a hex String
	 * @throws NoSuchAlgorithmException
	 */
	public static String hash(byte[] bytes) throws NoSuchAlgorithmException {
		MessageDigest sha = MessageDigest.getInstance(SHA1_TYPE);
		sha.update(bytes);
		return convertBytesToHexString( sha.digest() );
	}


	/**
	 * Quick hash of a String
	 * @param the String
	 * @return the SHA1 hash of the byte array as a hex String
	 * @throws NoSuchAlgorithmException
	 */
	public static String hash(String input) throws NoSuchAlgorithmException {
		return hash( input.getBytes() );
	}


	/**
	 * Converts a byte array into its more readable hex String representation.
	 * @param bytes the byte array
	 * @return a String of characters 0-9,a-z of the hex representation of the bytes.
	 */
	private static String convertBytesToHexString(byte[] bytes) {
		char[] table = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<bytes.length; i++) {
			int low  = (bytes[i]) & 0x0F;
			int high = (bytes[i] >>> 4) & 0x0F;
			buf.append( table[high] );
	    	buf.append( table[low] );
		}
		return buf.toString();
	}


	/*-----------------------------
	 *         In Limbo
	 *----------------------------- */


    public static void hashIntoStream(ByteArrayInputStream inputStream) {
    	MessageDigest hash;
		try {
			hash = MessageDigest.getInstance("SHA1");
			DigestInputStream digestInputStream = new DigestInputStream(inputStream, hash);
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			for (int r; (r=digestInputStream.read())>=0;) { byteArrayOutputStream.write(r); }
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
    }




    public static String hash3(String in) throws Exception {

        byte[] input = in.getBytes();
        System.out.println("input     : " + new String(input));
        MessageDigest hash = MessageDigest.getInstance("SHA1");

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input);
        DigestInputStream digestInputStream = new DigestInputStream(byteArrayInputStream, hash);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int ch;
        while ((ch = digestInputStream.read()) >= 0) {
          byteArrayOutputStream.write(ch);
        }

        byte[] newInput = byteArrayOutputStream.toByteArray();
        digestInputStream.getMessageDigest().digest();

        byteArrayOutputStream = new ByteArrayOutputStream();
        DigestOutputStream digestOutputStream = new DigestOutputStream(byteArrayOutputStream, hash);
        digestOutputStream.write(newInput);
        digestOutputStream.close();

        return convertBytesToHexString( digestOutputStream.getMessageDigest().digest() );
        //System.out.println("out digest: " + new String(digestOutputStream.getMessageDigest().digest()));
      }





    /**
     * Test the implementations
     * @throws NoSuchAlgorithmException
     */
    public static void main(String[] args) throws NoSuchAlgorithmException {

    	// Input
    	String input = "Hello World";

    	// Test the basic hash methods
    	System.out.println("Hash 1:");
		String h1 = SHA1.hash(input);
		System.out.println(h1);

		// Test the stream methods


    }

}
