package petablox.android.analyses;

import petablox.project.analyses.ProgramDom;
import soot.Local;
import soot.SootMethod;
import soot.jimple.internal.JimpleLocal;
import java.util.Map;
import java.util.HashMap;
/**
 * Domain of method parameter/argument indices
 * 
 * @author Saswat Anand
 */
public class DomU extends ProgramDom<Local>
{
	protected Map<Local, SootMethod> varToMethodMap;
	
	@Override
    public void init() {
        varToMethodMap = new HashMap<Local, SootMethod>();
    }

    public SootMethod getMethod(Local v) {
        return varToMethodMap.get(v);
    }
    
    public boolean add(Local v, SootMethod m){
    	assert v != null;
    	if(varToMethodMap == null)
    		varToMethodMap = new HashMap<Local, SootMethod>();
    	varToMethodMap.put(v,m);
    	return super.add(v);
    }
}