package stamp.analyses;

import stamp.util.FactsDumper;
import stamp.util.FileHelper;
import stamp.util.PropertyHelper;

import java.util.Set;

import chord.project.Chord;
import shord.project.analyses.JavaAnalysis;

@Chord(
	name = "facts-dumper-java"
)
public class FactsDumperAnalysis extends JavaAnalysis {
	@Override
	public void run() {
		String templatesDir =
			PropertyHelper.getProperty("stamp.dumper.templates.dir");
		Set<String> templateFiles =
			FileHelper.listRegularFiles(templatesDir, "fdt");
		String templatesList =
			PropertyHelper.getProperty("stamp.dumper.templates.list");
		Set<String> selectedTemplates = FileHelper.splitPath(templatesList);
		String outDir = PropertyHelper.getProperty("stamp.dumper.outdir");

		try {
			for (String fIn : templateFiles) {
				String templateName =
					FileHelper.basename(FileHelper.stripExtension(fIn));
				if (!selectedTemplates.remove(templateName)) {
					continue;
				}
				String fOut = FileHelper.changeDir(templateName, outDir);
				System.out.println("Processing template file " + fIn);
				// TODO: Should have a single dumper make one pass over all
				// templates, then a second full one.
				FactsDumper dumper = new FactsDumper();
				dumper.run(fIn, fOut);
			}
			if (!selectedTemplates.isEmpty()) {
				StringBuilder builder = new StringBuilder();
				for (String missingTemplate : selectedTemplates) {
					builder.append(" ");
					builder.append(missingTemplate);
				}
				throw new Exception("Missing templates:" + builder.toString());
			}
		} catch(Exception exc) {
			throw new RuntimeException(exc);
		}
	}
}
