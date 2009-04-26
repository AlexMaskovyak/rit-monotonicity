package raids;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import rice.Continuation;
import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
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

public class Client {

	/** Multiple applications */
	private Vector<Past> m_apps = new Vector<Past>();

	/** Number of Nodes */
	private int m_num_nodes;

	/** Configuration */
	private Properties m_config;

	/**
	 * TODO: Update This
	 * This constructor launches numNodes PastryNodes. They will bootstrap to an
	 * existing ring if one exists at the specified location, otherwise it will
	 * start a new ring.
	 *
	 * @param bindport the local port to bind to
	 * @param bootaddress the IP:port of the node to boot from
	 * @param numNodes the number of nodes to create in this JVM
	 * @param env the Environment
	 * @param config the Application Configuration Properties
	 */
	public Client(int bindport, InetSocketAddress bootaddress, String username,
			int numNodes, final Environment env, Properties config) throws Exception {

		// Application Configuration
		m_config = config;

		// Set so everyone can access
		m_num_nodes = numNodes;

		// Create the nodes
		createNodes(bindport, bootaddress, username, numNodes, env);

		// Get a Random Node for the originating Client
		RaidsApp originatingClient = (RaidsApp)m_apps.get( env.getRandomSource().nextInt(numNodes) );

		// wait 2 seconds
		speakingSleep(env, 2000);

		// ---------

		// Send out a PersonalFileList
		Id storageId = sendOutPersonalFileList(originatingClient);

		// wait 2 seconds
		speakingSleep(env, 2000);

		// ---------

		// Lookup that list
		lookupPersonalFileList(originatingClient, storageId);

		// wait 2 seconds
		speakingSleep(env, 2000);

		// ---------

		// Send out a MasterList
		Id masterId = sendOutMasterList(originatingClient);

		// wait 2 seconds
		speakingSleep(env, 2000);

		// ---------

		// Lookup that list
		lookupMasterList(originatingClient, masterId);

		// wait 2 seconds
		speakingSleep(env, 2000);

		// ---------

		// Done
		env.destroy();

	}


	/**
	 * Send out a PersonalFileList for the given node
	 */
	public Id sendOutPersonalFileList(RaidsApp originatingClient) {

		System.out.println();
		System.out.println("---------------------------");
		System.out.println(" Testing PersonalFileList");
		System.out.println("---------------------------");
		System.out.println();

		// Environment from node
		Environment env = originatingClient.getEnvironment();

		// FAKE Storage information for that list!
		Id fakeStorageId = PersonalFileListHelper.personalFileListIdFromNodeId(originatingClient.getLocalNodeHandle().getId(), env);

		// FAKE Personal File List for that user
		PersonalFileListContent pfl = new PersonalFileListContent(fakeStorageId);
		List<PersonalFileInfo> l = pfl.getList();
		l.add( new PersonalFileInfo("one.txt") );
		l.add( new PersonalFileInfo("two.txt") );
		l.add( new PersonalFileInfo("three.txt") );


		// FAKE Store on a random node
		RaidsApp fakeApp = (RaidsApp)m_apps.get( env.getRandomSource().nextInt(m_num_nodes));
		System.out.println("Inserting " + pfl.toString() + " at node " + fakeApp.getLocalNodeHandle());

		// Make the Insertion
		fakeApp.insert(pfl, new Continuation<Boolean[], Exception>() {

			public void receiveException(Exception e) {
				System.out.println("Error storing FAKE content");
				e.printStackTrace();
			}

			public void receiveResult(Boolean[] res) {
				Boolean[] results = ((Boolean[]) res);
				int numSuccess = 0;
				for (int i = 0; i < results.length; i++) {
					Boolean b = results[i];
					if ( b.booleanValue() ) {
						numSuccess++;
					}
				}
				System.out.println("Successfully stored FAKE at " + numSuccess + " locations.");
			}

		});

		// Here was the storage id
		return fakeStorageId;

	}


	/**
	 * Announce sleeping. Debugging Helper
	 */
	private void speakingSleep(Environment env, int i) throws Exception {
		System.out.println("Sleeping for " + i + "ms");
		System.out.flush();
		env.getTimeSource().sleep(i);
	}



