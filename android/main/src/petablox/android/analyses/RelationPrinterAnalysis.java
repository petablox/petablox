package petablox.android.analyses;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import petablox.project.analyses.JavaAnalysis;
import petablox.android.missingmodels.analysis.JCFLSolverRunner.RelationAdder;
import petablox.android.missingmodels.util.ConversionUtils.PetabloxRelationAdder;
import petablox.android.missingmodels.util.FileManager;
import petablox.android.missingmodels.util.FileManager.StampOutputFile;
import petablox.android.missingmodels.viz.flow.StampRelationOutputFile;
import petablox.project.Petablox;

/*
 * An analysis that runs the JCFLSolver to do the taint analysis.
 */

@Petablox(name = "relationprinter")
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
		run(JCFLSolverAnalysis.getFileManager(System.getProperty("stamp.out.dir")), new PetabloxRelationAdder());
	}
}
