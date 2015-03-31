package chord.analyses.logging;

import java.io.PrintWriter;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.bddbddb.Rel.*;
import chord.util.tuple.integer.*;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.analyses.field.DomF;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;


@Chord(
  name = "LogIdentifiers"
)
public class LogIdentifiers extends JavaAnalysis{
  
  DomI domI;
  DomV domV;
  DomF domF;

  public void run() {
    
    ClassicProject project = ClassicProject.g();
    
    project.runTask("LogStmts");
    project.runTask("cipa-0cfa-arr-dlog");
    project.runTask("ftrack-dlog");

    
    domI = (DomI) project.getTrgt("I");
    domV = (DomV) project.getTrgt("V");
    domF = (DomF) project.getTrgt("F");
    
    PrintWriter writer =
      OutDirUtils.newPrintWriter("logged_fields.txt");

    ProgramRel relConfUses =
      (ProgramRel) project.getTrgt("logF");//outputs I,F
    relConfUses.load();
    IntPairIterable tuples = relConfUses.getAry2IntTuples();
    for (IntPair p : tuples) {
      Quad q1 = (Quad) domI.get(p.idx0);
      jq_Field f = domF.get(p.idx1);

      String fname = "";
      if(f != null)
        fname = f.getDeclaringClass()+"." +f.getName();
      
      jq_Method m = q1.getMethod();
      int lineno = q1.getLineNumber();
      
      String filename = m.getDeclaringClass().getSourceFileName();
      String srcPoint = filename + " " + lineno;
      
      writer.println("line " + srcPoint + " uses field " + fname); 
    }
    
    writer.close();
 
  }

}
