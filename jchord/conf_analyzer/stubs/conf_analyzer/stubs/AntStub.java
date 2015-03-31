package conf_analyzer.stubs;

import org.apache.tools.ant.Target;

public class AntStub {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		org.apache.tools.ant.launch.Launcher.main(args);
		org.apache.tools.ant.Diagnostics.main(args);
		org.apache.tools.ant.Main.main(args);
		
		try {
			Class<org.apache.tools.ant.Task> taskCl = (Class<org.apache.tools.ant.Task>) Class.forName(args[0]);
			org.apache.tools.ant.Task task = taskCl.newInstance();
			
			task.init();
			task.maybeConfigure();
			task.perform();
			task.getDescription();
			task.reconfigure();
			task.execute();
			Target t = task.getOwningTarget();
			task.setOwningTarget(t);
			
		} catch(Exception e) {}
	}

}
