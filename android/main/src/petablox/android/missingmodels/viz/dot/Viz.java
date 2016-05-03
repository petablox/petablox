package petablox.android.missingmodels.viz.dot;

import petablox.android.missingmodels.util.FileManager.FileType;
import petablox.android.missingmodels.util.FileManager.StampOutputFile;

public abstract class Viz<X> {
	private int vizCount = 0;
	private final String filename;

	public Viz(String filename) {
		this.filename = filename;
	}

	//public abstract JSONObject vizJSON(X x);
	public abstract DotObject vizDot(X x);

	public StampOutputFile viz(final X x) {
		return new StampOutputFile() {
			@Override
			public String getName() {
				return filename + (vizCount++) + ".dot";
			}

			@Override
			public FileType getType() {
				return FileType.OUTPUT;
			}

			@Override
			public String getContent() {
				return vizDot(x).toDotString();
			}
		};

		/*
		String fullFilename = this.filename + (this.vizCount++);
		File file = File.createTempFile(fullFilename, ".dot");
		file.deleteOnExit();

		String command = "dot -Tpdf -o" + fullFilename + ".pdf " + (new File(fullFilename + ".dot")).getCanonicalPath();
		System.out.println("Need to execute command \"" + command + "\".");
		Process process = Runtime.getRuntime().exec(command);
		 */
	}
}
