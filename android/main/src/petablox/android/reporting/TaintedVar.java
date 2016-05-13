package petablox.reporting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import petablox.analyses.alias.Ctxt;
import petablox.analyses.var.DomV;
//import petablox.android.analyses.LocalLocal;
//import petablox.android.analyses.Local;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import petablox.android.srcmap.Expr;
import petablox.android.srcmap.sourceinfo.RegisterMap;
import petablox.android.srcmap.sourceinfo.SourceInfo;
import petablox.util.soot.SootUtilities;
import petablox.util.tuple.object.Pair;

// TODO thin out unused imports
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import petablox.analyses.alias.Ctxt;

import petablox.android.util.Partition;

import petablox.bddbddb.Rel.RelView;
import petablox.util.tuple.object.Trio;
import petablox.util.tuple.object.Pair;

import soot.util.BitVector;
import soot.util.MapNumberer;

/**
 * @author Saswat Anand
 */
public class TaintedVar extends XMLReport
{
	private static final Map<Pair<String,Ctxt>,List<Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>>> labelToFlows = new HashMap();
	private static final Map<Pair<String,Ctxt>,List<Integer>> labelToFlowNums = new HashMap();
	private static final double threshhold = Double.parseDouble(System.getProperty("stamp.flowcluster.threshhold", "0.9"));

	private static final Map<Local, String> varToFlows = new HashMap();
	
	private DomV domV = (DomV) ClassicProject.g().getTrgt("V");
	
	public TaintedVar()
	{

		super("Tainted Vars");
	}

    public void generate() {
		Map<String,Map<SootMethod,Set<Local>>> labelToTaintedVars = labelToTaintedVars();
		fillBuckets();

		for(Map.Entry<String,Map<SootMethod,Set<Local>>> entry1 : labelToTaintedVars.entrySet()){
			String label = entry1.getKey();
			Map<SootMethod,Set<Local>> taintedVars = entry1.getValue();
			Category labelCat = makeOrGetSubCat(label);
			for(Map.Entry<SootMethod,Set<Local>> entry2 : taintedVars.entrySet()){
				SootMethod meth = entry2.getKey();
				SootClass klass = meth.getDeclaringClass();
				RegisterMap regMap = this.sourceInfo.buildRegMapFor(meth);
				Set<Local> vars = entry2.getValue();
				Category methCat = labelCat.makeOrGetPkgCat(meth);
				for(Local v : vars){
					//Local lvn =  v;
					//System.out.println("taintedReg: " + reg + " "+meth);
					Set<Expr> locs = regMap.srcLocsFor(v);
					if(locs != null && locs.size() > 0){
						for(Expr l : locs){
							if(l.start() < 0 || l.length() < 0 || l.text() == null)
								continue;
							methCat.newTuple().addValueWithHighlight(klass, l, varToFlows.get(v));
						}
					}
				}
			}
		}
	}

