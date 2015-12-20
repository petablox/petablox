package petablox.analyses.point;

import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;

import java.util.Iterator;

import petablox.analyses.method.DomM;
import petablox.analyses.point.DomP;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.CFG;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (p,m) such that the statement
 * at program point p is contained in method m
 */
@Petablox(
	    name = "PM",
	    sign = "P0,M0:P0_M0" 
	)
public class RelPM extends ProgramRel {
	private DomP domP;
	private DomM domM;
	
	public void fill() {
		domP = (DomP) doms[0];
		domM = (DomM) doms[1];
		for (SootMethod m : Program.g().getMethods()) {
			if(!m.isConcrete())
				continue;
			CFG cfg = SootUtilities.getCFG(m);
			for(Block bb : cfg.getBlocks()){
				Iterator<Unit> itr = bb.iterator();
				while(itr.hasNext()){
					Unit u = itr.next();
					int mIdx = domM.indexOf(m);
		        	int pIdx = domP.indexOf(u);
		        	if(mIdx == -1 || pIdx == -1){
		        		System.out.println("WARN: Index not found! Unit: "+u+" Method:"+m+" Class:"+m.getDeclaringClass().getName());
		        	}else{
		        		add (pIdx, mIdx);
		        	}
				}
			}
		}
	}	
}
