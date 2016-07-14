package stamp.srcmap;

import soot.SootClass;
import soot.SootMethod;
import soot.AbstractJasminClass;
import soot.Unit;
import soot.jimple.Stmt;

import stamp.srcmap.sourceinfo.SourceInfo;

import java.util.*;

public class SyntheticMethodMap {
	public static void computeSyntheticToSrcMethodMap(SourceInfo sourceInfo, SootClass klass, Map<SootMethod,SootMethod> syntheticToSrcMethod) {
		String srcFileName = sourceInfo.filePath(klass);
		if(srcFileName == null)
			return;

		Map<String,SootMethod> subsigToMethod = new HashMap();
		for(SootMethod meth : klass.getMethods()){
			String subsig = meth.getName()+":"+AbstractJasminClass.jasminDescriptorOf(meth.makeRef());
			subsigToMethod.put(subsig, meth);
		}

		Set<SootMethod> synthMeths = new HashSet();
		for(Map.Entry<String,List<String>> aliasSigEntry : sourceInfo.allAliasSigs(klass).entrySet()){
			String chordSig = aliasSigEntry.getKey();
			List<String> aliasDescs = aliasSigEntry.getValue();
			if(aliasDescs.isEmpty())
				continue;
			String subsig = chordSig.substring(0, chordSig.indexOf('@'));
			SootMethod meth = subsigToMethod.get(subsig);
			//System.out.println("meth with alias: " + meth);
			String mname = chordSig.substring(0, chordSig.indexOf(':'));
			List<SootMethod> aliases = new ArrayList();
			for(String aliasDesc : aliasDescs){
				SootMethod synthMeth = subsigToMethod.get(mname+":"+aliasDesc);
				//System.out.println("synth meth: " + synthMeth);
				if(synthMeth == null){
					//chord seems to throw away synthetic methods that the compiler
					//to deal with covariant return type
					continue;
				}
				
				synthMeths.add(synthMeth);
				SootMethod prevBinding = syntheticToSrcMethod.put(synthMeth, meth);
				assert prevBinding == null : synthMeth + " " + prevBinding + " " + aliasDesc;
			}
		}

		for(SootMethod synthMeth : klass.getMethods()) {
			if(!synthMeth.isStatic())
				continue;
			if(sourceInfo.methodLineNum(synthMeth) > 0)
				continue;
			if(synthMeths.contains(synthMeth))
				continue;
			SootMethod srcMeth = performMapping(synthMeth);
			if(srcMeth == null)
				continue;
			//System.out.println("synthmap: "+synthMeth+" "+srcMeth);
			SootMethod prevBinding = syntheticToSrcMethod.put(synthMeth, srcMeth);
			assert prevBinding == null : synthMeth + " " + prevBinding + " " + srcMeth;
		}
	}

	private static SootMethod performMapping(SootMethod synthMeth) {
		if(!synthMeth.isConcrete())
			return null;
		
		SootMethod srcMeth = null;
		SootClass klass = synthMeth.getDeclaringClass();
		for(Unit u : synthMeth.retrieveActiveBody().getUnits()){
			Stmt stmt = (Stmt) u;
			if(!stmt.containsInvokeExpr())
				continue;
			if(srcMeth != null)
				return null;  //two invk instrs
			srcMeth = stmt.getInvokeExpr().getMethod();
			if(!srcMeth.getDeclaringClass().equals(klass) || !srcMeth.isPrivate())
				return null;
		}
		return srcMeth;
	}
}