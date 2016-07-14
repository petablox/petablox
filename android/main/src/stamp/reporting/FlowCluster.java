package stamp.reporting;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import shord.project.ClassicProject;
import shord.project.analyses.ProgramRel;
import shord.analyses.Ctxt;
import shord.analyses.VarNode;

import stamp.util.Partition;

import chord.bddbddb.Rel.RelView;
import chord.util.tuple.object.Trio;
import chord.util.tuple.object.Pair;

import soot.util.BitVector;
import soot.util.MapNumberer;

/*
 * @author Saswat Anand
**/
public class FlowCluster
{
	private static final Map<Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>,BitVector> flowToBucket = new HashMap();
	private static final Map<Pair<String,Ctxt>,List<Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>>> labelToFlows = new HashMap();
	private static final double threshhold = Double.parseDouble(System.getProperty("stamp.flowcluster.threshhold", "0.9"));

	static void cluster()
	{
		System.out.println("Begin clustering");
		fillBuckets();
		
		Object[] flows = flowToBucket.keySet().toArray();
		int numFlows = flows.length;

		Partition<Integer> part = new Partition();
		for(int i = 0; i < numFlows; i++){
			part.makeSet(i);
		}

		for(int i = 0; i < numFlows; i++){
			Pair<Pair<String,Ctxt>,Pair<String,Ctxt>> f1 = (Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>) flows[i];
			for(int j = i+1; j < numFlows; j++){
				Pair<Pair<String,Ctxt>,Pair<String,Ctxt>> f2 = (Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>) flows[j];
				double ochiaiIndex = ochiai(flowToBucket.get(f1), flowToBucket.get(f2));
				System.out.println("ochiai("+i+", "+j+") = "+ ochiaiIndex);
				if(ochiaiIndex > threshhold){
					part.union(part.find(i), part.find(j));
				}
			}
		}

		Collection<List<Integer>> clusters = part.allPartitions();
		System.out.println("No. of flows = "+numFlows+". No. of clusters = "+clusters.size());

		System.out.println("End clustering");
	}

	static double ochiai(BitVector set1, BitVector set2)
	{
		BitVector intersection = new BitVector(set1);
		intersection.and(set2);
		
		double index = intersection.cardinality() / Math.sqrt(set1.cardinality()*set2.cardinality());
		return index;
	}

	private static void initBuckets(int numVars)
	{
		final ProgramRel relCtxtFlows = (ProgramRel)ClassicProject.g().getTrgt("flow");
		relCtxtFlows.load();

		Iterable<Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>> res = relCtxtFlows.getAry2ValTuples();
		int count = 0;
		for(Pair<Pair<String,Ctxt>,Pair<String,Ctxt>> flow : res) {
			Pair<String,Ctxt> srcLabel = flow.val0;
			Pair<String,Ctxt> sinkLabel = flow.val1;
			
			BitVector bucket = flowToBucket.get(flow);
			if(bucket == null){
				bucket = new BitVector(numVars);
				flowToBucket.put(flow, bucket);
			}

			List<Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>> flows = labelToFlows.get(srcLabel);
			if(flows == null){
				flows = new ArrayList();
				labelToFlows.put(srcLabel, flows);
			}
			flows.add(flow);

			flows = labelToFlows.get(sinkLabel);
			if(flows == null){
				flows = new ArrayList();
				labelToFlows.put(sinkLabel, flows);
			}
			flows.add(flow);			
		}
		relCtxtFlows.close();
	}

	private static void fillBuckets()
	{
		final ProgramRel relRef = (ProgramRel) ClassicProject.g().getTrgt("labelRef");
		relRef.load();

		final ProgramRel relPrim = (ProgramRel) ClassicProject.g().getTrgt("labelPrim");
		relPrim.load();

		MapNumberer taintedVarNumberer = new MapNumberer();
		
		populateDomain(taintedVarNumberer, relRef);
		populateDomain(taintedVarNumberer, relPrim);
		
		initBuckets(taintedVarNumberer.size());

		fillBuckets(taintedVarNumberer, relRef);
		fillBuckets(taintedVarNumberer, relPrim);

		relRef.close();
		relPrim.close();
	}
	
	private static void populateDomain(MapNumberer varNumberer, ProgramRel rel)
	{
		RelView taintedVars = rel.getView();
		taintedVars.delete(2); //drop labels

		Iterable<Pair<Ctxt,VarNode>> iter = taintedVars.getAry2ValTuples();
		for(Pair<Ctxt,VarNode> pair : iter){
			varNumberer.add(pair);
		}
	}

	private static void fillBuckets(MapNumberer taintedVarNumberer, ProgramRel rel)
	{
		Iterable<Trio<Ctxt,VarNode,Pair<String,Ctxt>>> iter = rel.getAry3ValTuples();
		for(Trio<Ctxt,VarNode,Pair<String,Ctxt>> trio : iter) {
			Ctxt ctxt = trio.val0;
			VarNode var = trio.val1;
			Pair<String,Ctxt> label = trio.val2;

			List<Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>> flows = labelToFlows.get(label);
			if(flows == null)
				continue;
			Pair<Ctxt,VarNode> ctxtVar = new Pair(ctxt,var);
			int ctxtVarIndex = (int) taintedVarNumberer.get(ctxtVar);
			for(Pair<Pair<String,Ctxt>,Pair<String,Ctxt>> flow : flows){
				BitVector bucket = flowToBucket.get(flow);
				bucket.set(ctxtVarIndex);
			}
		}
	}
}