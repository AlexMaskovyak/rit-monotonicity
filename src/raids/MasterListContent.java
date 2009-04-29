package raids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;

/**
 * PastContent storage data structure that stores a List of NodeHandle objects
 * pointing to MasterNodes for the file key that mapped to this entry.
 *
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class MasterListContent extends PastContentList<NodeHandle> {

	/**
	 * Default Constructor
	 * Creates an Empty list of Masters.
	 * @param myId id of the content
	 */
	public MasterListContent(Id myId) {
		super( myId );
	}

	/**
	 * Constructor.
	 * Initializes storage with an array of NodeHandles
	 * @param myId id of the content
	 * @param handles list of node handles
	 */
	public MasterListContent( Id myId, NodeHandle... handles ) {
		this( myId, Arrays.asList( handles ) );
	}

	/**
	 * Constructor
	 * Initializes storage with a list of NodeHandles
	 * @param myId id of the content
	 * @param list list of node handles
	 */
	public MasterListContent(Id myId, List<NodeHandle> list) {
		super(myId, list);
	}

}
