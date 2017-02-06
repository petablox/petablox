package petablox.android.srcmap.sourceinfo;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

import soot.SootClass;
import soot.SootMethod;
import soot.SootField;
import soot.Type;
import soot.AbstractJasminClass;
import soot.jimple.Stmt;
import soot.tagkit.Tag;
import soot.tagkit.SourceFileTag;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceLineNumberTag;

import petablox.android.analyses.ContainerTag;
import petablox.program.Program;

/**
 * @author Osbert Bastani
 */
public interface SourceInfo {
	public abstract String filePath(SootClass klass);
	public abstract String javaLocStr(Stmt stmt);
    public abstract String srcClassName(Stmt stmt);
	public abstract int classLineNum(SootClass klass);
    public abstract int methodLineNum(SootMethod meth);
	public abstract int stmtLineNum(Stmt s);
    public abstract RegisterMap buildRegMapFor(SootMethod meth);
	public abstract String chordSigFor(SootMethod m);
	public abstract String chordSigFor(SootField f);
	public abstract String chordTypeFor(Type type);
	public abstract boolean hasSrcFile(String srcFileName);
	public abstract Map<String,List<String>> allAliasSigs(SootClass klass);
	public abstract File srcMapFile(String srcFileName);
	public abstract String srcInvkExprFor(Stmt invkQuad);
	public abstract String srcClassName(SootClass declKlass);
}
