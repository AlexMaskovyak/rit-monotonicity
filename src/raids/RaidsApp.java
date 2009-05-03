package raids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import rice.Continuation;
import rice.p2p.commonapi.CancellableTask;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastImpl;
import rice.pastry.PastryNode;
import rice.persistence.StorageManager;
import eve.EveReporter;
import eve.EveType;

/**
 * Our Application.
 * Rides on top of a Past Implementation
 *
 * @author Joseph Pecoraro
 */
public class RaidsApp extends PastImpl{


// Inner Classes

	/**
	 * Private Inner Class.  Gets run when a heartbeat
	 * does not get fired!  Did the other node die?!
	 *
	 * @author Joseph Pecoraro
	 */
	private class CardiacArrest extends TimerTask {

        /** The Node Handle this Timer watches */
        private NodeHandle m_nodeHandle;

        /**
         * Basic Constructor
         * @param handle the node handle of the node this observers
         */
        public CardiacArrest(NodeHandle handle) {
            m_nodeHandle = handle;
        }

        /**
         * Handle the missed heartbeat
         */
        @Override
        public void run() {
            debug("Missed our heartbeat for: " + m_nodeHandle.getId().toStringFull() );
            debug("Stopping Sending our thumps to it");
            m_thumps.get(m_nodeHandle.getId()).cancel();
        }

    }

// Constants

    /** Check Heartbeat Time */
    private static final int CHECK_HEARTBEAT = 5000; /* 5 seconds */

    /** Send Heartbeat Thump Time */
    private static final int SEND_HEARTBEAT = 1000; /* 1 second */

    /** Initial Send Heartbeat Thump Time */
    private static final int INITIAL_SEND_HEARTBEAT = 3000; /* 3 second */


// Fields

    /** EveReporter */
    private EveReporter m_reporter;

    /** Username */
    private String m_username;

    /** Listening Heartbeat Timers */
    private Map<Id, Timer> m_hearts;

    /** Sending Heartbeat Thumps */
    private Map<Id, CancellableTask> m_thumps;

    /** The actual pastry node */
    private Node m_node;

    /** Set when the node is dead but the environment is not */
    private boolean m_dead;

    /** The User's Personal File List */
    private List<PersonalFileInfo> m_personalFileList;

    /** Cheap Lock */
    private boolean m_isDone;

    /** Volatile temporary data not really state... used in a lock */
    private MasterListContent m_masterList;

    //private ProtocolApp m_pApp;

    private StorageApp ms;

    private Endpoint m_raidsEndpoint;


    /**
     * Basic Constructor that rides on top of the PastImpl Constructor
     * @param node the Pastry Node this wraps
     * @param manager Past's storage manager
     * @param replicas Past's storage replication
     * @param instance Past's instance
     * @param username the Client's username
     * @param eveHost Eve's hostname
     * @param evePort Eve's port number
     */
    public RaidsApp(Node node, StorageManager manager, int replicas, String instance, String username, String eveHost, int evePort) {

        // PastImpl
        super(node, manager, replicas, instance);

        // Set States
        m_dead = false;
        m_node = node;
        m_username = username;
        m_hearts = new HashMap<Id, Timer>();
        m_thumps = new HashMap<Id, CancellableTask>();
        m_personalFileList = new ArrayList<PersonalFileInfo>();
        m_isDone = true;
        m_masterList = null;
        m_raidsEndpoint = m_node.buildEndpoint(this, "x");
        m_raidsEndpoint.register();


        // Setup an EveReporter
        if ( eveHost == null ) {
            m_reporter = new EveReporter(); // Does nothing
        } else {
            m_reporter = new EveReporter(eveHost, evePort);
        }

        // Register Name:Id pair with Eve
        m_reporter.log(username, null, EveType.REGISTER, this.getLocalNodeHandle().getId().toStringFull());

        // Create the StorageApp for Multicasts
        ms = new StorageApp( node, m_reporter );

        // Setup the PersonalFileList
        Id storageId = PersonalFileListHelper.personalFileListIdForUsername(m_username, m_node.getEnvironment());
        this.lookup(storageId, new Continuation<PastContent, Exception>() {
            public void receiveException(Exception e) { e.printStackTrace(); }
            public void receiveResult(PastContent result) {
                synchronized (m_personalFileList) {
                    if ( result == null ) {
                        m_personalFileList = new ArrayList<PersonalFileInfo>();
                    } else {
                        m_personalFileList = ((PersonalFileListContent)result).getList();
                    }
                }
            }
        });

    }


// Heartbeat Helpers

    /**
     * Reset the Heartbeat Timer for a NodeHandle
     * @param other the NodeHandle to listen for
     */
    private void setHeartbeatTimerForHandle(NodeHandle other) {
        synchronized (m_hearts) {

            // Cancel the Previous Timer (if there was one)
            cancelHeartbeatTimer(other);

            // Set a new Timer
            Timer cpr = new Timer(true);
            cpr.schedule( new CardiacArrest(other), CHECK_HEARTBEAT);
            m_hearts.put(other.getId(), cpr);

        }
    }


