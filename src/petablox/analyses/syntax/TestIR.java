package petablox.analyses.syntax;

import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
@Petablox(
    name = "test-ir", 
    consumes = {
        /* Method */
        "Trap", "LocalVar", "VarType",
        /* Statement */
        "AssignInst", "BreakInst",
        "EnterMonitorInst", "ExitMonitorInst", "GotoInst", "IdentityInst",
        "IfInst", "InvokeInst", "LookupSwitchCaseInst", "LookupSwitchDefaultInst",
        "NopInst", "RetInst", "ReturnInst", "ReturnVoidInst", "MethodArg", "BootstrapMethodArg",
        "SpecialInvoke", "InterfaceInvoke", "StaticInvoke", "VirtualInvoke", "DynamicInvoke",
        "ThrowInst", "TableSwitchCaseInst", "TableSwitchDefaultInst",
        /* Expression */
        "AddExpr", "AndExpr", "ArrExpr", "CmpExpr", "CmpgExpr", "CmplExpr",
        "ConstExpr", "DivExpr", "EqExpr", "GeExpr", "GtExpr", "InstanceFieldExpr",
        "InvokeExpr", "LeExpr", "LengthExpr", "LtExpr", "MulExpr", "NeExpr",
        "NegExpr", "NewArrExpr", "NewExpr", "NewMultiArr", "NewMultiArrSize",
        "OrExpr", "RemExpr", "ShlExpr", "ShrExpr", "StaticFieldExpr",
        "SubExpr", "UshrExpr", "VarExpr", "XorExpr",
        "IntExpr", "LongExpr", "FloatExpr", "DoubleExpr", "NullExpr", "StringExpr",
        "CaughtExceptionExpr",
        "ClassConstant", "MethodHandle", "ParamVar"
    }
)
public class TestIR extends JavaAnalysis {
    public void run() {
		    String printDir = System.getProperty("petablox.printrel.dir", Config.outDirName);
		    System.out.println("Printing relations in: " + printDir);
	 
        ProgramRel rel;
        String[] targets = { "Trap", "LocalVar", "VarType", "AssignInst", "BreakInst",
            "EnterMonitorInst", "ExitMonitorInst", "GotoInst", "IdentityInst",
            "IfInst", "InvokeInst", "LookupSwitchCaseInst", "LookupSwitchDefaultInst",
            "NopInst", "RetInst", "ReturnInst", "ReturnVoidInst", "MethodArg", "BootstrapMethodArg",
            "SpecialInvoke", "InterfaceInvoke", "StaticInvoke", "VirtualInvoke", "DynamicInvoke",
            "ThrowInst", "TableSwitchCaseInst", "TableSwitchDefaultInst",
            "AddExpr", "AndExpr", "ArrExpr", "CmpExpr", "CmpgExpr", "CmplExpr",
            "ConstExpr", "DivExpr", "EqExpr", "GeExpr", "GtExpr", "InstanceFieldExpr",
            "InvokeExpr", "LeExpr", "LengthExpr", "LtExpr", "MulExpr", "NeExpr",
            "NegExpr", "NewArrExpr", "NewExpr", "NewMultiArr", "NewMultiArrSize",
            "OrExpr", "RemExpr", "ShlExpr", "ShrExpr", "StaticFieldExpr",
            "SubExpr", "UshrExpr", "VarExpr", "XorExpr",
            "IntExpr", "LongExpr", "FloatExpr", "DoubleExpr", "NullExpr", "StringExpr",
            "CaughtExceptionExpr",
            "ClassConstant", "MethodHandle", "ParamVar"
            };

        for(int i = 0; i < targets.length; i ++){
            rel = (ProgramRel) ClassicProject.g().getTrgt(targets[i]);
            System.out.println(rel.toString());
	          rel.load(); rel.printFI(printDir); rel.close();
        }
    }
}
