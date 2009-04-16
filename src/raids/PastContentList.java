package raids;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import rice.p2p.commonapi.Id;
import rice.p2p.past.ContentHashPastContent;

/**
 * PastContent storage data structure that stores a List of Items.
 *
 * @author Joseph Pecoraro
 */
public class PastContentList<E extends Serializable> extends ContentHashPastContent {

	/** The content of the message */
	protected List<E> m_list;


	/**
	 * Default Constructor
	 * Creates an Empty list of Personal Files.
	 * @param myId id of the content
	 */
	public PastContentList(Id myId) {
		super(myId);
		m_list = new Vector<E>();
	}


	/**
	 * Constructor
	 * Initializes storage with a list of files
	 * @param myId id of the content
	 * @param list list of files
	 */
	public PastContentList(Id myId, List<E> list) {
		super(myId);
		m_list = new Vector<E>();
		if (list != null) {
			m_list.addAll(list);
		}
	}


	/**
	 * Access to the list of files
	 * @return the list of files
	 */
	public List<E> getList() {
		return m_list;
	}


	/**
	 * Replace the list of files
	 * @param list new list of files
	 */
	public void setList(List<E> list) {
		m_list = list;
	}


	/**
	 * For Debugging.
	 */
	public String toString() {
		return "PastContentList: " + m_list.toString();
	}

}
