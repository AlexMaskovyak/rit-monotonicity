package eve;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;



/**
 * Thread that handles a single EveReporter
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
	    	}

		} catch (Exception e) {}
    }

}

/**
 * A Server that reads incoming report/status messages
 * from EveReporters over TCP
 *
 * @author Joseph Pecoraro
 */
public class Eve {

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


        // Continually Spawn Handlers
        while (true) {
        	try {
				Socket sock = server.accept();
	        	EveHandler handler = new EveHandler(sock);
	        	handler.setDaemon(true);
	        	handler.start();
        	} catch (IOException e) {
 				e.printStackTrace();
 				System.exit(2);
 			}
        }

	}

}
