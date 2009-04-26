package eve;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;




/**
 * A Server that reads incoming report/status messages
 * from EveReporters over TCP
 *
 * @author Joseph Pecoraro
 */
public class Eve extends Thread {


	/**
	 * Thread that handles a single EveReporter.
	 * This is an inner class so it can access
	 * the EveGraph Visualizer.
	 *
	 * @author Joseph Pecoraro
	 */
	class EveHandler extends Thread {

	    /** The Socket **/
	    private Socket m_socket;


	    /**
	     *  Constructor takes in the open socket
	     *  @param sock the open socket
	     */
	    public EveHandler(Socket sock) {
	        m_socket = sock;
	    }


	    /**
	     * Listen and print incoming messages
	     * TODO: Add Synchronization on Sys.out() ?
	     */
	    public void run() {
	    	try {

		    	// Open only an Input Stream from the Reporter
		    	ObjectInputStream in = new ObjectInputStream( m_socket.getInputStream() );

		    	// Continually read EveMessages and output them
		    	EveMessage msg = null;
		    	while ( (msg = (EveMessage) in.readObject()) != null ) {
		    		System.out.println(msg);

		    		String from = msg.getFrom();
		    		String to = msg.getTo();
		    		m_graph.addVertex(from);
		    		m_graph.addVertex(to);
		    		m_graph.addEdge(from, to);
		    	}

			} catch (Exception e) {}
	    }

	}




	/** The Server Socket */
	private ServerSocket m_server;

	/** The EveGraph Visualizer */
	private EveGraph m_graph;


	/**
	 * Provide a Socket to Listen to
	 * @param server the initialized server socket
	 */
	public Eve(ServerSocket server) {

		// The Server Socket
		m_server = server;

    	// Setup the EveGraph Frame and Display it
    	m_graph = new EveGraph();
    	JFrame frame = new JFrame();
    	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	frame.getContentPane().add(    	m_graph);
    	m_graph.init();
    	m_graph.start();
    	frame.pack();
    	frame.setVisible(true);

	}


	/**
	 * Get the Port Eve is Listening on
	 */
	public int getPort() {
		return m_server.getLocalPort();
	}


	/**
	 * Run Eve
	 */
	public void run() {

        // Continually Spawn Handlers
        while (true) {
        	try {
				Socket sock = m_server.accept();
	        	EveHandler handler = new EveHandler(sock);
	        	handler.setDaemon(true);
	        	handler.start();
        	} catch (IOException e) {
 				e.printStackTrace();
 				System.exit(2);
 			}
        }

	}



	/**
	 * Driver for the Eve Server
	 * usage: java eve.EveServer [port]
	 */
	public static void main(String args[]) {

        // Optional Port Number
        int port = 0;
        if (args.length == 1) {
            // Ignore Bad Forms
            String port_str = args[0];
            if (port_str.matches("^\\d+$")) {
                port = Integer.parseInt(port_str);
            }
        }

		// Server
        ServerSocket server = null;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Could not create the Server Socket.");
			e.printStackTrace();
			System.exit(1);
		}

		// Output the port we are listening on
        System.out.println( server.getLocalPort() );

        // Create and run Eve
        new Eve(server).start();

	}

}
