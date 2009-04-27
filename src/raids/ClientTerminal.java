package raids;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;

import rice.environment.Environment;

/**
 * Reads Commands from Standard Input, parses the input as
 * commands, and delegates actions to the provided Player.
 *
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

	/** Status Command */
	private static final String STATUS = "status";

	/** Switch Command */
	private static final String SWITCH = "switch";

	/** Kill Command */
	private static final String KILL = "kill";

	/** Help Command */
	private static final String HELP = "help";


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

			// Loop until QUIT Command
			while ( line != null && !line.startsWith(QUIT) ) {

				// Status Command
				if ( line.startsWith(STATUS) ) {
					String id = m_app.getLocalNodeHandle().getId().toStringFull();
					String name = m_app.getUsername();
					System.out.println("Username: " + name);
					System.out.println("ID: " + id);
				}

				// Switch Command - Choose a node to command
				else if ( line.startsWith(SWITCH) ) {
					line = line.replaceFirst(SWITCH, "").trim();
					try {
						int switchTo = Integer.parseInt(line);
						m_app = m_apps.get(switchTo);
					} catch (Exception e) {
						System.err.println("Bad switch command.  Usage: switch <switchNum>");
					}
				}

				// Kill Command
				else if ( line.startsWith(KILL) ) {
					line = line.replaceFirst(KILL, "").trim();
					killCommand(line);
				}

				// Help Command
				else if ( line.startsWith(HELP) ) {
					helpCommand();
				}

				// ...
				else if ( line.startsWith("cpr") ) {

					// Link two nodes
					RaidsApp alpha = m_apps.get(1);
					RaidsApp beta  = m_apps.get(2);

					System.out.println(alpha);
					System.out.println(beta);

					alpha.cpr(beta);
					beta.cpr(alpha);

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

			// Handle QUIT Message
			// TODO: nice cleanup?

		} catch ( NoSuchElementException e ) { // How Scanner reacts to a ^D
			// TODO: nice cleanup?
		}

		// Total Cleanup
		m_env.destroy();

	}


// Command Functions

	/**
	 * Process a Kill Command
	 * usage: kill [<killNum>]
	 * Kills the given process, or "this" if kill Num is not provided
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
	 * Help Command
	 * usage: help
	 * Prints out the help menu
	 */
	private void helpCommand() {
		System.out.println();
		System.out.println("Commands:");
		System.out.println("  help      This help menu");
		System.out.println("  kill [#]  Kills the given node, or the current if none is given");
		System.out.println("  status    Prints status information on the current node");
		System.out.println("  switch #  Switches to the given node");
		System.out.println("  quit      Exits the client program");
		System.out.println();
	}


}
