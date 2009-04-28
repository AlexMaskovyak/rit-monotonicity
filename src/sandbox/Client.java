package sandbox;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import eve.EveType;

import raids.ClientTerminal;
import raids.PersonalFileInfo;
import raids.PersonalFileListContent;
import raids.PersonalFileListHelper;
import raids.RaidsApp;
import rice.Continuation;
import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.Past;
import rice.p2p.past.messaging.InsertMessage;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.commonapi.PastryEndpointMessage;
import rice.pastry.commonapi.PastryIdFactory;
import rice.pastry.direct.DirectPastryNodeFactory;
import rice.pastry.direct.EuclideanNetwork;
import rice.pastry.direct.NetworkSimulator;
import rice.pastry.direct.SimulatorListener;
import rice.pastry.routing.RouteMessage;
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

		// Set the config
		m_config = config;

		// Create the nodes
		createNodes(bindport, bootaddress, username, numNodes, env);

		// Send all the apps (per this JVM) so the Terminal can switch between them
		new ClientTerminal( m_apps, env ).start();

		RaidsApp originatingClient = m_apps.get( env.getRandomSource().nextInt(numNodes) );


		// FAKE Storage information for that list!
		Id fakeStorageId = PersonalFileListHelper.personalFileListIdFromNodeId(originatingClient.getLocalNodeHandle().getId(), env);

		// FAKE Personal File List for that user
		PersonalFileListContent pfl = new PersonalFileListContent(fakeStorageId);
		List<PersonalFileInfo> l = pfl.getList();
		l.add( new PersonalFileInfo("one.txt") );
		l.add( new PersonalFileInfo("two.txt") );
		l.add( new PersonalFileInfo("three.txt") );


		// FAKE Store on a random node
		Past fakeApp = (Past)m_apps.get( env.getRandomSource().nextInt(numNodes));
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



	}


	/**
	 * Creates nodes with MemoryStorage
	 */
	public void createNodes(int bindport, InetSocketAddress bootaddress, String username,
			int n, final Environment env) throws Exception {

		// Generate the NodeIds Randomly
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

		// create a new network
		NetworkSimulator simulator = new EuclideanNetwork(env);

		// set the max speed of the simulator so we can create several nodes in a short amount of time
		// by default the simulator will advance very fast while we are slow on the main() thread
		simulator.setFullSpeed();
		simulator.addSimulatorListener(new SimulatorListener() {
			public void messageReceived(Message m, NodeHandle from, NodeHandle to) {}
			public void messageSent(Message m, NodeHandle from, NodeHandle to, int delay) {
				// System.out.println(m.getClass());
				if (m instanceof RouteMessage) {
					// System.out.println(m.getClass());
					RouteMessage rm = (RouteMessage)m;
					// System.out.println( rm.internalMsg.getClass() );
					if (rm.internalMsg instanceof PastryEndpointMessage) {
						PastryEndpointMessage rmm = (PastryEndpointMessage) rm.internalMsg;
						// System.out.println( rmm.getMessage().getClass() );
						if (rmm.getMessage() instanceof InsertMessage) {
							InsertMessage finalMessage = (InsertMessage) rmm.getMessage();
							System.out.println( "FROM: " + from.getId().toStringFull() + "\nTO: " + to.getId().toStringFull() + "\nMessage: " + finalMessage.toString());
							// Report to Eve
							//m_apps.get(0).m_reporter.log(from.getId().toStringFull(), to.getId().toStringFull(), EveType.MSG, "DHT");
						}
					}
				}
			}
		});

		// construct the PastryNodeFactory, this is how we use rice.pastry.socket
		PastryNodeFactory factory = new DirectPastryNodeFactory(nidFactory, simulator, env);
		//PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

		NodeHandle bootHandle = null;

		// loop to construct the nodes/apps
		for (int curNode = 0; curNode < n; curNode++) {




			// construct a node, passing the null boothandle on the first loop will cause the node to start its own ring
			PastryNode node = factory.newNode();
			node.boot(bootHandle);
//			System.out.println("here:"+node+" "+env.getTimeSource().currentTimeMillis());

			// this way we can boot off the previous node
			bootHandle = node.getLocalHandle();

			// the node may require sending several messages to fully boot into the ring
			synchronized(node) {
				while(!node.isReady() && !node.joinFailed()) {
					// delay so we don't busy-wait
					node.wait(500);

					// abort if can't join
					if (node.joinFailed()) {
						throw new IOException("Could not join the FreePastry ring.  Reason:"+node.joinFailedReason());
					}
				}
			}

			System.out.println("Finished creating new node "+node);


//			// construct a node, passing the null boothandle on the first loop will
//			// cause the node to start its own ring
//			PastryNode node = factory.newNode();
//			node.boot(boothandle);

//			// the node may require sending several messages to fully boot into the ring
//			synchronized(node) {
//			while(!node.isReady() && !node.joinFailed()) {
//			node.wait(500);
//			if (node.joinFailed()) {
//			throw new IOException("Could not join the FreePastry ring.  Reason:"+node.joinFailedReason());
//			}
//			}
//			}

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
					new LRUCache(new MemoryStorage(idf), 512 * 1024, node.getEnvironment())), 3, "",
					perNodeUsername, m_config.getProperty("EVE_HOST", null),
					Integer.parseInt( m_config.getProperty("EVE_PORT", "9999")) );
			m_apps.add(app);



		}

		// Back to normal time
		simulator.setMaxSpeed(1.0f);

	}


	/**
	 * Driver Program parses the command line
	 * arguments to setup some details.
	 */
	public static void main(String[] args) throws Exception {

		// Pastry Environment
		// Disable the UPnP setting (in case you are testing this on a NATted LAN)
		Environment env = Environment.directEnvironment();
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
		System.out.println("example: java raids.Client 9000 localhost 9000 joe 10 localhost 9999");
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
