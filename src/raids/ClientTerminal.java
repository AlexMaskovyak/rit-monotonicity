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

	/** Quit Command */
	private static final String QUIT = "quit";

	/** Status Command */
	private static final String STATUS = "status";

	/** Switch Command */
	private static final String SWITCH = "switch";

	/** Kill Command */
	private static final String KILL = "kill";


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

				// Kill Command - Optionally choose a node to kill
				else if ( line.startsWith(KILL) ) {
					line = line.replaceFirst(KILL, "").trim();

					// This one
					if ( line.length() == 0 ) {
						m_app.kill();
					}

					// Choose one to kill
					else {
						try {
							int killNum = Integer.parseInt(line);
							m_apps.get(killNum).kill();
						} catch (Exception e) {
							System.err.println("Bad kill command.  Usage: kill [<killNum>]");
						}
					}

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

}
