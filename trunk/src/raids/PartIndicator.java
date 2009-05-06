package raids;

import java.nio.ByteBuffer;

/**
 * Both the original File lookup Hash and the
 * part number.  This doesn't give away the
 * individual chunk's hash.
 *
 * @author Joseph Pecoraro
 */
public class PartIndicator {

//	Constants

	/** The Byte Size of this object */
	public static int SIZE = 40+4; /* SHA1 String + int */

//	Fields

	/** Original File Hash */
	private String m_origHash;

	/** The Part Number */
	private int m_partNum;


	/**
	 * Basic Constructor
	 * @param origHash the original file's hash
	 * @param partNum just a part of that file
	 */
	public PartIndicator(String origHash, int partNum) {
		m_origHash = origHash;
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
		m_origHash = new String( sha1_hash );
		m_partNum = buf.getInt();
	}


	/**
	 * Convert this structure to bytes
	 * @return an array of bytes representing this object
	 */
	public byte[] toBytes() {
		ByteBuffer buf = ByteBuffer.allocate(SIZE);
		buf.put( m_origHash.getBytes() );
		buf.putInt(m_partNum);
		System.out.println(buf.position());
		System.out.println(buf.limit());
		System.out.println(buf.capacity());
		return buf.array();
	}


	/**
	 * Debug Purposes
	 * @return a String representation of this object
	 */
	public String toString() {
		return m_origHash + ":" + m_partNum;
	}


// Getters and Setters

	public String getOrigHash() {
		return m_origHash;
	}

	public void setOrigHash(String origHash) {
		m_origHash = origHash;
	}

	public int getPartNum() {
		return m_partNum;
	}

	public void setPartNum(int partNum) {
		m_partNum = partNum;
	}

}
