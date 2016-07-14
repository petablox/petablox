package stamp.reporting;

import shord.program.Program;
import shord.project.ClassicProject;
import shord.project.analyses.ProgramRel;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

import chord.util.tuple.object.Pair;

import stamp.srcmap.SyntheticMethodMap;
import stamp.srcmap.sourceinfo.SourceInfo;

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
								  Program.containerMethod(stmt).getDeclaringClass(),
								  String.valueOf(this.sourceInfo.stmtLineNum(stmt)),
								  "invk",
								  this.sourceInfo.chordSigFor(stmt.getInvokeExpr().getMethod()));
		}

		relIM.close();
    }
}
