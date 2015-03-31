package chord.analyses.provenance.typestate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import CnCHJ.runtime.Collection;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeInterface;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.CIPAAnalysis;
import chord.analyses.alias.ICICG;
import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * @author Ravi Mangal
 */
@Chord(
    name = "allTypestateQueries",
	consumes = { "checkIncludedI","initTrackedH"},
    sign = "P0,H0:P0_H0"
)
public class RelAllTypestateQueries extends ProgramRel {
	CIPAAnalysis cipa;
	CICGAnalysis cicgAnalysis;
	ICICG cicg;
	ProgramRel relInitTrackedH;
	ProgramRel relCheckIncludedI;
	
    @Override
    public void fill() {
    	
    	String cipaName = System.getProperty("chord.typestate.cipa", "cipa-java");
        cipa = (CIPAAnalysis) ClassicProject.g().getTask(cipaName);
        ClassicProject.g().runTask(cipa);
        
        cicgAnalysis = (CICGAnalysis) ClassicProject.g().getTask("cicg-java");
		ClassicProject.g().runTask(cicgAnalysis);
		cicg = cicgAnalysis.getCallGraph();
        
        relInitTrackedH = (ProgramRel) ClassicProject.g().getTrgt("initTrackedH");
        relInitTrackedH.load();
        
        relCheckIncludedI = (ProgramRel) ClassicProject.g().getTrgt("checkIncludedI");
        relCheckIncludedI.load();
        Iterable<Quad> tuples = relCheckIncludedI.getAry1ValTuples();
		for (Quad q : tuples) {
			if ((q.getOperator() instanceof InvokeVirtual 
					|| q.getOperator() instanceof InvokeInterface)) {
				Register v = Invoke.getParam(q, 0).getRegister();
				for (Quad h: cipa.pointsTo(v).pts) {
					if (relInitTrackedH.contains(h)) {
						for (jq_Method m : cicg.getTargets(q)) {
							if (isInterestingMethod(m, h, q)) {
								add(q, h);
								continue;
							}
						}
					}
				}
			}
		}
		relCheckIncludedI.close();
		relInitTrackedH.close();
    }
    
    public boolean isInterestingMethod(jq_Method m, Quad allocSite, Quad invoke) {
		if (m.isStatic() || m.toString().equals("<init>:()V@java.lang.Object"))
			return false;
		if (relCheckIncludedI.contains(invoke)) {
			return true;
		}
		return false;
	}
}
