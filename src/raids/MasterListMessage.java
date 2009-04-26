package raids;

import rice.p2p.commonapi.Message;

/**
 * Sent when a file upload points have been determined.
 * Contains the list of Masters for Components.
 *
 * @author Joseph Pecoraro
 */
public class MasterListMessage implements Message {

	/**
	 * Default Priority
	 */
	public int getPriority() {
		return 0;
	}

}
