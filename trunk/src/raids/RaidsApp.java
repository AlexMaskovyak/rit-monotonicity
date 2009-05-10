package raids;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import chunker.Chunker;

import rice.Continuation;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.CancellableTask;
import rice.p2p.commonapi.DeliveryNotification;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.MessageReceipt;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastImpl;
import rice.pastry.PastryNode;
import rice.persistence.StorageManager;
import util.BufferUtils;
import util.SHA1;
import eve.EveReporter;
import eve.EveType;

/**
 * Our Application.
 * Uses a Past for a DHT, Scribe for Multicast, and our own
 * Application "MyApp" for Transferring Files and Messages.
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
			return DEFAULT_PRIORITY;
		}
	}


	/**
	 * Structure to store information regarding the storage of a file piece
	 * both locally and within other Masters in the ring responsible for this
	 * piece.
	 *
	 * @author Kevin Cheek
	 * @author Alex Maskovyak
	 * @author Joe Pecoraro
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
		 * Obtain the key to finding the master list associated with this file
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

		public void setPrevNode(NodeHandle prev) {
			m_prev = prev;
		}

		public void setNextNode(NodeHandle next) {
			m_next = next;
		}

		public void setLocalPath(String path) {
			m_localPath = path;
		}

	}


//	Constants

	/** Indicate a Missing File - Special Case where no-one had the file */
	private File MISSING_FILE = new File("/");  // Some Random Directory... Root is easy

