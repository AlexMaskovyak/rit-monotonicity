package raids;

import java.util.NoSuchElementException;
import java.util.Scanner;

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


// Fields and Class

	/** The Pastry Application */
	private RaidsApp m_app;


	/**
	 * Basic Constructor
	 * @param app the RaidsApp this is delegating to
	 */
	public ClientTerminal(RaidsApp app) {
		m_app = app;
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

			// Loop until QUIT Message
			while ( line != null && !line.startsWith(QUIT) ) {

				// Status Message
				if ( line.startsWith(STATUS) ) {
					String id = m_app.getLocalNodeHandle().getId().toStringFull();
					String name = m_app.getUsername();
					System.out.println("Username: " + name);
					System.out.println("ID: " + id);
				}

				// ...
				else if ( line.startsWith("xx") ) {

				}

				// UNKNOWN Message
				else {
					System.err.println("Ignored unknown Message (" + line + ")");
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

	}

}
