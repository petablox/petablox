package petablox.android.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Prototype for a simple line-oriented two-pass text file filter. Most of the
 * actual functionality is delegated to callback methods, which should be
 * overriden by concrete implementations.
 *
 * Limitations:
 * <ul>
 * <li>No multi-line support.</li>
 * <li>Assumes the contents of the input file won't change while the filter is
 *     running.</li>
 * </ul>
 */
public abstract class LineFilter {
	/**
	 * Run the filter on {@code inFile}, and print the output to
	 * {@code outFile}.
	 *
	 * Two passes are performed on the input file: one to gather any required
	 * information (where {@link #preProcessLine(String)} is used), and one to
	 * perform the actual filtering (where {@link #processLine(String)} is
	 * used).
	 *
	 * If {@code outFile} is {@code null}, the second filtering pass is not
	 * executed, and no output is produced.
	 */
	public final void run(String inFile, String outFile)
		throws IOException, FilterException {
		BufferedReader in1 = null;
		BufferedReader in2 = null;
		PrintStream out = null;
		String inLine;
		int lineNo = -1;
		init();
		try {
			// Pass #1: preprocessing
			in1 = new BufferedReader(new FileReader(inFile));
			for (inLine = in1.readLine(), lineNo = 1;
				 inLine != null;
				 inLine = in1.readLine(), lineNo++) {
				preProcessLine(inLine);
			}
			if (outFile != null) {
				// Pass #2: actual filterting
				in2 = new BufferedReader(new FileReader(inFile));
				out = new PrintStream(outFile);
				for (inLine = in2.readLine(), lineNo = 1;
					 inLine != null;
					 inLine = in2.readLine(), lineNo++) {
					for (String outLine : processLine(inLine)) {
						out.println(outLine);
					}
				}
			}
		} catch(FilterException exc) {
			// Propagate the error message, with added information about the
			// location where it occured.
			String posInfo = "File " + inFile + ", line " + lineNo;
			throw new FilterException(posInfo + ": " + exc.getMessage());
		} finally {
			cleanup();
			try {
				if (in1 != null) in1.close();
			} catch(IOException exc) {}
			try {
				if (in2 != null) in2.close();
			} catch(IOException exc) {}
			if (out != null) out.close();
		}
	}

	/**
	 * Called right before a filtering run starts. Override this method if you
	 * need to perform any custom per-run initialization. The default
	 * implementation does nothing.
	 */
	public void init() {}

	/**
	 * Called on each line of the input file during the first pass. Override
	 * this method if you need to collect any information from the input file
	 * before you start the actual processing. The default implementation does
	 * nothing.
	 *
	 * @param line the line to process
	 * @throws FilterException when the implementation encounters an error
	 *         that should cause filtering to halt
	 */
	public void preProcessLine(String line) throws FilterException {}

	/**
	 * Called on each line of the input file during the second pass, to perform
	 * the actual filtering. Define a custom filtering strategy by overriding
	 * this method. The default implementation simply returns the input line
	 * unchanged.
	 *
	 * @param line the line to process
	 * @return a collection of lines to output in place of the input line
	 * @throws FilterException when the implementation encounters an error
	 *         that should cause filtering to halt
	 */
	public Iterable<String> processLine(String line) throws FilterException {
		return new Cell<String>(line);
	}

	/**
	 * Called right after filtering ends, even if the filter encounters an
	 * error. Override this method if you need to perform any custom per-run
	 * cleanup. The default implementation does nothing.
	 */
	public void cleanup() {}

	/**
	 * An exception signifying that the filtering of the current line failed.
	 */
	public static class FilterException extends Exception {
		/**
		 * @param msg a String containing more information on the cause of the
		 *        error
		 */
		public FilterException(String msg) {
			super(msg);
		}
	}
}
