package petablox.reporting;

import petablox.program.Program;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

import petablox.util.tuple.object.Pair;

import petablox.android.srcmap.SyntheticMethodMap;
import petablox.android.srcmap.sourceinfo.SourceInfo;
import petablox.util.soot.SootUtilities;

import java.util.*;

/**
 * @author Saswat Anand
 * @author Osbert Bastani
 */
public class IM extends XMLReport
{
    public IM()
	{
		super("Invocation instruction to callees");
    }

    public void generate()
	{
        final ProgramRel relIM = (ProgramRel) ClassicProject.g().getTrgt("chaIM");
		relIM.load();

		Map<SootMethod,SootMethod> synthToSrcMethod = new HashMap();
		Set<SootClass> processedClasses = new HashSet();

		Iterable<Pair<Unit,SootMethod>> res = relIM.getAry2ValTuples();
		for(Pair<Unit,SootMethod> pair : res) {
			Stmt stmt = (Stmt) pair.val0;

			//if(SourceInfo.isSyntheticMethod(quad.getMethod()))
			//	continue;

			SootMethod callee = pair.val1;

			SootClass klass = callee.getDeclaringClass();
			if(!processedClasses.contains(klass)){
				SyntheticMethodMap.computeSyntheticToSrcMethodMap(this.sourceInfo, klass, synthToSrcMethod);
				processedClasses.add(klass);
			}

			SootMethod srcMeth = synthToSrcMethod.get(callee);
			if(srcMeth != null)
				callee = srcMeth;

			Tuple tuple = makeOrGetPkgCat(callee).newTuple();
			String invkExpr = this.sourceInfo.srcInvkExprFor(stmt);
			if(invkExpr != null)
				invkExpr = this.sourceInfo.javaLocStr(stmt) + "\n" + invkExpr;
			else
				invkExpr = this.sourceInfo.javaLocStr(stmt);

			tuple.addValueWithSig(invkExpr,
								  SootUtilities.getMethod(stmt).getDeclaringClass(),
								  String.valueOf(this.sourceInfo.stmtLineNum(stmt)),
								  "invk",
								  this.sourceInfo.chordSigFor(stmt.getInvokeExpr().getMethod()));
		}

		relIM.close();
    }
}
