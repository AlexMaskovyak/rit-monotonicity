package raids;

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
import rice.p2p.scribe.Scribe;
import rice.p2p.scribe.ScribeClient;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.Topic;
import rice.pastry.commonapi.PastryIdFactory;
import eve.EveReporter;
import eve.EveType;

/**
 * The storage app is responsible for multi-casting storage requests.
 *
 * @author Kevin Cheek
 * Based on Scribe tutorial
 */
@SuppressWarnings("deprecation")
public class StorageApp implements ScribeClient, Application {
	//How long the app will wait for a request before timing out
	private static final long m_TIMEOUT = 5000;

	private Scribe m_scribe; //Instance of the scribe implementation

	private Topic m_topic; //Topic for the multicasts

	private Endpoint m_endpoint; //End point for this node

	private boolean m_isDone; //Flag for when a request has completed

	private NodeHandle[] m_nodes; //Reference to nodes responding to a request

	private int m_response; //Number of responses requested for a multicast

	private EveReporter m_reporter; //Debugging

	private Node m_node; //The node this application is attached to



	  ByteBuffer[] outs;
	  ByteBuffer out;

	  ByteBuffer[] ins;
	  ByteBuffer in;

	  int MSG_LENGTH;


	/**
	 * The constructor for this scribe client. It will construct the
	 * ScribeApplication.
	 *
	 * @param node the PastryNode
	 * @param reporter an EveReporter for logging
	 */
	public StorageApp(Node node, EveReporter reporter) {
		this.m_endpoint = node.buildEndpoint(this, "StorageApp");

		// construct Scribe
		m_scribe = new ScribeImpl(node, "StorageApp");

		// Build a topic for publish/subscribe
		m_topic = new Topic(new PastryIdFactory(node.getEnvironment()), "storage request");
		System.out.println("myTopic = " + m_topic);

		//Register the end point since we are not using the one from past.
		m_endpoint.register();

		/*Subscribe to the topic, notify anyone that is watching the thread that we finished
			then setup the node and eve */
		m_scribe.subscribe(m_topic, this);
		m_isDone = true;
		m_reporter = reporter;
		m_node = node;




		//****new
	    MSG_LENGTH = node.getLocalNodeHandle().getId().toByteArray().length;
	    outs = new ByteBuffer[1];
	    out = ByteBuffer.wrap(node.getLocalNodeHandle().getId().toByteArray());
	    outs[0] = out;

	    ins = new ByteBuffer[1];
	    in = ByteBuffer.allocate(MSG_LENGTH);
	    ins[0] = in;
	}

	/**
	 * This function will request space on all available nodes then block until enough
	 * responses come back that it can successfully complete its task.
	 *
	 * @param num	The number of chunks to distribute.
	 * @param size 	Maximum chunk size
	 * @return		An array of NodeHandles for the nodes that responded or null if it times out
	 */
	public NodeHandle[] requestSpace(int num, long size) {
		ScribeContent myMessage = new StorageRequest(m_endpoint.getLocalNodeHandle(), size);
		m_isDone = false;
		m_response = num;
		m_nodes = new NodeHandle[num];
		m_scribe.publish(m_topic, myMessage);

		Long startTime = System.currentTimeMillis();
		synchronized( this ){
			while( !m_isDone ){
				try{
					/*
					 * Extremely primitive timeout feature, but because messages are delivered
					 * in an asynchronous manner via the deliver method this is the
					 * easiest method of accomplishing this task.
					 */
					if( System.currentTimeMillis() - startTime > m_TIMEOUT )
						return null;
					this.wait(500);
				}catch( InterruptedException e ){
					e.printStackTrace();
				}
			}
		}

		return m_nodes;
	}

