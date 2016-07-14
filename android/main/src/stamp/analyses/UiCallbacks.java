package stamp.analyses;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.FastHierarchy;

import shord.program.Program;

import java.util.*;

public class UiCallbacks
{
	private Map<SootMethod,Integer> callbackToParamIndex = new HashMap();

	UiCallbacks()
	{
		Map<String,Integer> baseCallbackMeths = new HashMap();
		baseCallbackMeths.put("<android.view.View$OnClickListener: void onClick(android.view.View)>", 0);
		baseCallbackMeths.put("<android.widget.AdapterView$OnItemClickListener: void onItemClick(android.widget.AdapterView,android.view.View,int,long)>", 1);
		
		//third-party known libraries
		baseCallbackMeths.put("<it.sephiroth.android.library.widget.AdapterView$OnItemClickListener: void onItemClick(it.sephiroth.android.library.widget.AdapterView,android.view.View,int,long)>", 1);
		
		identifyCallbacks(baseCallbackMeths);
	}

	public Integer isCallback(SootMethod meth)
	{
		return callbackToParamIndex.get(meth);
	}
	
	public Map<SootMethod,Integer> allCallbacks()
	{
		return callbackToParamIndex;
	}
	
	void identifyCallbacks(Map<String,Integer> baseCallbackMeths)
	{
		Map<SootMethod,Integer> baseCallbackToParamIndex = new HashMap();
		Set<String> subsigs = new HashSet();
		Map<String,Set<SootMethod>> subSigToCallbackMethods = new HashMap();

		for(Map.Entry<String,Integer> entry : baseCallbackMeths.entrySet()){
			String sig = entry.getKey();
			Integer pindex = entry.getValue();
			if(!Scene.v().containsMethod(sig))
				continue;
			SootMethod baseCallback = Scene.v().getMethod(sig);
			baseCallbackToParamIndex.put(baseCallback, pindex);

			String subsig = baseCallback.getSubSignature();
			subsigs.add(subsig);

			Set<SootMethod> meths = subSigToCallbackMethods.get(subsig);
			if(meths == null)
				subSigToCallbackMethods.put(subsig, (meths = new HashSet()));
			meths.add(baseCallback);
		}
		FastHierarchy fh = Program.g().scene().getOrMakeFastHierarchy();
		Program prog = Program.g();
		for(SootClass klass : prog.getClasses()){
			if(prog.isFrameworkClass(klass))
				continue;
			for(String ss : subsigs){
				if(klass.declaresMethod(ss)){
					Set<SootMethod> cbs = subSigToCallbackMethods.get(ss);
					for(SootMethod cb : cbs){
						SootClass cbClass = cb.getDeclaringClass();
						Integer paramIndex = baseCallbackToParamIndex.get(cb);
						assert !cb.isStatic();
						paramIndex++; 
						if(fh.canStoreType(klass.getType(), cbClass.getType())){ 
							SootMethod overridingMeth = klass.getMethod(ss);
							callbackToParamIndex.put(overridingMeth, paramIndex);
							System.out.println("UiCallback: "+overridingMeth+" "+paramIndex);
						}
					}
				}
			}	
		}
	}
}