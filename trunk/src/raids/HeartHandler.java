package raids;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;

public class HeartHandler {


//	Inner Classes

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

			// TODO: Delegate to m_app

		}

	}


//	Constants

	/** Check Heartbeat Time */
	public static final int CHECK_HEARTBEAT = 20000; /* 20 seconds */

	/** Send Heartbeat Thump Time */
	public static final int SEND_HEARTBEAT = 5000; /* 5 seconds */

	/** Initial Send Heartbeat Thump Time */
	public static final int INITIAL_SEND_HEARTBEAT = 3000; /* 3 seconds */


//	Fields

	/** Listening Heartbeat Timers */
	private Map<Id, Timer> m_hearts;

	/** List of Nodes to send a Heartbeat thump to, and for how many parts */
	private Map<NodeHandle, Integer> m_sendingThumpsTo;

	/** The Application to delegate to when a Node Failure is detected */
	private RaidsApp m_app;


	/**
	 * Basic Constructor
	 * @param app the RaidsApp to delegate to when a Node dies
	 */
	public HeartHandler(RaidsApp app) {
		m_app = app;
		m_hearts = new HashMap<Id, Timer>();
		m_sendingThumpsTo = new HashMap<NodeHandle, Integer>();
	}


// Public Methods

	/**
	 * Send Heartbeats to a Node
	 * Increment the Weight for that node
	 * @param nh the node to start sending thumps to
	 */
	public void sendHeartbeatsTo(NodeHandle nh) {
		synchronized (m_sendingThumpsTo) {
			if ( m_sendingThumpsTo.containsKey(nh) ) {
				Integer cnt = m_sendingThumpsTo.get(nh);
				m_sendingThumpsTo.put(nh, ++cnt);
			} else {
				m_sendingThumpsTo.put(nh, Integer.valueOf(1));
			}
		}
	}


	/**
	 * Stop Sending Heartbeats to a Node
	 * Decrement the weight for that node
	 * @param nh the node to stop sending thumps to
	 */
	public void stopSendingHeartbeatsTo(NodeHandle nh) {
		synchronized (m_sendingThumpsTo) {
			if ( m_sendingThumpsTo.containsKey(nh) ) {
				Integer cnt = m_sendingThumpsTo.get(nh);
				if ( cnt == 1 ) {
					m_sendingThumpsTo.remove(nh);
				} else {
					m_sendingThumpsTo.put(nh, --cnt);
				}
			}

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


	/**
	 * Received a Heartbeat from a Node
	 * @param nh the node this received the Heartbeat from
	 */
	public void receivedHeartbeatFrom(NodeHandle nh) {
		setHeartbeatTimerForHandle(nh); // resets the timer
	}


	/**
	 * Cancel all timers.
	 */
	public void cancelAll() {
		for (Timer x : m_hearts.values()) {
            x.purge();
            x.cancel();
            m_hearts.remove(x);
        }
	}


//	Getters

	/**
	 * Access the list of Nodes this is sending thumps to.
	 * @return the list of NodeHandles we are sending Heartbeats to
	 */
	public Set<NodeHandle> getSendingList() {
		return m_sendingThumpsTo.keySet();
	}


//	Private

	/**
	 * Debug Message
	 * @param obj Object to debug
	 */
	private void debug(Object obj) {
		System.out.println( m_app.getNode().getId().toStringFull() + ": " + obj.toString());
	}


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

}
