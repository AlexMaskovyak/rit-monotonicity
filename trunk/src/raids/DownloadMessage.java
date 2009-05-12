package raids;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * Request made to download a file part.
 *
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class DownloadMessage implements Message {

    /** Generated serial version */
	private static final long serialVersionUID = 4648589701795931152L;

	/** The PartIndicator of the file wanted */
    private PartIndicator m_partIndicator;

    /** Requester - the person to send the data back to */
    private NodeHandle m_requester;


    /**
     * Basic Constructor
     * @param partIndicator indicate which part is wanted
     */
    public DownloadMessage(PartIndicator partIndicator, NodeHandle requester) {
        m_partIndicator = partIndicator;
        m_requester = requester;
    }


    /**
     * Default Priority
     */
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }


//	Getters and Setters

    /**
     * Obtains the part indicator corresponding to this download request.
     * @return part indicator being requested.
     */
    public PartIndicator getPartIndicator() {
        return m_partIndicator;
    }

    /**
     * Specifies the part indicator being requested.
     * @param partIndicator part indicator being requested.
     */
    public void setPartIndicator(PartIndicator partIndicator) {
        m_partIndicator = partIndicator;
    }

    /**
     * Obtains the handle of the node requesting this download.
     * @return node requesting this download.
     */
	public NodeHandle getRequester() {
		return m_requester;
	}

	/**
	 * Specifies the handle of the node initiating this download request.
	 * @param requester node requesting this download.
	 */
	public void setRequester(NodeHandle requester) {
		m_requester = requester;
	}

}
