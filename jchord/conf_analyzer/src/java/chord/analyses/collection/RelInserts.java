package chord.analyses.collection;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;

/**
 * (i,u,v) if instruction i inserts v into u
 *
 */
@Chord(
    name = "IInsert",
    sign = "I0,V0,V1:I0_V0_V1"
  )
public class RelInserts extends ProgramRel implements IInvokeInstVisitor {
  
  DomI domI;
  DomV domV;
  jq_Method method;
  jq_Type OBJ_T;
  
  static boolean MAP_PUT = false;
  static {
    MAP_PUT = Utils.buildBoolProperty("putIsInsert", false);
  }
  public void init() {
    domI = (DomI) doms[0];
    domV = (DomV) doms[1];
//    MAP_PUT = Config.buildBoolProperty("modelPuts", false);
    OBJ_T = jq_Type.parseType("java.lang.Object");
    RelINewColl.tInit();
  }

  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    method = m;
  }
  
  public static boolean isInsert(Quad q) {
    jq_Method meth = Invoke.getMethod(q).getMethod();
    jq_Class cl = meth.getDeclaringClass();
    String classname = cl.getName();
    String mname = meth.getName().toString();
  	if(!meth.isStatic() && RelINewColl.isCollectionType(cl)) {
      //I'm nervous about "put" because of worries about tainting conf objects
      return mname.equals("offer") || mname.equals("add") || mname.toLowerCase().contains("set")
      		|| (MAP_PUT && classname.contains("Map") && mname.equals("put"));
  	}
  	return false;
  }

  @Override
  public void visitInvokeInst(Quad q) {
    ParamListOperand argList = Invoke.getParamList(q);
    int args = argList.length();
    if (args > 1) {

      Register thisObj = Invoke.getParam(q, 0).getRegister();
      int thisObjID = domV.indexOf(thisObj);

      if(isInsert(q)) {
        for(int i =1; i< args; ++i) {
          RegisterOperand op = argList.get(i);
          if(op.getType().isReferenceType()) {
            int iID = domI.indexOf(q);
            int idx1 = domV.indexOf(op.getRegister());
            super.add(iID,thisObjID, idx1);
          }
        }
      }
    }
  }
  

}
