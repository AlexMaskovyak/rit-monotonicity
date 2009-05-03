package raids;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;

public class MyApp implements Application {

	/** The endpoint this maintains */
	private Endpoint m_endpoint;

	/** The application that is really supposed to do the work */
	private Application m_delegate;

	/**
	 * Basic Constructor
	 * @param node the node to build an endpoint from
	 * @param delegate the Application that should be doing the work
	 */
	public MyApp(Node node, Application delegate) {
		m_delegate = delegate;
		m_endpoint = node.buildEndpoint(this, "x");
		m_endpoint.register();
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
