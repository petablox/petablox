package chord.analyses.confdep.docu;

import chord.analyses.alloc.DomH;
import chord.analyses.confdep.ConfDefines;
import chord.analyses.confdep.optnames.DomOpts;
import chord.analyses.primtrack.DomU;
import chord.analyses.var.DomV;
import chord.bddbddb.Rel.RelView;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;


import java.io.PrintWriter;
import java.util.*;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;

@Chord(
		name="DefaultOptValues")
//		consumes={"UConstFlow","VConstFlow"})//can't do this because 
		//we have two string and points-to analyses. Need to explicitly pick.
		public class ShowDefaultValues extends JavaAnalysis {

	@Override
	public void run() {
		ClassicProject project = ClassicProject.g();

		project.runTask("cipa-0cfa-arr-dlog");
		project.runTask("findconf-dlog");

		project.runTask("strcomponents-dlog");

		project.runTask("CnfNodeSucc");

		project.runTask("Opt");
		project.runTask("defaultConfOptions-dlog");

		DomV domV = (DomV) project.getTrgt("V");
		DomU domU = (DomU) project.getTrgt("U");
		
		
		Map<String, String> optDefaults = getDefaults(domU, domV);
		
    PrintWriter writer =
      OutDirUtils.newPrintWriter("default_conf_vals.txt");

		for(Map.Entry<String, String> e: optDefaults.entrySet()) {
			String oName = e.getKey();
			String oV = e.getValue();
			writer.println(oName + "\t" + oV);
		}
		writer.close();

	}
	
	public static Map<String, String> getDefaults(DomU domU, DomV domV) {
		Map<String, String> defaults = new LinkedHashMap<String, String>();
		ProgramRel UConstFlow =  (ProgramRel) ClassicProject.g().getTrgt("UConstFlow");//v, strconst
		UConstFlow.load();  
		ProgramRel VConstFlow =  (ProgramRel) ClassicProject.g().getTrgt("VConstFlow");//u, strconst
		VConstFlow.load();
		for(Pair<Quad,String> e: DomOpts.optSites()) {
			Quad q = e.val0;
			String optName = ConfDefines.optionPrefix(q) + e.val1;
			int optPos = ConfDefines.confOptionPos(q);
			if(optPos < 0) {
				System.out.println("option in DomOpts with no arg index; option " + optName +
						" read at " + q.toJavaLocStr());
				continue;
			}
			if(Invoke.getParamList(q).length() > optPos +1) {
				RegisterOperand defaultROp = Invoke.getParam(q, optPos + 1);
				Register r = defaultROp.getRegister();
				RelView defaultView;
				int idx;
				if(r.getType().isPrimitiveType()) {
					defaultView = UConstFlow.getView();
					idx = domU.indexOf(r);
				} else {
					defaultView = VConstFlow.getView();
					idx = domV.indexOf(r);
				}
				assert idx > -1;

  			String dflt = getDefault(idx, defaultView);
  			if(dflt != null)
  				defaults.put(optName, dflt);
			}
			
		}
		UConstFlow.close();
		VConstFlow.close();
		return defaults;
	}

	private static String getDefault(int rIdx, RelView constFlow) {
		constFlow.selectAndDelete(0, rIdx);
		String v = null;
		for(String s : constFlow.<String>getAry1ValTuples()) {
			if(v == null)
				v = s;
			else
				v = v + " or " + s;
		}
		return v;
	}

}
