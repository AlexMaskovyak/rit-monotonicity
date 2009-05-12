package raids;

import java.util.List;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.ContentHashPastContent;
import rice.p2p.past.PastContent;

/**
 * Sent when a file upload points have been determined. Contains the node list
 * for each part of the original file, and a lookup key for the DHT to find the
 * current master list.
 *
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class MasterListMessage extends ContentHashPastContent implements Message {

    /** Serial ID */
	private static final long serialVersionUID = 3588250315167081680L;

	/** Parts Map */
    private List<NodeHandle>[] m_parts;

    /** Lookup Id */
    private Id m_lookupId;


    /**
     * Basic Constructor
     *
     * @param lookupId the Pastry ID that can be used to lookup the current 
     * MasterList in the DHT
     * @param parts the Map of Part numbers to their list
     */
    public MasterListMessage(Id lookupId, List<NodeHandle>[] parts) {
    	super( lookupId );
        setParts( parts );
        setLookupId( lookupId );
    }


    /**
     * Default Priority
     */
    public int getPriority() {
        return DEFAULT_PRIORITY;
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


//	Required to allow Replacements

    /**
     * Always store the newest.
     */
    public PastContent checkInsert(Id id, PastContent existing) {
    	return this;
    }


    /**
     * Value can change
     */
    public boolean isMutable() {
    	return true;
    }


// Getters and Setters

    /**
     * Obtains the array of chunk holder lists.
     * @return structure containing a list of the nodes responsible for storing
     * 			specific pieces of a file.
     */
    public List<NodeHandle>[] getParts() {
        return m_parts;
    }

    /**
     * Specifies the array of chunk holder lists.
     * @param parts structure containing a list of the nodes responsible for
     * 				storing pieces of a file, where those nodes corresponding
     * 				to the first index store the first chunk of the file, etc.
     */
    public void setParts(List<NodeHandle>[] parts) {
        m_parts = parts;
    }

    /**
     * Obtains the Pastry ID that can be used to lookup the master list in the
     * DHT.
     * @return Pastry ID that can be used to lookup the master list.
     */
    public Id getLookupId() {
        return m_lookupId;
    }

    /**
     * Specifies the Pastry ID to use to lookup the master list in the DHT.
     * @param lookupId Pastry ID to use for master list lookup.
     */
    public void setLookupId(Id lookupId) {
        m_lookupId = lookupId;
    }
}
