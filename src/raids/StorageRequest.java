package raids;

import java.util.List;
import java.util.Vector;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeContent;

public class StorageRequest implements ScribeContent, Message{
	private static final long serialVersionUID = -9222355288468873754L;
	private NodeHandle m_from;
	private NodeHandle m_response;
	private List<NodeHandle> m_excluded;
	private long m_size;

	public StorageRequest(NodeHandle from, long size) {
		super();
		m_size = size;
		m_from = from;
		m_response = null;
		m_excluded = new Vector<NodeHandle>();
	}

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

	public NodeHandle getFrom() {
		return m_from;
	}

	public void setFrom(NodeHandle from) {
		m_from = from;
	}

	public NodeHandle getResponse() {
		return m_response;
	}

	public void setResponse(NodeHandle response) {
		m_response = response;
	}

	public List<NodeHandle> getExcluded() {
		return m_excluded;
	}

	public void setExcluded(List<NodeHandle> excluded) {
		m_excluded = excluded;
	}

	public long getSize() {
		return m_size;
	}

	public void setSize(long size) {
		m_size = size;
	}

}
