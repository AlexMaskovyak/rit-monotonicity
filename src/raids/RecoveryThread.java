package raids;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import util.BufferUtils;

/**
 * Recovers from the loss of a node holding a particular chunk of a file.  This
 * thread obtains a node to replace the lost node and establishes heartbeat 
 * messages to repair the storage ring.
 * 
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joe Pecoraro
 */
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
		String hash = m_part.getLookupId();
		NodeHandle me = m_delegate.getNode().getLocalNodeHandle();
		Environment env = m_delegate.getNode().getEnvironment();
		int partNum = m_part.getPartNum();

		// Fetch the DHT list
		Id lookupId = GeneralIdHelper.idFromString(hash, env);
		MasterListMessage mlm = m_delegate.lookupMasterList(lookupId);
		System.out.println(">>> Looked up the MasterList");
		System.out.println(">>> " + mlm.toString());

		// More Values
		List<NodeHandle>[] newParts = mlm.getParts();
		List<NodeHandle> oldPartList = newParts[partNum];
		NodeHandle prevNode = getPreviousToDeadNode( newParts[partNum] );
		File file = m_delegate.lookupInInventory(m_part);
		System.out.println(">>> Looked up the File for the part: " + file);

		// At this point we either have the file or we don't.
		// If we don't have the file we don't know how large it is.
		// Thus its impossible to know how much space to request.
		// Fortunately, our algorithm right now assumes you always have
		// enough space... But in the real world, this wouldn't be available.
		// TODO: Somehow know how big the file needs to be? Store in MasterListMessage and then Inventory?
		int size = (file == null) ? 0 : (int)file.length();


		// Multicast for Available Space, get 1 node back
		NodeHandle[] storageNode = m_delegate.requestSpace(1, size, oldPartList);
		NodeHandle choosen = storageNode[0];
		System.out.println(">>> Requested space, got back: " + choosen.getId().toStringFull());

		// Update the MasterListMessage
		oldPartList.set( oldPartList.indexOf(m_died), choosen);
		mlm.setParts(newParts);
		System.out.println(">>> Updated the MasterList in memory");

		// Update the DHT
		m_delegate.updateMasterList(lookupId, mlm.getParts());
		System.out.println(">>> Updated the MasterList in the DHT");

		// TODO: Have the recovering node cascade the file (this is foolproof?)
		RecoverMessage recoverMessage = new RecoverMessage(me, m_part, choosen);
		m_delegate.routeMessageDirect(recoverMessage, prevNode);
		System.out.println(">>> Sent the RecoverMessage to " + prevNode.getId().toStringFull());

		// Send MasterListMessage to the new replacement node
		m_delegate.routeMessageDirect(mlm, choosen);
		System.out.println(">>> Sent the MasterListMessage to " + choosen.getId().toStringFull());

		// Fix our own inventory to point to the new previous node
		m_delegate.setPreviousNodeForPart(choosen, m_part);
		System.out.println(">>> Updated our own table to have the new previous for this part be " + choosen.getId().toStringFull());


		// Send the component to the replacement node
		// NOTE: if file is null this will fail.  We Need a special case
		// NOTE: See the earlier TODO about the RecoverMessage
		if ( file == null ) {
			System.out.println(">>> We don't even have the file... HANDLE THIS LATER.");
		} else {
			ByteBuffer buf = BufferUtils.getBufferForFile(file.getAbsolutePath(), size+PartIndicator.SIZE, m_part);
			buf.flip();
			m_delegate.sendBufferToNode(buf, choosen);
			System.out.println(">>> Sent the File Data to " + choosen.getId().toStringFull());
		}

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
		int idx = list.indexOf( m_died );
		if ( idx == -1 ) {
			return null;
		} else if ( idx == 0 ) {
			return list.get( list.size()-1 );
		} else {
			return list.get( idx-1 );
		}
	}

}
