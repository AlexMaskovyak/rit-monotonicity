package eve;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class EveReporter {

	/** Is this connected */
	private boolean m_connected;

	/** The socket connection */
	private Socket m_socket;

	/** Stream out to the Eve */
	private ObjectOutputStream m_out;


	/**
	 * Default Constructor
	 * No Eve Server
	 */
	public EveReporter() {
		m_connected = false;
	}


	/**
	 * Constructor opens a socket to the given server.
	 * Fails silently.
	 * @param host eve server's address
	 * @param port eve server's port
	 */
	public EveReporter(String host, int port) {
		m_connected = false;
		try {
			m_socket = new Socket(host, port);
			m_out = new ObjectOutputStream( m_socket.getOutputStream() );
			m_connected = true;
		} catch (Exception e) {
			System.err.println("Could Not Connect to Eve.");
		}
	}


	/**
	 * Send a Message to Eve of raw input
	 */
	public void log(String from, String to, String type, String data) {
		log( new EveMessage(from, to, type, data) );
	}


	/**
	 * Send a Message to Eve
	 * @param msg the message to send.
	 */
	public void log(EveMessage msg) {
		if ( m_connected ) {
			try {
				m_out.writeObject(msg);
			} catch (IOException e) {}
		}
	}

	/**
	 * Close the connection to Eve
	 */
	public void close() {
		if ( m_connected && m_socket != null) {
			try {
				m_out.close();
				m_socket.close();
			} catch (IOException e) {}
		}
	}


	/**
	 * Simple Test Program
	 */
	public static void main(String args[]) {
		int port = Integer.parseInt(args[0]);
		String host = args[1];
		EveReporter reporter = new EveReporter(host, port);
		reporter.log("joe", "eve", "hello", "testing connectivity");
		System.out.print("Send Eve a Message: ");
		String input = (new java.util.Scanner(System.in)).nextLine();
		reporter.log("joe", "eve", "tty", input);
		reporter.close();
	}

}
