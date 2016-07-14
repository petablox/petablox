package stamp.analyses;

import soot.Unit;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.DominatorTree;
import soot.toolkits.graph.DominatorNode;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.SimpleDominatorsFinder;
import soot.toolkits.graph.HashReversibleGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.Pair;
import java.util.*;

/** 
    Intra-procedural Control Dependence Graph
	@author Saswat Ananad
 */
public class ControlDependenceGraph
{
    private Map<Object,Set<Pair<Unit,Unit>>> nodeToDependees = new HashMap();
    
    public ControlDependenceGraph(SootMethod method)
    {
		BlockGraph cfg = new BriefBlockGraph(method.retrieveActiveBody());
		
		for(Block block : cfg.getBlocks())
			nodeToDependees.put(block, new HashSet());
		
		//Add a super exit node to the cfg
		HashReversibleGraph reversibleCFG = new HashReversibleGraph(cfg);
		
		List tails = reversibleCFG.getTails();
		if(tails.size() > 1){
			Object superExitNode = new Object();
			reversibleCFG.addNode(superExitNode);
			for(Iterator it = tails.iterator(); it.hasNext();){
				Object tail = it.next();
				//System.out.println("tail " + tail);
				reversibleCFG.addEdge(tail, superExitNode);
			}
		}

		
		DominatorsFinder domfinder = new SimpleDominatorsFinder(reversibleCFG.reverse());
		DominatorTree domlysis = new DominatorTree(domfinder);
		/*
		  System.out.println("**** Postdominator Tree of " + method);
		  for(Iterator it = cfg.iterator(); it.hasNext();){
		  Block a = (Block) it.next();
		  Object b = domfinder.getImmediateDominator(a);
		  if(b instanceof Block)
		  System.out.print(((Block) b).getIndexInMethod());
		  else
		  System.out.print("Exit");
		  System.out.println("  --->  " + a.getIndexInMethod());
		  }
		*/
		
		for(Block a : cfg.getBlocks()){
			// Step 1
			// if node a had more than one successors then 
			// each successor does not post-dominate a
			// So S is the set succs
			List<Block> succs = cfg.getSuccsOf(a);
			if(succs.size() > 1){
				
				// Step 2
				// for each b in S (i.e., succs) find the
				// least common ancestor of a and b
				
				Set ancestorsA = new HashSet();
				Object parent = a;
				while(parent != null){
					ancestorsA.add(parent);
					parent = getImmediateDominator(domlysis, parent);
					//System.out.println("!! " + parent);
				}
				
				for(Block b : succs){
					Set marked = new HashSet();
					Object l = b;
					do{
						if(ancestorsA.contains(l)){
							// l is the least common ancestors
							//System.out.println("LCA: " + l );
							break;
						}
						else{
							marked.add(l);
							//System.out.print("Immediate dominator of " + l + " is ");
							l = getImmediateDominator(domlysis, l);
							assert l != null;
							//System.out.println(l);
						}
					}while(true);
					if(l == a)
						marked.add(l);

					Unit dependee = a.getTail();
					for(Object node : marked){
						if(!(node instanceof Block))
							continue;
						if(!dependee.branches()) 
							assert false: dependee+"@"+method.getSignature() + " does not branch!";
						nodeToDependees.get(node).add(new Pair(dependee, b.getHead()));
					}
				}
			}
		}
    }

	/*
	private int label(Block dependee, Block successor)
	{
		if(t instanceof SwitchStmt){
			for(Unit target : ((SwitchStmt) t).getTargets()){
				if(target.equals(
			}
		} else if(t instanceof IfStmt){
			Stmt target = ((IfStmt) t).getTarget();
			return target.equals(successor.getHead()) ? 1 : 0;
		} 
		throw new RuntimeException("unexpected "+t);
	}
	*/
	public Map<Pair<Unit,Unit>,Set<Unit>> dependeeToDependentsSetMap()
	{
		Map<Pair<Unit,Unit>,Set<Unit>> result = new HashMap();
		for(Map.Entry<Object,Set<Pair<Unit,Unit>>> e : nodeToDependees.entrySet()){
			Object block = e.getKey();
			if(!(block instanceof Block))
				continue; //block is the super-exit node
			Set<Pair<Unit,Unit>> dependees = e.getValue();
			for(Pair<Unit,Unit> dependee : dependees){
				Set<Unit> dependents = result.get(dependee);
				if(dependents == null){
					dependents = new HashSet();
					result.put(dependee, dependents);
				}
				for(Iterator<Unit> uit = ((Block) block).iterator(); uit.hasNext();){
					dependents.add(uit.next());
				}
			}
		}
		return result;
	}

	public Map<Unit,Set<Pair<Unit,Unit>>> dependentToDependeesSetMap()
	{
		Map<Unit,Set<Pair<Unit,Unit>>> result = new HashMap();
		for(Map.Entry<Object,Set<Pair<Unit,Unit>>> e : nodeToDependees.entrySet()){
			Object block = e.getKey();
			if(!(block instanceof Block))
				continue; //block is the super-exit node
			Set<Pair<Unit,Unit>> dependees = e.getValue();
			
			for(Iterator<Unit> uit = ((Block) block).iterator(); uit.hasNext();){
				Unit dependent = uit.next();
				result.put(dependent, new HashSet(dependees));
			}
		}
		return result;
	}

    
    private Object getImmediateDominator(DominatorTree domlysis, Object node)
    {
		DominatorNode n = domlysis.getParentOf(domlysis.getDode(node));
		if(n == null) return null; else return n.getGode();
    }    
}
