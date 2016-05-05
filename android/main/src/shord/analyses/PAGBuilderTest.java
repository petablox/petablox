package shord.analyses;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootField;
import soot.Local;
import soot.Immediate;
import soot.Value;
import soot.Unit;
import soot.Body;
import soot.MethodOrMethodContext;
import java.util.*;
import soot.Type;
import soot.RefLikeType;
import soot.RefType;
import soot.PrimType;
import soot.VoidType;
import soot.NullType;
import soot.AnySubType;
import soot.UnknownType;
import soot.FastHierarchy;

import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;


import soot.PatchingChain;
import soot.jimple.Constant;
import soot.jimple.Stmt;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.AnyNewExpr;
import soot.jimple.ThrowStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.CastExpr;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;
import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.NegExpr;
import soot.jimple.NewExpr;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.pag.ArrayElement;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.tagkit.Tag;
import soot.util.NumberedSet;
import petablox.android.missingmodels.jimplesrcmapper.Printer;

import petablox.project.analyses.JavaAnalysis;           //PRTno change
import petablox.project.analyses.ProgramRel;			//PRT no change
import petablox.util.IndexSet;
import petablox.project.analyses.ProgramDom;				//PRT no change
import petablox.project.ClassicProject;							//PRT no change
import petablox.analyses.alias.CICGAnalysis;
import petablox.analyses.alias.ICICG;
import petablox.program.Program;							

import petablox.project.Petablox;
//import shord.project.Config;
//import stamp.analyses.method.DomM;               //PRT

import java.util.*;

@Petablox(name="test-java", 
	   produces={
				 "chaIM"
				}  ,
             
  		namesOfSigns = {    "chaIM"
							},

				signs = {   "I0,M0:I0_M0",}  
		   )
public class PAGBuilderTest extends JavaAnalysis
{
	
	
//PRT use relIM instead
//	void populateCallgraph()
//	{	//ICICG cg = new CICGAnalysis().getCallGraph(); 
//		CallGraph cg = Scene.v().getCallGraph();
//		ProgramRel relChaIM = (ProgramRel) ClassicProject.g().getTrgt("chaIM");
//        relChaIM.zero();
//		Iterator<Edge> edgeIt = cg.listener();
//		while(edgeIt.hasNext()){
//			Edge edge = edgeIt.next();
//			if(!edge.isExplicit())
//				continue;
//			Stmt stmt = edge.srcStmt();
//			//int stmtIdx = domI.getOrAdd(stmt);
//			SootMethod tgt = (SootMethod) edge.tgt();
//			SootMethod src = (SootMethod) edge.src();
//			if(tgt.isAbstract())
//				assert false : "tgt = "+tgt +" "+tgt.isAbstract();
//			if(tgt.isPhantom())
//				continue;
//			//System.out.println("stmt: "+stmt+" tgt: "+tgt+ "abstract: "+ tgt.isAbstract());
//			if(ignoreStubs){
//				if(stubMethods.contains(tgt) || (src != null && stubMethods.contains(src)))
//					continue;
//			}
//			relChaIM.add(stmt, tgt);
//		}
//		relChaIM.save();
//	}


	public void run()
	{	CallGraphBuilder cgb = new CallGraphBuilder(DumbPointerAnalysis.v());
		cgb.build();
		
		CallGraph cg = Scene.v().getCallGraph();
		
		SootMethod main = Program.g().getMainMethod();
		Set<MethodOrMethodContext> start = new HashSet<MethodOrMethodContext>();
		start.add((MethodOrMethodContext)main);
		ReachableMethods rm = new ReachableMethods(cg,start);
		//ReachableMethods rm = new ReachableMethods(cg,Collections.<MethodOrMethodContext>singletonList(main));
		rm.update();
		System.out.println("PRT rm size: "+rm.size());
		System.out.println("PRT main method: "+ main);
		System.out.println("PRT CG size: "+ cg.size() +"\n");
		System.out.println("PRT CG: "+ cg.toString());
		//populateCallgraph();
		
		
	}
}