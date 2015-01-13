package com.logicblox.unit.translator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.logicblox.unit.AbstractTest;
import com.logicblox.unit.AssertLiteral;
import com.logicblox.unit.AssertTest;
import com.logicblox.unit.Assertion;
import com.logicblox.unit.Main;
import com.logicblox.unit.Test;
import com.logicblox.unit.TestSuite;

/**
 * Translates from old logicblox-unit test suite descriptions to new bloxunit suites.
 * 
 * <p>See the description of the main method for details.</p>
 * 
 * @author Thiago T. Bartolomei
 */
public class Translator {

	/**
	 * True if we are translating to a single file.
	 */
	protected final boolean single;
	
	/**
	 * A simple code generator for single or multiple files.
	 */
	protected final CodeGenerator generator;
	
	/**
	 * A new translator.
	 *
	 * @param single
	 */
	public Translator(boolean single) {
		this.single = single;
		if (single) {
			generator = new SingleFileCodeGenerator();
		} else {
			generator = new CodeGenerator();
		}
	}
	
	/**
	 * Translates the suite with this filename to a suite in this output dir, using this prefix for the database
	 * lookup on setUp files.
	 *
	 * @param suiteFileName
	 * @param suite
	 * @param outputDir
	 * @param prefix
	 * @throws IOException
	 */
	public void translate(String suiteFileName, TestSuite suite, File outputDir, String prefix) throws IOException {
		System.out.println(
				"-----------------------------------------------------------------------\n" +
				"Translating testsuite " + suite.getName() + "\n" +
				"  defined in file " + suiteFileName + "\n" +
				"  to directory " + outputDir + "\n" +
			(prefix.isEmpty() ? "" :
				"  using database prefix " + prefix + "\n" ) +
			(single ? 
				"  to a single file.\n" :
				"  to multiple files.\n") +
				"-----------------------------------------------------------------------"
		);

		if (single) {
			translateSingle(suiteFileName, suite, outputDir, prefix);
		} else {
			translateMultiple(suiteFileName, suite, outputDir, prefix);
		}
	}
	
	public void translateSingle(String suiteFileName, TestSuite suite, File outputDir, String prefix) throws IOException {
		final Map<String, File> databaseDirs = new HashMap<String, File>();
		final Map<String, String> databaseCode = new HashMap<String, String>();
		
		final List<AbstractTest> tests = suite.getTests();
		for(int i = 0; i < tests.size(); i++) {
			AbstractTest test = tests.get(i);
			
			File dbDir = verifyDBDir(outputDir, test.getDatabase());
			databaseDirs.put(test.getDatabase(), dbDir);
			
			// Translating to a single file
			if (test instanceof Test)
				appendCode(databaseCode, test.getDatabase(), generator.generateQuery((Test) test));
			else {
				AssertTest t = (AssertTest) test;
				StringBuilder b = new StringBuilder(generator.startAssertionTest(t));
				for (Assertion assertion : t.getAssertions())
					b.append(generator.generateAssertion((AssertLiteral) assertion));
				appendCode(databaseCode, test.getDatabase(), b.toString());
			}
		}
		
		// Generate setUp and tearDown for each database used
		for (Entry<String, File> entry : databaseDirs.entrySet()) {
			generator.generateSetUpTearDown(prefix + entry.getKey(), entry.getValue());
			
			// And dump the test code
			FileWriter writer = new FileWriter(new File(entry.getValue(), "test.lb"));
			try {
				writer.write(databaseCode.get(entry.getKey()));
			} finally {
				writer.close();
			}
		}
	}
	
	public void appendCode(final Map<String, String> databaseCode, String db, String code) {
		if (! databaseCode.containsKey(db)) {
			databaseCode.put(db, code);
		} else {
			databaseCode.put(db, databaseCode.get(db) + code);
		}
	}
	
	
	public void translateMultiple(String suiteFileName, TestSuite suite, File outputDir, String prefix) throws IOException {
		final Map<String, File> databaseDirs = new HashMap<String, File>();
		
		final List<AbstractTest> tests = suite.getTests();
		for(int i = 0; i < tests.size(); i++) {
			AbstractTest test = tests.get(i);
			
			File dbDir = verifyDBDir(outputDir, test.getDatabase());
			databaseDirs.put(test.getDatabase(), dbDir);
			
			// Translating to multiple files
			File targetFile = new File(
					dbDir, 
					computeFilename(i, test.getDescription()));	

			if (test instanceof Test)
				translate((Test) test, targetFile);
			else
				translate((AssertTest) test, targetFile);
		}
		
		// Generate setUp and tearDown for each database used
		for (Entry<String, File> entry : databaseDirs.entrySet())
			generator.generateSetUpTearDown(prefix + entry.getKey(), entry.getValue());	
	}
	
