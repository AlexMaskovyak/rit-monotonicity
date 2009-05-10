package raids;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Both the original File lookup Hash and the
 * part number.  This doesn't give away the
 * individual chunk's hash.
 *
 * @author Joseph Pecoraro
 */
public class PartIndicator implements Serializable {

//	Constants

	/** The Byte Size of this object */
	public static int SIZE = 40+4; /* SHA1 String + int */

//	Fields

	/** Original File Hash */
	private String m_lookupId;

	/** The Part Number */
	private int m_partNum;


	/**
	 * Basic Constructor
	 * @param lookupId the original file's lookup id
	 * @param partNum just a part of that file
	 */
	public PartIndicator(String lookupId, int partNum) {
		m_lookupId = lookupId;
		m_partNum = partNum;
	}


	/**
	 * Constructor from bytes
	 * @param bytes the hash and partNum in binary
	 */
	public PartIndicator(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.position(0);
		byte[] sha1_hash = new byte[40];
		buf.get(sha1_hash);
		m_lookupId = new String( sha1_hash );
		m_partNum = buf.getInt();
	}


	/**
	 * Convert this structure to bytes
	 * @return an array of bytes representing this object
	 */
	public byte[] toBytes() {
		ByteBuffer buf = ByteBuffer.allocate(SIZE);
		buf.put( m_lookupId.getBytes() );
		buf.putInt(m_partNum);
		return buf.array();
	}


	/**
	 * Equality by checking components
	 * @return true if both objects deal with the same file hash and part number
	 */
	public boolean equals(Object other) {
		if ( other instanceof PartIndicator ) {
			PartIndicator pi = (PartIndicator) other;
			return ( m_partNum == pi.getPartNum() ) && ( m_lookupId.equals(pi.getLookupId()) );
		}
		return false;
	}


	/**
	 * Attempt at a unique hash code in 2^32 space
	 * @return a primitive hash of the SHA1 and part number
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_lookupId == null) ? 0 : m_lookupId.hashCode());
		result = prime * result + m_partNum;
		return result;
	}


	/**
	 * For Debug
	 * @return a String representation of this object
	 */
	public String toString() {
		return m_lookupId + ":" + m_partNum;
	}


// Getters and Setters

	public String getLookupId() {
		return m_lookupId;
	}

	public void setLookupId(String origHash) {
		m_lookupId = origHash;
	}

	public int getPartNum() {
		return m_partNum;
	}

	public void setPartNum(int partNum) {
		m_partNum = partNum;
	}

}
