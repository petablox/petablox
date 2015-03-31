package dacapo.hsqldb;

import dacapo.Benchmark;
import dacapo.parser.Config;
import dacapo.Callback;
import java.io.File;

/**
 * Stub class which exists <b>only</b> to facilitate whole program
 * static analysis on a per-benchmark basis.  See also the "split-deps"
 * ant build target, which is also provided to enable whole program
 * static analysis.
 * 
 * @author Eric Bodden
 * @date $Date: 2006-10-03 17:24:11 +1000 (Tue, 03 Oct 2006) $
 * @id $Id: Main.java 137 2006-10-03 07:24:11Z rgarner $
 */
public class Main {
  public static void main(String args[]) throws Exception {
	  File scratchDir = new File("./scratch");
	  if (!scratchDir.exists()) {
		  scratchDir.mkdir();
	  }
	  (new HsqldbHarness(Config.parse(new File("./cnf/hsqldb.cnf")), scratchDir)).run(new Callback(), args[0], true);
  }
}
