package petablox.project.analyses.parallelizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import petablox.util.Execution;
import petablox.util.Utils;

/**
 * Class implementing Master functionality.
 */
public class Master {
	boolean shouldExit;
	Execution X = Execution.v();
	int port; // Port that Master listens on
	JobDispatcher dispatcher;
	int workerDataTransferTimeOut, workerDeadTimeOut; 
	HashMap<Integer,Scenario> inprogress = new HashMap<Integer,Scenario>(); // Map from Scenario ID to Scenario
	HashMap<Integer,Long> lastContact = new HashMap<Integer, Long>();
	HashMap<Integer,Integer> workerToScenarioMap = new HashMap<Integer, Integer>(); // Map from Worker ID to assigned Scenario ID

	final boolean waitForWorkersToExit = true; // Wait for all workers to exit, before exiting

	int numWorkers() { return lastContact.size(); }

	public Master(int port, JobDispatcher dispatcher, int workerDataTranferTimeOut, int workerDeadTimeOut) {
		this.port = port;
		this.dispatcher = dispatcher;
		this.workerDataTransferTimeOut = workerDataTranferTimeOut;
		this.workerDeadTimeOut = workerDeadTimeOut;
	}

	/**
	 *  Master run method. Spins continuously till JobDispatcher declares that its done.
	 */
	public void run() {
		X.logs("MASTER: listening at port %s", port);
		try {
			ServerSocket socket = new ServerSocket(port);
			commController(socket);
			socket.close();
			dispatcher.saveState();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Master-Worker communication protocol enabler
	 */
	void commController(ServerSocket socket) {
		boolean exitFlag = false;
		int contactID = 0;
		while (true) {
			try {
				if (exitFlag && (!waitForWorkersToExit || numWorkers() == 0)) break;
				X.putOutput("numWorkers", numWorkers());
				X.flushOutput();
				X.logs("============================================================");
				boolean dispatcherIsDone = dispatcher.isDone();
				if (dispatcherIsDone) {
					if (!waitForWorkersToExit || numWorkers() == 0) break;
					X.logs("Dispatcher is done but still waiting for %s workers to exit...", numWorkers());
				}
				Socket worker = socket.accept();
				String hostname = worker.getInetAddress().getHostAddress() /*+ ":" + worker.getPort()*/;
				BufferedReader in = new BufferedReader(new InputStreamReader(worker.getInputStream()));
				PrintWriter out = new PrintWriter(worker.getOutputStream(), true);

				worker.setSoTimeout(workerDataTransferTimeOut);

				X.logs("MASTER: Got connection from worker %s [hostname=%s]", worker, hostname);
				String cmd = in.readLine();
				
				if (cmd.equals("ID")) { //New worker request for a unique ID
					
					if (dispatcherIsDone || numWorkers() > dispatcher.maxWorkersNeeded() + 1) { // 1 for extra buffer
						// If dispatcher is done or we have more workers than we need, then quit
						out.println("EXIT");
					} else {
						lastContact.put(contactID, System.currentTimeMillis()); // Only add if it's getting stuff
						out.println("ID " + contactID);
						contactID++;
					}
					
				} else if (cmd.startsWith("GET")) { //Worker request for a new job
					
					String[] tokens = Utils.split(cmd, " ", false, false, 2);
					int wID = Integer.parseInt(tokens[1]);
					X.logs("MASTER: Worker ID %s", wID);
					lastContact.put(wID, System.currentTimeMillis()); // Only add if it's getting stuff
					
					if (dispatcherIsDone || numWorkers() > dispatcher.maxWorkersNeeded() + 1) { // 1 for extra buffer
						// If dispatcher is done or we have more workers than we need, then quit
						out.println("EXIT");
						workerToScenarioMap.remove(wID);
						lastContact.remove(wID);
					} else if (workerToScenarioMap.containsKey(wID)) { //Worker already has a pending job, report error to dispatcher
						int sID = workerToScenarioMap.remove(wID);
						Scenario scenario = inprogress.remove(sID);
						dispatcher.onError(sID);
						out.println("WAIT");
					} else if (!workerToScenarioMap.containsKey(wID)) {  //Generate new job for worker
						Scenario reqScenario = dispatcher.createJob();   
						if (reqScenario == null) {
							X.logs("  No job, waiting (%s workers, %s workers needed)", numWorkers(), dispatcher.maxWorkersNeeded());
							out.println("WAIT");
						} else {
							inprogress.put(reqScenario.id, reqScenario);
							workerToScenarioMap.put(wID, reqScenario.id);
							out.println("APPLY " + reqScenario.id + " " + reqScenario.encode()); // Response: <ID> <task spec>
							X.logs("  GET => id=%s", reqScenario.id);
							reqScenario.clear();
						}
					}
					
				} else if (cmd.startsWith("CLEAR")) { //Not used currently
					
					lastContact.clear();
					X.logs("Cleared workers");
					
				} else if (cmd.startsWith("SAVE")) { //Not used currently
					
					dispatcher.saveState();
					X.logs("Saved");
					
				} else if (cmd.startsWith("EXIT")) { //Not used currently
					
					exitFlag = true;
					X.logs("Going to exit...");
					
				} else if (cmd.startsWith("FLUSH")) { //Not used currently
					
					flushDeadWorkers();
					X.logs("%d workers", lastContact.size());
					//out.println(lastContact.size()+" workers left");
					
				} else if (cmd.startsWith("PUT")) { //Worker replies with the result of running a job
					
					String[] tokens = Utils.split(cmd, " ", false, false, 4);
					int wID = Integer.parseInt(tokens[1]);
					int sID = Integer.parseInt(tokens[2]);
					X.logs("MASTER: Worker ID %s",wID);
					
					Integer sIDinMap = workerToScenarioMap.get(wID);
					
					if (sIDinMap != null) { //Worker is mapped to some scenario
						if (sIDinMap.intValue() == sID){ //Mapped worker scenario matches the reply scenario 
							workerToScenarioMap.remove(wID);
							Scenario scenario = inprogress.remove(sID);
							if (scenario == null) {
								X.logs("  PUT id=%s, but doesn't exist", sID);
								dispatcher.onError(sID);
								out.println("INVALID");
							} else {
								X.logs("  PUT id=%s", sID);
								scenario.decode(tokens[3]);
								dispatcher.onJobResult(scenario);
								out.println("OK");
							}
						} else {
							dispatcher.onError(sIDinMap.intValue());
						}
					}
				}
				
				in.close();
				out.close();
				
				flushDeadWorkers(); // Flush all workers that haven't contacted for workerDeadTimeOut milliseconds.
									// Report error to the dispatcher for all corresponding mapped scenarios
				
			} catch(SocketTimeoutException e) {
				X.logs("Socket read from worker timed out");
			} catch(IOException e) {
				X.logs("Some error in socket comm with a worker. Continuing with other workers");
			}
		}
		return;
	}
	
	void flushDeadWorkers() {
		HashMap<Integer,Long> newLastContact = new HashMap<Integer, Long>();
		for (Integer wID : lastContact.keySet()) {
			long t = lastContact.get(wID);
			if (System.currentTimeMillis() - t < 12*60*60*1000)
				newLastContact.put(wID, t);
			else{
				if(workerToScenarioMap.containsKey(wID)){
					int sID = workerToScenarioMap.remove(wID);
					Scenario scenario = inprogress.remove(sID); 
					dispatcher.onError(sID);
				}
			}
		}
		lastContact = newLastContact;
	}
}
