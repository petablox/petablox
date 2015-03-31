package chord.analyses.confdep;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import chord.analyses.method.DomM;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 *  Functions that should be treated as library functions.
 *  Intended for test/debug usage.
 */
	@Chord(
	    name = "extraSummary",
	    sign = "M0:M0"
	  )
	public class ExtraSummary extends ProgramRel implements IMethodVisitor {
	  DomM domM;
  	jq_Class cl;

	  public void init() {
	    domM = (DomM) doms[0];
	  }

	  public void visit(jq_Class c) { cl = c; }
	  public void visit(jq_Method m) {
	  	String clname = cl.getName();
	  	String mname = m.getName().toString();
	  	if(false //clname.startsWith("org.apache.hadoop.net.SocketIOWithTimeout") 
	  		//	  			 clname.startsWith("org.apache.tools.ant.BuildEvent")
	  //			|| (clname.startsWith("org.apache.hadoop.util") && mname.startsWith("compare"))
	  //			|| (clname.startsWith("org.apache.hadoop.net.NetUtils") && mname.startsWith("getStaticResolution"))
	  ///			||	clname.startsWith("org.apache.hadoop.fs.FileStatus")
	  			)
		      add(m);
	  }
}
