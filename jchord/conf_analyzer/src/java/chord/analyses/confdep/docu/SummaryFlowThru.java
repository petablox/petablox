package chord.analyses.confdep.docu;

import java.util.Map;
import joeq.Class.jq_Method;
import chord.analyses.confdep.AbstractSummaryAnalysis;
import chord.analyses.confdep.ConfDefines;
import chord.project.Chord;

@Chord(name = "summarizeFlow",
produces={"summarizedFlowThru"},
     signs = {"M0,Z0:M0xZ0"},
     namesOfSigns = {"summarizedFlowThru"}
)
public class SummaryFlowThru extends AbstractSummaryAnalysis {
  @Override
  protected void fillInit(Map<jq_Method, Summary> summaries) {
    

    for(jq_Method meth: domM) {
      String classname = meth.getDeclaringClass().getName();
      String methname = meth.getName().toString();
      int args = meth.getParamTypes().length;
      
//      System.out.println("passThru: classname = " + classname + " methname = " + methname);
      
      if( (ConfDefines.wrapperClasses.contains(classname) && methname.startsWith("valueOf")) ||
          ("java.lang.String".equals(classname) &&  (args == 1 || methname.endsWith("erCase") )) ) {
        Summary summary = new Summary();
        summary.setArg(0);
        summaries.put(meth, summary);
      }
      
    }
  }
  
  

  @Override
  protected String outputName() {
    return "summarizedFlowThru";
  }


}
