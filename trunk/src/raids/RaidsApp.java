package raids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import rice.Continuation;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.CancellableTask;
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
 * @author Kevin Cheek
 * @author Alex Maskovyak
 * @author Joseph Pecoraro
 */
public class RaidsApp implements Application {


// Inner Classes

	/**
	 * Private Inner Class.  Gets run when a Heartbeat
	 * does not get fired!  This means the other node died!
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
         * Handle the missed Heartbeat
         * TODO: Fault Tolerance requirement: duplicate part on a new node...
         */
        public void run() {
            debug("Missed our heartbeat for: " + m_nodeHandle.getId().toStringFull() );
            debug("Stopping Sending our thumps to it");
            synchronized (m_sendingThumpsTo) {
            	m_sendingThumpsTo.remove( m_nodeHandle );
			}
        }

    }


	/**
	 * Empty Self Reminder Message
	 *
	 * @author Joseph Pecoraro
	 */
	private class SelfReminder implements Message {
		public int getPriority() {
			return 0;
		}
	}


	/**
	 * Structure to store information regarding the storage of a file piece
	 * both locally and within other Masters in the ring responsible for this
	 * piece.
	 * @author Kevin Cheek
	 * @author Alex Maskovyak
	 * @author Joe Pecoraro
	 *
	 */
	private class MasterListFilePieceInfo {

		private String m_localPath;
		private String m_DHTLookupId;
		private NodeHandle m_prev;
		private NodeHandle m_next;

		/**
		 * Default constructor.
		 * @param localPath local path to the file piece stored
		 * @param DHTLookupId
		 * @param prev
		 * @param next
		 */
		public MasterListFilePieceInfo(
				String localPath,
				String DHTLookupId,
				NodeHandle prev,
				NodeHandle next ) {

			m_localPath = localPath;
			m_DHTLookupId = DHTLookupId;
			m_prev = prev;
			m_next = next;
		}

		/**
		 * Obtain the local path to the file piece stored.
		 * @return Local path of the file piece.
		 */
		public String getLocalPath() {
			return m_localPath;
		}

		/**
		 * Obtain the key to finding the masterlist associated with this file
		 * piece.
		 * @return Key to the DHT.
		 */
		public String getDHTLookupId() {
			return m_DHTLookupId;
		}

		/**
		 * Obtain the NodeHandle of the Node upstream from us in the file
		 * replication ring.
		 * @return NodeHandle of the previous node.
		 */
		public NodeHandle getPrevNode() {
			return m_prev;
		}

		/**
		 * Obtain the NodeHandle of the Node downstream from us in the file
		 * replication ring.
		 * @return NodeHandle of the next node.
		 */
		public NodeHandle getNextNode() {
			return m_next;
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

    /** PAST */
    private PastImpl m_past;

    /** EveReporter */
    private EveReporter m_reporter;

    /** Username */
    private String m_username;

    /** Listening Heartbeat Timers */
    private Map<Id, Timer> m_hearts;

    /** List of Nodes to send a Heartbeat thump to */
    private ArrayList<NodeHandle> m_sendingThumpsTo;

    /** The actual pastry node */
    private Node m_node;

    /** Set when the node is dead but the environment is not */
    private boolean m_dead;

    /** The User's Personal File List */
    private List<PersonalFileInfo> m_personalFileList;

    /** Cheap Lock */
    private boolean m_isDone;

    /** Volatile temporary data not really state... used in a lock */
    private MasterListMessage m_masterList;

    /** Self-Reminder Message (currently just for Heartbeats) */
    private CancellableTask m_selfTask;

    /** Scribe Application - Multicast for Storage Requests */
    private StorageApp ms;

    /** MyApp Hack around FreePastry issue... */
    private MyApp m_myapp;


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

        // Set States
        m_dead = false;
        m_node = node;
        m_username = username;
        m_hearts = new HashMap<Id, Timer>();
        m_personalFileList = new ArrayList<PersonalFileInfo>();
        m_isDone = true;
        m_masterList = null;
        m_sendingThumpsTo = new ArrayList<NodeHandle>();

        // Setup an EveReporter
        if ( eveHost == null ) {
            m_reporter = new EveReporter(); // Does nothing
        } else {
            m_reporter = new EveReporter(eveHost, evePort);
        }

        // Register Name:Id pair with Eve
        m_reporter.log(username, null, EveType.REGISTER, m_node.getLocalNodeHandle().getId().toStringFull());

        // Past Application - For a DHT
    	m_past = new PastImpl(node, manager, replicas, instance);

        // Script Application - For Multicasts
        ms = new StorageApp( node, m_reporter );

        // My App - For Regular Messages
        m_myapp = new MyApp(node, this);

        // Reminder to send Heartbeats
        m_selfTask = m_myapp.getEndpoint().scheduleMessageAtFixedRate( new SelfReminder(), INITIAL_SEND_HEARTBEAT, SEND_HEARTBEAT );

        // Setup the PersonalFileList
        Id storageId = PersonalFileListHelper.personalFileListIdForUsername(m_username, m_node.getEnvironment());
        m_past.lookup(storageId, new Continuation<PastContent, Exception>() {
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
     * @param other the NodeHandle to remove the listener for
     */
    private void cancelHeartbeatTimer(NodeHandle other) {
    	synchronized (m_hearts) {
            Timer cpr = m_hearts.get(other.getId());
            if ( cpr != null ) {
                cpr.purge();
                cpr.cancel();
                m_hearts.remove(cpr);
            }
		}
    }


    /**
     * Send Heartbeats to a Node
     * @param nh the node to start sending thumps to
     */
    public void sendHeartbeatsTo(NodeHandle nh) {
    	synchronized (m_sendingThumpsTo) {
			m_sendingThumpsTo.add(nh);
		}
    }


    /**
     * Stop Sending Heartbeats to a Node
     * @param nh the node to stop sending thumps to
     */
    public void stopSendingHeartbeatsTo(NodeHandle nh) {
    	synchronized (m_sendingThumpsTo) {
			m_sendingThumpsTo.remove(nh);
		}
    }


    /**
     * Listen for Heartbeats from a Node
     * @param nh the node to start listening to
     */
    public void listenForHearbeatsFrom(NodeHandle nh) {
    	setHeartbeatTimerForHandle(nh);
    }


    /**
     * Stop Listening for Heartbeats from a Node
     * @param nh the node to stop listening to
     */
    public void stopListeningForHeartbeatsFrom(NodeHandle nh) {
    	cancelHeartbeatTimer(nh);
    }


// Personal File List Helpers

    /**
     * Grab the latest Personal File List (asynchronous)
     */
    public void lookupPersonalFileList() {
        Id storageId = PersonalFileListHelper.personalFileListIdForUsername(m_username, m_node.getEnvironment());
        m_past.lookup(storageId, new Continuation<PastContent, Exception>() {
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
        m_past.insert(pfl, new Continuation<Boolean[], Exception>() {
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
    public MasterListMessage lookupMasterList(String filename) {
    	Id fileId = PersonalFileListHelper.masterListIdForFilename(filename, m_node.getEnvironment());
    	return lookupMasterList(fileId);
    }

    /**
     * Grab the latest Master List For the Given Id (synchronous)
     * @param fileId the PastryId for the Filename
     */
    public MasterListMessage lookupMasterList(Id fileId) {

    	// Locked Variables
    	m_isDone = false;
    	m_masterList = null;

    	// Lookup
    	m_past.lookup(fileId, new Continuation<PastContent, Exception>() {
            public void receiveException(Exception e) {}
            public void receiveResult(PastContent result) {
                m_masterList = (MasterListMessage) result;
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
    public MasterListMessage updateMasterList(String filename, List<NodeHandle>[] list) {
    	Id fileId = PersonalFileListHelper.masterListIdForFilename(filename, m_node.getEnvironment());
    	return updateMasterList(fileId, list);
    }


    /**
     * Submit a new Master List for the Given Id (asynchronous)
     * @param fileId id of the Pastry Filename
     * @param list the new Master List
     */
    public MasterListMessage updateMasterList(final Id fileId, List<NodeHandle>[] list) {
    	MasterListMessage mlm = new MasterListMessage(fileId, list);
    	m_past.insert(mlm, new Continuation<Boolean[], Exception>() {
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

    	return mlm;
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

        // Heartbeat message - Reset Timer for whoever sent
        if ( msg instanceof HeartbeatMessage ) {
            debug("received HeartbeatMessage");
            debug(msg.toString());
            HeartbeatMessage thump = (HeartbeatMessage) msg;
            NodeHandle thumper = thump.getHandle();
            setHeartbeatTimerForHandle(thumper);
            m_reporter.log(thumper.getId().toStringFull(),
            		m_node.getId().toStringFull(),
            		EveType.MSG, "Heartbeat");
        }

        // Self Reminder Message - Send Heartbeat Thumps
        else if ( msg instanceof SelfReminder ) {
        	synchronized (m_sendingThumpsTo) {
		    	for (NodeHandle nh : m_sendingThumpsTo) {
		    		debug("sending heartbeat to " + nh.getId().toStringFull());
		    		routeMessageDirect(new HeartbeatMessage(m_node.getLocalNodeHandle()), nh);
				}
        	}
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
            m_past.deliver(id, msg);
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
		NodeHandle[] nh =  ms.requestSpace(num, size);

		ms.sendFile(nh[0]);
		return nh;
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
    		return m_past.forward(msg);
    	} catch (Exception e) {}
    	return true;
    }


    @Override
    public void update(NodeHandle handle, boolean joined) {

        // Debug
        // debug("inside update");

        // Delegate the normal details to the PastImpl
    	m_past.update(handle, joined);

    }


// Public Methods

    /**
     * Send a Message through MyApp
     * NOTE: This is a Hack around a FreePastry issue
     * @param msg the message to send
     * @param nh the NodeHandle to send this message directly to
     */
    public void routeMessageDirect(Message msg, NodeHandle nh) {
    	m_myapp.getEndpoint().route(null, msg, nh);
    }

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
    	m_selfTask.cancel();

        // Timers
        for (Timer x : m_hearts.values()) {
            x.purge();
            x.cancel();
        }

        // Node
        ((PastryNode)m_node).destroy();
        m_dead = true;

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


// Debug

    /**
     * This is a Debug method written by joe
     * TODO: Remove when done debugging with it.
     */
    public void cpr(RaidsApp other) {
    	NodeHandle nh = other.getNode().getLocalNodeHandle();
        setHeartbeatTimerForHandle( nh );
        synchronized (m_sendingThumpsTo) {
        	m_sendingThumpsTo.add(nh);
        }
    }

    /**
     * Debug Helper, prints out the node id then the string
     * @param str the string to print
     */
    private void debug(String str) {
        System.out.println( m_node.getLocalNodeHandle().getId().toStringFull() + ": " + str);
    }

}
