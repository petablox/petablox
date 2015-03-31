package chord.analyses.provenance.typestate;

import java.util.HashSet;
import java.util.Set;

import joeq.Class.jq_Class;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.New;
import chord.analyses.alloc.DomH;
import chord.analyses.type.DomT;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;
import chord.util.tuple.object.Pair;

/*
 * @author Ravi Mangal
 */
@Chord(
		name = "initTrackedH",
		consumes = { "checkExcludedH", "sub" },
		sign = "H0"
		)
public class RelInitTrackedH extends ProgramRel {
	private DomH domH;
	ProgramRel relCheckExcludedH;
	Set<jq_Type> trackedTypes;
	boolean isOnlyTrackedTypes;

	@Override
	public void fill() {
		isOnlyTrackedTypes = Utils.buildBoolProperty("chord.provenance.onlyTrackedTypes", true);
		domH = (DomH) doms[0];

		relCheckExcludedH = (ProgramRel) ClassicProject.g().getTrgt("checkExcludedH");
		//ClassicProject.g().runTask("checkExcludedH-dlog");
		relCheckExcludedH.load();
		int numH = domH.getLastA() + 1;
		
		calculateTrackedTypes();
		
		for(int hIdx = 1; hIdx < numH; hIdx++){
			Quad q = (Quad) domH.get(hIdx);
			if(!relCheckExcludedH.contains(q) && !isBannedType(q) && isTrackedType(q)){
				add(hIdx);
			}
		}
		
		relCheckExcludedH.close();
	}
	
	private boolean isBannedType(Quad q){
		String qType = DomH.getType(q);
		boolean isBannedType = false;

		if (qType.startsWith("java.lang.String") || qType.startsWith("java.lang.StringBuilder") 
				|| qType.startsWith("java.lang.StringBuffer") || qType.contains("Exception"))
			isBannedType = true;

		return (isBannedType);
	}
	
	private boolean isTrackedType(Quad q){
		if(!isOnlyTrackedTypes)
			return true;
		
		if ((q.getOperator() instanceof New && trackedTypes.contains(New.getType(q).getType()))
				|| (q.getOperator() instanceof NewArray && trackedTypes.contains(NewArray.getType(q).getType()))
				|| (q.getOperator() instanceof MultiNewArray && trackedTypes.contains(MultiNewArray.getType(q).getType()))) {
			return true;
		}
		
		return false;
	}
	
	private void calculateTrackedTypes(){
		if(!isOnlyTrackedTypes)
			return;
		
		trackedTypes = new HashSet<jq_Type>();
		
		String[] trackedTypesStr = getTrackedTypes();
		
		ProgramRel relSub = (ProgramRel) ClassicProject.g().getTrgt("sub");
		relSub.load();
		DomT domT = (DomT) ClassicProject.g().getTrgt("T");
		
		
		for(String trackedTypeStr : trackedTypesStr){
			jq_Type trackedType = jq_Type.parseType(trackedTypeStr);
			trackedTypes.add(trackedType);
			
			if(domT.contains(trackedType)){
				for (jq_Type type : domT) {
					if (relSub.contains(type, trackedType))
						trackedTypes.add(type);
				}
			}
		}
		relSub.close();
	}
	
	private String[] getTrackedTypes(){
			return new String[]{"java.util.Enumeration", "java.io.InputStream", 
					"java.util.Iterator", "java.security.KeyStore", "java.io.PrintStream", 
					"java.io.PrintWriter", "java.security.Signature", "java.net.Socket",
					"java.util.Stack", "java.net.URLConnection", "java.util.Vector"};		
	}
}
