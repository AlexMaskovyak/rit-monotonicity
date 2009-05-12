package raids;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * Keep-alive message.
 *
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class HeartbeatMessage implements Message {

	/** Generated serial id */
	private static final long serialVersionUID = 2937247831074452170L;
	
	/** From NodeHandle */
	private NodeHandle m_fromHandle;

	/**
	 * Basic Constructor to create a Heartbeat message
	 * @param fromHandle Node this Heartbeat is coming from
	 */
	public HeartbeatMessage(NodeHandle fromHandle) {
		m_fromHandle = fromHandle;
	}

	/**
	 * Default Priority
	 */
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}


// Getters and Setters

	/**
	 * Obtains the handle of the Node which sent this message.
	 * @return sending node's handle.
	 */
	public NodeHandle getHandle() {
		return m_fromHandle;
	}

	/**
	 * Specifies the handle of the node from which this message was sent.
	 * @param fromHandle sending node's handle.
	 */
	public void setHandle(NodeHandle fromHandle) {
		m_fromHandle = fromHandle;
	}

}
