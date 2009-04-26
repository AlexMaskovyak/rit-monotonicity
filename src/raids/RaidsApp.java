package raids;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.past.PastImpl;
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

	/** EveReporter */
	private EveReporter m_reporter;

	/** Username */
	private String m_username;

	/**
	 * Basic Constructor that rides on top of the PastImpl Constructor
	 * @param node
	 * @param manager
	 * @param replicas
	 * @param instance
	 */
	public RaidsApp(Node node, StorageManager manager, int replicas, String instance, String username, String eveHost, int evePort) {

		// PastImpl
		super(node, manager, replicas, instance);

		// Set States
		m_username = username;

		// Setup an EveReporter
		if ( eveHost == null ) {
			m_reporter = new EveReporter(); // Does nothing
		} else {
			m_reporter = new EveReporter(eveHost, evePort);
		}

		// Say "Hello World"
		m_reporter.log(username, null, EveType.REGISTER, this.getLocalNodeHandle().getId().toStringFull());

	}



	@Override
	public void deliver(Id id, Message msg) {

		// Debug
		debug("received message");
		debug(msg.toString());

		// Delegate the normal details to the PastImpl
		super.deliver(id, msg);

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
		debug("inside update");

		// Delegate the normal details to the PastImpl
		super.update(handle, joined);

	}


	/**
	 * Debug Helper, prints out the node id then the string
	 * @param str the string to print
	 */
	private void debug(String str) {
		System.out.println( getLocalNodeHandle().getId().toStringFull() + ": " + str);
	}


// Getters and Setters

	public String getUsername() {
		return m_username;
	}

	public void setUsername(String username) {
		m_username = username;
	}

}
