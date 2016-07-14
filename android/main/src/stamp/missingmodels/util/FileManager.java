package stamp.missingmodels.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

/*
 * Manages scratch and output directories.
 * Currently primarily for JCFLSolverAnalysis.
 * 
 * @author Osbert Bastani
 */
public class FileManager implements Serializable {
	private static final long serialVersionUID = -3693317914605735230L;
	
	/*
	 * An enum of possible file types, currently
	 * either scratch files or output files.
	 */
	public static enum FileType {
		PERMANENT, OUTPUT, SCRATCH;		
	}

	/*
	 * A basic data structure for files, to be used as input
	 * to the file manager.
	 */
	public static interface StampFile {
		/*
		 * Returns the file name (not including the scratch
		 * or output directory portion of the path).
		 * 
		 * @return: The file name.
		 */
		public abstract String getName();
		
		/*
		 * Returns the file type.
		 * 
		 * @return: The file type.
		 */
		public abstract FileType getType();		
	}
	
	/*
	 * A basic data structure for generating file content,
	 * to be used as input to the file manager.
	 */
	public static interface StampOutputFile extends StampFile {
		/*
		 * Returns the content to be written to the file.
		 * 
		 * @return: The content to be written to the file.
		 */
		public abstract String getContent();
	}
	
	/*
	 * A basic data structure for inputting objects from
	 * a file, to be used as input to the file manager.
	 * 
	 * @param T The object associated to the file.
	 */
	public static interface StampInputFile<T> extends StampFile {
		/*
		 * Returns an object based on the given file.
		 * 
		 * @param br: A buffered reader from which to read input.
		 * @return: The object associated to the inputted file
		 * content.
		 */
		public abstract T getObject(BufferedReader br) throws IOException;
		
	}

	private final File permanentDirectory;
	private final File outputDirectory;
	private final File scratchDirectory;
	
	/*
	 * The generic constructor. If useScratch is false, then it
	 * clears the scratch directory.
	 * 
	 * @param outputDirectory: The directory to put output files.
	 * @param scratchDirectory: The directory to put input files.
	 * @param useScratch: Whether or not to use the existing
	 * scratch directory.
	 */
	public FileManager(File permanentDirectory, File outputDirectory, File scratchDirectory, boolean useScratch) throws IOException {
		// STEP 1: delete the scratch directory if needed, and
		// ensure that the directories exist.
		if(!useScratch) {
			//scratchDirectory.delete();
		}
		
		permanentDirectory.mkdirs();
		outputDirectory.mkdirs();
		scratchDirectory.mkdirs();
		
		// STEP 2: set the fields.
		this.permanentDirectory = permanentDirectory;
		this.outputDirectory = outputDirectory;
		this.scratchDirectory = scratchDirectory;
	}
	
	/*
	 * Returns the directory associated with the file type.
	 * 
	 * @param type: The file type which for we want to get the
	 * directory
	 * @return: The file representing the directory corresponding
	 * to the given file type. 
	 */
	public File getDirectory(FileType type) {
		switch(type) {
		case PERMANENT:
			return this.permanentDirectory;
		case OUTPUT:
			return this.outputDirectory;
		case SCRATCH:
			return this.scratchDirectory;
		default:
			return null;					
		}
	}
	
	/*
	 * Returns the corresponding file.
	 * 
	 * @param file: The file to be read.
	 * @param type: The file type.
	 */
	public File getFile(StampFile stampFile) throws IOException {
		return new File(this.getDirectory(stampFile.getType()), stampFile.getName());
	}
	
	/*
	 * Reads the contents of the stamp file and
	 * returns the associated object.
	 * 
	 * @param file: The content to be written.
	 * @param type: The file type. 
	 * @return: The object associated with the file.
	 */	
	public <T> T read(StampInputFile<T> stampFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(this.getFile(stampFile)));
		return stampFile.getObject(br);
	}
	
	/*
	 * Writes the contents of the stamp file to the
	 * appropriate directory.
	 * 
	 * @param file: The content to be written.
	 * @param type: The file type. 
	 */
	public void write(StampOutputFile stampFile) throws IOException {
		File file = new File(this.getDirectory(stampFile.getType()), stampFile.getName());
		file.getParentFile().mkdirs();
		PrintWriter printWriter = new PrintWriter(file);
		printWriter.println(stampFile.getContent());
		printWriter.close();
	}
}
