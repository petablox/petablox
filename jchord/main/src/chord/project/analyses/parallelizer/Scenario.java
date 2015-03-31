package chord.project.analyses.parallelizer;

import chord.util.Utils;

/**
 * Means of data exchange between the Master and JobDispatcher.
 * Encode/decode functionalities used for data exchange between Master and Workers.
 */
public final class Scenario {
	private static int ID = 0;
	int id = ID++;

	/**
	 * Strings 'type', 'in', and 'out' can be used in any manner as long as the
	 * definitions are consistent for the JobDispatcher and the BlackBox
	 * implementations being used.
	 * Typically, 'type' is used to define the type of scenario being exchanged,
	 * 'in' is used to exchange the abstractions being used, and 'out' is used
	 * to exchange the tracked queries.
	 */
	private String type;
	private String in;
	private String out;
	
	/**
	 * sep is used as a delimiter while encoding the scenario strings. 
	 * Blackbox and JobDispatcher should use the same sep.
	 */
	private String sep;
	
	public int getID() { return id; }

	public void setType(String type) { this.type = type; }

	public void setIn(String in) { this.in = in; }

	public void setOut(String out) { this.out = out; }

	public String getType() { return type; }
	
	public String getIn() { return in; }

	public String getOut() { return out; }

	public Scenario(String line, String sep) {
		this.sep = sep;
		String[] tokens = Utils.split(line, sep, false, false, -1);
		type = tokens[0];
		in = tokens[1];
		out = tokens[2];
	}

	public Scenario(String type, String in, String out, String sep) { 
		this.type = type;
		this.in = in;
		this.out = out;
		this.sep = sep;
		if (in == null) this.in = "";
		if (out == null) this.out = "";
		if (type == null || type.equalsIgnoreCase("")) this.type = "1";
		if (sep == null) this.sep = "";
	}
	
	@Override
	public String toString() { return type + sep + in + sep + out; }
	
	public String encode() {
		return this.toString();
	}

	public void decode(String line) {
		String[] tokens = Utils.split(line, sep, false, false, -1);
		type = tokens[0];
		in = tokens[1];
		out = tokens[2];
	}
	
	public void clear() {
		type = null;
		in = null;
		out = null;
	}
}
