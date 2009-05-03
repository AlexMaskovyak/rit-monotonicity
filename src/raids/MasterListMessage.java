package raids;

import java.util.List;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * Sent when a file upload points have been determined.
 * Contains the list of masters, the node list for
 * each part of the original file, and a lookup key
 * for the DHT to find the current master list.
 *
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class MasterListMessage implements Message {

    /** List of Masters */
    private List<NodeHandle> m_masterList;

    /** Parts Map */
    private List<NodeHandle>[] m_parts;

    /** Lookup Id */
    private Id m_lookupId;


    /**
     * Basic Constructor
     *
     * @param masterList the list of Masters
     * @param parts the Map of Part numbers to their list
     * @param lookupId the Pastry ID that can be used to lookup the current MasterList in the DHT
     */
    public MasterListMessage(List<NodeHandle> masterList, List<NodeHandle>[] parts, Id lookupId) {
        m_masterList = masterList;
        m_parts = parts;
        m_lookupId = lookupId;
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
        buf.append( m_masterList.toString() + "\n" );
        return buf.toString();
    }


// Getters and Setters

    public List<NodeHandle> getMasterList() {
        return m_masterList;
    }

    public void setMasterList(List<NodeHandle> masterList) {
        m_masterList = masterList;
    }

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

}
