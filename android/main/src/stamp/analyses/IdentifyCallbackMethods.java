package stamp.analyses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import shord.program.Program;

import soot.SootClass;
import soot.SootMethod;
import soot.util.NumberedString;

/*
 * @author Saswat Anand
 */
public class IdentifyCallbackMethods
{
	private Map<SootMethod, List<SootMethod>> frameworkMethodToCallbacks = new HashMap();
	private Map<NumberedString, SootMethod> sigToMethod;
	private Set<NumberedString> sigs;

    public void analyze()
	{
        Program program = Program.g();
		for(SootClass c : program.getClasses()) {
			if(program.isFrameworkClass(c))
				continue;
			sigs = new HashSet();
			sigToMethod = new HashMap();

            for(SootMethod m : c.getMethods()) {
				boolean potentialCallback = 
					m.isConcrete() &&
					m.isPublic() &&
					!m.isStatic() &&
					!m.getName().startsWith("<");

				if(!potentialCallback)
					continue;
				NumberedString s = m.getNumberedSubSignature();
				sigs.add(s);
				sigToMethod.put(s, m);
			}

			findCallbacks(c);
		}
	}
	
	public Map<SootMethod, List<SootMethod>> frameworkMethodToCallbacks()
	{
		return frameworkMethodToCallbacks;
	}
	
	public Set<SootMethod> allCallbacks()
	{
		Set<SootMethod> ret = new HashSet();
		for(List<SootMethod> meths : frameworkMethodToCallbacks.values()){
			ret.addAll(meths);
		}
		return ret;
	}

	private void addCallback(SootMethod frameworkMethod, SootMethod callback)
	{
		List<SootMethod> callbacks = frameworkMethodToCallbacks.get(frameworkMethod);
		if(callbacks == null){
			callbacks = new ArrayList();
			frameworkMethodToCallbacks.put(frameworkMethod, callbacks);
		}
		callbacks.add(callback);
	}

	private void findCallbacks(SootClass klass)
	{
		if(!klass.hasSuperclass())
			return;
		SootClass superClass = klass.getSuperclass();

		if(Program.g().isFrameworkClass(superClass)){
			if(!superClass.getName().equals("java.lang.Object")) {
				for(SootMethod superMeth : superClass.getMethods()){
					boolean canBeOverriden = 
						superMeth.isPublic() &&
						!superMeth.isFinal() &&
						!superMeth.isStatic() &&
						!superMeth.getName().startsWith("<");
					if(!canBeOverriden)
						continue;
					NumberedString superMethSig = superMeth.getNumberedSubSignature();
					if(sigs.contains(superMethSig)){
						addCallback(superMeth, sigToMethod.get(superMethSig));
					}
				}
			}
		}
		findCallbacks(superClass);

		for(SootClass iface : klass.getInterfaces()){
			if(Program.g().isFrameworkClass(iface)){
				for(SootMethod superMeth : iface.getMethods()){
					NumberedString superMethSig = superMeth.getNumberedSubSignature();
					if(sigs.contains(superMethSig)){
						addCallback(superMeth, sigToMethod.get(superMethSig));
					}
				}
			}
			findCallbacks(iface);
		}
	}
}
