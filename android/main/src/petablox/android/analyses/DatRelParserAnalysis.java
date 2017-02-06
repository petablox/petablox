package petablox.android.analyses;

import petablox.android.util.FileHelper;
import petablox.android.util.Pair;
import petablox.android.util.PropertyHelper;
import petablox.android.util.RelParser;

import java.io.File;
import java.util.Set;

import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;

@Petablox(
	name = "dat-rel-parser-java"
)
public class DatRelParserAnalysis extends JavaAnalysis {
	@Override
	public void run() {
		String inputsDir = PropertyHelper.getProperty("stamp.datparser.indir");
		Set<String> inputFiles = FileHelper.listRegularFiles(inputsDir, "dat");
		String templatesDir =
			PropertyHelper.getProperty("stamp.datparser.templatesdir");
		Set<String> templateFiles =
			FileHelper.listRegularFiles(templatesDir, "dpt");
		Set<Pair<String,String>> inputTemplatePairs =
			FileHelper.matchBasenames(inputFiles, templateFiles);

		try {
			for (Pair<String,String> inputAndTemplate : inputTemplatePairs) {
				String inputFile = inputAndTemplate.getX();
				String templateFile = inputAndTemplate.getY();
				RelParser parser = new RelParser(templateFile);
				parser.parse(inputFile);
				System.out.println(inputFile + ": " + parser.getMatchedLines()
								   + "/" + parser.getProcessedLines()
								   + " lines matched");
			}
		} catch(Exception exc) {
			throw new RuntimeException(exc);
		}
	}
}
