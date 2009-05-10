package raids;

import java.util.List;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;

public class RecoveryThread extends Thread {

	/** The Application to delegate to */
	private RaidsApp m_delegate;

	/** The Part that must be Restored */
	private PartIndicator m_part;

	/** The Handle of the nodes that died */
	private NodeHandle m_died;


	/**
	 * Basic Constructor
	 * @param app the core Application
	 * @param part the part that must be restored
	 * @param died the node that died and should be cleaned up
	 */
	public RecoveryThread(RaidsApp app, PartIndicator part, NodeHandle died) {
		m_delegate = app;
		m_part = part;
		m_died = died;
	}


	/**
	 * When the thread runs
	 */
	public void run() {

		// Values we need
		String hash = m_part.getOrigHash();
		NodeHandle me = m_delegate.getNode().getLocalNodeHandle();
		Environment env = m_delegate.getNode().getEnvironment();
		int partNum = m_part.getPartNum();

		// TODO: Fetch the DHT list
		Id lookupId = GeneralIdHelper.idFromString(hash, env);
		MasterListMessage mlm = m_delegate.lookupMasterList(lookupId);
		System.out.println(">>> Looked up the MasterList");
		System.out.println(">>> " + mlm.toString());

		// More Values
		List<NodeHandle>[] newParts = mlm.getParts();
		NodeHandle prevNode = getPreviousToDeadNode( newParts[partNum] );

		// TODO: Multicast for Available Space

		// TODO: Choose a node
		NodeHandle choosen = null;
		// modify newParts;

		// TODO: Update the DHT list
		m_delegate.updateMasterList(lookupId, mlm.getParts());
		System.out.println(">>> Updated the MasterList");

		// TODO: Send Recover Message to the "previous" of dead node
		RecoverMessage recoverMessage = new RecoverMessage(me, m_part, choosen);
		m_delegate.routeMessageDirect(recoverMessage, prevNode);

		// TODO: Send MasterListMessage to the new replacement node
		mlm.setParts(newParts);
		m_delegate.routeMessageDirect(mlm, choosen);

		// TODO: Send the component to the replacement node

	}


	/**
	 * Given a list of nodes get the node prior to the dead node.
	 * This should wrap properly, so if the dead node is the first
	 * node in the list, then the last node in the list should
	 * be returned.  Returns null if the node was not in the list.
	 * @param list the list of nodes
	 * @return the node "before" this node, null if dead was not found
	 */
	private NodeHandle getPreviousToDeadNode(List<NodeHandle> list) {
		int idx = list.indexOf(m_died);
		if ( idx == -1 ) {
			return null;
		} else if ( idx == 0 ) {
			return list.get( list.size()-1 );
		} else {
			return list.get( idx-1 );
		}
	}

}
