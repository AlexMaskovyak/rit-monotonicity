package eve;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JRootPane;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.algorithms.layout.util.VisRunner;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.ObservableGraph;
import edu.uci.ics.jung.graph.event.GraphEvent;
import edu.uci.ics.jung.graph.event.GraphEventListener;
import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.util.Animator;

/**
 * A Graphical Visualization Frame
 *
 * @author Tom Nelson [Original Jung Sample Code]
 * @author Kevin Cheek
 * @author Joseph Pecoraro
 */
public class EveGraph extends JApplet {

	/** Serial number */
	private static final long serialVersionUID = 4698247817469334573L;

	/** Constant value for edge length */
    public static final int EDGE_LENGTH = 100;

	/** The Graph itself */
	private Graph<Number,Number> m_graph = null;
    private VisualizationViewer<Number,Number> m_viewer = null;
    private AbstractLayout<Number,Number> m_layout = null;

    /** GUI Elements */
    protected JButton switchLayout;

    /** A Map of Names to Vertex Numbers */
    private HashMap<String, Integer> m_vertex_names = null;

    /** ??? */
    Integer v_prev = null;

    /** A hash map of edges to messages */
    private HashMap<Integer, EveMessage> m_message = null;

    /** A queue to keep track of heartbeat messages that are added */
    private LinkedBlockingQueue<HeartBeat> m_heartbeat = null;

    /** A timer task for removal of the heartbeat */
    private Timer m_timer = null;

    /** Amount of time before a heartbeat message is removed */
    private static final long REMOVALTIME = 1500;

    /** A counter of how many edges have been created */
    private int m_edgeCount = 0;

