package raids;

import java.io.Serializable;

/**
 * Data Structure representing a File a User has uploaded
 */
public class PersonalFileInfo implements Serializable {

	/** File Name */
	private String m_name;

	/**
	 * Create a Personal File with a given name
	 * @param name the file name
	 */
	public PersonalFileInfo(String name) {
		m_name = name;
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
	public void setName(String name) {
		m_name = name;
	}

	/**
	 * For Debugging
	 * @return a string representation of this object
	 */
	public String toString() {
		return super.toString() + ":" + m_name;
	}

}
