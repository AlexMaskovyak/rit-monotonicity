package tests.eve;

import java.net.ServerSocket;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;

import eve.Eve;
import eve.EveMessage;
import eve.EveReporter;

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
	 */
	public void testReporter() {
		EveReporter reporter = new EveReporter("localhost", eve_port);
		reporter.log("junit", "eve", "test", "Unit Test Message");
		reporter.log( new EveMessage("junit", "eve", "test", "Unit Test Message 2") );
		reporter.close();
		assertTrue(true);
	}

}
