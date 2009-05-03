package raids;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import chunker.ChunkedFileInfo;

/**
 * Reads Commands from Standard Input, parses the input as
 * commands, and delegates actions to the provided Player.
 *
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class ClientTerminal extends Thread {

// Command Constants

	/*
	 * NOTE: If you update/add a new command remember to
	 * update the private helpCommand() function below!
	 */

	/** Quit Command */
	private static final String QUIT = "quit";

	/** Exit Command (Undocumented, but its the same as quit) */
	private static final String EXIT = "exit";

	/** Status Command */
	private static final String STATUS = "status";

	/** Switch Command */
	private static final String SWITCH = "switch";

	/** Upload Command */
	private static final String UPLOAD = "upload";

	/** Kill Command */
	private static final String KILL = "kill";

	/** Help Command */
	private static final String HELP = "help";

	/** List Command */
	private static final String LIST = "list";

	/** Store */
	private static final String STORE = "store";



// Fields and Class

	/** List of Applications (if multiple on this JVM) */
	private Vector<RaidsApp> m_apps;

	/** The Current Application */
	private RaidsApp m_app;

	/** Environment */
	private Environment m_env;


	/**
	 * Basic Constructor
	 * @param apps the RaidsApp this is delegating to
	 */
	public ClientTerminal(Vector<RaidsApp> apps, Environment env) {
		m_env = env;
		m_apps = apps;
		m_app = m_apps.get( env.getRandomSource().nextInt(apps.size()) );
	}


	/**
	 * Nice Prompt
	 */
	private void prompt() {
		System.out.print(">> ");
		System.out.flush();
	}


	/**
	 * Read Commands, Parses, and Delegates
	 */
	public void run() {

		// Catch a ^D and handle as EXIT
		try {

			// Scanner on Standard In to read lines
			prompt();
			Scanner in = new Scanner( System.in );
			String line = in.nextLine().trim();

			// Loop until QUIT (or EXIT) Command
			while ( line != null && !(line.startsWith(QUIT) || line.startsWith(EXIT)) ) {

				// Status Command
				if ( line.startsWith(STATUS) ) {
					m_app.status();
				}

				// Switch Command - Choose a node to command
				else if ( line.startsWith(SWITCH) ) {
					line = line.replaceFirst(SWITCH, "").trim();
					switchCommand(line);
				}

				// Kill Command
				else if ( line.startsWith(KILL) ) {
					line = line.replaceFirst(KILL, "").trim();
					killCommand(line);
				}

				// Upload Command
				else if ( line.startsWith(UPLOAD)) {
					line = line.replaceFirst(UPLOAD, "").trim();
					uploadCommand(line);
				}

				// Help Command
				else if ( line.startsWith(HELP) ) {
					helpCommand();
				}

				// List Command
				else if ( line.startsWith(LIST) ) {
					System.out.println();
					for (RaidsApp app : m_apps) {
						if ( app.equals(m_app) ) { System.out.println("---------"); }
						app.status();
						if ( app.equals(m_app) ) { System.out.println("---------"); }
						System.out.println();
					}
				}

				// TODO: Debug Method for Demonstration, remove once integrated normally
				else if ( line.startsWith("cpr") ) {

					line = line.replaceFirst("cpr", "").trim();

					// Args
					String[] args = line.split( " " );
					int node1 = Integer.parseInt(args[0]);
					int node2 = Integer.parseInt(args[1]);

					// Link two nodes
					RaidsApp alpha = m_apps.get(node1);
					RaidsApp beta  = m_apps.get(node2);
					alpha.cpr(beta);
					beta.cpr(alpha);

				}

				// TODO: Debug Method for Demonstration, remove once integrated normally
				else if ( line.startsWith(STORE) ) {
					System.out.println("Storing");
					m_app.requestSpace(5, 20);
				}

				// Empty Command
				else if ( line.length() == 0 ) {
					// Do nothing, the User just had a blank line.
				}

				// UNKNOWN Command
				else {
					System.err.println("Ignored unknown Command (" + line + ")");
				}

				// Prompt and Read next line for looping
				prompt();
				line = in.nextLine().trim();

			}

			// Cleanup
			in.close();

		} catch ( NoSuchElementException e ) {}

		// Total Cleanup
		m_env.destroy();

	}

// Private Helpers


	/**
	 * Send a Direct Message to a Node identified by the given NodeHandle
	 * @param msg the Message to send
	 * @param nh the Node to send the message to
	 */
	private void routeDirectMessage(Message msg, NodeHandle nh) {
		System.out.println(m_app.toString() +" sending direct to " + nh.toString());

		// TODO: How can we change this so it will work across a network?
		// We need the other "Applications" Endpoint.  I have no idea.
		for (RaidsApp a : m_apps) {
			if ( a.getLocalNodeHandle().equals(nh) ) {
				a.getRaidsEndpoint().route(null, msg, nh);
				return;
			}
		}

		System.err.println("No Such NodeHandle");
	}


