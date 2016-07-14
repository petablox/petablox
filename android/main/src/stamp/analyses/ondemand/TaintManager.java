package stamp.analyses.ondemand;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Local;
import soot.Type;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.VarNode;
import soot.jimple.spark.pag.LocalVarNode;

import stamp.analyses.SootUtils;

import chord.util.tuple.object.Pair;

import java.util.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
public class TaintManager
{
	public Map<AllocNode,Set<String>> allocNodeToTaints = new HashMap();

	private Map<LocalVarNode,Set<LocalVarNode>> dstToSourceTransfer = new HashMap();
	private Map<LocalVarNode,Set<LocalVarNode>> srcToDestTransfer = new HashMap();

	private Map<SootMethod,Map<Integer,Set<String>>> methodToSinkLabels = new HashMap();
	private Map<SootMethod,Map<Integer,Set<String>>> methodToSourceLabels = new HashMap();

	private Set<String> interestingSinkLabels;
	private Set<String> interestingSourceLabels;
	private OnDemandPTA dpta;

	public TaintManager(OnDemandPTA dpta)
	{
		this.dpta = dpta;
	}

	public void setSinkLabels(Collection<String> sinkLabels)
	{
		interestingSinkLabels = new HashSet();
		interestingSinkLabels.addAll(sinkLabels);
	}

	public void setSourceLabels(Collection<String> sourceLabels)
	{
		interestingSourceLabels = new HashSet();
		interestingSourceLabels.addAll(sourceLabels);
	}

	public Map<Integer,Set<String>> sinkParamsOf(SootMethod method)
	{
		return methodToSinkLabels.get(method);
	}

	public Set<SootMethod> sinkMethods()
	{
		Set<SootMethod> ret = new HashSet();
		for(Map.Entry<SootMethod,Map<Integer,Set<String>>> e : methodToSinkLabels.entrySet()){
			SootMethod m = e.getKey();
			boolean interesting = false;
			for(Set<String> labels : e.getValue().values()){
				for(String label : labels)
					if(interestingSinkLabels.contains(label)){
						interesting = true;
						break;
					}
			}
			if(interesting)
				ret.add(m);
		}
		return ret;
	}

	public Collection<LocalVarNode> findTaintTransferSourceFor(VarNode dstVar)
	{
		if(!(dstVar instanceof LocalVarNode))
			return null;
		return dstToSourceTransfer.get((LocalVarNode) dstVar);
	}

	public Collection<LocalVarNode> findTaintTransferDestFor(VarNode srcVar)
	{
		if(!(srcVar instanceof LocalVarNode))
			return null;
		return srcToDestTransfer.get((LocalVarNode) srcVar);
	}

	public Set<String> getTaint(AllocNode object)
	{
		return allocNodeToTaints.get(object);
	}

	public Set<AllocNode> getTaintedAllocs()
	{
		return allocNodeToTaints.keySet();
	}
	
	public void setTaint(Local local, final String taint)
	{
		PointsToSetInternal pt = dpta.ciPointsToSetFor(local);
		pt.forall(new P2SetVisitor() {
				public final void visit(Node n) {
					AllocNode an = (AllocNode) n;
					Set<String> taints = allocNodeToTaints.get(an);
					if(taints == null){
						taints = new HashSet();
						allocNodeToTaints.put(an, taints);
					}
					taints.add(taint);
				}
			});
	}

	protected boolean isInterestingSource(String label)
	{
		return interestingSourceLabels == null || interestingSourceLabels.contains(label);
	}

	protected boolean isInterestingSink(String label)
	{
		return interestingSinkLabels == null || interestingSinkLabels.contains(label);
	}

	private String[] split(String line)
	{
		String[] tokens = new String[3];
		int index = line.indexOf(" ");
		tokens[0] = line.substring(0, index);
		
		String delim = "$stamp$stamp$";
		
		index++;
		char c = line.charAt(index);
		if((c == '$' || c == '!') && line.startsWith(delim, index+1)){
			int j = line.indexOf(delim, index+2);
			tokens[1] = c+line.substring(index+1+delim.length(), j);
			index = j+delim.length();
			if(line.charAt(index) != ' ')
				throw new RuntimeException("Cannot parse annotation "+line);
		} else {
			int j = line.indexOf(' ', index);
			tokens[1] = line.substring(index, j);
			index = j;
		}
		
		index++;		
		c = line.charAt(index);
		if(c == '!' && line.startsWith(delim, index+1)){
			int j = line.indexOf(delim, index+2);
			tokens[2] = c+line.substring(index+1+delim.length(), j);
			index = j+delim.length();
			if(index != line.length())
				throw new RuntimeException("Cannot parse annotation "+line);
		} else {
			assert c != '$';
			tokens[2] = line.substring(index, line.length());
		}

		return tokens;
	}

