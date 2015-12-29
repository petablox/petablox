package petablox.analyses.provenance.kobj;

import petablox.analyses.alloc.DomH;
import petablox.analyses.argret.DomK;
import petablox.analyses.invk.DomI;
import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;

/**
 * The class that produces relations for SimpleCtxtsAnalysis. It can
 * only generate the relations for a fixed k values for all invocation
 * sites or allocation sites. So it shouldn't be used in any refinement
 * based algorithm.
 * @author xin
 *
 */
@Petablox(name = "kobj-cmp-java")
public class KObjComp extends JavaAnalysis {
	DomI domI;
	DomK domK;
	DomH domH;
	@Override
	public void run() {
		System.setProperty("chord.ctxt.kind", "co");
		System.setProperty("chord.kobj.k","1");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domK = (DomK) ClassicProject.g().getTrgt("K");
		ClassicProject.g().runTask(domI);
		ClassicProject.g().runTask(domK);
		
		ProgramRel ikRel = (ProgramRel) ClassicProject.g().getTrgt("IK");
		ProgramRel allowI = (ProgramRel)ClassicProject.g().getTrgt("AllowI");
		ProgramRel denyI = (ProgramRel)ClassicProject.g().getTrgt("DenyI");
		ikRel.zero();
		allowI.zero();
		denyI.zero();
		int k = Integer.getInteger("chord.kcfa.k", 0);
		int max = 20;
		for (int i = 0; i < domI.size(); i++) {
			ikRel.add(domI.get(i), k);
			for(int j = 1; j <=k; j++){
				allowI.add(domI.get(i), j);
			}
			for(int j = k+1; j <= max;j ++)
				denyI.add(domI.get(i),j);
		}
		ikRel.save();
		allowI.save();
		denyI.save();
		
		ProgramRel HKRel = (ProgramRel) ClassicProject.g().getTrgt("HK");
		domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);
		ProgramRel allowH = (ProgramRel)ClassicProject.g().getTrgt("AllowH");
		ProgramRel denyH = (ProgramRel)ClassicProject.g().getTrgt("DenyH");
		HKRel.zero();
		allowH.zero();
		denyH.zero();
		k = Integer.getInteger("chord.kobj.k", 1);
		for (int i = 0; i < domH.size(); i++) {
			HKRel.add(domH.get(i), k);
			for(int j = 1; j <=k; j++){
				allowH.add(domH.get(i), j);
			}
			for(int j = k+1; j <= max;j ++)
				denyH.add(domH.get(i),j);
		}
		HKRel.save();
		allowH.save();
		denyH.save();
		System.out.println("First, the original k-obj");
		ClassicProject.g().runTask("cipa-0cfa-dlog");
		ClassicProject.g().runTask("ctxts-java");
		ClassicProject.g().runTask("argCopy-dlog");
		ClassicProject.g().runTask("cspa-kobj-dlog");		
		
		System.out.println("Then, the new bit version k-obj");
		ClassicProject.g().runTask("cipa-0cfa-dlog");
		ClassicProject.g().runTask("simple-pro-ctxts-java");
		ClassicProject.g().runTask("pro-argCopy-dlog");
		ClassicProject.g().runTask("kobj-bit-init-dlog_XZ89_");
		ClassicProject.g().runTask("pro-cspa-kobj-dlog_XZ89_");
	}

}
