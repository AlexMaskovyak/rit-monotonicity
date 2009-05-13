package raids;

import java.util.List;
import java.util.Vector;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeContent;

/**
 * Storage request messages are multicast to nodes as an acknowledgement of 
 * willingness to store a file of a particular size and to take responsibility
 * for maintaining their section of the heartbeat ring.  Nodes responding
 * positively to a storage request modify this message accordingly.
 * 
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joe Pecoraro
 */
public class StorageRequest implements ScribeContent, Message {
	
	/** Generated serial version. */
	private static final long serialVersionUID = -9222355288468873754L;
	
	/** Node requesting storage. */
	private NodeHandle m_from;

	/** Node responding positively to a request for storage. */
	private NodeHandle m_response;
	
	/** List of nodes which are not allowed to respond positively to this 
	 *  request.  These are nodes which are already storing a copy of the 
	 *  file chunk. */
	private List<NodeHandle> m_excluded;
	
	/** Storage space being requested. */
	private long m_size;

	/**
	 * Default constructor.  Creates the initial request message when no copies
	 * of a chunk have been stored.
	 * @param from node requesting space.
	 * @param size amount of space being requested to store a file chunk.
	 */
	public StorageRequest(NodeHandle from, long size) {
		super();
		m_size = size;
		m_from = from;
		m_response = null;
		m_excluded = new Vector<NodeHandle>();
	}

	/**
	 * Constructor.  Creates a request message when copies of a chunk have 
	 * already been stored.  Used when a node in a ring has failed.
	 * @param from node requesting space.
	 * @param size amount of space being requested to store a file chunk.
	 * @param excluded list of nodes which already store this chunk and hence
	 * 					are not allowed to positively respond.
	 */
	public StorageRequest(NodeHandle from, long size, List<NodeHandle> excluded) {
		super();
		m_size = size;
		m_from = from;
		m_response = null;
		m_excluded = excluded;
	}

	public int getPriority() {
		return MAX_PRIORITY;
	}

	public String toString(){
		return "from: "+ m_from.getId().toString() + " size: " + m_size;
	}


//	Getters and Setters

	/**
	 * Obtains the node that originated this request.
	 * @return node which originated this request.
	 */
	public NodeHandle getFrom() {
		return m_from;
	}

	/**
	 * Specifies the node that is originating this request.
	 * @param from the node originating this request.
	 */
	public void setFrom(NodeHandle from) {
		m_from = from;
	}

	/**
	 * Obtains the handle of a node which has acknowledged that it is willing
	 * and able to store a file chunk.
	 * @return node which has the ability to fulfill the request.
	 */
	public NodeHandle getResponse() {
		return m_response;
	}

	/**
	 * Specifies the handle of a node which has acknowledged that it is willing
	 * and able to store a file chunk.
	 * @param response node which has the ability to fulfill the request.
	 */
	public void setResponse(NodeHandle response) {
		m_response = response;
	}

	/**
	 * Obtains the list of nodes which are disallowed from responding to this 
	 * request message since they already are storing a copy of the chunk for 
	 * which space is being requested.
	 * @return list of nodes which are disallowed from responding to this 
	 * 			request.
	 */
	public List<NodeHandle> getExcluded() {
		return m_excluded;
	}

	/**
	 * Specifies the list of nodes which are disallowed from responding to this
	 * request message since they already are storing a copy of the chunk for
	 * which space is being requested. 
	 * @param excluded list of nodes which are disallowed from responding to
	 * 			this request.
	 */
	public void setExcluded(List<NodeHandle> excluded) {
		m_excluded = excluded;
	}

	/**
	 * Obtain the amount of storage being requested
	 * @return the amount of storage being requested.
	 */
	public long getSize() {
		return m_size;
	}

	/**
	 * Specifies the amount of storage being requested.
	 * @param size the amount of storage being requested.
	 */
	public void setSize(long size) {
		m_size = size;
	}
}
