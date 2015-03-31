package chord.analyses.confdep.rels;

import java.util.HashMap;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;


/***
 * Method call m0 can remotely trigger m1 of type t
 *
 */
@Chord(
    name = "resolveRPC",
    sign = "M0,M1,T0:M0xM1xT0"
  )

public class ResolveRPCRel extends ProgramRel implements IMethodVisitor{

  HashMap<String,String> ifaceToImplClass = new HashMap<String,String>();
  
  @Override
  public void init() {
//    ifaceToImplClass.put("org.apache.hadoop.hdfs.protocol.ClientProtocol","org.apache.hadoop.hdfs.server.namenode.NameNode");
  }
  
  jq_Class curClass;
  jq_Class matchingConcrete;
  
  @Override
  public void visit(jq_Method m) {
    if(matchingConcrete != null) {
      jq_Method resolved = matchingConcrete.getDeclaredInstanceMethod(m.getNameAndDesc());
      if(resolved != null)
        super.add(m, resolved, matchingConcrete);
      else
        System.out.println("WARN: can't resolve concrete RPC method " + matchingConcrete.getName() + "." +
            m.getNameAndDesc().toString());
    }
    
  }

  @Override
  public void visit(jq_Class c) {
    curClass = c;
    String concreteName = ifaceToImplClass.get(c.getName());
    if(concreteName != null) {
      matchingConcrete = (jq_Class) jq_Type.parseType(concreteName);
      if(matchingConcrete == null) {
        System.out.println("ERR: couldn't instantiate concrete RPC class " + concreteName);
      }
    }else
      matchingConcrete = null;
  }
  

}
