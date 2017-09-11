package petablox.analyses.syntax;

import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
@Petablox(
    name = "test-ir", 
    consumes = {
        "Trap",
        "AssignInst", "BreakPointInst",
        "EnterMonitorInst", "ExitMonitorInst", "GotoInst", "IfInst",
        "LookupSwitchCaseInst", "LookupSwitchDefaultInst",
        "NopInst", "RetInst", "ReturnInst", "ReturnVoidInst", "ThrowInst",
        "TableSwitchCaseInst", "TableSwitchDefaultInst"
    }
)
public class TestIR extends JavaAnalysis {
    public void run() {
		    String printDir = System.getProperty("petablox.printrel.dir", Config.outDirName);
		    System.out.println("Printing relations in: " + printDir);
	 
        ProgramRel rel;
        String[] targets = { "Trap", "AssignInst", //"BreakPointInst",
            "EnterMonitorInst", "ExitMonitorInst", "GotoInst", "IfInst",
            "LookupSwitchCaseInst", "LookupSwitchDefaultInst",
            "NopInst", "RetInst", "ReturnInst", "ReturnVoidInst", "ThrowInst",
            "TableSwitchCaseInst", "TableSwitchDefaultInst",
            };

        for(int i = 0; i < targets.length; i ++){
            rel = (ProgramRel) ClassicProject.g().getTrgt(targets[i]);
	          rel.load(); rel.printFI(printDir); rel.close();
        }
    }
}
