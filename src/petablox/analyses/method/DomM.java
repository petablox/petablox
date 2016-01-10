package petablox.analyses.method;

import soot.SootClass;
import soot.SootMethod;
import soot.tagkit.SourceFileTag;
import petablox.program.Program;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;
import petablox.util.Utils;

/**
 * Domain of methods.
 * <p>
 * The 0th element in this domain is the main method of the program.
 * <p>
 * The 1st element in this domain is the <tt>start()</tt> method of class <tt>java.lang.Thread</tt>,
 * if this method is reachable from the main method of the program.
 * <p>
 * The above two methods are the entry-point methods of the implicitly created main thread and each
 * explicitly created thread, respectively.  Due to Chord's emphasis on concurrency, these methods
 * are referenced frequently by various pre-defined program analyses expressed in Datalog, and giving
 * them special indices makes it convenient to reference them in those analyses.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "M")
public class DomM extends ProgramDom<SootMethod> implements IMethodVisitor {
    @Override
    public void init() {
        // Reserve index 0 for the main method of the program.
        // Reserve index 1 for the start() method of java.lang.Thread if it exists.
        Program program = Program.g();
        SootMethod mainMethod = program.getMainMethod();
        assert (mainMethod != null);
        getOrAdd(mainMethod);
        SootMethod startMethod = program.getThreadStartMethod();
        if (startMethod != null)
            getOrAdd(startMethod);
    }

    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        getOrAdd(m);
    }

    @Override
    public String toFIString(SootMethod m) {
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if(printId) sb.append("(" + indexOf(m) +")");
    	sb.append(m.getName() + "@" + m.getDeclaringClass().getName());
    	return sb.toString();
    }
    
    @Override
    public String toXMLAttrsString(SootMethod m) {
        SootClass c = m.getDeclaringClass();
        String methName = m.getName().toString();
        String sign = c.getName() + ".";
        if (methName.equals("<init>"))
            sign += "&lt;init&gt;";
        else if (methName.equals("<clinit>"))
            sign += "&lt;clinit&gt;";
        else
            sign += methName;
        String desc = m.getBytecodeParms().toString();
        String args = desc.substring(1, desc.indexOf(')'));
        sign += "(" + Program.typesToStr(args) + ")";
        String file = ((SourceFileTag)c.getTags().get(0)).getSourceFile();
        int line = m.getNumber(); 
        return "sign=\"" + sign + "\" file=\"" + file + "\" line=\"" + line + "\"";
    }
}
