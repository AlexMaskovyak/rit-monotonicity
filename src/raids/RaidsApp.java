package raids;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import util.BufferUtils;
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
		private Id m_DHTLookupId;
		private NodeHandle m_prev;
		private NodeHandle m_next;
		private boolean m_last;

		/**
		 * Default constructor.
		 * @param localPath local path to the file piece stored
		 * @param DHTLookupId
		 * @param prev
		 * @param next
		 */
		public MasterListFilePieceInfo(
				String localPath,
				Id DHTLookupId,
				NodeHandle prev,
				NodeHandle next,
				boolean last) {

			m_localPath = localPath;
			m_DHTLookupId = DHTLookupId;
			m_prev = prev;
			m_next = next;
			m_last = last;
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
		public Id getDHTLookupId() {
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

		/**
		 * Check if this is the last node in a ring
		 * @return true if this is the last node in a ring, false otherwise.
		 */
		public boolean isLast() {
			return m_last;
		}

		public void setLocalPath(String path) {
			m_localPath = path;
		}

	}


// Fields

    /** PAST */
    private PastImpl m_past;

    /** EveReporter */
    private EveReporter m_reporter;

    /** Username */
    private String m_username;

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

    /** Handle the Heartbeat Stuff */
    private HeartHandler m_heartHandler;

    /** Self-Reminder Message (currently just for Heartbeats) */
    private CancellableTask m_selfTask;

    /** Scribe Application - Multicast for Storage Requests */
    private StorageApp ms;

    /** MyApp Hack around FreePastry issue... */
    private MyApp m_myapp;

    /** Map of Files stored on this Node */
    private Map<PartIndicator, MasterListFilePieceInfo> m_inventory;

    /** Expected parts */
    private Map<PartIndicator, File> m_expectedParts;


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
        m_personalFileList = new ArrayList<PersonalFileInfo>();
        m_isDone = true;
        m_masterList = null;
        m_heartHandler = new HeartHandler(this);
        m_inventory = new HashMap<PartIndicator, MasterListFilePieceInfo>();
        m_expectedParts = new HashMap<PartIndicator, File>();

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
        m_selfTask = m_myapp.getEndpoint().scheduleMessageAtFixedRate( new SelfReminder(),
        		HeartHandler.INITIAL_SEND_HEARTBEAT, HeartHandler.SEND_HEARTBEAT );

        // Setup the PersonalFileList
        Id storageId = GeneralIdHelper.personalFileListIdForUsername(m_username, m_node.getEnvironment());
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


// Personal File List Helpers

    /**
     * Grab the latest Personal File List (asynchronous)
     */
    public void lookupPersonalFileList() {
        Id storageId = GeneralIdHelper.personalFileListIdForUsername(m_username, m_node.getEnvironment());
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
        Id storageId = GeneralIdHelper.personalFileListIdForUsername(m_username, m_node.getEnvironment());
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
    	Id fileId = GeneralIdHelper.masterListIdForFilename(filename, m_username, m_node.getEnvironment());
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
        synchronized (m_node) {
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
    	Id fileId = GeneralIdHelper.masterListIdForFilename(filename, m_username, m_node.getEnvironment());
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
                System.out.println("Successfully stored the MasterListContent for " + fileId.toString() + " at " + numSuccess + " locations.");
            }
        });

    	return mlm;
    }