	/**
	 * Generates assertion for this query test in this target file.
	 *
	 * @param test
	 * @param targetFile
	 * @throws IOException 
	 */
	public void translate(Test test, File targetFile) throws IOException {
		FileWriter writer = new FileWriter(targetFile);
		
		try {
			writer.write(generator.generateQuery(test));
		} finally {
			writer.close();
		}
	}
	
	/**
	 * Generates assertions for this assert test in this target file.
	 *
	 * @param test
	 * @param targetFile
	 * @throws IOException
	 */
	public void translate(AssertTest test, File targetFile) throws IOException {
		FileWriter writer = new FileWriter(targetFile);
		
		try {
			writer.write(generator.startAssertionTest(test));
			for (Assertion assertion : test.getAssertions())
				writer.write(generator.generateAssertion((AssertLiteral) assertion));
			
		} finally {
			writer.close();
		}
	}
	
	/**
	 * Checks whether this assert literal checks for the presence of an entity,
	 * i.e., it is not negated.
	 *
	 * @param al
	 * @return
	 */
	public boolean isAssertTrueTest(AssertLiteral al) {
		return ! al.getLiteral().isNegated();
	}
	
	/**
	 * Computes a filename for the target test, based on this test description
	 * and the index of the test.
	 *
	 * @param index
	 * @param description
	 * @return
	 */
	public String computeFilename(int index, String description) {
		if (null == description || description.isEmpty())
			return "Test" + index + ".lb";
		
		String[] words = description.split(" ");
		
		// First compute the filename with the first 2 words
		StringBuilder b = new StringBuilder("Test").append(index).append("_");
		b.append(words[0]);
		
		if (words.length > 1)
			b.append("_").append(words[1]);
		
		// Now cleanup the string
		String s = b.toString();
		s = s.replaceAll("\\[|\\]|\\(|\\)", "");
		s = s.replaceAll("\\.", "_");
		
		return s + ".lb";
	}

	/**
	 * Verifies that the output directory for this database exists.
	 * 
	 * <p>We currently store all tests that target a certain database in a single
	 * sub directory of the output directory.</p> 
	 *
	 * @param outputDir
	 * @param database
	 * @return
	 */
	public File verifyDBDir(File outputDir, String database) {
		return verifyDirectory(new File(outputDir, "suite" + new File(database).getName()));
	}
	
	/**
	 * Verifies that this filename is a directory, creating if needed.
	 * 
	 * @param filename
	 * @return
	 */
	public static File verifyDirectory(String filename) {
		return verifyDirectory(new File(filename));
	}
	
	/**
	 * Verifies that this file is a directory, creating if needed.
	 *
	 * @param f
	 * @return
	 */
	public static File verifyDirectory(File f) {	
		if (f.exists()) {
			if (! f.isDirectory())
				throw new RuntimeException(f.getAbsolutePath() + " is not a directory.");
		} else {
			if (! f.mkdirs())
				throw new RuntimeException("Could not create directory at " + f.getAbsolutePath());
		}
		return f;
	}
	
	/**
	 * Executes the translator from the command line.
	 * 
	 * <p>The translator accepts the following parameters:</p>
	 * 
	 * <ol>
	 *   <li>a mandatory path to an old logicblox-unit test file. The path can be absolute or it
	 *   can be relative to the executing directory.</li>
	 *   <li>a mandatory path to the diretory where the translated tests will be stored. The path
	 *   can also be relative or absolute, and it must point to a directory. If it does not exist
	 *   the directory will be created.</li>
	 *   <li>an optional prefix string to be prepended to the "using database" value when creating
	 *   setUp files. The problem is that setUp must open a workspace, and the provided argument 
	 *   must be a path relative to the directory of the generated suite. Since legacy tests are 
	 *   not compatible, we provide a means to fix the path so that when executing from the suite
	 *   directory we can find the test workspace.
	 * </ol>
	 * 
	 * <p>Note that logicblox-unit files often contained tests using different databases in the 
	 * same file. But because we need to open the database on setUp, we have to create one test
	 * suite for each database used. They will be created in separate sub-directories of output-dir.
	 *
	 * @param params
	 */
	public static void main(String[] params) {
		if (params.length != 2 && params.length != 3) {
			System.err.println("error: please specify a path to a LogicBlox testsuite");
			System.err.println("Usage: Translator <testsuite> <output-dir> [prefix]");
			System.exit(1);
		}
		
		File outputDir = verifyDirectory(params[1]);
		String prefix = params.length == 3 ? params[2] : "";
		
		try {
			new Translator(true).translate(params[0], Main.load(params[0]), outputDir, prefix);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
