package raids;

import rice.p2p.commonapi.Message;

/**
 * Request made to download a file part.
 *
 * @author Joseph Pecoraro
 */
public class DownloadMessage implements Message {

	/** The PartIndicator of the file wanted */
	private PartIndicator m_partIndicator;


	/**
	 * Basic Constructor
	 * @param partIndicator indicate which part is wanted
	 */
	public DownloadMessage(PartIndicator partIndicator) {
		m_partIndicator = partIndicator;
	}


	/**
	 * Default Priority
	 */
	public int getPriority() {
		return 0;
	}


//	Getters and Setters

	public PartIndicator getPartIndicator() {
		return m_partIndicator;
	}

	public void setPartIndicator(PartIndicator partIndicator) {
		m_partIndicator = partIndicator;
	}

}
