package conf_analyzer.stubs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobTracker;
import org.apache.hadoop.net.ScriptBasedMapping;

public class StubJT {
	
	public static void main(String[] args) throws Exception {
		
		JobTracker.main(args);
		
    ScriptBasedMapping t = new ScriptBasedMapping();
    t.setConf(new Configuration());
    t.resolve(new ModelList<String>());
	}

}
