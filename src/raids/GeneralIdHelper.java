package raids;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Node;
import rice.pastry.commonapi.PastryIdFactory;

public class GeneralIdHelper {

	/** Prefix for Personal File List entries in the DHT */
	private static final String PERSONAL_LIST_PREFIX = "PERSONAL_FILE_LIST";

	/** Prefix for Master List entries in the DHT */
	private static final String MASTER_LIST_PREFIX = "MASTER";


	/**
	 * Most Basic, Convert a String to an ID
	 */
	public static Id idFromString(String str, Environment env) {
		PastryIdFactory localFactory = new PastryIdFactory(env);
		return localFactory.buildIdFromToString(str);
	}


	/**
	 * Generate the Master List PastryId (the key for the DHT) for a given file name
	 * @param filename the filename
	 * @param username the username is prepended to the file to make it unique per user
	 * @param env the common environment
	 */
	public static Id masterListIdForFilename(String filename, String username, Environment env) {
		PastryIdFactory localFactory = new PastryIdFactory(env);
		String key = MASTER_LIST_PREFIX + username + filename;
		return localFactory.buildId(key);
	}


	/**
	 * Generate the Personal File List PastryId (the key for the DHT) for a given user
	 * @param username the user's username which when hashed gives the DHT Key
	 * @param env the common environment
	 * @return key for this user's personal file list
	 */
	public static Id personalFileListIdForUsername(String username, Environment env) {
		PastryIdFactory localFactory = new PastryIdFactory(env);
		String key = PERSONAL_LIST_PREFIX + username;
		return localFactory.buildId(key);
	}


	/**
	 * Generate the Personal File List PastryId (the key for the DHT) for a given Node
	 * @param node the node for which we generate their personal file list key
	 * @return key for this node's personal file list
	 */
	public static Id personalFileListIdForNode(Node node) {
		return personalFileListIdFromNodeId(node.getId(), node.getEnvironment());
	}


	/**
	 * Generate the Personal File List PastryId (the key for the DHT) from a Node Id
	 * @param nodeId the id of the node for which we generate the personal file list key
	 * @param env the common environment
	 * @return key for this node id
	 */
	public static Id personalFileListIdFromNodeId(Id nodeId, Environment env) {
		PastryIdFactory localFactory = new PastryIdFactory(env);
		String key = PERSONAL_LIST_PREFIX + nodeId.toStringFull();
		return localFactory.buildId(key);
	}

}
