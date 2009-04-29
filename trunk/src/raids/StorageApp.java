package raids;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.scribe.Scribe;
import rice.p2p.scribe.ScribeClient;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.Topic;
import rice.pastry.commonapi.PastryIdFactory;
import eve.EveReporter;
import eve.EveType;

/**
 * @author Kevin Cheek Based on Scribe tutorial
 */
@SuppressWarnings("deprecation")
public class StorageApp implements ScribeClient, Application {

	Scribe m_scribe;

	Topic m_topic;

	private Endpoint endpoint;

	private boolean m_isDone;

	private NodeHandle[] m_nodes;

	private int m_response;

	private EveReporter m_reporter;

	private Node m_node;

	/**
	 * The constructor for this scribe client. It will construct the
	 * ScribeApplication.
	 *
	 * @param node
	 *            the PastryNode
	 * @param m_reporter
	 */
	public StorageApp(Node node, EveReporter reporter) {
		// you should recognize this from lesson 3
		this.endpoint = node.buildEndpoint(this, "myinstance");

		// construct Scribe
		m_scribe = new ScribeImpl(node, "myScribeInstance");

		// construct the topic
		m_topic = new Topic(new PastryIdFactory(node.getEnvironment()), "example topic");
		System.out.println("myTopic = " + m_topic);

		// now we can receive messages
		endpoint.register();
		m_scribe.subscribe(m_topic, this);
		m_isDone = true;
		m_reporter = reporter;
		m_node = node;
	}

	public NodeHandle[] requestSpace(int num, long size) {
		System.out.println("requestSpace");
		ScribeContent myMessage = new StorageRequest(endpoint.getLocalNodeHandle(), size);
		//	    MyScribeContent myMessage = new MyScribeContent(endpoint.getLocalNodeHandle(), seqNum++);
		m_isDone = false;
		m_response = num;
		m_nodes = new NodeHandle[num];
		m_scribe.publish(m_topic, myMessage);
		//sendMulticast();

		synchronized( this ){
			while( !m_isDone ){
				try{
					this.wait(500);
				}catch( InterruptedException e ){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return m_nodes;
	}

	/**
	 */
	public void deliver(Id id, Message message) {
		//  System.out.println("Deliver1");
		if( message instanceof StorageRequest ){
			if( m_response > 0 ){
				// m_nodes[ --m_response ] = ((StorageRequest) message).getFrom();
				m_nodes[ --m_response ] = ((StorageRequest) message).getResponse();
				m_reporter.log( ((StorageRequest) message).getResponse().getId().toStringFull(), m_node.getId().toStringFull(), EveType.MSG, "Storage Response" );
				//		m_reporter.log(   m_node.getId().toStringFull(), ((StorageRequest) message).getFrom().getId().toStringFull(), EveType.MSG, "ScribeMulticast");
							System.out.println("Got Storage Response: "
									+ ((StorageRequest) message).getFrom().getId());
			}else{
				m_isDone = true;
			}
		}

	}

	/**
	 * Called whenever we receive a published message.
	 */
	public void deliver(Topic topic, ScribeContent content) {
	/*	System.out.println("MyScribeClient.deliver(" + topic + "," + content
				+ ")");*/
		if( content instanceof StorageRequest ){
			System.out.println("Got Storage Request... Sending response");

			m_reporter.log(  ((StorageRequest)content).getFrom().getId().toStringFull(), m_node.getId().toStringFull(), EveType.MSG, "ScribeMulticast");
			//   Message msg = new MyMsg(endpoint.getId(), nh.getId());
			StorageRequest c = (StorageRequest)content;
			c.setResponse( m_node.getLocalNodeHandle() );
			endpoint.route(null, c, ((StorageRequest) content).getFrom());
		}
	}

	/**
	 * Called when we receive an anycast. If we return false, it will be
	 * delivered elsewhere. Returning true stops the message here.
	 */
	public boolean anycast(Topic topic, ScribeContent content) {
		/*	boolean returnValue = m_scribe.getEnvironment().getRandomSource().nextInt(
					3) == 0;
			System.out.println("MyScribeClient.anycast(" + topic + "," + content
					+ "):" + returnValue);
			return returnValue;*/
		return false;
	}

	public void childAdded(Topic topic, NodeHandle child) {
		//    System.out.println("MyScribeClient.childAdded("+topic+","+child+")");
	}

	public void childRemoved(Topic topic, NodeHandle child) {
		//    System.out.println("MyScribeClient.childRemoved("+topic+","+child+")");
	}

	public void subscribeFailed(Topic topic) {
		//   System.out.println("MyScribeClient.childFailed("+topic+")");
	}

	@Override
	public boolean forward(RouteMessage msg) {

		// Debug
		//		debug("inside forward");

		// Try out Eve
/*		System.out.println("Inside forward me: " + m_node.getId()
				+ " Destination: " + ((StorageRequest) msg.getMessage()).getFrom().getId().toString() );
		if( msg.getMessage() instanceof StorageRequest ){
			System.out.println("insidefloop");
			if( ((StorageRequest) msg.getMessage()).getPrevHop() != null ){
				//		System.out.println("Forward From: " + ((StorageRequest)msg.getMessage(endpoint.getDeserializer()).getPrevHop().getId().toString() + " to: "+ msg.getNextHopHandle().getId().toString());
				m_reporter.log(
						m_node.getId().toStringFull(),
						((StorageRequest) msg.getMessage()).getPrevHop().getId().toStringFull(),
						EveType.MSG, "ScribeMulticast");
			}*/
			//	((StorageRequest)msg.getMessage()).addHop(m_node.getLocalNodeHandle());
	//		((StorageRequest) msg.getMessage()).addHop(m_node.getLocalNodeHandle());

			/*		((StorageRequest)msg.getMessage()).prevHop = m_node.getLocalNodeHandle();
					((StorageRequest)msg.getMessage()).test();*/
		//}
		return true;
	}

	public void update(NodeHandle handle, boolean joined) {

	}

}
