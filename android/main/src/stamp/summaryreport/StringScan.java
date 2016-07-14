package stamp.summaryreport;

import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;

import soot.jimple.StringConstant;
import shord.program.Program;
import stamp.srcmap.sourceinfo.abstractinfo.AbstractSourceInfo;

import java.util.*;

public class StringScan
{
	public final Set<String> scs = new HashSet();

	public void analyze()
	{
		Iterator mIt = Program.g().scene().getReachableMethods().listener();
		while(mIt.hasNext()){
			SootMethod m = (SootMethod) mIt.next();
			if(!m.isConcrete())
				continue;
			SootClass declKlass = m.getDeclaringClass();
			if(AbstractSourceInfo.isFrameworkClass(declKlass))
				continue;

			for(ValueBox vb : m.retrieveActiveBody().getUseBoxes()){
				Value val = vb.getValue();
				if(!(val instanceof StringConstant))
					continue;
				String str = ((StringConstant) val).value;
				scs.add(str);
			}
		}
	}
}