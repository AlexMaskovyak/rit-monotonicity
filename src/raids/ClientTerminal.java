package raids;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;

import chunker.ChunkedFileInfo;

import rice.environment.Environment;

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
	 * @param app the RaidsApp this is delegating to
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

				// TODO: Debug Method for Demonstration, remove once integrated normally
				else if ( line.startsWith("cpr") ) {

					// Link two nodes
					RaidsApp alpha = m_apps.get(1);
					RaidsApp beta  = m_apps.get(2);

					System.out.println(alpha);
					System.out.println(beta);

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

			// find storage nodes

			// upload to these nodes

			// update the PersonalFileList
			List<PersonalFileInfo> list = m_app.getPersonalFileList();
			list.add( new PersonalFileInfo(fileName) );
			m_app.updatePersonalFileList(list);


		} catch ( Exception e ) {
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
		System.out.println("  kill [#]         Kills the given node, or the current if none is given");
		System.out.println("  status           Prints status information on the current node");
		System.out.println("  switch #         Switches to the given node");
		System.out.println("  upload [path] #  Chunks and uploads a file to a # nodes in the network");
		System.out.println("  quit             Exits the client program");
		System.out.println();
	}


}
