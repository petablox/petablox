package chord.analyses.type;

import soot.NullType;
import soot.Type;
import soot.SootClass;
import soot.RefLikeType;
import soot.RefType;
import soot.Unit;
import soot.tagkit.SourceFileTag;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;
import chord.util.IndexSet;
import chord.util.Utils;

/**
 * Domain of classes.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "T")
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
    	boolean printId = Utils.buildBoolProperty("chord.printrel.printID", false);
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
            file = ((SourceFileTag)c.getTags().get(0)).getSourceFile();                      
        } else
            file = "";
        int line = 0;  // TODO
        return "name=\"" + name + "\" file=\"" + file + "\" line=\"" + line + "\"";
    }
}