	public void readAnnotations()
	{
		Scene scene = Scene.v();
		try{
			BufferedReader reader = new BufferedReader(new FileReader(new File(System.getProperty("stamp.out.dir"), "stamp_annotations.txt")));
			String line = reader.readLine();
			while(line != null){
				final String[] tokens = split(line);
				String chordMethodSig = tokens[0];
				int atSymbolIndex = chordMethodSig.indexOf('@');
				String className = chordMethodSig.substring(atSymbolIndex+1);
				if(scene.containsClass(className)){
					SootClass klass = scene.getSootClass(className);
					String subsig = SootUtils.getSootSubsigFor(chordMethodSig.substring(0,atSymbolIndex));
					SootMethod meth = klass.getMethod(subsig);
					
					String from = tokens[1];
					String to = tokens[2];

					boolean b1 = from.charAt(0) == '$' || from.charAt(0) == '!';
					boolean b2 = to.charAt(0) == '$' || to.charAt(0) == '!';
					if(b1 && b2){
						System.out.println("Unsupported annotation type "+line);
					} else {
						addFlow(meth, from, to);
					}
				}
				line = reader.readLine();
			}
			reader.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}

	private void addFlow(SootMethod meth, String from, String to) //throws NumberFormatException
	{
		//System.out.println("+++ " + meth + " " + from + " " + to);

		//handle compiler-generated methods that result from co-variant return types
		Set<SootMethod> covariantMethods = new HashSet();
		String mName = meth.getName();
		List<Type> mTypes = meth.getParameterTypes();
		for(SootMethod method : meth.getDeclaringClass().getMethods())
			if(mName.equals(method.getName()) && mTypes.equals(method.getParameterTypes()))
				covariantMethods.add(method);

		Set<SootMethod> meths = new HashSet();
		for(SootMethod m : covariantMethods)
			meths.addAll(SootUtils.overridingMethodsFor(m));

		char from0 = from.charAt(0);
		if(from0 == '$' || from0 == '!') {
			if(to.equals("-1")){
				//return value is tainted
				for(SootMethod m : meths){
					addLabel(methodToSourceLabels, m, -1, from);
				}
			} else{
				//parameter is tainted
				for(SootMethod m : meths){
					addLabel(methodToSourceLabels, m, Integer.valueOf(to), from);
				}
			}
		} else {
			Integer fromArgIndex = Integer.valueOf(from);
			char to0 = to.charAt(0);
			if(to0 == '!'){
				//sink
				for(SootMethod m : meths){
					if(Scene.v().getReachableMethods().contains(m)){
						addLabel(methodToSinkLabels, m, fromArgIndex, to);
					}
				}
			} else if(to0 == '?'){
				//TODO
				;
			} else if(to.equals("-1")){
				//transfer from fromArgIndex to return
				for(SootMethod m : meths){
					LocalVarNode src = (LocalVarNode) dpta.parameterNode(m, Integer.valueOf(fromArgIndex));
					LocalVarNode dst = (LocalVarNode) dpta.retVarNode(m);
					if(dst != null && src != null){
						System.out.println("xfer "+src+" "+dst);
						Set<LocalVarNode> sources = dstToSourceTransfer.get(dst);
						if(sources == null){
							sources = new HashSet();
							dstToSourceTransfer.put(dst, sources);
						}
						sources.add(src);
				 
						Set<LocalVarNode> dests = srcToDestTransfer.get(src);
						if(dests == null){
							dests = new HashSet();
							srcToDestTransfer.put(src, dests);
						}
						dests.add(dst);
					}
				}
			} else {
				//transfer from fromArgIndex to toArgIndex
				Integer toArgIndex = Integer.valueOf(to);
				for(SootMethod m : meths){
					LocalVarNode src = (LocalVarNode) dpta.parameterNode(m, Integer.valueOf(fromArgIndex));
					LocalVarNode dst = (LocalVarNode) dpta.parameterNode(m, Integer.valueOf(toArgIndex));
					if(src != null && dst != null){
						Set<LocalVarNode> sources = dstToSourceTransfer.get(dst);
						if(sources == null){
							sources = new HashSet();
							dstToSourceTransfer.put(dst, sources);
						}
						sources.add(src);
						
						Set<LocalVarNode> dests = srcToDestTransfer.get(src);
						if(dests == null){
							dests = new HashSet();
							srcToDestTransfer.put(src, dests);
						}
						dests.add(dst);
					}
				}
			}
		}
	}
	
	private void addLabel(Map<SootMethod,Map<Integer,Set<String>>> methodToLabels, SootMethod m, int paramIndex, String label)
	{
		Map<Integer,Set<String>> paramIndexToLabels = methodToLabels.get(m);
		if(paramIndexToLabels == null){
			paramIndexToLabels = new HashMap();
			methodToLabels.put(m, paramIndexToLabels);
		}
		Set<String> labels = paramIndexToLabels.get(paramIndex);
		if(labels == null){
			labels = new HashSet();
			paramIndexToLabels.put(paramIndex, labels);
		}
		labels.add(label);
	}
}