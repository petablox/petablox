package chord.analyses.provenance.kcfa;

import chord.analyses.alloc.DomH;
import chord.analyses.argret.DomK;
import chord.analyses.invk.DomI;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

/**
 * The class that produces relations for SimpleCtxtsAnalysis. It can
 * only generate the relations for a fixed k values for all invocation
 * sites or allocation sites. So it shouldn't be used in any refinement
 * based algorithm.
 * @author xin
 *
 */
@Chord(name = "HIDumper-java", produces = { "HK", "IK", "OK", "AllowI", "DenyI", "AllowH", "DenyH",
		"AllowO", "DenyO"})
public class HIDumper extends JavaAnalysis {
	DomI domI;
	DomK domK;
	DomH domH;
	@Override
	public void run() {
		System.setProperty("chord.ctxt.kind", "co");
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
		k = Integer.getInteger("chord.kheap.k", 1);
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
		
		ProgramRel OKRel = (ProgramRel) ClassicProject.g().getTrgt("OK");
		domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);
		ProgramRel allowO = (ProgramRel)ClassicProject.g().getTrgt("AllowO");
		ProgramRel denyO = (ProgramRel)ClassicProject.g().getTrgt("DenyO");
		OKRel.zero();
		allowO.zero();
		denyO.zero();
		k = Integer.getInteger("chord.kobj.k", 0);
		for (int i = 0; i < domH.size(); i++) {
			OKRel.add(domH.get(i), k);
			for(int j = 1; j <=k; j++){
				allowO.add(domH.get(i), j);
			}
			for(int j = k+1; j <= max;j ++)
				denyO.add(domH.get(i),j);
		}
		OKRel.save();
		allowO.save();
		denyO.save();
	}

}
