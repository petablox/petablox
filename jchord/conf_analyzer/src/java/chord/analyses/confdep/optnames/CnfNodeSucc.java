package chord.analyses.confdep.optnames;

import java.util.HashSet;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.var.DomV;
import chord.analyses.alloc.DomH;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * (h,b, v) if h is a site where v is appended to b
 * site i2.
 * 
 */
@Chord(
    name = "CnfNodeSucc",
    sign = "H0,V0,V1:H0_V0_V1"
  )
public class CnfNodeSucc extends ProgramRel implements IInvokeInstVisitor{
  DomH domH;
  DomV domV;
  
  public void init() {
    domH = (DomH) doms[0];
    domV = (DomV) doms[1];
    


    
  }
  
  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
  }
  
  static HashSet<String> isSuccTable;

  static {
    isSuccTable = new HashSet<String>();
    
    isSuccTable.add("org.apache.cassandra.utils.XMLUtils getAttributeValue");
//    isSuccTable.add("org.apache.cassandra.utils.XMLUtils item");
    isSuccTable.add("org.w3c.dom.NodeList item");
    isSuccTable.add("org.apache.cassandra.utils.XMLUtils getRequestedNodeList");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getChildren");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getChild");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getValue");
    
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getValueAsBoolean");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getValueAsFloat");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getValueAsDouble");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getValueAsLong");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getValueAsInteger");


    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getAttribute");    
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getAttributeAsBoolean");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getAttributeAsFloat");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getAttributeAsInteger");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getAttributeAsDouble");
    isSuccTable.add("org.apache.avalon.framework.configuration.Configuration getAttributeAsLong");


  }


  @Override
  public void visitInvokeInst(Quad q) {
    jq_Method meth = Invoke.getMethod(q).getMethod();
    String classname = meth.getDeclaringClass().getName();
    String methname = meth.getName().toString();
    if(methname.equals("item"))
      System.out.println("call to " + classname + " item");
    if( isSuccTable.contains(classname + " " + methname) ) {
      int hIdx = domH.indexOf(q);
      
      if(Invoke.getParamList(q).length() > 1) {
        Register bVar = Invoke.getParam(q, 0).getRegister();
        Register argVar = Invoke.getParam(q, 1).getRegister();
        
        int bIdx = domV.indexOf(bVar);
        int aIdx = domV.indexOf(argVar);
        if(aIdx == -1)
          aIdx = 0;
        if(hIdx > -1 && bIdx > -1 && aIdx > -1)
          add(hIdx,bIdx, aIdx);
      }
    }
  }

  public static boolean isRelevant(String cname, String mname) {
    return isSuccTable.contains(cname + " " + mname);
   // return false;
  }


}
