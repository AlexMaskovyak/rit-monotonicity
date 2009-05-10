package raids;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;

import rice.environment.Environment;
import rice.p2p.commonapi.NodeHandle;
import util.BufferUtils;
import util.SHA1;
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

	/** Download Command */
	private static final String DOWNLOAD = "download";

	/** Second Download Command */
	private static final String DL = "dl";

	/** Kill Command */
	private static final String KILL = "kill";

	/** Help Command */
	private static final String HELP = "help";

	/** List Command */
	private static final String LIST = "list";


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
					uploadCommand(line, 4);
				}

				// Download Command
				else if ( line.startsWith(DOWNLOAD) ) {
					line = line.replaceFirst(DOWNLOAD, "").trim();
					downloadCommand(line);
				}

				// Also a Download Command
				else if ( line.startsWith(DL) ) {
					line = line.replaceFirst(DL, "").trim();
					downloadCommand(line);
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
	 * Process a file Download Command
	 * usage: download <filename>
	 */
	private void downloadCommand(String line) {
		try {

			// Handle bad input (filename with spaces, or empty filename)
			String[] args = line.split("\\s");
			if ( args.length != 1 ) {
				System.err.println("Bad download command. Filename contains a space. Usage: download <filename>");
				return;
			} else if ( args[0].length() == 0 ) {
				System.err.println("Bad download command.  Usage: download <filename>");
			}

			if( !m_app.isReadyForNewDownloadRequests() ) {
				System.err.println("Currently servicing a download request.  Please wait until the current download request finishes.");
				return;
			}
			
			// The Filename
			String filename = args[0];

			// Check personal file list to ensure the file has been uploaded before
			List<PersonalFileInfo> fileList = m_app.getPersonalFileList();
			if ( !fileList.contains(
					new PersonalFileInfo(
							filename,
							null ) ) ) {
				System.err.printf("This User has never stored a file named '%s'\n", filename );
				return;
			}

			// Grab the most current Master List for who holds the parts from the DHT
			MasterListMessage mlm = m_app.lookupMasterList(filename);

			// Debug
			List<NodeHandle>[] parts = mlm.getParts();
			System.out.println();
			for (int i = 0; i < parts.length; i++) {
				System.out.println("Ask the following nodes for part (" + i + "):");
				for (NodeHandle nh : parts[i]) { System.out.println(nh); }
				System.out.println();
			}

			// Tell the App we are expecting these files
			String lookupIdString = mlm.getLookupId().toStringFull();
			m_app.setExpectedReassembledFileName( filename );
			m_app.setExpectedParts(lookupIdString, mlm.getParts().length, parts );

			// Send a Download message to the first node in the list containing a file
			for (int i = 0; i < parts.length; i++) {
				NodeHandle nh = parts[i].get(0);
				PartIndicator pi = new PartIndicator(lookupIdString, i);
				m_app.attemptDownloadPartFrom(i, nh);
				m_app.sendDownloadMessage(new DownloadMessage(pi, m_app.getNode().getLocalNodeHandle()), nh);
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Bad download command.  Usage: download <filename>");
		}
	}


	/**
	 * Process a file Upload Command.
	 * usage: upload <filepath> <chunks>
	 * Obtains the provided file from the file system, chunks it, and uploads
	 * it to the network.
	 * @param line Line stripped of command hook.
	 */
	private void uploadCommand(String line, int attempts) {

		// Stop trying to upload
		if (attempts <= 0) {
			System.err.println("Upload Command failed after too many attempts.");
			return;
		}

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
			final ChunkedFileInfo cfi = chunker.Chunker.chunk( filePath, fileName, chunks);

			// find nodes with storage
			int replicas = 3; // TODO: Make this an optional parameter for upload?
			NodeHandle[] storageNodes = m_app.requestSpace( chunks*(replicas), cfi.getMaxChunkSize(), null );

			// Handle a Failed Multicast
			if ( storageNodes == null ) {
				System.err.println("Storage Request Failed.  Trying again... " + (attempts-1) + " tries left.");
				uploadCommand(line, attempts-1);
				return;
			}

			// Debug
			System.out.println("Replication Factor: " + replicas);
			System.out.println();
			System.out.println("Nodes that Responded with Storage Space: (" + storageNodes.length + ")");
			for (NodeHandle nh : storageNodes) { System.out.println(nh); }
			System.out.println();

			// Split the chunks appropriately among the nodes
			List<NodeHandle>[] masters = new ArrayList[ chunks ];
			for (int i=0,k=0; k<(chunks*replicas); i++,k+=replicas) {
				masters[ i ] = new ArrayList<NodeHandle>();
				for (int j = 0; j < replicas; ++j) {
					masters[ i ].add( storageNodes[ k+j ] );
				}
			}

			// Debug the lists
			for (int i = 0; i < masters.length; i++) {
				List<NodeHandle> list = masters[i];
				System.out.println("Division Group (" + i + "):");
				for (NodeHandle nh : list) { System.out.println(nh); }
				System.out.println();
			}

			// Upload the master list into the DHT
			MasterListMessage mlm = m_app.updateMasterList(fileName, masters );

			// Update the PersonalFileList in the DHT
			List<PersonalFileInfo> list = m_app.getPersonalFileList();
			list.add( new PersonalFileInfo(fileName, cfi.getOriginalFileHash() ) );

			System.out.println( "SHA HASH: " + SHA1.getInstance().hash( new File( path ) ) ) ;
			m_app.updatePersonalFileList(list);

			// Send the MasterListMessage to Everyone
			for (NodeHandle nh : storageNodes) {
				m_app.routeMessageDirect(mlm, nh);
			}

			// Send the raw file bytes to the First Node in the list for each part
			final int maxSize = (int) cfi.getMaxChunkSize();
			for (int i = 0; i < masters.length; i++) {
				List<NodeHandle> partList = masters[i];
				final NodeHandle nh = partList.get(0);
				final String filename = cfi.getChunkPaths()[i];
				final PartIndicator pi = new PartIndicator(mlm.getLookupId().toStringFull(), i);
				System.out.println("Sending " + pi.toString() + " to " + nh.getId().toStringFull() );
				new Thread() {
					public void run() {
						ByteBuffer buf = BufferUtils.getBufferForFile(filename, maxSize + PartIndicator.SIZE, pi);
						buf.flip();
						m_app.sendBufferToNode(buf, nh);
						// cleanup pieces
						(new File( filename ) ).delete();
					}
				}.start();
			}

			// Print Out Message to the User
			System.out.println("Successfully Submitted the File, it will be uploading in the background.");

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
		System.out.println("  help               This help menu");
		System.out.println("  list               Prints the status information of all nodes on this JVM");
		System.out.println("  kill [#]           Kills the given node, or the current if none is given");
		System.out.println("  status             Prints status information on the current node");
		System.out.println("  switch #           Switches to the given node");
		System.out.println("  upload path #      Chunks and uploads a file to a # nodes in the network");
		System.out.println("  download filename  Downloads the file uploaded by this user with that name");
		System.out.println("  quit               Exits the client program");
		System.out.println();
	}


}
