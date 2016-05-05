package stamp.submitting;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Arrays;

/**
 * Convenience functions for invoking external OS programs.
 */
public class ShellProcessRunner 
{
	private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.

	public static void run(String[] cmdLine, File dir, boolean printStats, File outFile)
		throws InvocationFailureException {
		if (printStats) {
			String[] cmdWrapper = new String[]{"/usr/bin/time", "-v"};

			String[] result = Arrays.copyOf(cmdWrapper, cmdWrapper.length + cmdLine.length);
			System.arraycopy(cmdLine, 0, result, cmdWrapper.length, cmdLine.length);

			cmdLine = result;
		}
		Process proc = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(cmdLine);
			builder.directory(dir);
			builder.redirectErrorStream(true);
			proc = builder.start();
			InputStream input = new BufferedInputStream(proc.getInputStream(), DEFAULT_BUFFER_SIZE);
			OutputStream output = new BufferedOutputStream(new FileOutputStream(outFile), DEFAULT_BUFFER_SIZE);
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			for (int length = 0; ((length = input.read(buffer)) > 0);) {
				output.write(buffer, 0, length);
			}
			input.close();
			output.close();
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
