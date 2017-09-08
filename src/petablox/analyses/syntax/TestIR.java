package petablox.analyses.syntax;

import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
@Petablox(
    name = "test-ir", 
    consumes = { "AssignInst", "IfInst", "NopInst", "BreakPointInst", "GotoInst",
        "LookupSwitchCaseInst", "LookupSwitchDefaultInst",
        "TableSwitchCaseInst", "TableSwitchDefaultInst",
        "ThrowInst", "EnterMonitorInst", "ExitMonitorInst"
    }
)
public class TestIR extends JavaAnalysis {
    public void run() {
		    String printDir = System.getProperty("petablox.printrel.dir", Config.outDirName);
		    System.out.println("Printing relations in: " + printDir);
	 
        ProgramRel rel;
        String[] targets = { "AssignInst", "IfInst", "NopInst", "GotoInst",
            // "BreakPointInst",
            "LookupSwitchCaseInst", "LookupSwitchDefaultInst", 
            "TableSwitchCaseInst", "TableSwitchDefaultInst",
            "ThrowInst", "EnterMonitorInst", "ExitMonitorInst" };

        for(int i = 0; i < targets.length; i ++){
            rel = (ProgramRel) ClassicProject.g().getTrgt(targets[i]);
	          rel.load(); rel.printFI(printDir); rel.close();
        }
    }
}
