package petablox.analyses.alloc;

import java.util.List;
import java.util.Iterator;

import soot.RefLikeType;
import soot.SootMethod;
import soot.Unit;
import soot.Type;
import soot.Value;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNewMultiArrayExpr;
import soot.jimple.internal.JAssignStmt;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.Block;
import petablox.analyses.method.DomM;
import petablox.program.PhantomClsVal;
import petablox.program.Program;
import petablox.program.Reflect;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.ProgramDom;
import petablox.util.Utils;
import petablox.util.soot.ICFG;
import petablox.util.soot.SootUtilities;
import petablox.util.tuple.object.Pair;

/**
 * Domain of object allocation quads.
 * <p>        
 * The 0th element of this domain is null and denotes a distinguished hypothetical
 * object allocation quad that may be used for various purposes.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "H", consumes = { "M" })
public class DomH extends ProgramDom<Object> {
    protected DomM domM;
    protected int lastA;
    protected int lastI;
    private static boolean PHANTOM_CLASSES = true;

    public int getLastA() {
        return lastA;
    }

    public int getLastI() {
        return lastI;
    }
    
    public void setLastA(int lastA) {
        this.lastA = lastA;
    }

    public void setLastI(int lastI) {
        this.lastI = lastI;
    }

    @Override
    public void init() {
        domM = (DomM) (Config.classic ? ClassicProject.g().getTrgt("M") : consumes[0]);
        PHANTOM_CLASSES = Utils.buildBoolProperty("petablox.add.phantom.classes", false);
    }

    @Override
    public void fill() {
        int numM = domM.size();
        add(null);    
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            SootMethod m = domM.get(mIdx);
            if (m == null || !m.isConcrete())
                continue;
            ICFG cfg = SootUtilities.getCFG(m);
            for (Block bb : cfg.reversePostOrder()) {
            	Iterator<Unit> uit = bb.iterator();
            	while(uit.hasNext()){
            		Unit u = uit.next();
            		if(u instanceof JAssignStmt){
                    	JAssignStmt as = (JAssignStmt) u;
                    	if (SootUtilities.isNewStmt(as) || SootUtilities.isNewArrayStmt(as) || SootUtilities.isNewMultiArrayStmt(as)) 
                            add(u);
            		}	
            	}    
            }
        }

        lastA = size() - 1;
        Reflect reflect = Program.g().getReflect();
        processResolvedNewInstSites(reflect.getResolvedObjNewInstSites());
        processResolvedNewInstSites(reflect.getResolvedConNewInstSites());
        processResolvedNewInstSites(reflect.getResolvedAryNewInstSites());
        lastI = size() - 1;
        if (PHANTOM_CLASSES) {
            for (RefLikeType r : Program.g().getClasses()) {
                add(new PhantomClsVal(r));
            }
        }
    }

    private void processResolvedNewInstSites(List<Pair<Unit, List<RefLikeType>>> l) {
        for (Pair<Unit, List<RefLikeType>> p : l)
            add(p.val0);
    }

    public static Type getType(Unit u) {                                 
        Type t=null;
        if(u instanceof JAssignStmt){
        	JAssignStmt as = (JAssignStmt) u;
        	Value right=as.rightBox.getValue();
        	if (SootUtilities.isNewStmt(as)) 
                t = ((JNewExpr)right).getType();
            else if (SootUtilities.isNewArrayStmt(as)) 
            	t = ((JNewArrayExpr)right).getType();
            else if (SootUtilities.isNewMultiArrayStmt(as))
            	t = ((JNewMultiArrayExpr)right).getType();
        }
        else {
            assert (SootUtilities.isInvoke(u));
            t = null;
        }
        return t;
    }

    @Override
    public String toUniqueString(Object o) {
        if (o instanceof Unit) {
            Unit u = (Unit) o;
            return SootUtilities.toByteLocStr(u);
        }
        if (o instanceof PhantomClsVal) {
            RefLikeType r = ((PhantomClsVal) o).r;
            return r.toString() + "@phantom_cls";
        }
        assert (o == null);
        return "null";
    }

    @Override
    public String toFIString(Object o) {
    	StringBuilder sb = new StringBuilder();
    	Unit u = (Unit)o;
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if (printId) sb.append("(" + indexOf(u) + ")");
    	Type t = getType(u);
    	if (t == null)
    		sb.append("null");
    	else if (t instanceof RefLikeType)
    		sb.append(t.toString());
    	else
    		sb.append("primitive");
    	sb.append(":" + SootUtilities.getMethod(u).getName() + "@" + SootUtilities.getMethod(u).getDeclaringClass().getName());
    	return sb.toString();
    }
    
    @Override
    public String toXMLAttrsString(Object o) {
        if (o instanceof Unit) {
            Unit u = (Unit) o;
            Type t = getType(u);
            String type = (t != null) ? t.toString() : "null";
            SootMethod m = SootUtilities.getMethod(u);            
            //String file = ((SourceFileTag)m.getDeclaringClass().getTags().get(0)).getSourceFile();
            List<Tag> tags = m.getDeclaringClass().getTags();
            String file = null;
            for(Tag x : tags) {
            	if( x instanceof SourceFileTag) {
            		file = ((SourceFileTag)x).getSourceFile();
            		break;
            	}
            }
            int line = -1; // ((LineNumberTag)u.getTag("LineNumberTag")).getLineNumber();
            Tag tg = u.getTag("LineNumberTag");
            if(tg != null && tg instanceof LineNumberTag) {
            	line = ((LineNumberTag) tg).getLineNumber();
            }
            int mIdx = domM.indexOf(m);
            return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
            "Mid=\"M" + mIdx + "\"" + " type=\"" + type + "\"";
        }
        return "";
    }
}