// Command Functions

	/**
	 * Process a Switch Command
	 * usage: switch <switchNum>
	 * Switches to the given node.
	 */
	private void switchCommand(String line) {
		try {
			int switchTo = Integer.parseInt(line);
			m_app = m_apps.get(switchTo);
		} catch (NumberFormatException e) {
			System.err.println("Bad switch command.  Usage: switch <switchNum>");
		} catch (Exception e) {
			System.err.println("Bad switch command.  Use a number from 0 to " + (m_apps.size()-1) );
		}
	}

	/**
	 * Process a Kill Command
	 * usage: kill [<killNum>]
	 * Kills the given process, or "this" if killNum is not provided
	 */
	private void killCommand(String line) {
		if ( line.length() == 0 ) {
			m_app.kill();
		} else {
			try {
				int killNum = Integer.parseInt(line);
				m_apps.get(killNum).kill();
			} catch (Exception e) {
				System.err.println("Bad kill command.  Usage: kill [<killNum>]");
			}
		}
	}

	/**
	 * Process a file Upload Command.
	 * usage: upload <filepath> <chunks>
	 * Obtains the provided file from the file system, chunks it, and uploads
	 * it to the network.
	 * @param line Line stripped of command hook.
	 */
	private void uploadCommand(String line) {
		try {

			// obtain arguments
			String[] args = line.split( " " );
			String path = args[ 0 ];
			int chunks = Integer.parseInt( args[ 1 ] );

			// split into path and filename
			File f = new File( path );
			String filePath = f.getParentFile().getAbsolutePath() + "/";
			String fileName = f.getName();

			// chunk the file
			ChunkedFileInfo cfi = chunker.Chunker.chunk( filePath, fileName, chunks );

			// find nodes with storage
			NodeHandle[] storageNodes = m_app.requestSpace( chunks, cfi.getMaxChunkSize() );

			// make the first 3 masters (arbitrary)
			NodeHandle[] masters = null;
			if ( storageNodes.length < 3 ) {
				System.arraycopy(storageNodes, 0, masters, 0, storageNodes.length);
			} else {
				masters = new NodeHandle[] { storageNodes[0], storageNodes[1], storageNodes[2] };
			}

			// Debug for Demonstration
			System.out.println();
			System.out.println("Nodes That Will Accept Storage: ");
			for (NodeHandle nh : storageNodes) { System.out.println(nh); }
			System.out.println();
			System.out.println("Master Nodes: ");
			for (NodeHandle nh : masters) { System.out.println(nh); }
			System.out.println();

			// Master List information
			Id fileId = PersonalFileListHelper.masterListIdForFilename(fileName, m_env);

			// Upload the master list into the DHT
			m_app.updateMasterList(fileName, Arrays.asList(masters) );

			// Update the PersonalFileList in the DHT
			List<PersonalFileInfo> list = m_app.getPersonalFileList();
			list.add( new PersonalFileInfo(fileName) );
			m_app.updatePersonalFileList(list);

			// Create the lists to be sent out in the MasterListMessage
			List<NodeHandle> masterList = Arrays.asList(masters);
			List<NodeHandle>[] parts = new List[chunks];
			for (int i = 0; i < chunks; i++) {
				List l = new ArrayList<NodeHandle>();
				l.add( storageNodes[i] );
				parts[i] = l;
			}

			// Create the MasterListMessage and send it to everyone who needs it
			MasterListMessage mlm = new MasterListMessage(masterList, parts, fileId);
			System.out.println(mlm);
			for (NodeHandle nh : storageNodes) {
				routeDirectMessage(mlm, nh);
			}

		} catch ( Exception e ) {
			e.printStackTrace();
			System.err.println( "Bad upload command.  Usage: upload <path> <# chunks>" );
		}

	}

	/**
	 * Help Command
	 * usage: help
	 * Prints out the help menu
	 */
	private void helpCommand() {
		System.out.println();
		System.out.println("Commands:");
		System.out.println("  help             This help menu");
		System.out.println("  list             Prints the status information of all nodes on this JVM");
		System.out.println("  kill [#]         Kills the given node, or the current if none is given");
		System.out.println("  status           Prints status information on the current node");
		System.out.println("  switch #         Switches to the given node");
		System.out.println("  upload [path] #  Chunks and uploads a file to a # nodes in the network");
		System.out.println("  quit             Exits the client program");
		System.out.println();
	}


}
