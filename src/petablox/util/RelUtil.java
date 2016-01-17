package petablox.util;

import petablox.analyses.alloc.DomH;
import petablox.analyses.argret.DomK;
import petablox.analyses.invk.DomI;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;

/** Use by saying {@code import static petablox.util.RelUtil.*;}. */
public final class RelUtil {
    private RelUtil() { /* no instance */ }
	public static ProgramRel pRel(String name) { return (ProgramRel) ClassicProject.g().getTrgt(name); }
	public static DomI domI() { return (DomI) ClassicProject.g().getTrgt("I"); }
	public static DomK domK() { return (DomK) ClassicProject.g().getTrgt("K"); }
	public static DomH domH() { return (DomH) ClassicProject.g().getTrgt("H"); }
}