    /**
     * Remove the Heartbeat Timer for a NodeHandle
     * NOTE: Be careful of synchronization of the m_hearts datastructure
     * @param other the NodeHandle to remove the listener for
     */
    private void cancelHeartbeatTimer(NodeHandle other) {
        Timer cpr = m_hearts.get(other.getId());
        if ( cpr != null ) {
            cpr.purge();
            cpr.cancel();
        }
    }


// Personal File List Helpers

    /**
     * Grab the latest Personal File List (asynchronous)
     */
    public void lookupPersonalFileList() {
        Id storageId = PersonalFileListHelper.personalFileListIdForUsername(m_username, m_node.getEnvironment());
        this.lookup(storageId, new Continuation<PastContent, Exception>() {
            public void receiveException(Exception e) {}
            public void receiveResult(PastContent result) {
                synchronized (m_personalFileList) {
                    if ( result == null ) {
                        m_personalFileList = new ArrayList<PersonalFileInfo>();
                    } else {
                        m_personalFileList = ((PersonalFileListContent)result).getList();
                    }
                }
            }
        });
    }


    /**
     * Submit a new File List (asynchronous)
     * @param list The new personal file list
     */
    public void updatePersonalFileList(List<PersonalFileInfo> list) {
        m_personalFileList = list;
        Id storageId = PersonalFileListHelper.personalFileListIdForUsername(m_username, m_node.getEnvironment());
        PersonalFileListContent pfl = new PersonalFileListContent(storageId, m_personalFileList);
        insert(pfl, new Continuation<Boolean[], Exception>() {
            public void receiveException(Exception e) { e.printStackTrace(); }
            public void receiveResult(Boolean[] res) {
                Boolean[] results = ((Boolean[]) res);
                int numSuccess = 0;
                for (int i = 0; i < results.length; i++) {
                    Boolean b = results[i];
                    if ( b.booleanValue() ) {
                        numSuccess++;
                    }
                }
                System.out.println("Successfully stored PersonalFileList for " + m_username + " at " + numSuccess + " locations.");
            }
        });
    }


// Master List Helpers

    /**
     * Grab the Master List for a File by using the file name (synchronous)
     * @param filename the filename for which you want the Master List
     * @return the Master List as it was stored in the DHT
     */
    public MasterListContent lookupMasterList(String filename) {
    	Id fileId = PersonalFileListHelper.masterListIdForFilename(filename, m_node.getEnvironment());
    	return lookupMasterList(fileId);
    }

    /**
     * Grab the latest Master List For the Given Id (synchronous)
     * @param fileId the PastryId for the Filename
     */
    public MasterListContent lookupMasterList(Id fileId) {

    	// Locked Variables
    	m_isDone = false;
    	m_masterList = null;

    	// Lookup
        this.lookup(fileId, new Continuation<PastContent, Exception>() {
            public void receiveException(Exception e) {}
            public void receiveResult(PastContent result) {
                m_masterList = (MasterListContent) result;
                m_isDone = true; // release the lock
            }
        });

        // Busy Wait to force this to be synchronous
        synchronized (this) {
            while ( !m_isDone ) {
            	try {
            		this.wait(500);
            	} catch (InterruptedException e) {
    				e.printStackTrace();
            	}
            }
		}

        // Return the List
        return m_masterList;

    }


    /**
     * Submit a new Master List for a File (asynchronous)
     * @param filename the filename the Master List is for
     * @param list the new Master List
     */
    public void updateMasterList(String filename, List<NodeHandle> list) {
    	Id fileId = PersonalFileListHelper.masterListIdForFilename(filename, m_node.getEnvironment());
    	updateMasterList(fileId, list);
    }


    /**
     * Submit a new Master List for the Given Id (asynchronous)
     * @param fileId id of the Pastry Filename
     * @param list the new Master List
     */
    public void updateMasterList(final Id fileId, List<NodeHandle> list) {
    	MasterListContent mlc = new MasterListContent(fileId, list);
        insert(mlc, new Continuation<Boolean[], Exception>() {
            public void receiveException(Exception e) { e.printStackTrace(); }
            public void receiveResult(Boolean[] res) {
                Boolean[] results = ((Boolean[]) res);
                int numSuccess = 0;
                for (int i = 0; i < results.length; i++) {
                    Boolean b = results[i];
                    if ( b.booleanValue() ) {
                        numSuccess++;
                    }
                }
                System.out.println("Successfully the MasterListContent for " + fileId.toString() + " at " + numSuccess + " locations.");
            }
        });

    }


// Application Interface

