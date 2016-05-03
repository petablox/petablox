package petablox.reporting;

import petablox.program.Program;
import soot.SootClass;
import soot.RefType;
import soot.RefLikeType;

public class FileNames extends XMLReport {
    public FileNames() {
		super("FileNames");
    }

    public void generate() {
        Program program = Program.g();
        for(RefLikeType r : program.getClasses())
		{	if(r instanceof RefType){
				SootClass c = ((RefType)r).getSootClass();
				newTuple()
				.setAttr("chordsig", c.getName())
				.setAttr("srcFile", this.sourceInfo.filePath(c))
				.setAttr("lineNum", String.valueOf(this.sourceInfo.classLineNum(c)));
			}
		}
    }
}
