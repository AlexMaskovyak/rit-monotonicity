package raids;

import java.util.List;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.PastException;

/**
 * Sent when a file upload points have been determined. Contains the node list 
 * for each part of the original file, and a lookup key for the DHT to find the 
 * current master list.
 *
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class MasterListMessage implements Message, PastContent {

    /** Parts Map */
    private List<NodeHandle>[] m_parts;

    /** Lookup Id */
    private Id m_lookupId;


    /**
     * Basic Constructor
     *
     * @param lookupId the Pastry ID that can be used to lookup the current MasterList in the DHT
     * @param parts the Map of Part numbers to their list
     */
    public MasterListMessage(Id lookupId, List<NodeHandle>[] parts) {
        setParts( parts );
        setLookupId( lookupId );
    }


    /**
     * Default Priority
     */
    public int getPriority() {
        return 0;
    }


    /**
     * For Debugging
     * @return String representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append( m_lookupId.toStringFull() );
        for (List<NodeHandle> l : m_parts) { buf.append( l.toString() + "\n" ); }
        return buf.toString();
    }


// Getters and Setters

    public List<NodeHandle>[] getParts() {
        return m_parts;
    }

    public void setParts(List<NodeHandle>[] parts) {
        m_parts = parts;
    }

    public Id getLookupId() {
        return m_lookupId;
    }

    public void setLookupId(Id lookupId) {
        m_lookupId = lookupId;
    }


    
	@Override
	public PastContent checkInsert(Id arg0, PastContent arg1)
			throws PastException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PastContentHandle getHandle(Past arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isMutable() {
		// TODO Auto-generated method stub
		return false;
	}

}