    /**
     * Receive a Message
     * @param id the destination
     * @param msg the message
     */
    @Override
    public void deliver(Id id, Message msg) {

        // Dead Nodes will still receive messages as long as the environment exists?
        if ( m_dead ) {
            debug("I'm dead and received: " + msg);
            return;
        }

        // Heartbeat message
        if ( msg instanceof HeartbeatMessage ) {
            debug("received HeartbeatMessage");
            debug(msg.toString());
            HeartbeatMessage thump = (HeartbeatMessage) msg;
            NodeHandle thumper = thump.getHandle();
            setHeartbeatTimerForHandle(thumper);
            m_reporter.log(thumper.getId().toStringFull(),
            		m_node.getId().toStringFull(),
            		EveType.MSG,
            		"Heartbeat");
        }

        // DumbMessage
        // TODO: REmove, debug
        else if ( msg instanceof DumbMessage ) {
        	debug("x");
        }

        // MasterListMessage message
        else if ( msg instanceof MasterListMessage ) {
        	debug("received MasterListMessage");
        }

        // ...
        else if ( false ) {

        }


        // Delegate the normal messages to the PastImpl
        else {
            super.deliver(id, msg);
        }

    }


	/**
	 * Access StoreApp's request space method, making the masters obtained
	 * visible to a client.
	 * @param num number of nodes required.
	 * @param size maximum storage size requested.
	 * @return NodeHandles which have responded positively to our storage
	 * 			request/master request.
	 */
	public NodeHandle[] requestSpace(int num, long size){

/*	    System.out.println("Requesting Space Node "+endpoint.getLocalNodeHandle()+" anycasting "+size/num);
        ScribeContent myMessage = new StorageRequest(endpoint.getLocalNodeHandle(), "test");
     //   Topic myTopic = new Topic(m_node.getId());

	    m_scribe.publish(m_topic, myMessage);*/
		return ms.requestSpace(num, size);
//		m_pApp.requestSpace(num, size);
    }


    @Override
    public boolean forward(RouteMessage msg) {

        // Debug
        // debug("inside forward");

        // Try out Eve
        // m_reporter.log(m_username, msg.getNextHopHandle().getId().toStringFull(), EveType.FORWARD, "I'm routing a message!");

        // Delegate the normal details to the PastImpl
    	try {
    		return super.forward(msg);
    	} catch (Exception e) {}
    	return true;
    }


    @Override
    public void update(NodeHandle handle, boolean joined) {

        // Debug
        // debug("inside update");

        // Delegate the normal details to the PastImpl
        super.update(handle, joined);

    }


// Public Methods

    /**
     * Status information on this node.
     */
    public void status() {
    	if ( m_dead ) { System.out.println("This Node is DEAD"); }
		System.out.println("Username: " + m_username);
		System.out.println("ID: " + m_node.getId().toStringFull());
		System.out.println("Personal File List: " + m_personalFileList);
    }


    /**
     * Best attempt to politely kill this node.
     * End its Tasks and Timers
     * Destroy the node and the node's environment.
     */
    public void kill() {

        // Tasks
        for (CancellableTask x : m_thumps.values()) {
            x.cancel();
        }

        // Timers
        for (Timer x : m_hearts.values()) {
            x.purge();
            x.cancel();
        }

        // Node
        ((PastryNode)m_node).destroy();
        m_dead = true;
        //m_node.getEnvironment().destroy();

    }


// Getters and Setters

    public String getUsername() {
        return m_username;
    }

    public void setUsername(String username) {
        m_username = username;
    }

    public Node getNode() {
        return m_node;
    }

    public List<PersonalFileInfo> getPersonalFileList() {
        return m_personalFileList;
    }

    public void setPersonalFileList(List<PersonalFileInfo> personalFileList) {
        m_personalFileList = personalFileList;
    }

	public Endpoint getRaidsEndpoint() {
		return m_raidsEndpoint;
	}


// Debug

    /**
     * This is a Debug method written by joe
     * TODO: Remove when done debugging with it.
     */
    public void cpr(RaidsApp other) {

        // Listen for Heartbeats from the other node
        setHeartbeatTimerForHandle( other.getLocalNodeHandle() );

        // Start sending messages to the other side
	    Endpoint endpoint = m_node.buildEndpoint(other, "heartbeating");
	    endpoint.register();
	    CancellableTask task = endpoint.scheduleMessageAtFixedRate(new HeartbeatMessage(m_node.getLocalNodeHandle()), INITIAL_SEND_HEARTBEAT, SEND_HEARTBEAT);

        // Save it as cancellable
        // TODO: Synchronize?
        m_thumps.put(other.getLocalNodeHandle().getId(), task);

    }

    /**
     * Debug Helper, prints out the node id then the string
     * @param str the string to print
     */
    private void debug(String str) {
        System.out.println( getLocalNodeHandle().getId().toStringFull() + ": " + str);
    }


}
