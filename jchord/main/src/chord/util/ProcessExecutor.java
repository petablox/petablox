package chord.util;

import static chord.util.ExceptionUtil.fail;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.TimerTask;
import java.util.Timer;

/**
 * Utility to execute a system command specified as a string in a separate process.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class ProcessExecutor {
	/**
	 * Executes a given system command specified as a string in a separate process.
	 * <p>
	 * The invoking process waits till the invoked process finishes. The stdout and 
	 * stderr of the invoked process are printed in the file specified by outputFile.
	 * 
	 * @param cmdarray A system command to be executed.
	 * 
	 * @return The exit value of the invoked process.  By convention, 0 indicates normal termination.
	 */
	public static final int executeWithRedirect(String[] cmdarray, File outputFile, int timeout) throws Throwable {
		/*	Process p = Runtime.getRuntime().exec(cmdarray);
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		PrintWriter rpw = new PrintWriter(outputFile);
		while ((line = in.readLine()) != null) {
			rpw.println(line);
		}
		in.close();
		rpw.flush();
		rpw.close();
		p.waitFor();
		if(p.exitValue() != 0)
			throw new RuntimeException("The process did not terminate normally");
		 */	
		ProcessBuilder pb = new ProcessBuilder(cmdarray);
		pb.redirectErrorStream(true);
		final Process p = pb.start();
		final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		final PrintWriter rpw = new PrintWriter(outputFile);

		TimerTask killOnDelay = null;
		if (timeout > 0) {
			Timer t = new Timer();
			killOnDelay = new KillOnTimeout(p);
			t.schedule(killOnDelay, timeout);
		}

		Thread th = new Thread() {
			public void run() {
				try {
					String line;
					while ((line = in.readLine()) != null) {
						rpw.println(line);
					}
					in.close();
					rpw.flush(); rpw.close();
				} catch (Exception e) {
					fail("Error in writing process output to file");
				}
			}
		};
		th.start();

		int exitValue = p.waitFor();
		if (timeout > 0) {
			killOnDelay.cancel();
		}

		th.join();
		if(p != null){
			if(p.getOutputStream() != null){
				p.getOutputStream().close();
			}
			if(p.getErrorStream() != null){
				p.getErrorStream().close();
			}
			if(p.getInputStream() != null){
				p.getInputStream().close();
			}
			p.destroy();
		}
		return exitValue;
	}

	public static final int execute(String[] cmdarray) throws Throwable {
		return execute(cmdarray, null, null, -1);
	}

    /**
     * Executes a given system command specified as a string in a separate process.
     * <p>
     * The invoking process waits till the invoked process finishes.
     * 
     * @param cmdarray A system command to be executed.
     * 
     * @return The exit value of the invoked process.  By convention, 0 indicates normal termination.
     */
    public static final int execute(String[] cmdarray, String[] envp, File dir, int timeout) throws Throwable {
        Process proc = executeAsynch(cmdarray, envp, dir);
        TimerTask killOnDelay = null;
        if (timeout > 0) {
            Timer t = new Timer();
            killOnDelay = new KillOnTimeout(proc);
            t.schedule(killOnDelay, timeout);
        }
        int exitValue = proc.waitFor();
        if (timeout > 0)
            killOnDelay.cancel();
        return exitValue;
    }

    public static final Process executeAsynch(String[] cmdarray, String[] envp, File dir) throws Throwable {
        Process proc = Runtime.getRuntime().exec(cmdarray, envp, dir);
        StreamGobbler err = new StreamGobbler(proc.getErrorStream(), System.err);
        StreamGobbler out = new StreamGobbler(proc.getInputStream(), System.out);
        err.start();
        out.start();
        return proc;
    }  
  
    private static class StreamGobbler extends Thread {
        private final InputStream is;
        private final PrintStream os;
        private StreamGobbler(InputStream is, PrintStream os) {
            this.is = is;
            this.os = os;
            this.setDaemon(true);
        }
        public void run() {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                String l;
                while ((l = r.readLine()) != null)
                    os.println(l);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static class KillOnTimeout extends TimerTask {
        private Process p;
        public KillOnTimeout(Process p) {
            this.p = p;
        }
        public void run() {
            p.destroy();
        }
    }
}