	/**
	 * Lookup the given storageId
	 */
	public void lookupPersonalFileList(RaidsApp originatingClient, Id storageId) {

		System.out.println();
		System.out.println("---------------------");
		System.out.println(" Performing Lookup");
		System.out.println("---------------------");
		System.out.println();

		// Lookup the FAKE storage from the original Node
		originatingClient.lookup(storageId, new Continuation<PastContent, Exception>() {
			public void receiveResult(PastContent result) {
				System.out.println("Successfully looked up storage for FAKE key:");
				System.out.println(result.toString());
			}

			public void receiveException(Exception e) {
				System.out.println("Error looking up FAKE key");
				e.printStackTrace();
			}
		});

	}


	/**
	 * Send out a MasterList
	 */
	public Id sendOutMasterList(RaidsApp originatingClient) {

		System.out.println();
		System.out.println("---------------------");
		System.out.println(" Testing MasterList");
		System.out.println("---------------------");
		System.out.println();

		Environment env = originatingClient.getEnvironment();

		// Generate Fake id for a file
		PastryIdFactory localFactory = new PastryIdFactory(env);
		String fakeFile = "a.txt";
		Id fakeFileId = localFactory.buildId( "MASTER_LOOKUP" + fakeFile );

		// Add to random masters to the list
		MasterListContent mlc = new MasterListContent(fakeFileId);
		mlc.getList().add( ((RaidsApp)m_apps.get( env.getRandomSource().nextInt(m_num_nodes))).getLocalNodeHandle() );
		mlc.getList().add( ((RaidsApp)m_apps.get( env.getRandomSource().nextInt(m_num_nodes))).getLocalNodeHandle() );


		// FAKE Store on a random node
		RaidsApp randomApp = (RaidsApp)m_apps.get( env.getRandomSource().nextInt(m_num_nodes));
		System.out.println("FAKE: Inserting " + mlc.toString() + " at node " + randomApp.getLocalNodeHandle());

		// Make the Insertion
		originatingClient.insert(mlc, new Continuation<Boolean[], Exception>() {

			public void receiveException(Exception e) {
				System.out.println("Error storing MASTER content");
				e.printStackTrace();
			}

			public void receiveResult(Boolean[] res) {
				Boolean[] results = ((Boolean[]) res);
				int numSuccess = 0;
				for (int i = 0; i < results.length; i++) {
					Boolean b = results[i];
					if ( b.booleanValue() ) {
						numSuccess++;
					}
				}
				System.out.println("Successfully stored MASTER at " + numSuccess + " locations.");
			}

		});

		return fakeFileId;

	}


	/**
	 * Lookup MasterList
	 */
	public void lookupMasterList(RaidsApp originatingClient, Id storageId) {

		System.out.println();
		System.out.println("---------------------");
		System.out.println(" Performing Lookup");
		System.out.println("---------------------");
		System.out.println();

		// Lookup the FAKE storage from the original Node
		originatingClient.lookup(storageId, new Continuation<PastContent, Exception>() {
			public void receiveResult(PastContent result) {
				System.out.println("Successfully looked up storage for MASTER list on \"a.txt\":");
				System.out.println(result.toString());
			}

			public void receiveException(Exception e) {
				System.out.println("Error looking up MASTER list");
				e.printStackTrace();
			}
		});

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

			System.out.println("Finished creating new node " + node);


			// used for generating PastContent object Ids.
			// this implements the "hash function" for our DHT
			PastryIdFactory idf = new PastryIdFactory(env);

			// create a different storage root for each node
			@SuppressWarnings("unused")
			String storageDirectory = "./storage" + node.getId().hashCode();

			// create the persistent part
			// Storage stor = new PersistentStorage(idf, storageDirectory, 4 * 1024 * 1024, node.getEnvironment());
			Storage stor = new MemoryStorage(idf);
			String perNodeUsername = (curNode==0) ? username : username+(curNode+1);
			RaidsApp app = new RaidsApp(node, new StorageManagerImpl(idf, stor,
					new LRUCache(new MemoryStorage(idf), 512 * 1024, node.getEnvironment())), 1, "",
					perNodeUsername, m_config.getProperty("EVE_HOST", null),
					Integer.parseInt( m_config.getProperty("EVE_USER", "9999")) );
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
		System.out.println("example: java Client 9500 jpecoraro.rit.edu 9000 10 ~/clientcfg.properties");
		System.out.println("usage: java Client listenPort bootIP bootPort username numNodes [eve_host eve_port] ]");
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
