package chord.util;

import chord.analyses.alloc.DomH;
import chord.analyses.argret.DomK;
import chord.analyses.invk.DomI;
import chord.project.analyses.ProgramRel;
import chord.project.ClassicProject;

/** Use by saying {@code import static chord.util.RelUtil.*;}. */
public final class RelUtil {
    private RelUtil() { /* no instance */ }
	public static ProgramRel pRel(String name) { return (ProgramRel) ClassicProject.g().getTrgt(name); }
	public static DomI domI() { return (DomI) ClassicProject.g().getTrgt("I"); }
	public static DomK domK() { return (DomK) ClassicProject.g().getTrgt("K"); }
	public static DomH domH() { return (DomH) ClassicProject.g().getTrgt("H"); }
}
