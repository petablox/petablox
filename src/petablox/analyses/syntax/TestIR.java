package petablox.analyses.syntax;

import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
@Petablox(
    name = "test-ir", 
    consumes = { "AssignInst", "IfInst", "NopInst", "BreakPointInst", "GotoInst",
        "LookupSwitchCaseInst", "LookupSwitchDefaultInst"
    }
)
public class TestIR extends JavaAnalysis {
    public void run() {
		    String printDir = System.getProperty("petablox.printrel.dir", Config.outDirName);
		    System.out.println("Printing relations in: " + printDir);
	  
        ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("AssignInst");
	      rel.load(); rel.printFI(printDir); rel.close();

        rel = (ProgramRel) ClassicProject.g().getTrgt("IfInst");
	      rel.load(); rel.printFI(printDir); rel.close();

        rel = (ProgramRel) ClassicProject.g().getTrgt("NopInst");
	      rel.load(); rel.printFI(printDir); rel.close();
/* TODO
        rel = (ProgramRel) ClassicProject.g().getTrgt("BreakPointInst");
	      rel.load(); rel.printFI(printDir); rel.close();
*/
        rel = (ProgramRel) ClassicProject.g().getTrgt("GotoInst");
	      rel.load(); rel.printFI(printDir); rel.close();

        rel = (ProgramRel) ClassicProject.g().getTrgt("LookupSwitchCaseInst");
	      rel.load(); rel.printFI(printDir); rel.close();

        rel = (ProgramRel) ClassicProject.g().getTrgt("LookupSwitchDefaultInst");
	      rel.load(); rel.printFI(printDir); rel.close();
    }
}
