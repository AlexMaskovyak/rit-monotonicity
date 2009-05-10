package raids;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * Authoritative Recovery Message.  Whoever receives
 * this message is being told that for the given part
 * their new next node is the given node.  The sender
 * has worked out all of the details.
 *
 * @author Joseph Pecoraro
 */
public class RecoverMessage implements Message {

	/** From NodeHandle */
	private NodeHandle m_fromHandle;

	/** Part this is concerning */
	private PartIndicator m_part;

	/** The Next Node */
	private NodeHandle m_newNext;


	/**
	 * Basic Constructor to create a Recovery message
	 * @param fromHandle Node this Message is coming from
	 * @param part the part this is concerning
	 * @param newNextNode the new next node to send thumps to
	 */
	public RecoverMessage(NodeHandle fromHandle, PartIndicator part, NodeHandle newNextNode) {
		m_fromHandle = fromHandle;
		m_part = part;
		m_newNext = newNextNode;
	}


	/**
	 * Default Priority
	 */
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}


// Getters and Setters

	public NodeHandle getHandle() {
		return m_fromHandle;
	}

	public void setHandle(NodeHandle fromHandle) {
		m_fromHandle = fromHandle;
	}

	public PartIndicator getPart() {
		return m_part;
	}

	public void setPart(PartIndicator part) {
		m_part = part;
	}

	public NodeHandle getNewNext() {
		return m_newNext;
	}

	public void setNewNext(NodeHandle newNext) {
		m_newNext = newNext;
	}

}
