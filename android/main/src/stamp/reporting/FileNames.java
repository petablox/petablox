package stamp.reporting;

import shord.program.Program;
import soot.SootClass;

public class FileNames extends XMLReport {
    public FileNames() {
		super("FileNames");
    }

    public void generate() {
        Program program = Program.g();
        for(SootClass c : program.getClasses())
		{
			newTuple()
				.setAttr("chordsig", c.getName())
				.setAttr("srcFile", this.sourceInfo.filePath(c))
				.setAttr("lineNum", String.valueOf(this.sourceInfo.classLineNum(c)));
		}
    }
}