// 	Fields

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

    /** Expected reassembled file name after downloading all pieces */
    private String m_expectedReassembledFileName;

    /** Expected parts should come from */
    private List<NodeHandle>[] m_expectedPartOwners;

    /** Allows others to know when new requests can be made */
    private boolean m_readyForNewDownloadRequests;

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

        // set when downloading is to occur
        m_expectedParts = new HashMap<PartIndicator, File>();
        m_expectedReassembledFileName = null;
        m_expectedPartOwners = null;
        m_readyForNewDownloadRequests = true;

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
    	debug("Received part: " + partIndicator);

    	// Was this an expected file for a download? Null or otherwise report it!
    	synchronized (m_expectedParts) {

    		if ( m_expectedParts.containsKey(partIndicator) ) {
    			debug("Received Expected part: " + partIndicator);
        		expectedPartDownloaded(partIndicator, tempFile);
        		return;
        	}
		}

    	// If the file was null then we can ignore it
    	if ( tempFile == null ) {
    		debug("WARNING: Ignoreing Empty File sent to use. This should never happen!");
    		return;
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

        // Recover Message
        if ( msg instanceof RecoverMessage ) {
        	debug("received recover message");
        	RecoverMessage recoverMessage = (RecoverMessage)msg;
        	PartIndicator pi = recoverMessage.getPart();
        	MasterListFilePieceInfo mlfpi = m_inventory.get(pi);
        	mlfpi.setNextNode( recoverMessage.getNewNext() );
        	m_heartHandler.sendHeartbeatsTo( recoverMessage.getNewNext() );
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
	    	for (final NodeHandle nh : m_heartHandler.getSendingList()) {
	    		// debug("sending heartbeat to " + nh.getId().toStringFull());
	    		HeartbeatMessage hb = new HeartbeatMessage(m_node.getLocalNodeHandle());
	    		m_myapp.getEndpoint().route(null, hb, nh, new DeliveryNotification() {
	    			public void sent(MessageReceipt receipt) {}
					public void sendFailed(MessageReceipt receipt, Exception e) {
						debug("TOTALLY TIMED OUT A HEARTBEAT WE SENT TO: " + nh.getId().toStringFull());
						m_heartHandler.stopSendingHeartbeatsTo(nh);
					}
	    		});
			}
        }

        // MasterListMessage message
        else if ( msg instanceof MasterListMessage ) {
        	// debug("received MasterListMessage");

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
							// debug("PUT: " + pi.toString());
						}

					}

					// Start Heartbeats
					m_heartHandler.listenForHearbeatsFrom(prev);
					m_heartHandler.sendHeartbeatsTo(next);

				}
			}

        }

        // DownloadMessage
        else if ( msg instanceof DownloadMessage ) {
        	debug("received DownloadMessage");

        	DownloadMessage dlmsg = (DownloadMessage) msg;
        	NodeHandle requester = dlmsg.getRequester();
        	MasterListFilePieceInfo mlfpi = m_inventory.get(dlmsg.getPartIndicator());

        	// We Don't Have the File - Send back an empty file (special indicator that we have it!)
        	if ( mlfpi == null ) {
        		debug( requester.getId().toStringFull() + " asked me for something I don't have yet. Sending them an empty file");
        		ByteBuffer buf = ByteBuffer.wrap( dlmsg.getPartIndicator().toBytes() );
        		sendBufferToNode(buf, requester);
        		return;
        	}

        	// Send the file to the requester
        	File file = new File(mlfpi.getLocalPath());
        	String filename = file.getAbsolutePath();
        	int maxSize = (int) file.length();

        	// Special Case, the requester is me!
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
	public NodeHandle[] requestSpace(int num, long size, List<NodeHandle> excluded){
		return ms.requestSpace(num, size, excluded);
    }

	/**
	 * Determines whether the application is currently serving a download
	 * request.
	 * @return True if the app can service a new download request, false
	 * 		 	otherwise.
	 */
	public boolean isReadyForNewDownloadRequests() {
		return m_expectedParts.isEmpty();
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


//	Send Messages

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
     * Send a DownloadMessage through MyApp, with a Timeout Failsafe
     * NOTE: Anything with MyApp is a hack around a FreePastry issue!
     * @param msg the message to send
     * @param nh the NodeHandle to send this message directly to
     */
    public void sendDownloadMessage(DownloadMessage msg, NodeHandle nh) {
    	m_readyForNewDownloadRequests = false;
    	final PartIndicator pi = msg.getPartIndicator();
    	m_myapp.getEndpoint().route(null, msg, nh, new DeliveryNotification() {

			/**
			 * Timeout when trying to download a file.  So we assume the
			 * node DOES NOT have the file and continue as though we
			 * received a null file.
			 * @param receipt information on the Message
			 * @param e an exception from the message
			 */
			public void sendFailed(MessageReceipt receipt, Exception e) {
				receivedFile(pi, null);
			}

			/** Ignored */
			public void sent(MessageReceipt arg0) {}

    	});
    }


//	Send Files

    /**
     * Send a Buffer through MyApp, uses AppSockets
     * @param buf the buffer of data
     * @param nh the node to send the data to
     */
    public void sendBufferToNode(ByteBuffer buf, NodeHandle nh) {
    	m_myapp.sendBufferToNode(buf, nh);
    }


//	Download Files

    /**
     * Expecting incoming parts.
     * @param fildId the file id
     * @param parts the number of parts to expect
     */
    public void setExpectedParts(String fileId, int parts, List<NodeHandle>[] owners) {
    	m_expectedPartOwners = owners;
    	for (int i = 0; i < parts; i++) {
    		PartIndicator pi = new PartIndicator(fileId, i);
    		debug("Expecting: " + pi);
			m_expectedParts.put(pi, null);
		}
    }

    /**
     * Expecting the parts to be reassembled into a file with this name.
     * @param reassembledFileName the reassembled file's name.
     */
    public void setExpectedReassembledFileName( String reassembledFileName ) {
    	m_readyForNewDownloadRequests = false;
    	m_expectedReassembledFileName = reassembledFileName;
    }

    /**
     * When attempting to download a part from a Node, remove that
     * node from the list so we don't retry that node.
     * @param part the part number
     * @param nh the node
     */
    public void attemptDownloadPartFrom(int part, NodeHandle nh) {
    	m_expectedPartOwners[part].remove(nh);
    }


    /**
     * When an expected part has been downloaded.
     * @param partIndicator the part that was downloaded
     * @param file file where the date is stored
     */
    public void expectedPartDownloaded(PartIndicator partIndicator, File file) {
    	File f = file;
    	synchronized (m_expectedParts) {

    		// If the file was null, then we have to try downloading from someone else
    		if ( f == null ) {

    			// Look at the list of nodes for this part to choose a random node,
    			int part = partIndicator.getPartNum();
    			int size = m_expectedPartOwners[part].size();
    			if ( size != 0 ) {

	    			// Choose a random node and try the download again
	    			debug("Expected part was null, sending another download message out!");
	    			int rand = m_node.getEnvironment().getRandomSource().nextInt(size);
	    			NodeHandle nh = m_expectedPartOwners[part].get(rand);
	    			attemptDownloadPartFrom(part, nh);
	    			sendDownloadMessage(new DownloadMessage(partIndicator, m_node.getLocalNodeHandle()), nh);

	    			// Break the algorithm, keeping the value in the m_expectedParts
	    			// null, so we don't think we downloaded the part yet.
	    			return;

    			}

    			// The size of the list was 0, therefore no-one had the file!
    			// Continue with the normal algorithm, marking the file as MISSING
    			// so that we will end eventually and we will know that this part is gone.
    			debug("AHHHH!  NO-ONE HAS: " + partIndicator + ". Marked as MISSING_FILE!");
				f = MISSING_FILE;

    		}

    		// Fill in the entry in the expected data structure
    		m_expectedParts.put(partIndicator, f);

	    	// Check if all parts were downloaded
	    	boolean allDownloaded = true;
	    	for (PartIndicator pi : m_expectedParts.keySet()) {
				if ( m_expectedParts.get(pi) == null ) {
					allDownloaded = false;
					break;
				}
			}

	    	// When all are downloaded
	    	if ( allDownloaded ) {
	    		allPartsDownloaded();
	    	}

    	}
    }


    /**
     * Called when all of the expected parts are potentially downloaded,
     * meaning we should have everything we need!  First, check to make
     * sure all the parts are valid.
     */
    private void allPartsDownloaded() {

    	m_readyForNewDownloadRequests = true;

    	// Count missing files, ones that couldn't be downloaded because no-one had them
    	int missingFiles = 0;
    	for (PartIndicator pi : m_expectedParts.keySet()) {
			if ( m_expectedParts.get(pi).equals(MISSING_FILE) ) {
				m_expectedParts.put(pi, null);
				missingFiles++;
			}
		}

    	// Too many missing files to reassemble
    	if (missingFiles > 1) {
    		debug("TOO MANY MISSING FILES. YOU'RE DOOMED.");
    		return;
    	}

    	// All Good
    	debug("ENOUGH PARTS SUCCESSFULLY DOWNLOADED!!!!");

    	// Reassemble file
    	int numberOfChunks = m_expectedParts.size();
    	String[] fileChunks = new String[ numberOfChunks ];
    	String iPath = null;
    	String outPath = System.getProperty("user.home") + File.separatorChar;

    	Set<Entry<PartIndicator, File>> partsAndFiles = m_expectedParts.entrySet();
    	for( Entry<PartIndicator, File> entry : partsAndFiles ) {
    		PartIndicator pi = entry.getKey();
    		File file = entry.getValue();
    		fileChunks[ pi.getPartNum() ] = file.getName();
    		if( iPath == null ) {
    			iPath = file.getParentFile().getAbsolutePath() + File.separatorChar;
    		}
    	}

    	debug( "Path to chunks: " + iPath );
    	for( String s : fileChunks ) {
    		debug( "Chunks to assemble: " + s );
    	}

    	Chunker.reassemble(
    			iPath,
    			fileChunks,
    			outPath,
    			m_expectedReassembledFileName );

    	debug( String.format(
    			"FINISHED ASSEMBLING FILE TO '%s'.",
    			outPath + m_expectedReassembledFileName ) );

    	// Verify existence after assembly
    	File reassembledFile = new File( outPath + m_expectedReassembledFileName );
    	if( !reassembledFile.exists() ) {
    		debug( "REASSEMBLED FILE DOESN'T EXIST!  ERROR OCCURRED SOMEWHERE!");
    		return;
    	}

    	// Verify hash after assembly
    	String originalHash = m_personalFileList.get(
    			m_personalFileList.indexOf(
    					new PersonalFileInfo(
    							m_expectedReassembledFileName,
    							null ) ) ).getHash();

    	String reassembledHash = SHA1.getInstance().hash( reassembledFile );

    	debug( String.format(
    			"Original hash: '%s' new hash: '%s'\n", originalHash, reassembledHash ) );

    	if( !reassembledHash.equals( originalHash ) ) {
    		debug( "REASSEMBLED FILE'S HASH IS INCORRECT!  RECOMMENDATION: ATTEMPT REDOWNLOAD" );
    		return;
    	}

    	debug( String.format(
    			"DOWNLOAD, REASSEMBLY AND VERIFICATION COMPLETE FOR: '%s'",
    			reassembledFile.getAbsolutePath() ) );

    	m_expectedReassembledFileName = null;
    	m_expectedParts.clear();

    }


//	Node Death

    /**
     * Handle a Node's death, by spawning the proper
     * Recovery Threads if that was the node we were
     * watching (previous).
     * @param dead the Node that died
     */
    public void handleNodeDeath(NodeHandle dead) {
    	synchronized (m_inventory) {
        	for (PartIndicator pi : m_inventory.keySet()) {
    			MasterListFilePieceInfo mlfpi = m_inventory.get(pi);
    			if ( mlfpi.getPrevNode().equals(dead) ) {
    				mlfpi.setPrevNode(null); // clear the value, it will be reset later
    				m_inventory.put(pi, mlfpi);
    				new RecoveryThread(this, pi, dead).start();
    			}
    		}
		}
    }


    /**
     * Update Previous Node for Part
     * @param prev the new previous node due to a recovery
     * @param pi the part indicator
     */
    public void setPreviousNodeForPart(NodeHandle prev, PartIndicator pi) {
    	synchronized (m_inventory) {
    		MasterListFilePieceInfo mlfpi = m_inventory.get(pi);
    		mlfpi.setPrevNode(prev);
    		m_inventory.put(pi, mlfpi);
		}
    }


//	Public Methods

    /**
     * Grab the local file given a PartIndicator
     * @param pi the part indicator
     * @return the file stored for that part, null if not found
     */
    public File lookupInInventory(PartIndicator pi) {
    	synchronized (m_inventory) {
    		MasterListFilePieceInfo mlfpi = m_inventory.get(pi);
    		return (mlfpi == null || mlfpi.getLocalPath() == null) ? null : new File(mlfpi.getLocalPath());
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
		System.out.println("Inventory: " + m_inventory.keySet());
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
