package raids;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Vector;

import rice.environment.Environment;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.commonapi.PastryIdFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import rice.persistence.LRUCache;
import rice.persistence.MemoryStorage;
import rice.persistence.Storage;
import rice.persistence.StorageManagerImpl;


/**
 * Initializes potentially multiple Raids Pastry nodes on a
 * single JVM, connecting them to Eve and passing the
 * final configuration to a Terminal.
 *
 * @author Jeff Hoye (FreePastry Tutorial)
 * @author Joseph Pecoraro
 */
public class Client {

	/** Multiple applications */
	private Vector<RaidsApp> m_apps = new Vector<RaidsApp>();

	/** Configuration */
	private Properties m_config;


	/**
	 * This constructor launches numNodes PastryNodes. They will bootstrap to an
	 * existing ring if one exists at the specified location, otherwise it will
	 * start a new ring.
	 *
	 * @param bindport the local port to bind to
	 * @param bootaddress the IP:port of the node to boot from
	 * @param username the client's username
	 * @param numNodes the number of nodes to create in this JVM
	 * @param env the Environment
	 * @param config the Application Configuration Properties
	 * @throws Exception
	 */
	public Client(int bindport, InetSocketAddress bootaddress, String username,
			int numNodes, final Environment env, Properties config) throws Exception {

		// Create the nodes
		createNodes(bindport, bootaddress, username, numNodes, env);

		// Send all the apps (per this JVM) so the Terminal can switch between them
		new ClientTerminal( m_apps, env ).start();

	}


	/**
	 * Creates nodes with MemoryStorage
	 */
	public void createNodes(int bindport, InetSocketAddress bootaddress, String username,
			int n, final Environment env) throws Exception {

		// Generate the NodeIds Randomly
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

		// construct the PastryNodeFactory, this is how we use rice.pastry.socket
		PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

		// loop to construct the nodes/apps
		for (int curNode = 0; curNode < n; curNode++) {

			// construct a node, passing the null boothandle on the first loop will
			// cause the node to start its own ring
			PastryNode node = factory.newNode();
			node.boot(bootaddress);

			// the node may require sending several messages to fully boot into the ring
			synchronized(node) {
				while(!node.isReady() && !node.joinFailed()) {
					node.wait(500);
					if (node.joinFailed()) {
						throw new IOException("Could not join the FreePastry ring.  Reason:"+node.joinFailedReason());
					}
				}
			}

			// Debug
			System.out.println("Finished creating new node " + node);

			// used for generating PastContent object Ids.
			// this implements the "hash function" for our DHT
			PastryIdFactory idf = new PastryIdFactory(env);

			// TODO: Do Persistent Storage?
			// create a different storage root for each node
			@SuppressWarnings("unused")
			String storageDirectory = "./storage" + node.getId().hashCode();

			// create the persistent part
			// Storage stor = new PersistentStorage(idf, storageDirectory, 4 * 1024 * 1024, node.getEnvironment());
			Storage stor = new MemoryStorage(idf);
			String perNodeUsername = (curNode==0) ? username : username+(curNode+1);
			RaidsApp app = new RaidsApp(node, new StorageManagerImpl(idf, stor,
					new LRUCache(new MemoryStorage(idf), 512 * 1024, node.getEnvironment())), 1, "",
					perNodeUsername, m_config.getProperty("EVE_HOST"),
					Integer.parseInt( m_config.getProperty("EVE_USER")) );
			m_apps.add(app);

		}
	}


	/**
	 * Driver Program parses the command line
	 * arguments to setup some details.
	 */
	public static void main(String[] args) throws Exception {

		// Pastry Environment
		// Disable the UPnP setting (in case you are testing this on a NATted LAN)
		Environment env = new Environment();
		env.getParameters().setString("nat_search_policy","never");

		try {

			// Too Few or Too Many Command line arguments
			if ( args.length < 5 ) {
				System.err.println("Too Few Command Line Arguments");
				usage();
			} else if ( args.length > 7 ) {
				System.err.println("Too Many Command Line Arguments");
				usage();
			}

			// Parse Command Line Arguments
			int bindport = Integer.parseInt(args[0]);
			InetAddress bootaddr = args[1].equals("localhost") ? InetAddress.getLocalHost() : InetAddress.getByName(args[1]);
			int bootport = Integer.parseInt(args[2]);
			InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);
			String username = args[3];
			int numNodes = Integer.parseInt(args[4]);

			// Optional Configuration File
			Properties config = new Properties();
			/*
			if ( args.length >= 5 ) {
				File configFile = new File(args[4]);
				if ( configFile.exists() ) {
					config.load( new FileInputStream(configFile) );
				} else {
					System.err.println("WARNING: Config File did not exist: " + args[4]);
				}
			} else {
				System.err.println("WARNING: No Config File Given");
			}
			*/

			// Optional Eve host and Port overrides
			if ( args.length > 5 ) {
				config.setProperty("EVE_HOST", args[5]);
				config.setProperty("EVE_PORT", args[6]);
			} else {
				config.setProperty("EVE_HOST", null);
				config.setProperty("EVE_PORT", "0");
			}

			// Launch the Client
			System.out.println("New Client");
			new Client(bindport, bootaddress, username, numNodes, env, config);

		} catch (Exception e) {
			System.err.println("Couldn't Setup the Client");
			e.printStackTrace();
			env.destroy();
			usage();
		}

	}


	/**
	 * Usage Details
	 */
	private static void usage() {
		System.out.println("example: ava raids.Client 9000 localhost 9000 joe 10 localhost 9999");
		System.out.println("usage: java Client listenPort bootIP bootPort username numNodes [eve_host eve_port]");
		System.out.println("  listenPort = port this node will listen on");
		System.out.println("  bootIP     = bootstrap node's IP");
		System.out.println("  bootPort   = bootstrap node's port");
		System.out.println("  username   = the Client's username");
		System.out.println("  numNodes   = number of nodes to create on this JVM");
		//System.out.println("  pathToCfgFile = optional properties file");
		System.out.println("  eve_host = optional override EVE_HOST property");
		System.out.println("  eve_port = optional override EVE_PORT property");
		System.exit(1);
	}

}
