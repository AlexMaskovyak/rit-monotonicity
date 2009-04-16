package raids;

import java.util.List;
import java.util.Vector;

import rice.p2p.commonapi.Id;
import rice.p2p.past.ContentHashPastContent;

/**
 * PastContent storage data structure that stores
 * a User's List of Files.
 *
 * @author Joseph Pecoraro
 */
public class PersonalFileListContent extends ContentHashPastContent {

	/** The content of the message */
	private List<PersonalFileInfo> m_list;


	/**
	 * Default Constructor
	 * Creates an Empty list of Personal Files.
	 * @param myId id of the content
	 */
	public PersonalFileListContent(Id myId) {
		super(myId);
		m_list = new Vector<PersonalFileInfo>();
	}


	/**
	 * Constructor
	 * Initializes storage with a list of files
	 * @param myId id of the content
	 * @param list list of files
	 */
	public PersonalFileListContent(Id myId, List<PersonalFileInfo> list) {
		super(myId);
		m_list = new Vector<PersonalFileInfo>();
		if (list != null) {
			m_list.addAll(list);
		}
	}


	/**
	 * Access to the list of files
	 * @return the list of files
	 */
	public List<PersonalFileInfo> getList() {
		return m_list;
	}


	/**
	 * Replace the list of files
	 * @param list new list of files
	 */
	public void setList(List<PersonalFileInfo> list) {
		m_list = list;
	}


	/**
	 * For Debugging.
	 */
	public String toString() {
		return "PersonalFileListContent: " + m_list.toString();
	}

}
