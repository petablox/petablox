package stamp.analyses.ondemand;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.MethodOrMethodContext;
import soot.Body;
import soot.Local;
import soot.Type;
import soot.RefLikeType;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.NewExpr;
import soot.jimple.spark.pag.VarNode;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.LocalVarNode;
import soot.jimple.spark.ondemand.genericutil.ImmutableStack;
import soot.jimple.spark.ondemand.AllocAndContextSet;
import soot.jimple.spark.ondemand.AllocAndContext;
import soot.jimple.spark.ondemand.DemandCSPointsTo.VarAndContext;

import chord.project.Chord; 
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

import com.google.gson.stream.JsonWriter;

import java.util.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
@Chord(name="taint-propagation-forward-java")
public class ForwardTaintPropagation extends MethodReachabilityAnalysis
{
	protected Map<SootMethod,List<AllocNode>> methToAllocNodes = new HashMap();
    protected Map<VarNode,Map<String,Set<ImmutableStack<Integer>>>> variableToTaints = new HashMap();
	protected TaintManager taintManager;

	public ForwardTaintPropagation()
	{
	}

	public void run()
	{
		for(AllocNode an : taintManager.getTaintedAllocs()){
			SootMethod m = an.getMethod();
			if(m != null){
				targetMethods.add(m);
				List<AllocNode> ans = methToAllocNodes.get(m);
				if(ans == null){
					ans = new ArrayList();
					methToAllocNodes.put(m, ans);
				}
				ans.add(an);
			}
		}
		super.run();
	}

	protected void setup(OnDemandPTA dpta, TaintManager taintManager)
	{
		setup(dpta);
		this.taintManager = taintManager;
	}

	protected Set<String> computeTaintSetFor(Local loc, ImmutableStack<Integer> locContext)
	{
		Set<String> taints = new HashSet();
		if(!(loc.getType() instanceof RefLikeType)){
			//TODO
			return taints;
		}

		VarNode vn = dpta.varNode(loc);
		Map<String,Set<ImmutableStack<Integer>>> taintLabelToContextSet = variableToTaints.get(vn);
		if(taintLabelToContextSet != null){
			for(Map.Entry<String,Set<ImmutableStack<Integer>>> e : taintLabelToContextSet.entrySet()){
				String label = e.getKey();
				for(ImmutableStack<Integer> context : e.getValue()){
					///check if context is matches top of locContext
					if(locContext.topMatches(context)){
						taints.add(label);
						break;
					}
				}
			}
		}
		return taints;
	}

	protected void visitFinal(SootMethod caller, 
							  Stmt callStmt, 
							  SootMethod callee, 
							  ImmutableStack<Integer> calleeContext, 
							  Object data)
	{
		List<AllocNode> ans = methToAllocNodes.get(callee);
		for(AllocNode an : ans){
			forwardPropagate(an, calleeContext);
		}
	}

	protected void forwardPropagate(AllocNode an, ImmutableStack<Integer> anContext)
	{
		List<AllocAndContext> objectsWL = new ArrayList();
		Set<AllocAndContext> objectsVisited = new HashSet();
		objectsWL.add(new AllocAndContext(an, anContext));

		List<VarAndContext> varsWL = new ArrayList();
		Set<VarAndContext> varsVisited = new HashSet();

		Set<String> taintLabels = taintManager.getTaint(an);

		//debug
		StringBuilder builder = new StringBuilder("[");
		for(String taint : taintLabels)
			builder.append(", "+taint);
		String taintStr = builder.append("]").toString();
		System.out.println("propagating taints "+taintStr);

		do{
			while(!objectsWL.isEmpty()){
				AllocAndContext oc = objectsWL.remove(0);
				if(objectsVisited.contains(oc))
					continue;
				objectsVisited.add(oc); 
				//System.out.println("to find flows-to of "+oc);
				Set<VarAndContext> vcs = dpta.flowsToSetFor(oc);
				if(vcs == null)
					System.out.println("Warning: Flows-to set for "+ oc+" is null.");
				else
					for(VarAndContext vc : vcs){
						VarNode src = vc.var;
						ImmutableStack<Integer> srcContext = vc.context;
						System.out.println("taintedvar: "+src+" "+srcContext);

						Map<String,Set<ImmutableStack<Integer>>> labelToContexts = variableToTaints.get(src);
						if(labelToContexts == null){
							labelToContexts = new HashMap();
							variableToTaints.put(src, labelToContexts);
						}

						for(String label : taintLabels){
							Set<ImmutableStack<Integer>> contexts = labelToContexts.get(label);
							if(contexts == null){
								contexts = new HashSet();
								labelToContexts.put(label, contexts);
							}
							contexts.add(srcContext);
						}

						SootMethod method = dpta.transferEndPoint(src);
						if(method == null)
							continue;

						Collection<LocalVarNode> dsts = taintManager.findTaintTransferDestFor(src);
						if(dsts == null){
							//System.out.println("possibly missing models. endpoint: "+
							//				   ((LocalVarNode) src).getVariable()+" in method: "+method.getSignature());
							continue;
						}
						for(LocalVarNode dst : dsts)
							varsWL.add(new VarAndContext(dst, srcContext));
					}
			}

			while(!varsWL.isEmpty()){
				VarAndContext vc = varsWL.remove(0);
				if(varsVisited.contains(vc))
					continue;
				varsVisited.add(vc);
				//System.out.println("to find points-to set of "+vc.var+" "+vc.context);
				AllocAndContextSet pt = (AllocAndContextSet) dpta.pointsToSetFor(vc);
				if(pt == null)
					System.out.println("Warning: Points-to set for "+vc.var+" is null.");
				else
					for(AllocAndContext obj : pt)
						objectsWL.add(obj);
			}			
		} while(!objectsWL.isEmpty());
	}


	/*
	protected List<AllocNode> identifyWidgetAllocNodes()
	{
		List<AllocNode> taintedAllocNodes = new ArrayList();
		SootClass gClass = Scene.v().getSootClass("stamp.harness.G");
		SootMethod gClinit = gClass.getMethod("void <clinit>()");
		for(Unit unit : gClinit.retrieveActiveBody().getUnits()){
			Stmt stmt = (Stmt) unit;
			if(!(stmt instanceof DefinitionStmt))
				continue;
			Value right = ((DefinitionStmt) stmt).getRightOp();
			if(right instanceof NewExpr){
				if(right.getType().toString().equals("com.eat24.app.widgets.Eat24EditText"))
					taintedAllocNodes.add(dpta.allocNodeFor(gClinit, (NewExpr) right));
			}
		}
		return taintedAllocNodes;
	}*/
}