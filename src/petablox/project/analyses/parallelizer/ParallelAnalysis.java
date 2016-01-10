package petablox.project.analyses.parallelizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import petablox.project.analyses.JavaAnalysis;
import petablox.util.Execution;
import petablox.util.Utils;

/**
 * Basic framework to parallelize a client analysis.
 * Master and Worker communication protocol implementation is included in the class. 
 *
 * Client analysis must extend this class and implement the following methods:
 * - JobDispatcher getJobDispatcher()
 * - String apply(String)
 *
 * Recognized system properties:
 * - petablox.parallel.host (default = localhost)
 * - petablox.parallel.port (default = 8888)
 * - petablox.parallel.mode (default = null; must be [master|worker])
 */
public abstract class ParallelAnalysis extends JavaAnalysis implements BlackBox {
	protected Execution X;

	private String masterHost;	// IP or DNS address of Master
	private int masterPort = -1;	// Port that Master listens on
	private Mode mode;			// worker or master or null
	
	protected Master master; // The Master object the parallel analysis uses. Null, if current analysis is a worker

	// Number of consecutive master reply failures before worker kills itself
	protected int masterFailureLimit = 20;

	// Number of milliseconds before worker deems a master to have timed out while requesting a new job.
	protected int masterReplyTimeOut = 1000 *60 * 60; 
	
	// Max number of milliseconds allowed from the time that a worker connection is accepted to the time
	// when data is available on the connection.
	protected int workerDataTransferTimeOut = 1000 * 60 * 60;

	// Number of milliseconds from last contact after which a worker is declared dead.
	protected int workerDeadTimeOut = 24*1000 * 60 * 60;

	protected JobDispatcher dispatcher = null;

	////////////////////////////////////////////////////////////////////////////////////
	// Methods that client analyses extending this class must implement.
	////////////////////////////////////////////////////////////////////////////////////

	public abstract JobDispatcher getJobDispatcher();

	/**
	 * Blackbox apply() implementation.
	 */
	public abstract String apply(String line);

	////////////////////////////////////////////////////////////////////////////////////
	// Methods that client analyses extending this class may override.
	////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Method invoked once and for all at the start of the parallel analysis.
	 */
	public void init() {
		// do nothing by default
	}

	/**
	 * Method invoked once and for all at the end of the parallel analysis.
	 */
	public void done() {
		// do nothing by default
	}

	public String getMasterHost() {
		if (masterHost == null)
			masterHost = System.getProperty("petablox.parallel.host", "localhost");
		return masterHost;
	}

	public int getMasterPort() {
		if (masterPort == -1)
			masterPort = Integer.getInteger("petablox.parallel.port", 8888);
		return masterPort;
	}

	public Mode getMode() {
		if (mode == null) {
			String s = System.getProperty("petablox.parallel.mode", null);
			if (s == null)
				throw new RuntimeException("Property petablox.parallel.mode=[master|worker] expected by analysis " + getName());
			if (s.equals("master"))
				mode = Mode.MASTER;
			else if (s.equals("worker"))
				mode = Mode.WORKER;
			else
				throw new RuntimeException("Unknown value '" + mode + "' for property petablox.parallel.mode=[master|worker]");
		}
		return mode;
	}

	public int getNumWorkers(){
		return master.numWorkers();
	}
	
	/**
	 * Primary run() method for the parallel analysis framework.
	 * Invokes the master or worker run() methods depending on the mode in which the analysis in initiated.
	 */
	public void run() {
		X = Execution.v();
		init();

		masterHost = getMasterHost();
		masterPort = getMasterPort();
		mode = getMode();

		if (mode == Mode.WORKER)
			runWorker();
		else {
			dispatcher = getJobDispatcher();
			if (dispatcher == null) {
				Exception e = new Exception("Dispatcher not initialized in Master");
				throw new RuntimeException(e);
			}
			master = new Master(masterPort, dispatcher, workerDataTransferTimeOut, workerDeadTimeOut);
			master.run();
		}

		done();
		X.finish(null);
	}

	////////////////////////////////////////////////////////////////////////////////////
	// Internal methods.
	////////////////////////////////////////////////////////////////////////////////////

	private void sleep(int seconds) {
		try { Thread.sleep(seconds*1000); } catch(InterruptedException e) { }
	}

	/**
	 * Method implementing worker functionality.
	 * Spins continuously till Master explicitly commands it to EXIT or Master times out.
	 */
	private void runWorker() {
		int failCount = 0;
		Integer ID = null;
		X.logs("Starting worker...");
		int numJobs = 0;

		while (true) {
			X.logs("============================================================");
			// Get a job
			String line;

			if (ID == null) { //First, ask Master for a unique ID
				line = callMaster("ID");
			} else {
				line = callMaster("GET " + ID.intValue()); //Ask master for a new job
			}

			if (line == null) { //Master failed to reply, increase failure count
				X.logs("Got null, something bad happened to master..."); 
				failCount++;
				this.sleep(5);
				if (failCount == masterFailureLimit) {
					X.logs("Master probably exited, exiting...");
					break;
				}
			} else if (line.equals("WAIT")) {
				// Wait for 5 seconds before pinging Master again
				failCount = 0;
				X.logs("Waiting...");
				this.sleep(5);
				X.putOutput("exec.status", "waiting");
				X.flushOutput();
			} else if (line.equals("EXIT")) { // Exit
				X.logs("Exiting...");
				break;
			} else if (line.startsWith("ID")) { //Set ID
				failCount = 0;
				String[] tokens = Utils.split(line, " ", false, false, 2);
				ID = new Integer(tokens[1]);
			} else if (line.startsWith("APPLY") && ID != null) {
				// Invoke client analysis with the Master provided parameters and
				// reply back to the Master with the output of the analysis.
				failCount = 0;
				X.putOutput("exec.status", "running");
				X.flushOutput();
				String[] tokens = Utils.split(line, " ", false, false, 3);
				String id = tokens[1];
				String input = tokens[2];
				String output = apply(input);
				line = callMaster("PUT "+ID.intValue()+" "+id+" "+ output);
				X.logs("Sent result to master, got reply: %s", line);
				numJobs++;
				X.putOutput("numJobs", numJobs);
				X.flushOutput();
			} else {
				if (ID == null)
					X.logs("ID not set for worker. Try again...");
				else
					X.logs("Incorrect command issued by master. Try again...");
			}
		}
	}
	
	/**
	 * Worker to Master communication
	 */
	private String callMaster(String line) {
		try {
			InetAddress addrM = InetAddress.getByName(masterHost);
			Socket socket = new Socket(addrM, masterPort);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			socket.setSoTimeout(masterReplyTimeOut);
			out.println(line);
			out.flush();
			line = in.readLine();
			in.close();
			out.close();
			socket.close();
			return line;
		} catch(SocketTimeoutException e) {
			X.logs("Read from master timed out");
			return null;
		} catch (IOException e) {
			return null;
		}
	}
}

