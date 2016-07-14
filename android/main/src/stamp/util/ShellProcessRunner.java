package stamp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;

/**
 * Convenience functions for invoking external OS programs.
 */
public class ShellProcessRunner {
	public static void run(String[] cmdLine, String dir, boolean printStats)
		throws InvocationFailureException {
		if (printStats) {
			String[] cmdWrapper = new String[]{"/usr/bin/time", "-v"};
			cmdLine = ArrayHelper.concat(cmdWrapper, cmdLine);
		}
		Process proc = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(cmdLine);
			builder.directory(new File(dir));
			builder.redirectErrorStream(true);
			proc = builder.start();
			InputStream procOut = proc.getInputStream();
			BufferedReader procOutBuf =
				new BufferedReader(new InputStreamReader(procOut));
			String line;
			while ((line = procOutBuf.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException exc) {
			if (proc != null) {
				proc.destroy();
			}
			throw new InvocationFailureException(exc);
		}
		int exitStatus;
		try {
			exitStatus = proc.waitFor();
		} catch (InterruptedException exc) {
			proc.destroy();
			throw new InvocationFailureException(exc);
		}
		if (exitStatus != 0) {
			throw new InvocationFailureException(exitStatus);
		}
	}

	public static class InvocationFailureException extends RuntimeException {
		public InvocationFailureException(int exitStatus) {
			super("Process exited with status = " + exitStatus);
		}

		public InvocationFailureException(Exception exc) {
			super(exc);
		}
	}
}
