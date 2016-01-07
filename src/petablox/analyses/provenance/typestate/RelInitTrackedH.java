package petablox.analyses.provenance.typestate;

import java.util.HashSet;
import java.util.Set;

import petablox.analyses.alloc.DomH;
import petablox.analyses.type.DomT;
import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.Utils;
import petablox.util.soot.SootUtilities;
import soot.Scene;
import soot.Type;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;

/*
 * @author Ravi Mangal
 */
@Petablox(
		name = "initTrackedH",
		consumes = { "checkExcludedH", "sub" },
		sign = "H0"
		)
public class RelInitTrackedH extends ProgramRel {
	private DomH domH;
	ProgramRel relCheckExcludedH;
	Set<Type> trackedTypes;
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
			Unit q = (Unit) domH.get(hIdx);
			if(!relCheckExcludedH.contains(q) && !isBannedType(q) && isTrackedType(q)){
				add(hIdx);
			}
		}
		
		relCheckExcludedH.close();
	}
	
	private boolean isBannedType(Unit q){
		String qType = DomH.getType(q).toString();
		boolean isBannedType = false;

		if (qType.startsWith("java.lang.String") || qType.startsWith("java.lang.StringBuilder") 
				|| qType.startsWith("java.lang.StringBuffer") || qType.contains("Exception"))
			isBannedType = true;

		return (isBannedType);
	}
	
	private boolean isTrackedType(Unit q){
		if(!isOnlyTrackedTypes)
			return true;
		
		if(q instanceof JAssignStmt){
			JAssignStmt q1 = (JAssignStmt)q;
			if ((SootUtilities.isNewStmt(q1) || SootUtilities.isNewArrayStmt(q1) || SootUtilities.isNewMultiArrayStmt(q1))){
				if(trackedTypes.contains(q1.rightBox.getValue().getType()))
					return true;
			}
		}
		
		return false;
	}
	
	private void calculateTrackedTypes(){
		if(!isOnlyTrackedTypes)
			return;
		
		trackedTypes = new HashSet<Type>();
		
		String[] trackedTypesStr = getTrackedTypes();
		
		ProgramRel relSub = (ProgramRel) ClassicProject.g().getTrgt("sub");
		relSub.load();
		DomT domT = (DomT) ClassicProject.g().getTrgt("T");
		
		
		for(String trackedTypeStr : trackedTypesStr){
			Type trackedType = Scene.v().getSootClass(trackedTypeStr).getType();
			trackedTypes.add(trackedType);
			
			if(domT.contains(trackedType)){
				for (Type type : domT) {
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
