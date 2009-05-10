package raids;

import java.io.Serializable;

/**
 * Data Structure representing a File a User has uploaded
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joe Pecoraro
 */
public class PersonalFileInfo implements Serializable {

	/** File Name */
	private String m_name;
	private String m_hash;
	
	/**
	 * Create a Personal File with a given name
	 * @param name the file name
	 */
	public PersonalFileInfo(String name, String hash) {
		m_name = name;
		m_hash = hash;
	}

	/**
	 * Access the file name
	 * @return the file name
	 */
	public String getName() {
		return m_name;
	}

	/**
	 * Modify the file name
	 * @param name the new file name
	 */
	public void setName( String name ) {
		m_name = name;
	}

	/**
	 * Access the file's hash
	 * @return Hash of the file's contents when this structure was created.
	 */
	public String getHash() {
		return m_hash;
	}
	
	/**
	 * Modify the file's hash
	 * @param hash Hash of the file's contents.
	 */
	public void setHash( String hash ) {
		m_hash = hash;
	}
	
	/**
	 * For Debugging
	 * @return a string representation of this object
	 */
	public String toString() {
		return super.toString() + ":" + m_name;
	}

	/**
	 * Equality by checking filenames
	 * @return true if both objects had the same filename
	 */
	public boolean equals(Object other) {
		if ( other instanceof PersonalFileInfo ) {
			return m_name.equals( ((PersonalFileInfo)other).getName() );
		}
		return false;
	}

}
