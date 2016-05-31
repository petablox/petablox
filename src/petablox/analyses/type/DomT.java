package petablox.analyses.type;

import soot.NullType;
import soot.Type;
import soot.SootClass;
import soot.RefLikeType;
import soot.RefType;
import soot.Unit;
import soot.tagkit.SourceFileTag;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;
import petablox.util.IndexSet;
import petablox.util.Utils;
import petablox.util.soot.SootUtilities;

/**
 * Domain of classes.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "T")
public class DomT extends ProgramDom<Type> {
    @Override
    public void fill() {
        Program program = Program.g();
        IndexSet<Type> types = program.getTypes();
        for (Type t : types)
            add(t);
    }

    @Override
    public String toFIString(Type t){
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if(printId) sb.append("(" + indexOf(t) + ")");
    	if (t == null || t instanceof NullType)
    		sb.append("null");
    	else if (t instanceof RefLikeType)
    		sb.append(t.toString());
    	else
    		sb.append("primitive");
    	return sb.toString();
    }
    
    @Override
    public String toXMLAttrsString(Type t) {
        String name = t.toString();
        String file;
        if (t instanceof RefType) {
            SootClass c = ((RefType)t).getSootClass();
            file = SootUtilities.getSourceFile(c);
        } else
            file = "";
        int line = 0;  // TODO
        return "name=\"" + name + "\" file=\"" + file + "\" line=\"" + line + "\"";
    }
}
