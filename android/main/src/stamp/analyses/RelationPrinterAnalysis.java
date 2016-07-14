package stamp.analyses;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import shord.project.analyses.JavaAnalysis;
import stamp.missingmodels.analysis.JCFLSolverRunner.RelationAdder;
import stamp.missingmodels.util.ConversionUtils.ChordRelationAdder;
import stamp.missingmodels.util.FileManager;
import stamp.missingmodels.util.FileManager.StampOutputFile;
import stamp.missingmodels.viz.flow.StampRelationOutputFile;
import chord.project.Chord;

/*
 * An analysis that runs the JCFLSolver to do the taint analysis.
 */

@Chord(name = "relationprinter")
public class RelationPrinterAnalysis extends JavaAnalysis {
	public static void run(FileManager manager, RelationAdder relationAdder) {		
		Set<StampOutputFile> files = new HashSet<StampOutputFile>();
		files.add(new StampRelationOutputFile("flow"));
		files.add(new StampRelationOutputFile("flow2"));
		files.add(new StampRelationOutputFile("labelRef"));
		files.add(new StampRelationOutputFile("labelRef2"));
		files.add(new StampRelationOutputFile("labelPrim"));
		files.add(new StampRelationOutputFile("labelPrim2"));
		try {
			for(StampOutputFile file : files) {
				manager.write(file);
			}
		} catch(IOException e) { e.printStackTrace(); }
	}
	
	@Override public void run() {
		run(JCFLSolverAnalysis.getFileManager(System.getProperty("stamp.out.dir")), new ChordRelationAdder());
	}
}