// AppSocket Callbacks

    /**
     * When an AppSocket successfully pulls a file over the wire it
     * calls this callback saying it has completed the file download.
     * @param partIndicator details on the file and the part of the file received
     * @param tempFile File where the data is now stored
     */
    public void receivedFile(final PartIndicator partIndicator, File tempFile) {
    	debug("-- received part: " + partIndicator + " --");

    	// Was this an expected file for a download?
    	synchronized (m_expectedParts) {
    		if ( m_expectedParts.containsKey(partIndicator) ) {
    			debug("Got an expected part: " + partIndicator);
        		expectedPartDownloaded(partIndicator, tempFile);
        		return;
        	}
		}

    	// Update the inventory with the Local File Path
    	MasterListFilePieceInfo mlfpi;
    	synchronized (m_inventory) {

    		// NOTE: Possible race condition if you receive the file date faster then the
    		// MasterListMessage saying you should store it.  This shouldn't happen, but
    		// its possible.  So, in that case store it anyways.
        	mlfpi = m_inventory.get(partIndicator);
        	if ( mlfpi == null ) {
        		debug("**** RACE CONDITION? ADDED TO TABLE BEFORE WE NEEDED IT *****");
        		mlfpi = new MasterListFilePieceInfo(tempFile.getAbsolutePath(), null, null, null, false);
        		m_inventory.put(partIndicator, mlfpi);
        	} else {
        		mlfpi.setLocalPath( tempFile.getAbsolutePath() );
        		m_inventory.put(partIndicator, mlfpi);
        	}

		}

    	// If there is a Next node in the path, push the file to them
    	// this is called "cascading" the file around the ring
    	// in the background
    	if ( !mlfpi.isLast() ) {
    		final NodeHandle next = mlfpi.getNextNode();
    		final String filename = tempFile.getAbsolutePath();
    		final int maxSize = (int) tempFile.length();
			new Thread() {
				public void run() {
					ByteBuffer buf = BufferUtils.getBufferForFile(filename, maxSize + PartIndicator.SIZE, partIndicator);
					buf.flip();
					sendBufferToNode(buf, next);
				}
			}.start();
    	}

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
            // debug("received HeartbeatMessage");
            // debug(msg.toString());
            HeartbeatMessage thump = (HeartbeatMessage) msg;
            NodeHandle thumper = thump.getHandle();
            m_heartHandler.receivedHeartbeatFrom(thumper);
            m_reporter.log(thumper.getId().toStringFull(),
            		m_node.getId().toStringFull(),
            		EveType.MSG, "Heartbeat");
        }

        // Self Reminder Message - Send Heartbeat Thumps
        else if ( msg instanceof SelfReminder ) {
	    	for (NodeHandle nh : m_heartHandler.getSendingList()) {
	    		// debug("sending heartbeat to " + nh.getId().toStringFull());
	    		routeMessageDirect(new HeartbeatMessage(m_node.getLocalNodeHandle()), nh);
			}
        }

        // MasterListMessage message
        else if ( msg instanceof MasterListMessage ) {
        	debug("received MasterListMessage");

        	// States
        	MasterListMessage mlm = (MasterListMessage) msg;

        	// For Each Part List
        	NodeHandle prev, next;
        	List[] allParts = mlm.getParts();
        	for (int i = 0; i < allParts.length; i++) {
        		List<NodeHandle> parts = allParts[i];
        		int idx = parts.indexOf( m_node.getLocalNodeHandle() );
				if ( idx != -1 ) {
					boolean last = false;

					// Setup Prev
					if ( idx == 0 ) {
						prev = parts.get(parts.size()-1);
					} else {
						prev = parts.get(idx-1);
					}

					// Setup Next
					if ( idx == parts.size()-1 ) {
						last = true;
						next = parts.get(0);
					} else {
						next = parts.get(idx+1);
					}

					// The Hash Key
					PartIndicator pi = new PartIndicator( mlm.getLookupId().toStringFull(), i);

					// Setup MasterListFilePieceInfo for this File Part
					synchronized (m_inventory) {

						// NOTE: Possible Race condition if the file data is received before this message.
						// If that was the case then there was a mostly blank entry already written, check for it
						if ( m_inventory.containsKey(pi) ) {
							debug("**** RACE CONDITION RECOVERED - NOTICED TABLE ENTRY AND REUSING IT *****");
							MasterListFilePieceInfo oldInfo = m_inventory.get(pi);
							MasterListFilePieceInfo newInfo = new MasterListFilePieceInfo(oldInfo.getLocalPath(), mlm.getLookupId(), prev, next, last);
							m_inventory.put(pi, newInfo);
						} else {
							m_inventory.put(pi, new MasterListFilePieceInfo(null, mlm.getLookupId(), prev, next, last));
							debug("PUT: " + pi.toString());
						}

					}

					// Setup Heartbeats
					m_heartHandler.listenForHearbeatsFrom(prev);
					m_heartHandler.sendHeartbeatsTo(next);

				}
			}

        }

        // DownloadMessage
        else if ( msg instanceof DownloadMessage ) {
        	debug("received DownloadMessage");

        	DownloadMessage dlmsg = (DownloadMessage) msg;
        	MasterListFilePieceInfo mlfpi = m_inventory.get(dlmsg.getPartIndicator());

        	// Requested a download for a file we don't have yet
        	// TODO: Add Fault Tolerance
        	if ( mlfpi == null ) {
        		return;
        	}

        	// Send the file to the requester
        	File file = new File(mlfpi.getLocalPath());
        	String filename = file.getAbsolutePath();
        	int maxSize = (int) file.length();
        	NodeHandle requester = dlmsg.getRequester();

        	// Special Case, the requester is me!
        	// TODO: Refactor this is duplicated code
        	if ( dlmsg.getRequester().equals(m_node.getLocalNodeHandle()) ) {
        		debug("Special Case... sending to myself!");
				expectedPartDownloaded( dlmsg.getPartIndicator(), file );
        	}

        	// Normal case, send the file
        	else {
	        	ByteBuffer buf = BufferUtils.getBufferForFile(filename, maxSize + PartIndicator.SIZE, dlmsg.getPartIndicator());
	        	buf.flip();
	        	debug("Sending: " + dlmsg.getPartIndicator() + " to " + requester.getId().toStringFull());
	        	sendBufferToNode(buf, requester);
        	}
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


    /**
     * Whenever Messages pass through this node
     */
    public boolean forward(RouteMessage msg) {
    	try {
    		return m_past.forward(msg);
    	} catch (Exception e) {}
    	return true;
    }


    /**
     * Normal Updates
     */
    public void update(NodeHandle handle, boolean joined) {
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
     * Send a Buffer through MyApp, uses AppSockets
     * @param buf the buffer of data
     * @param nh the node to send the data to
     */
    public void sendBufferToNode(ByteBuffer buf, NodeHandle nh) {
    	m_myapp.sendBufferToNode(buf, nh);
    }


    /**
     * Expecting incoming parts.
     * @param fildId the file id
     * @param parts the number of parts to expect
     */
    public void setExpectedParts(String fileId, int parts) {
    	for (int i = 0; i < parts; i++) {
    		PartIndicator pi = new PartIndicator(fileId, i);
    		debug("Expecting: " + pi);
			m_expectedParts.put(pi, null);
		}
    }


    /**
     * When an expected part has been downloaded.
     * @param partIndicator the part that was downloaded
     * @param file file where the date is stored
     */
    public void expectedPartDownloaded(PartIndicator partIndicator, File file) {
    	synchronized (m_expectedParts) {

    		// Fill in the entry in the expected data structure
    		m_expectedParts.put(partIndicator, file);

	    	// Check if all downloaded
	    	boolean allDownloaded = true;
	    	for (PartIndicator pi : m_expectedParts.keySet()) {
				if ( m_expectedParts.get(pi) == null ) {
					allDownloaded = false;
					break;
				}
			}

	    	// When all downloaded
	    	if ( allDownloaded ) {
	    		System.out.println("ALL PARTS DOWNLOADED!!!!");
	    	}

    	}
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
        m_heartHandler.cancelAll();

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
     * Debug Helper, prints out the node id then the string
     * @param str the string to print
     */
    private void debug(String str) {
        System.out.println( m_node.getLocalNodeHandle().getId().toStringFull() + ": " + str);
    }

}
