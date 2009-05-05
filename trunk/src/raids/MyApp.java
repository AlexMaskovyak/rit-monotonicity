package raids;

import java.io.IOException;
import java.nio.ByteBuffer;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.commonapi.appsocket.AppSocket;
import rice.p2p.commonapi.appsocket.AppSocketReceiver;

public class MyApp implements Application {


// Inner Classes

	/**
	 * AppSocket Accepter Class
	 * Opens Readers for incoming Socket Requests
	 * @author Joseph Pecoraro
	 */
	class AppSocketAccepter implements AppSocketReceiver {

		/**
		 * A Socket has been received (opened), setup an AppSocketReader
		 * to handle reading from the socket.
		 */
		public void receiveSocket(AppSocket socket) {

			// TODO: Give the AppSocketReader's a Unique Identifier? Maybe for temp filenames.
			// NOTE: You have access to MyApp, so we can keep a synchronized counter.

			socket.register(true, false, 30000, new AppSocketReader());
			m_endpoint.accept(this);
		}

		// Should not happen, this class only accepts sockets, it does not read results
		public void receiveException(AppSocket socket, Exception e) {}
		public void receiveSelectResult(AppSocket socket, boolean canRead, boolean canWrite) {}

	}


	/**
	 * AppSocket Reading Class
	 * Reads from a Socket into an internal ByteBuffer
	 * @author Joseph Pecoraro
	 */
	class AppSocketReader implements AppSocketReceiver {

		/** Buffer Size */
		private static final int BUFFER_SIZE = 4*1024; /* 4 kilobytes */

		/** The buffer of data being read from the socket */
		private ByteBuffer m_inputBuffer;

		/**
		 * Basic Constructor
		 */
		public AppSocketReader() {
			m_inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		}

		/**
		 * The socket is ready to read or write
		 * @param socket the socket to read or write to
		 * @param canRead can this socket be read from
		 * @param canWrite can this socket be written to
		 */
		public void receiveSelectResult(AppSocket socket, boolean canRead, boolean canWrite) {
			System.out.println("reading");
			m_inputBuffer.clear();
			try {

				long ret = socket.read(m_inputBuffer);

				// TODO: Do Something With ins!!! Because, the next read its data will get lost
				// or the socket will be closed right now.
				// NOTE: you have access to m_delegate which is the RaidsApp, so you can
				// do ANYTHING.

				System.out.println("read: " + ret);
				if ( ret == -1 ) {
					System.out.println("Socket we were reading from is empty... closing");
					socket.close();
				} else if (ret != BUFFER_SIZE) {

					// TODO: This won't work when we stop sending strings, it just shows it works.
					// Debug, pull the "Hello, World" string
					String s = new String( m_inputBuffer.array() );
					System.out.println("  ---> " + s);

					// Could indicate there is more to send!
					System.out.println("Did not fill the entire buffer!  Only read: " + ret + " from the socket.");
					socket.register(true, false, 3000, this);

				}

			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

		}


		/**
		 * Handle Exceptions on read
		 * @param socket the socket we are reading from
		 * @param e the Exception
		 */
		public void receiveException(AppSocket socket, Exception e) {
			e.printStackTrace();
		}

		// Should not happen, this class only receives results, it does not accept sockets
		public void receiveSocket(AppSocket socket) {}

	}


	/**
	 * AppSocket Writing Class
	 * Writes to a Socket until the Internal Buffer is empty
	 * @author Joseph Pecoraro
	 */
	class AppSocketWriter implements AppSocketReceiver {

		/** The buffer of data this will be writing out the socket */
		private ByteBuffer m_outputBuffer;

		/**
		 * Basic constructor
		 * @param buf the buffer to send out over the socket
		 */
		public AppSocketWriter(ByteBuffer buf) {
			m_outputBuffer = buf;
		}

		/**
		 * On Receiving a Socket, register it for writing
		 * @param socket the socket being received
		 */
		public void receiveSocket(AppSocket socket) {
			socket.register(false, true, 30000, this);
		}

		/**
		 * The socket is ready to read or write
		 * @param socket the socket to read or write to
		 * @param canRead can this socket be read from
		 * @param canWrite can this socket be written to
		 */
		public void receiveSelectResult(AppSocket socket, boolean canRead, boolean canWrite) {
			System.out.println("writing");
			try {

				// Write, Close if done, otherwise keep writing
				socket.write(m_outputBuffer);
				if (!m_outputBuffer.hasRemaining()) {
					socket.close();
					m_outputBuffer.clear();
				} else {
					socket.register(false, true, 30000, this);
				}

			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		/**
		 * Handle Exceptions
		 */
		public void receiveException(AppSocket socket, Exception e) {
			e.printStackTrace();
		}

	}


// Fields

	/** The endpoint this maintains */
	private Endpoint m_endpoint;

	/** The application that is really supposed to do the work */
	private Application m_delegate;

	/**
	 * Basic Constructor
	 * @param node the node to build an endpoint from
	 * @param delegate the Application that should be doing the work
	 */
	public MyApp(final Node node, Application delegate) {
		m_delegate = delegate;
		m_endpoint = node.buildEndpoint(this, "x");
		m_endpoint.accept(new AppSocketAccepter());
		m_endpoint.register();
	}


// Public Methods

	/**
	 * Opening the AppSocket to another node and sending the given Buffer
	 * @param buf the buffer to send
	 * @param nh the node to send the buffer to
	 */
	public void sendBufferToNode(ByteBuffer buf, NodeHandle nh) {
		m_endpoint.connect(nh, new AppSocketWriter(buf), 30000);
	}


// Getters

	public Endpoint getEndpoint() {
		return m_endpoint;
	}


// Application Interface

	/**
	 * Delegate to the provided application
	 */
	public void deliver(Id arg0, Message arg1) {
		m_delegate.deliver(arg0, arg1);
	}

	/**
	 * Ignored
	 */
	public void update(NodeHandle arg0, boolean arg1) {}

	/**
	 * Ignored
	 */
	public boolean forward(RouteMessage arg0) {
		return true;
	}

}
