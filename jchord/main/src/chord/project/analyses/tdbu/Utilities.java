package chord.project.analyses.tdbu;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.ICICG;
import chord.analyses.typestate.AccessPath;
import chord.analyses.typestate.Helper;
import chord.analyses.typestate.RegisterAccessPath;
import chord.util.ArraySet;

public class Utilities {
public static <T extends Object> int getCount(Map<T,Integer> map, T key){
	Integer count = map.get(key);
	if(count == null)
		return 0;
	else
		return count.intValue();
}

/**
 * Return the reachable methods of m including itself
 * @param m
 * @param callGraph
 * @return
 */
public static Set<jq_Method> getReachableMethods(jq_Method m,ICICG callGraph){
	Set<jq_Method> ret = new HashSet<jq_Method>();
	Queue<jq_Method> workList = new LinkedList<jq_Method>();
	workList.add(m);
	ret.add(m);
	while(!workList.isEmpty()){
		jq_Method m1 = workList.poll();
		for(jq_Method m2 : callGraph.getSuccs(m1)){
			if(ret.add(m2)){
				workList.add(m2);
			}
		}
	}
	return ret;
}

public static <T extends Object> boolean ifIntersect(Set<T> l, Set<T> r){
	for(T lEle : l){
		if(r.contains(lEle))
			return true;
	}
	return false;
}
	
public static <T extends Object> Set<T> intersect(Set<T> l, Set<T> r){
	Set<T> ret = new HashSet<T>(l);
	ret.retainAll(r);
	return ret;
}

public static <T extends Object> Set<T> union(Set<T> l, Set<T> r){
	Set<T> ret = new HashSet<T>(l);
	ret.addAll(r);
	return ret;
}


}
