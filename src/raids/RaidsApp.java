package raids;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import rice.p2p.commonapi.CancellableTask;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
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
public class RaidsApp extends PastImpl {


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

		// Setup an EveReporter
		if ( eveHost == null ) {
			m_reporter = new EveReporter(); // Does nothing
		} else {
			m_reporter = new EveReporter(eveHost, evePort);
		}

		// Register Name:Id pair with Eve
		m_reporter.log(username, null, EveType.REGISTER, this.getLocalNodeHandle().getId().toStringFull());

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


// Application Interface

	/**
	 * Receive a Message
	 * @param id the destination
	 * @param msg the message
	 */
	public void deliver(Id id, Message msg) {

		// Dead Nodes will still receive messages as long as the environment exists?
		if ( m_dead ) {
			debug("I'm dead and received: " + msg);
			return;
		}

		// Debug
		debug("received message");
		debug(msg.toString());

		// Heartbeat message
		if ( msg instanceof HeartbeatMessage ) {
			HeartbeatMessage thump = (HeartbeatMessage) msg;
			NodeHandle thumper = thump.getHandle();
			setHeartbeatTimerForHandle(thumper);
		}

		// ...
		else if ( false ) {

		}


		// Delegate the normal messages to the PastImpl
		else {
			super.deliver(id, msg);
		}

	}

	@Override
	public boolean forward(RouteMessage msg) {

		// Debug
		debug("inside forward");

		// Try out Eve
		m_reporter.log(m_username, msg.getNextHopHandle().getId().toStringFull(), EveType.FORWARD, "I'm routing a message!");

		// Delegate the normal details to the PastImpl
		return super.forward(msg);
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
