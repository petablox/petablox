package org.apache.hadoop.mapred;

public class TTMainStub {

	static boolean isStatic= false;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Throwable {
		org.apache.hadoop.mapred.TaskTracker.main(args);

//		org.apache.hadoop.mapred.IsolationRunner.main(args);
		if(isStatic) //don't explore dynamically
			org.apache.hadoop.mapred.Child.main(args);
	}

}
