package raids;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * Authoritative Recovery Message.  Whoever receives
 * this message is being told that for the given part
 * their new next node is the given node.  The sender
 * has worked out all of the details.
 *
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class RecoverMessage implements Message {

	/** Generated serial version. */
	private static final long serialVersionUID = 7189058367368838816L;

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

	/**
	 * Obtain the handle of the sending node.
	 * @return handle of the sending node.
	 */
	public NodeHandle getHandle() {
		return m_fromHandle;
	}

	/**
	 * Specify the handle of the sending node.
	 * @param fromHandle hanlde of the sending node.
	 */
	public void setHandle(NodeHandle fromHandle) {
		m_fromHandle = fromHandle;
	}

	/**
	 * Obtain the part indicator which needs to be recovered.
	 * @return the part indicator to recover.
	 */
	public PartIndicator getPart() {
		return m_part;
	}
 
	/**
	 * Specifies the part indicator which needs to be recovered.
	 * @param part part indicator  to recover
	 */
	public void setPart(PartIndicator part) {
		m_part = part;
	}

	/**
	 * Obtains the new next node to which a heartbeat must be established for
	 * full chain recovery.
	 * @return the new next node to which a heartbeat must be established.
	 */
	public NodeHandle getNewNext() {
		return m_newNext;
	}

	/**
	 * Specifies the new replacement node to which a heartbeat must be 
	 * established by the receiving node.
	 * @param newNext new replacement node to which to set a heartbeat.
	 */
	public void setNewNext(NodeHandle newNext) {
		m_newNext = newNext;
	}
}