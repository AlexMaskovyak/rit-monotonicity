package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Wrapper around Java's SHA1 Functionality implemented as a singleton.
 * 
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class SHA1 {

	///
	///
	/// Hidden variables.
	///
	///

	/** Used for getting the proper java.security.MessageDigest */
	private static final String SHA1_TYPE = "SHA-1";

	/** Singleton instance. */
	private static SHA1 m_instance;

	/** MessageDigest is responsible for hashing. */
	private static MessageDigest m_MessageDigest;

	///
	///
	/// Constructors.
	///
	///

	/**
	 * Hidden constructor.  Non-accessible for this singleton.
	 */
	private SHA1() {
		try {
			m_MessageDigest = MessageDigest.getInstance( SHA1_TYPE );
		} catch (NoSuchAlgorithmException e) {
			/* this should never occur since we know java implements this hash
			 * here we're wrapping the error so that we never ever see it
			 * again. */
			m_MessageDigest = null;
		}
	}

	/**
	 * Get the SHA1 instance.
	 * @return SHA1 instance.
	 */
	public static SHA1 getInstance() {
		if( m_instance == null ) {
			m_instance = new SHA1();
		}

		return m_instance;
	}

	///
	///
	/// Operations.
	///
	///

	/**
	 * Quick hash of a byte array.
	 * @param bytes the byte array
	 * @return the SHA1 hash of the byte array as a hex String
	 */
	public String hash( byte[] bytes ) {
		m_MessageDigest.reset();
		m_MessageDigest.update( bytes );
		return convertBytesToHexString( m_MessageDigest.digest() );
	}


	/**
	 * Quick hash of a file.
	 * @param p_File file whose contents are to be hashed.
	 * @return the SHA1 hash of the file contents as a hex String.
	 */
	public String hash( File p_File ) {
		String hashValue = null;

		try {
			hashValue = hash( new FileInputStream( p_File ) );
		} catch ( Exception e ) {
			e.printStackTrace();
		} finally {
		}

		return hashValue;
	}

	/**
	 * Quick hash of an input stream.
	 * @param p_inStream inputstream whose contents are to be hashed.
	 * @return the SHA1 hash of the inputstream contents as a hex String.
	 */
	public String hash( InputStream p_inStream ) {
	    String hashValue = null;

	    try {
			int dataSize = p_inStream.available();
		    byte[] bytes = new byte[ dataSize ];
		    p_inStream.read( bytes );
		    hashValue = hash( bytes );
	    } catch ( Exception e ) {
	    } finally {
	    }

	    return hashValue;
	}

	/**
	 * Quick hash of a String
	 * @param the String
	 * @return the SHA1 hash of the byte array as a hex String
	 */
	public String hash( String input ) {
		return hash( input.getBytes() );
	}


	/**
	 * Converts a byte array into its more readable hex String representation.
	 * @param bytes the byte array
	 * @return a String of characters 0-9,a-z of the hex representation of the bytes.
	 */
	private static String convertBytesToHexString( byte[] bytes ) {
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
        //System.out.println("input     : " + new String(input));
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

}
