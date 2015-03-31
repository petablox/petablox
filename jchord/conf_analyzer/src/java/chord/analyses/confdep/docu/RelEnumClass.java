package chord.analyses.confdep.docu;

import joeq.Class.jq_Class;
import joeq.Class.jq_Type;
import chord.program.visitors.IClassVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "enumT",
    sign = "T0:T0"
  )
public class RelEnumClass extends ProgramRel implements IClassVisitor {
  
  jq_Type ENUM;
  public void init() {
    ENUM = jq_Type.parseType("java.lang.Enum");
    ENUM.prepare();
  }
  
  public void visit(jq_Class c) {
    if(c.isSubtypeOf(ENUM)) {
      add(c);
    }
    
  }

}
