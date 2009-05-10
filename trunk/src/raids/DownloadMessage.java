package raids;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * Request made to download a file part.
 *
 * @author Joseph Pecoraro
 */
public class DownloadMessage implements Message {

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

    public PartIndicator getPartIndicator() {
        return m_partIndicator;
    }

    public void setPartIndicator(PartIndicator partIndicator) {
        m_partIndicator = partIndicator;
    }

	public NodeHandle getRequester() {
		return m_requester;
	}

	public void setRequester(NodeHandle requester) {
		m_requester = requester;
	}

}
