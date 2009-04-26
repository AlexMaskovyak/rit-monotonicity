package raids;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * Keep-alive message.
 *
 * @author Joseph Pecoraro
 */
public class HeartbeatMessage implements Message {

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
		return 0;
	}


// Getters and Setters

	public NodeHandle getHandle() {
		return m_fromHandle;
	}

	public void setHandle(NodeHandle fromHandle) {
		m_fromHandle = fromHandle;
	}

}
