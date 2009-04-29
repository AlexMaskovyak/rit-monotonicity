package raids;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeContent;

public class StorageRequest implements ScribeContent, Message{
	private NodeHandle m_from;
	private NodeHandle m_response;
	private long m_size;

	public StorageRequest(NodeHandle from, long size) {
		super();
		m_size = size;
		m_from = from;
		m_response = null;
	}

	public NodeHandle getFrom() {
		return m_from;
	}
	public void setFrom(NodeHandle from) {
		m_from = from;
	}

	public String toString(){
		return "from: "+ m_from.getId().toString() + " size: " + m_size;
	}

	@Override
	public int getPriority() {
		return MAX_PRIORITY;
	}

	public NodeHandle getResponse() {
		return m_response;
	}

	public void setResponse(NodeHandle response) {
		m_response = response;
	}




}