    /**
     * Initialization
     */
    @Override
	public void init() {
    	//Initialize the hashmap for storage of edge types
    	m_message = new HashMap<Integer, EveMessage>();

    	//Initialize a queue for keeping track of what edges to remove.
    	m_heartbeat = new LinkedBlockingQueue<HeartBeat>();

        // Create a graph
    	Graph<Number,Number> ig = Graphs.<Number,Number>synchronizedDirectedGraph(new DirectedSparseMultigraph<Number,Number>());
        ObservableGraph<Number,Number> og = new ObservableGraph<Number,Number>(ig);

        // Listener that detects Graph Events
        // NOTE: Currently just produces debug output
        og.addGraphEventListener(new GraphEventListener<Number,Number>() {
			public void handleGraphEvent(GraphEvent<Number, Number> evt) {
				System.err.println("got "+evt);
			}
		});

        // Setup the fields
        m_graph = og;
        m_layout = new FRLayout<Number,Number>(m_graph);
        m_layout.setSize(new Dimension(600,600));
		Relaxer relaxer = new VisRunner((IterativeContext)m_layout);
		relaxer.stop();
		relaxer.prerelax();

		// Setup the Name->Int mapping
		m_vertex_names = new HashMap<String, Integer>();

		// Static layout for the viewer
		Layout<Number,Number> staticLayout = new StaticLayout<Number,Number>(m_graph, m_layout);
        m_viewer = new VisualizationViewer<Number,Number>(staticLayout, new Dimension(600,600));

        // Setup the GUI
        JRootPane rp = this.getRootPane();
        rp.putClientProperty("defeatSystemEventQueueCheck", Boolean.TRUE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(java.awt.Color.lightGray);
        getContentPane().setFont(new Font("Serif", Font.PLAIN, 12));

        // More GUI for the viewer
        m_viewer.setGraphMouse(new DefaultModalGraphMouse<Number,Number>());
        m_viewer.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
        m_viewer.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Number>());
        m_viewer.setForeground(Color.black); //Color for the test b/c the rest all have transfomers
        m_viewer.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				super.componentResized(event);
				System.err.println("resized");
				m_layout.setSize(event.getComponent().getSize());
			}
		});

        // Add the viewer to the GUI
        getContentPane().add(m_viewer);

        // Switch Layout Button
        switchLayout = new JButton("Switch to SpringLayout");
        switchLayout.addActionListener(new ActionListener() {

            @SuppressWarnings("unchecked")
            public void actionPerformed(ActionEvent ae) {
            	Dimension d = m_viewer.getSize();//new Dimension(600,600);
                if (switchLayout.getText().indexOf("Spring") > 0) {
                    switchLayout.setText("Switch to FRLayout");
                    m_layout =
                    	new SpringLayout<Number,Number>(m_graph, new ConstantTransformer(EDGE_LENGTH));
                    m_layout.setSize(d);
            		Relaxer relaxer = new VisRunner((IterativeContext)m_layout);
            		relaxer.stop();
            		relaxer.prerelax();
            		StaticLayout<Number,Number> staticLayout =
            			new StaticLayout<Number,Number>(m_graph, m_layout);
    				LayoutTransition<Number,Number> lt =
    					new LayoutTransition<Number,Number>(m_viewer, m_viewer.getGraphLayout(),
    							staticLayout);
    				Animator animator = new Animator(lt);
    				animator.start();
    				m_viewer.repaint();

                } else {
                    switchLayout.setText("Switch to SpringLayout");
                    m_layout = new FRLayout<Number,Number>(m_graph, d);
                    m_layout.setSize(d);
            		Relaxer relaxer = new VisRunner((IterativeContext)m_layout);
            		relaxer.stop();
            		relaxer.prerelax();
            		StaticLayout<Number,Number> staticLayout =
            			new StaticLayout<Number,Number>(m_graph, m_layout);
    				LayoutTransition<Number,Number> lt =
    					new LayoutTransition<Number,Number>(m_viewer, m_viewer.getGraphLayout(),
    							staticLayout);
    				Animator animator = new Animator(lt);
    				animator.start();
    			//	vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
    				m_viewer.repaint();

                }
            }
        });

        // Finalize the GUI
        getContentPane().add(switchLayout, BorderLayout.SOUTH);


        //Tranformer to convert a type of message to a specific stroke
		float dash[] = { 10.0f };
		final Stroke edgeStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
		final Stroke normStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,  BasicStroke.JOIN_MITER, 10.0f);
		Transformer<Number, Stroke> edgeStrokeTransformer = new Transformer<Number, Stroke>() {
			@Override
			public Stroke transform(Number arg0) {
				switch( m_message.get(arg0).getType() ){
					case HEARTBEAT:
						return edgeStroke;
					default:
						return normStroke;
				}

			}
		};

		//Transformer for converting an edge to a type of color based on the message type
		Transformer< Number, Paint> edgePaintTransformer = new Transformer<Number, Paint>(){
			public Paint transform( Number arg0){
				switch( m_message.get(arg0).getType()  ){
					case UPLOAD:
						return Color.red;

					case DOWNLOAD:
						return Color.green;

					default:
						return Color.black;
				}
			}
		};

		// Add a label to the edge based on the message data.
		Transformer<Number, String> edgeLabelTransformer = new Transformer<Number, String>() {
			public String transform(Number arg0) {
				switch( m_message.get(arg0).getType() ){
					case UPLOAD:
					case DOWNLOAD:
						return m_message.get(arg0).getData();
					default:
						return "";
				}
			}
		};

		//Tell the render to use the appropriate transformers
		m_viewer.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);
		m_viewer.getRenderContext().setEdgeDrawPaintTransformer(edgePaintTransformer);
		m_viewer.getRenderContext().setEdgeLabelTransformer(edgeLabelTransformer);

		//Schedule the timer to remove heart beat messages.
        m_timer = new Timer();
        m_timer.schedule(new HeartBeatRemoverTask(), 1000, 250);

    }


    /**
     * Start the Applet
     */
    @Override
	public void start() {
        validate();
    }




    /**
     * Add an edge between two vertexes, given by
     * their vertex id.
     * @param vFrom from vertex id
     * @param vTo to vertex id
     * @param edgeId give the edge an id
     */
    public void addEdge( int vFrom, int vTo, int edgeId, EveMessage m){
    	m_message.put(edgeId, m);
    	try{
			if( m.getType() == EveType.HEARTBEAT )
				m_heartbeat.put( new HeartBeat( edgeId, System.currentTimeMillis() ) );
		}catch( InterruptedException e ){
			System.err.println("Error: An error occured in addEdge");
			e.printStackTrace();
		}
        m_graph.addEdge(edgeId, vFrom, vTo);
        m_layout.initialize();
		Relaxer relaxer = new VisRunner((IterativeContext)m_layout);
		relaxer.stop();
		relaxer.prerelax();
		StaticLayout<Number,Number> staticLayout = new StaticLayout<Number,Number>(m_graph, m_layout);
		LayoutTransition<Number,Number> lt = new LayoutTransition<Number,Number>(m_viewer, m_viewer.getGraphLayout(), staticLayout);
		Animator animator = new Animator(lt);
		animator.start();
		m_viewer.repaint();
    }





    /**
     * Add a vertex by its number
     * @param vertexNumber the vertex number
     */
    public void addVertex( int vertexNumber ){
        m_graph.addVertex(vertexNumber);
        m_layout.initialize();
		Relaxer relaxer = new VisRunner((IterativeContext)m_layout);
		relaxer.stop();
		relaxer.prerelax();
		StaticLayout<Number,Number> staticLayout = new StaticLayout<Number,Number>(m_graph, m_layout);
		LayoutTransition<Number,Number> lt = new LayoutTransition<Number,Number>(m_viewer, m_viewer.getGraphLayout(), staticLayout);
		Animator animator = new Animator(lt);
		animator.start();
		m_viewer.repaint();
    }


    /**
     * Add a new named vertex, or if it already exists
     * do not change the graph.
     * @param name The name of the vertex
     */
    public void addVertex(String name) {
    	if ( !m_vertex_names.containsKey(name) ) {
    		int index = m_vertex_names.size();
    		m_vertex_names.put(name, index);
    		addVertex(index);
    	}
    }

    /**
     * Add an edge via names
     * @param from the name of the from vertex
     * @param to the name of the to vertex
     */
    public void addEdge(String from, String to, EveMessage msg) {
    	int fromVertex = m_vertex_names.get(from);
    	int toVertex = m_vertex_names.get(to);
    	addEdge(fromVertex, toVertex, m_edgeCount++, msg);
    }

    /**
     * A simple storage class for heart beat messages.
     *
     */
    private class HeartBeat{
    	private int edgeId;
    	private long timeStamp;
    	public HeartBeat( int edgeId, long timeStamp){
    		this.edgeId = edgeId;
    		this.timeStamp = timeStamp;
    	}
		public int getEdgeId() {
			return edgeId;
		}
		public void setEdgeId(int edgeId) {
			this.edgeId = edgeId;
		}
		public long getTimeStamp() {
			return timeStamp;
		}
		public void setTimeStamp(long timeStamp) {
			this.timeStamp = timeStamp;
		}
    }

    /**
     * A simple timer task for removing heart beat messages. This is so the display is
     * not as cluttered.
     *
     */
    private class HeartBeatRemoverTask extends TimerTask {
			@Override
			public void run() {
            	while( m_heartbeat.peek() != null && System.currentTimeMillis() - m_heartbeat.peek().getTimeStamp() > REMOVALTIME ){
            		m_graph.removeEdge(m_heartbeat.peek().getEdgeId() );
            		m_message.remove(m_heartbeat.peek().getEdgeId());
            		m_heartbeat.remove();
            		m_viewer.repaint();
            	}

            }
        }

}


