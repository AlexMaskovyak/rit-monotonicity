package examples;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Vector;

import raids.MasterListContent;
import raids.PersonalFileInfo;
import raids.PersonalFileListContent;
import raids.PersonalFileListHelper;
import rice.Continuation;
import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.past.*;
import rice.pastry.*;
import rice.pastry.commonapi.PastryIdFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import rice.persistence.*;

public class PastExampleSendingLists {

	/**
	 * this will keep track of our Past applications
	 */
	Vector<Past> apps = new Vector<Past>();

	/**
	 * Number of nodes
	 */
	int numNodes;

	/**
	 * Announce sleeping.
	 */
	private void speakingSleep(Environment env, int i) throws Exception {
		System.out.println("Sleeping for " + i + "ms");
		System.out.flush();
		env.getTimeSource().sleep(i);
	}

	/**
	 * Based on the rice.tutorial.scribe.ScribeTutorial
	 *
	 * This constructor launches numNodes PastryNodes. They will bootstrap to an
	 * existing ring if one exists at the specified location, otherwise it will
	 * start a new ring.
	 *
	 * @param bindport the local port to bind to
	 * @param bootaddress the IP:port of the node to boot from
	 * @param numNodes the number of nodes to create in this JVM
	 * @param env the Environment
	 */
	public PastExampleSendingLists(int bindport, InetSocketAddress bootaddress, int numNodes, final Environment env) throws Exception {

		// Set so everyone can access
		this.numNodes = numNodes;

		// Create the nodes
		createNodes(bindport, bootaddress, numNodes, env);

		// Get a Random Node for the originating Client
		Past originatingClient = (Past)apps.get( env.getRandomSource().nextInt(numNodes));

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
	public Id sendOutPersonalFileList(Past originatingClient) {

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
		Past fakeApp = (Past)apps.get( env.getRandomSource().nextInt(numNodes));
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
	 * Lookup the given storageId
	 */
	public void lookupPersonalFileList(Past originatingClient, Id storageId) {

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
	public Id sendOutMasterList(Past originatingClient) {

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
		mlc.getList().add( ((Past)apps.get( env.getRandomSource().nextInt(numNodes))).getLocalNodeHandle() );
		mlc.getList().add( ((Past)apps.get( env.getRandomSource().nextInt(numNodes))).getLocalNodeHandle() );


		// FAKE Store on a random node
		Past randomApp = (Past)apps.get( env.getRandomSource().nextInt(numNodes));
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
	public void lookupMasterList(Past originatingClient, Id storageId) {

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
	public void createNodes(int bindport, InetSocketAddress bootaddress, int numNodes, final Environment env) throws Exception {

		// Generate the NodeIds Randomly
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

		// construct the PastryNodeFactory, this is how we use rice.pastry.socket
		PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

		// loop to construct the nodes/apps
		for (int curNode = 0; curNode < numNodes; curNode++) {

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
//			Storage stor = new PersistentStorage(idf, storageDirectory, 4 * 1024 * 1024, node.getEnvironment());
			Storage stor = new MemoryStorage(idf);
			Past app = new PastImpl(node, new StorageManagerImpl(idf, stor, new LRUCache(new MemoryStorage(idf), 512 * 1024, node.getEnvironment())), 1, "");

			apps.add(app);
		}
	}





	/**
	 * Usage: java [-cp FreePastry- <version>.jar]
	 * rice.tutorial.past.PastTutorial localbindport bootIP bootPort numNodes
	 * example java rice.tutorial.past.PastTutorial 9001 pokey.cs.almamater.edu 9001 10
	 */
	public static void main(String[] args) throws Exception {
		// Loads pastry configurations
		Environment env = new Environment();

		// disable the UPnP setting (in case you are testing this on a NATted LAN)
		env.getParameters().setString("nat_search_policy","never");

		try {
			// the port to use locally
			int bindport = Integer.parseInt(args[0]);

			// build the bootaddress from the command line args
			InetAddress bootaddr = InetAddress.getByName(args[1]);
			int bootport = Integer.parseInt(args[2]);
			InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);

			// the port to use locally
			int numNodes = Integer.parseInt(args[3]);

			// launch our node!
			new PastExampleSendingLists(bindport, bootaddress, numNodes, env);
		} catch (Exception e) {
			// remind user how to use
			System.out.println("Usage:");
			System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.past.PastTutorial localbindport bootIP bootPort numNodes");
			System.out.println("example java rice.tutorial.past.PastTutorial 9001 pokey.cs.almamater.edu 9001 10");
			env.destroy();
			throw e;
		}

	}




}
