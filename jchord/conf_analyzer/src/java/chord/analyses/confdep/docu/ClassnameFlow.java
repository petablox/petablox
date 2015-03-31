package chord.analyses.confdep.docu;

import java.util.Map;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Type;
import chord.analyses.confdep.AbstractSummaryAnalysis;
import chord.project.Chord;


@Chord(name = "classnameFlowAnalysis",
//    produces={"classnameFlowEdge","restIM"},
//    signs = {"M0,Z0:M0xZ0", "I0,M0:I0xM0"},
//    namesOfSigns = {"classnameFlowEdge", "restIM"}
  produces={"classnameFlowEdge"},
       signs = {"M0,Z0:M0xZ0"},
       namesOfSigns = {"classnameFlowEdge"}
)
public class ClassnameFlow extends AbstractSummaryAnalysis {

  
  String[] flowThru = {
      "java.lang.Class forName (Ljava/lang/String;)Ljava/lang/Class; 0",
      "java.lang.Class forName (Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class; 0",
      "java.lang.Class newInstance ()Ljava/lang/Object; 0",
      "java.lang.reflect.Constructor newInstance ([Ljava/lang/Object;)Ljava/lang/Object; 0",
      "java.lang.reflect.Constructor newInstance ()Ljava/lang/Object; 0",
      "java.lang.Class getConstructor ([Ljava/lang/Class;)Ljava/lang/reflect/Constructor; 0",
      "java.lang.Class getDeclaredConstructor ([Ljava/lang/Class;)Ljava/lang/reflect/Constructor; 0",
      "java.lang.ClassLoader loadClass (Ljava/lang/String;)Ljava/lang/Class; 1",
      "org.apache.hadoop.conf.Configuration getClassByName (Ljava/lang/String;)Ljava/lang/Class; 1"
      //need more to detail with Constructor, etc
  };
    
  @Override
  protected void fillInit(Map<jq_Method, Summary> summaries) {
    
    for(String s: flowThru) {
      String[] parts = s.split(" ");
      String clname = parts[0];
      
      try {  //check to make sure class is available
        Class.forName(clname);
      } catch (ClassNotFoundException e) {
        System.out.println("no summary for " + s + ", " + e.getMessage());
        continue;
      }
      
      int argID = Integer.parseInt(parts[3]);
      jq_Class ty = (jq_Class)  jq_Type.parseType(clname);
      ty.prepare();
      jq_Method m = (jq_Method) ty.getDeclaredMember(new jq_NameAndDesc(parts[1], parts[2]));
      
      if(m == null || !domM.contains(m))
        continue;
      
      Summary summary = summaries.get(m);
      if(summary == null) {
        summary = new Summary();
        summaries.put(m, summary);
      }
      summary.setArg(argID);
    }
    System.out.println(summaries.size() + " initial summaries, before solve");
  }

  @Override
  protected String outputName() {
    return "classnameFlowEdge";
  }
  
  @Override
  protected Summary analyze(jq_Method meth) {
    System.out.println("ClassnameFlow analyze: " + meth.getName());

    if(meth.getDeclaringClass().getName().equals("org.apache.hadoop.util.ReflectionUtils") &&
        meth.getName().toString().equals("newInstance")) {
      System.out.print("extra-summary for reflutils");
      Summary s = new Summary();
      s.setArg(0);
      return s;
    }
    return null;
  }
   

}
