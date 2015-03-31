package chord.analyses.confdep.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Type;
import chord.program.visitors.IFieldVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation holds tuples (t, f, t2) where f is an instance field, belonging to class
 * t, of type t2. 
 * 
 */
@Chord(
    name = "TFT",
    sign = "T0,F0,T1:F0_T0_T1"
  )
public class RelTFT extends ProgramRel
    implements IFieldVisitor {
  private jq_Class ctnrClass;
  public void visit(jq_Class c) {
    ctnrClass = c;
  }
  public void visit(jq_Field f) {
    jq_Type fType = f.getType();
    if(!f.isStatic())
      add(ctnrClass, f,fType);
  }

}
