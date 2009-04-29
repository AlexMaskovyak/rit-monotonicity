package raids;

import rice.p2p.commonapi.Message;

/**
 * Sent when a file upload points have been determined.
 * Contains the list of Masters for Components.
 *
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class MasterListMessage implements Message {

	MasterListContent m_masterListContent;
	
	/**
	 * Default constructor.
	 * @param masterListContent master content list contains node handles to
	 * 			all masters servicing this file.
	 */
	public MasterListMessage( MasterListContent masterListContent ) {
		m_masterListContent = masterListContent;
	}
	
	/**
	 * Default Priority
	 */
	public int getPriority() {
		return 0;
	}

}
