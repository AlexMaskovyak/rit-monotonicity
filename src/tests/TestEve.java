package tests;

import java.net.ServerSocket;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;

import eve.Eve;
import eve.EveMessage;
import eve.EveReporter;
import eve.EveType;

/**
 * Make sure that an Eve Server can be Setup
 * and that an EveReporter can pass it messages.
 *
 * @author Joseph Pecoraro
 */
public class TestEve extends TestCase {

	/** Global Eve */
	private Eve eve;

	/** The Global Port eve is listening on */
	private int eve_port;

	/**
	 * Setup Details before Running tests
	 */
	@Before
	public void setUp() throws Exception {
		eve = new Eve( new ServerSocket(0) );
		eve_port = eve.getPort();
		eve.start();
	}


	/**
	 * Tear Down Details after Running tests
	 */
	@After
	@SuppressWarnings("deprecation")
	public void tearDown() throws Exception {
		eve.stop();
	}

	/**
	 * Test that a Reporter can access Eve
	 * @throws InterruptedException
	 */
	public void testReporter() throws InterruptedException {

		// 5 Node Example
		final int PAUSE = 1000;
		EveReporter nodeExample = new EveReporter("localhost", eve_port);
		nodeExample.log("node0", null, EveType.REGISTER, "node0"); Thread.sleep(PAUSE);
		nodeExample.log("node1", null, EveType.REGISTER, "node1"); Thread.sleep(PAUSE);
		nodeExample.log("node2", null, EveType.REGISTER, "node2"); Thread.sleep(PAUSE);
		nodeExample.log("node3", null, EveType.REGISTER, "node3"); Thread.sleep(PAUSE);
		nodeExample.log("node4", null, EveType.REGISTER, "node4"); Thread.sleep(PAUSE);
		nodeExample.log("node0", "node1", EveType.DEBUG, "Empty"); Thread.sleep(PAUSE);
		nodeExample.log("node1", "node2", EveType.DEBUG, "Empty"); Thread.sleep(PAUSE);
		nodeExample.log("node2", "node3", EveType.DEBUG, "Empty"); Thread.sleep(PAUSE);
		nodeExample.log("node3", "node4", EveType.DEBUG, "Empty"); Thread.sleep(PAUSE);
		nodeExample.log("node4", "node3", EveType.DEBUG, "Empty"); Thread.sleep(PAUSE);
		nodeExample.log("node3", "node2", EveType.DEBUG, "Empty"); Thread.sleep(PAUSE);
		nodeExample.log("node2", "node1", EveType.DEBUG, "Empty"); Thread.sleep(PAUSE);
		nodeExample.log("node1", "node0", EveType.DEBUG, "Empty"); Thread.sleep(PAUSE);
		nodeExample.log("node0", "node4", EveType.DEBUG, "Empty"); Thread.sleep(PAUSE);
		nodeExample.log("node5", null, EveType.REGISTER, "node5"); Thread.sleep(PAUSE);
		nodeExample.log("node5", "node0", EveType.DEBUG, "Empty");
		nodeExample.log("node5", "node1", EveType.DEBUG, "Empty");
		nodeExample.log("node5", "node2", EveType.DEBUG, "Empty");
		nodeExample.log("node5", "node3", EveType.DEBUG, "Empty");
		nodeExample.log("node5", "node4", EveType.DEBUG, "Empty"); Thread.sleep(PAUSE);


		// Generic Data Structure Tests
		EveReporter reporter = new EveReporter("localhost", eve_port);
		EveReporter reporter2 = new EveReporter("localhost", eve_port);
		reporter.log("junit", "eve", EveType.DEBUG, "Unit Test Message");
		reporter2.log("junit2", "eve", EveType.DEBUG, "Unit Test Message");
		reporter.log( new EveMessage("junit", "eve", EveType.DEBUG, "Unit Test Message") );
		reporter.log( "junit", "junit2", EveType.DEBUG, "Unit Test Message");
		reporter.close();
		reporter2.close();
		Thread.sleep(3000);
		assertTrue(true);

	}

}