	private Map<String,Map<SootMethod,Set<Local>>> labelToTaintedVars() {
		Map<String,Map<SootMethod,Set<Local>>> labelToTaintedVars = new HashMap();

		final ProgramRel relRef = (ProgramRel) ClassicProject.g().getTrgt("out_taintedRefVar");
		relRef.load();

		Iterable<Pair<Pair<String,Ctxt>,Local>> res1 = relRef.getAry2ValTuples();
		for(Pair<Pair<String,Ctxt>,Local> pair : res1) {
			Local[] methArgs = SootUtilities.getMethArgLocals(domV.getMethod(pair.val1));
			if (!(Arrays.asList(methArgs).contains(pair.val1))) continue;

			String label = pair.val0.val0;
			Local var = pair.val1;

			Map<SootMethod,Set<Local>> taintedVars = labelToTaintedVars.get(label);
			if(taintedVars == null){
				taintedVars = new HashMap();
				labelToTaintedVars.put(label, taintedVars);
			}
			
			SootMethod meth = domV.getMethod(var);
			Set<Local> vars = taintedVars.get(meth);
			if(vars == null){
				vars = new HashSet();
				taintedVars.put(meth, vars);
			}
			vars.add(var);
		}
		relRef.close();

		final ProgramRel relPrim = (ProgramRel) ClassicProject.g().getTrgt("out_taintedPrimVar");
		relPrim.load();

		Iterable<Pair<Pair<String,Ctxt>,Local>> res2 = relPrim.getAry2ValTuples();
		for(Pair<Pair<String,Ctxt>,Local> pair : res2) {
			Local[] methArgs = SootUtilities.getMethArgLocals(domV.getMethod(pair.val1));
			if (!(Arrays.asList(methArgs).contains(pair.val1))) continue;

			String label = pair.val0.val0;
			Local var = pair.val1;

			Map<SootMethod,Set<Local>> taintedVars = labelToTaintedVars.get(label);
			if(taintedVars == null){
				taintedVars = new HashMap();
				labelToTaintedVars.put(label, taintedVars);
			}
			
			SootMethod meth = domV.getMethod(var);
			Set<Local> vars = taintedVars.get(meth);
			if(vars == null){
				vars = new HashSet();
				taintedVars.put(meth, vars);
			}
			vars.add(var);
		}
		relPrim.close();

		return labelToTaintedVars;
	}

	private static void initBuckets(int numVars)
	{
		final ProgramRel relCtxtFlows = (ProgramRel)ClassicProject.g().getTrgt("flow");
		relCtxtFlows.load();

		Iterable<Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>> res = relCtxtFlows.getAry2ValTuples();
		int flowNum = 0;
		for(Pair<Pair<String,Ctxt>,Pair<String,Ctxt>> flow : res) {
			flowNum++;
			Pair<String,Ctxt> srcLabel = flow.val0;
			Pair<String,Ctxt> sinkLabel = flow.val1;
			
			List<Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>> flows = labelToFlows.get(srcLabel);
			List<Integer> flowNums = labelToFlowNums.get(srcLabel);
			if(flows == null){
				flows = new ArrayList();
				flowNums = new ArrayList();
				labelToFlows.put(srcLabel, flows);
				labelToFlowNums.put(srcLabel, flowNums);
			}
			flows.add(flow);
			if (!flowNums.contains(flowNum))
				flowNums.add(flowNum);

			flows = labelToFlows.get(sinkLabel);
			flowNums = labelToFlowNums.get(sinkLabel);
			if(flows == null){
				flows = new ArrayList();
				flowNums = new ArrayList();
				labelToFlows.put(sinkLabel, flows);
				labelToFlowNums.put(sinkLabel, flowNums);
			}
			flows.add(flow);			
			if (!flowNums.contains(flowNum))
				flowNums.add(flowNum);			
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

		Iterable<Pair<Ctxt,Local>> iter = taintedVars.getAry2ValTuples();
		for(Pair<Ctxt,Local> pair : iter){
			varNumberer.add(pair);
		}
	}

	private static void fillBuckets(MapNumberer taintedVarNumberer, ProgramRel rel)
	{
		Iterable<Trio<Ctxt,Local,Pair<String,Ctxt>>> iter = rel.getAry3ValTuples();
		for(Trio<Ctxt,Local,Pair<String,Ctxt>> trio : iter) {
			Ctxt ctxt = trio.val0;
			Local var = trio.val1;
			Pair<String,Ctxt> label = trio.val2;

			List<Pair<Pair<String,Ctxt>,Pair<String,Ctxt>>> flows = labelToFlows.get(label);
			List<Integer> flowNums = labelToFlowNums.get(label);
			if(flows == null)
				continue;
			String str = "";
			if (!varToFlows.containsKey(var)) {
				varToFlows.put(var, str);
			}
			for (int n : flowNums) {
				str = str + ":" + n;
			}
			str += ":";
			if (!varToFlows.get(var).contains(str))
				varToFlows.put(var, varToFlows.get(var)+str);
		}
	}
}