	public void sendFile(NodeHandle nh){

		//TODO: Send out a request so that the endpoint can build the socket

		  System.out.println(this+" opening to "+nh);
		    m_endpoint.connect(nh, new AppSocketReceiver() {

		      /**
		       * Called when the socket comes available.
		       */
		      public void receiveSocket(AppSocket socket) {
		        // register for writing
		        socket.register(false, true, 30000, this);
		      }

		      /**
		       * Called if there is a problem.
		       */
		      public void receiveException(AppSocket socket, Exception e) {
		        e.printStackTrace();
		      }

		      /**
		       * Example of how to write some bytes
		       */
		      public void receiveSelectResult(AppSocket socket, boolean canRead, boolean canWrite) {
		        try {
		          long ret = socket.write(outs,0,outs.length);
		          // see if we are done
		          if (!out.hasRemaining()) {
		            socket.close();
		            out.clear();
		          } else {
		            // keep writing
		            socket.register(false, true, 30000, this);
		          }
		        } catch (IOException ioe) {
		          ioe.printStackTrace();
		        }
		      }
		    }, 30000);


	}

	public void recieveFile(){
		//TODO: Launch when we recieve a request from the deliver function.

		m_endpoint.accept(new AppSocketReceiver() {
			   /**
		       * When we accept a new socket.
		       */
		      public void receiveSocket(AppSocket socket) {
		        // this code reuses "this" AppSocketReceiver, and registers for reading only, and a timeout of 30000.
		        socket.register(true, false, 30000, this);

		        // it's critical to call this to be able to accept multiple times
		        m_endpoint.accept(this);
		      }

		      /**
		       * Called when the socket is ready for reading or writing.
		       */
		      public void receiveSelectResult(AppSocket socket, boolean canRead, boolean canWrite) {
		        in.clear();
		        try {
		          // read from the socket into ins
		          long ret = socket.read(ins, 0, ins.length);

		          if (ret != MSG_LENGTH) {
		            // if you sent any kind of long message, you would need to handle this case better
		            System.out.println("Error, we only received part of a message."+ret+" from "+socket);
		            return;
		          }

		          System.out.println("result Recieved");
		        } catch (IOException ioe) {
		          ioe.printStackTrace();
		        }
		        // only need to do this if expecting more messages
//		        socket.register(true, false, 3000, this);
		      }

			@Override
			public void receiveException(AppSocket socket, Exception e) {
				 e.printStackTrace();
			}

		});

		m_endpoint.register();
	}

	/**
	 * This method is called when the node receives a direct message. If the message
	 * received is a response to a storage request then add the node handle to a list of nodes
	 * if enough of the nodes have responded ignore futher responses.
	 *
	 * @param id		The message id.
	 * @param message 	The message content.
	 */
	public void deliver(Id id, Message message) {
		//TODO: Add instance of check for open socket request
		if( message instanceof StorageRequest ){
			if( m_response > 0 ){
				m_nodes[ --m_response ] = ((StorageRequest) message).getResponse();
				m_reporter.log(
						((StorageRequest) message).getResponse().getId().toStringFull(),
						m_node.getId().toStringFull(), EveType.MSG,
						"Storage Response");
				//System.out.println("Got Storage Response: " + ((StorageRequest) message).getFrom().getId());
			}else{
				m_isDone = true;
			}
		}

	}

	/**
	 * This method is called when the node received a multicast message. If the message
	 * received is a storage request we will check for free space then send a response
	 * back to the originating node if we have enough available space.
	 *
	 * @param topic		The topic that this node was subscribed to.
	 * @param content 	The content contained in the message received.
	 */
	public void deliver(Topic topic, ScribeContent content) {
		if( content instanceof StorageRequest ){
			//System.out.println("Got Storage Request... Sending response");

			//TODO: Add check for available storage space

			m_reporter.log(
					((StorageRequest) content).getFrom().getId().toStringFull(),
					m_node.getId().toStringFull(), EveType.MSG,
					"ScribeMulticast");
			StorageRequest c = (StorageRequest) content;
			c.setResponse(m_node.getLocalNodeHandle());
			m_endpoint.route(null, c, ((StorageRequest) content).getFrom());
		}
	}


	/**
	 * Called if an Anycast message is recieved.
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
