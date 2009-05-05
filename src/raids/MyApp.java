package raids;

import java.io.File;
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
import util.BufferUtils;

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
     * and dumps the output to a unique TempFile.
     * @author Joseph Pecoraro
     */
    class AppSocketReader implements AppSocketReceiver {

    //	Constants

        /** Tempfile Prefix */
        private static final String TEMP_PREFIX = "RAIDS-appsocketreader-";

        /** Tempfile Suffix */
        private static final String TEMP_SUFFIX = ".tmp";

        /** Buffer Size */
        private static final int BUFFER_SIZE = 4*1024; /* 4 kilobytes */

    //	Fields

        /** The buffer of data being read from the socket */
        private ByteBuffer m_inputBuffer;

        /** The temporary file we will be dumping to */
        private File m_tempFile;

        /** The hash of file part */
        private String m_partHash;

        /**
         * Basic Constructor
         */
        public AppSocketReader() {
            try {
                m_partHash = null;
                m_inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
                m_tempFile = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * The socket is ready to read or write
         * @param socket the socket to read or write to
         * @param canRead can this socket be read from
         * @param canWrite can this socket be written to
         */
        public void receiveSelectResult(AppSocket socket, boolean canRead, boolean canWrite) {
            m_inputBuffer.clear();
            try {

                // Read
                long ret = socket.read(m_inputBuffer);
                m_inputBuffer.flip();

                // Done Reading
                if ( ret == -1 ) {

                    System.out.println("Socket we were reading from is empty... closing");
                    socket.close();

                    // TODO: Pass m_tempFile data to m_delegate the RaidsApp telling him where the file is
                    m_delegate.receivedFile(m_partHash, m_tempFile);

                }

                // Still Reading - Filled the Buffer up with some data dump it to the temp file
                else if (ret != 0) {

                    // If this is the first read, then there is a SHA1 in it!
                    if ( m_partHash == null ) {
                        byte[] sha1_ascii_bytes = new byte[BufferUtils.HASH_STRING_SIZE];
                        m_inputBuffer.get(sha1_ascii_bytes, 0, sha1_ascii_bytes.length);
                        System.out.println(m_inputBuffer.position());
                        m_partHash = new String(sha1_ascii_bytes);
                    }

                    // Raw data is in the buffer
                    BufferUtils.writeBufferToFile(m_inputBuffer, m_tempFile.getAbsolutePath(), true);
                    System.out.println("Dumped the " + ret + " bytes from the buffer into the temp file >> " + m_tempFile.getAbsolutePath() );
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
    private RaidsApp m_delegate;

    /**
     * Basic Constructor
     * @param node the node to build an endpoint from
     * @param delegate the Application that should be doing the work
     */
    public MyApp(final Node node, RaidsApp delegate) {
        m_delegate = delegate;
        m_endpoint = node.buildEndpoint(this, "RaidsAppEndpoint");
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
