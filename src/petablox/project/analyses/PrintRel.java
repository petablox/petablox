package petablox.project.analyses;

import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;

/*
 * petablox.printrel.dir      directory where all the .txt files containing the rels will be dumped.
 * petablox.printrel.printID  [default: false] will print the index in the dom for each element if set to true.
 *                         property used in the printFI method of individual doms.
 */

@Petablox(
	    name = "printrel-java"
	)
public class PrintRel extends JavaAnalysis {

	public void run() {
		String printDir = null;
		ProgramRel rel;
		printDir = System.getProperty("petablox.printrel.dir", Config.outDirName);
		System.out.println("Printing relations in: " + printDir);
		
	    rel = (ProgramRel) ClassicProject.g().getTrgt("deadlock");
	    rel.load(); rel.printFI(printDir); rel.close();
	}
}
