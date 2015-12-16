package chord.project.analyses;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

/*
 * chord.printrel.dir      directory where all the .txt files containing the rels will be dumped.
 * chord.printrel.printID  [default: false] will print the index in the dom for each element if set to true.
 *                         property used in the printFI method of individual doms.
 */

@Chord(
	    name = "printrel-java"
	)
public class PrintRel extends JavaAnalysis {

	public void run() {
		String printDir = null;
		ProgramRel rel;
		printDir = System.getProperty("chord.printrel.dir", Config.outDirName);
		System.out.println("Printing relations in: " + printDir);
		
	    rel = (ProgramRel) ClassicProject.g().getTrgt("reachableM");
	    rel.load(); rel.printFI(printDir); rel.close();
	    
	    rel = (ProgramRel) ClassicProject.g().getTrgt("reachableT");
	    rel.load(); rel.printFI(printDir); rel.close();
	    
	    rel = (ProgramRel) ClassicProject.g().getTrgt("HT");
	    rel.load(); rel.printFI(printDir); rel.close();
	    
	    rel = (ProgramRel) ClassicProject.g().getTrgt("cha");
		rel.load(); rel.printFI(printDir); rel.close();
		
	    rel = (ProgramRel) ClassicProject.g().getTrgt("VT");
		rel.load(); rel.printFI(printDir); rel.close();
		
	    rel = (ProgramRel) ClassicProject.g().getTrgt("sub");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("IinvkArg0");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("IinvkArg");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("IinvkRet");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("statIM");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("specIM");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("virtIM");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("classT");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("staticTM");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("staticTF");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("clinitTM");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("MmethArg");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("MmethRet");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("MI");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("MobjValAsgnInst");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("MobjVarAsgnInst");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("MgetInstFldInst");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("MputInstFldInst");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("MgetStatFldInst");
		rel.load(); rel.printFI(printDir); rel.close();
		
		rel = (ProgramRel) ClassicProject.g().getTrgt("MputStatFldInst");
		rel.load(); rel.printFI(printDir); rel.close();
	}
}
