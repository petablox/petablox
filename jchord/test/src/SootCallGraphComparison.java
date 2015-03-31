import chord.analyses.alias.*;
import chord.project.*;
import chord.program.Program;
import chord.project.analyses.JavaAnalysis;
import chord.bddbddb.Rel.TrioIterable;
import chord.project.analyses.ProgramRel;
import chord.project.Config;
import chord.util.tuple.object.Trio;
import java.io.*;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import java.util.*;

import soot.*;
import soot.util.queue.*;
import soot.jimple.toolkits.callgraph.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import soot.jimple.*;
import soot.tagkit.*;

@Chord(name="soot-cg-java", consumes={"MIM"})
public class SootCallGraphComparison extends JavaAnalysis {
	private Map<String, Set<Tgt>> ccg;
	private Map<String, Set<Tgt>> scg;
	private Set<String> visitedMethods;
	@Override
	public void run() {
		buildChordCG();
		buildSootCG();
		compare();
	}
	private void compare() {
		visitedMethods = new HashSet<String>();
		String main = mstr(Program.g().getMainMethod());
		visit(main);
	}
	private void visit(String m) {
		if (visitedMethods.add(m)) {
			Set<Tgt> c = ccg.get(m);
			Set<Tgt> s = scg.get(m);
			if (c == null) c = Collections.EMPTY_SET;
			if (s == null) s = Collections.EMPTY_SET;
			boolean printed = false;
			List<String> commonMethods = new ArrayList<String>();
			for (Tgt tgt : c) {
				String m2 = tgt.meth;
				if (occurs(m2, s))
					commonMethods.add(m2);
				else {
					if (!printed) {
						System.out.println("*** " + m);
						printed = true;
					}
					System.out.println("\tONLY IN C: " + tgt);
				}
			}
			for (Tgt tgt : s) {
				String m2 = tgt.meth;
				if (!occurs(m2, c)) {
					if (!printed) {
						System.out.println("*** " + m);
						printed = true;
					}
					System.out.println("\tONLY IN S: " + tgt);
				}
			}
			for (String m2 : commonMethods)
				visit(m2);
		}
	}
	private static boolean occurs(String m, Set<Tgt> s) {
		for (Tgt tgt : s) {
			if (tgt.meth.equals(m))
				return true;
		}
		return false;
	}
	private void buildChordCG() {
		ccg = new HashMap<String, Set<Tgt>>();
		ProgramRel r = (ProgramRel) ClassicProject.g().getTrgt("MIM");
		r.load();
		try {
			TrioIterable<jq_Method, Quad, jq_Method> tuples = r.getAry3ValTuples();
			for (Trio<jq_Method, Quad, jq_Method> t : tuples) {
				String m1s = mstr(t.val0);
				int bci = t.val1.getBCI();
				String m2s = mstr(t.val2);
				Set<Tgt> set = ccg.get(m1s);
				if (set == null) {
					set = new HashSet<Tgt>(2);
					ccg.put(m1s, set);
				}
				set.add(new Tgt(bci, m2s));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		r.close();
	}
	private void buildSootCG() {
		scg = new HashMap<String, Set<Tgt>>();
		String[] args = new String[] { "-keep-offset",  "-w",  "-p",  "cg.spark", "enabled", Config.mainClassName };
        soot.Main.main(args);
        CallGraph cg = Scene.v().getCallGraph();
        for (Iterator<MethodOrMethodContext> it = cg.sourceMethods(); it.hasNext();) {
            MethodOrMethodContext m1 = it.next();
            String m1s = mstr(m1.method());
            Set<Tgt> set = new HashSet<Tgt>(2);
            scg.put(m1s, set);
            for (Iterator<Edge> it2 = cg.edgesOutOf(m1); it2.hasNext();) {
                Edge e = it2.next();
				SootMethod m2 = e.tgt();
				if (ignore(m2))
					continue;
                Stmt stmt = e.srcStmt();
                int bci = -1;
                if (stmt != null) {
                    BytecodeOffsetTag t = (BytecodeOffsetTag) stmt.getTag("BytecodeOffsetTag");
                    if (t != null)
                        bci = t.getBytecodeOffset();
                }
				String m2s = mstr(m2);
				// System.out.println(m1s + " -> " + bci + ": " + m2s);
                set.add(new Tgt(bci, m2s));
            }
        }
	}
	private static boolean ignore(SootMethod m) {
		String name = m.getName();
		return name.equals("<clinit>") ||
			(name.equals("println") && m.getDeclaringClass().getName().equals("java.io.PrintStream"));
	}
	private static String mstr(jq_Method m) {
		return m.toString();
	}
    private static String mstr(SootMethod m) {
        String desc = AbstractJasminClass.jasminDescriptorOf(m.makeRef());
        return m.getName() + ":" + desc + "@" + m.getDeclaringClass().getName();
	}
}

class Tgt {
	int bci;
	String meth;
	public Tgt(int b, String m) {
		bci = b;
		meth = m;
	}
	@Override
	public int hashCode() {
		return meth.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof Tgt) {
			Tgt t = (Tgt) o;
			return t.bci == this.bci && t.meth.equals(this.meth);
		}
		return false;
	}
	@Override
	public String toString() {
		return bci + ": " + meth;
	}
}

