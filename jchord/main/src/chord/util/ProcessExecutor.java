package chord.util;

import static chord.util.ExceptionUtil.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Utility to execute a system command specified as a string in a separate process.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class ProcessExecutor {
    
    public static class Result {
        private final int exitCode;
        private final String output;
        private final String error;
        public Result(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }
        
        public int getExitCode() {
            return exitCode;
        }
        
        public String getError() {
            return error;
        }
        
        public String getOutput() {
            return output;
        }
    }
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

    public static final Process executeAsynch(String[] cmdarray, String[] envp, File dir) throws IOException {
        return executeAsynch(cmdarray, envp, dir, System.out, System.err);
    }
    
    /**
     * Executes a process asynchronously.
     * 
     * @param cmdarray the commands to run
     * @param envp     the environment
     * @param dir      the working directory, may be <code>null</code>
     * @param outStream the stream for output, may not be <code>null</code>
     * @param errStream the stream for standard error, may not be <code>null</code>
     * @return the process handle
     * @throws IOException
     * @throws NullPointerException if a required argument is <code>null</code>
     * @see Runtime#exec(String[], String[], File)
     */
    public static Process executeAsynch(String[] cmdarray, String[] envp, File dir, 
            PrintStream outStream, PrintStream errStream) throws IOException {
        if( outStream == null ) throw new NullPointerException("outStream is null");
        if( errStream == null ) throw new NullPointerException("errStream is null");
        
        Process proc = Runtime.getRuntime().exec(cmdarray, envp, dir);
        StreamGobbler err = new StreamGobbler(proc.getErrorStream(), errStream);
        StreamGobbler out = new StreamGobbler(proc.getInputStream(), outStream);
        err.start();
        out.start();
        return proc;
    }
    
    
    /**
     * Executes a command and captures the complete result.
     * 
     * @param cmds the command and any arguments
     * @param envp the environment
     * @param dir  the working directory, may be <code>null</code>
     * @param timeout the time to wait before aborting, or a negative number to wait forever
     * @return the result
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    public static Result executeCaptureOutput(String... cmds) 
            throws IOException, InterruptedException {
        return executeCaptureOutput(cmds, null, null, -1);
    }
    
    /**
     * Executes a command and captures the complete result.
     * 
     * @param cmds the command and any arguments
     * @param envp the environment
     * @param dir  the working directory, may be <code>null</code>
     * @param timeout the time to wait before aborting, or a negative number to wait forever
     * @return the result
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    public static Result executeCaptureOutput(String[] cmds, String[] envp, File dir, int timeout) 
            throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        
        Process proc = executeAsynch(cmds, envp, dir, 
            new PrintStream(out, true, "UTF-8"),
            new PrintStream(err, true, "UTF-8"));
        TimerTask killOnDelay = null;
        if (timeout > 0) {
            Timer t = new Timer();
            killOnDelay = new KillOnTimeout(proc);
            t.schedule(killOnDelay, timeout);
        }
        int exitValue = proc.waitFor();
        if (timeout > 0)
            killOnDelay.cancel();
        return new Result(exitValue, out.toString("UTF-8"), out.toString("UTF-8"));
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

