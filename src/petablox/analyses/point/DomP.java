package petablox.analyses.point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JBreakpointStmt;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.internal.JNopStmt;
import soot.jimple.internal.JRetStmt;
import soot.jimple.internal.JTableSwitchStmt;
import petablox.analyses.method.DomM;
import petablox.logicblox.LogicBloxAnnotExporter;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.ProgramDom;
import petablox.util.soot.ICFG;
import petablox.util.soot.SootUtilities;
import petablox.util.tuple.object.Pair;
import petablox.util.tuple.object.Trio;
import petablox.util.Utils;

/**
 * Domain of quads.
 * <p>
 * The 0th element in this domain is the unique entry basic block of the main method of the program.
 * <p>
 * The quads of each method in the program are assigned contiguous indices in this domain, with the
 * unique basic blocks at the entry and exit of each method being assigned the smallest and largest
 * indices, respectively, of all indices assigned to quads in that method.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "P", consumes = { "M" })
public class DomP extends ProgramDom<Unit> {
    protected DomM domM;
    protected Map<Unit, SootMethod> unitToMethodMap;

    public SootMethod getMethod(Unit u) {
        return unitToMethodMap.get(u);
    }
    
    @Override
    public void init() {
        domM = (DomM) (Config.classic ? ClassicProject.g().getTrgt("M") : consumes[0]);
        unitToMethodMap = new HashMap<Unit, SootMethod>();
    }
    
    @Override
    public void fill() {
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            SootMethod m = domM.get(mIdx);
            if (m == null || m.isAbstract())
                continue;
            ICFG cfg = SootUtilities.getCFG(m);
            for (Block bb : cfg.reversePostOrder()) {
                Iterator<Unit> uit = bb.iterator();
                while(uit.hasNext()){
                    Unit u = uit.next();
                    int indx = getOrAdd(u);
                    parseAnnotations(u, indx);
                    unitToMethodMap.put(u, m);
                }  
            }
        }
    }

    @Override
    public String toUniqueString(Unit u) {
        String x = Integer.toString(SootUtilities.getBCI((Unit) u));
        return x + "!" + getMethod(u);
    }
    
    @Override
    public String toFIString(Unit u) {		 
        StringBuilder sb = new StringBuilder();
        boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
        if (printId) sb.append("(" + indexOf(u) + ")");
        String type;
        if(u instanceof JAssignStmt)
            type = "Assign";
        else if(u instanceof JBreakpointStmt)
            type = "Breakpoint";
        else if(u instanceof JGotoStmt)
            type = "Goto";
        else if(u instanceof JIfStmt) 
            type = "If";
        else if(u instanceof JIdentityStmt) 
            type = "Identity";
        else if(u instanceof JInvokeStmt) 
            type = "Invoke";
        else if(u instanceof JLookupSwitchStmt) 
            type = "LookupSwitch";
        else if(u instanceof JNopStmt)
            type = "Nop";
        else if(u instanceof JRetStmt) 
            type = "Return";
        else if(u instanceof JTableSwitchStmt) 
            type = "TablelSwitch";
        else
            type = "Other";
        sb.append(type);
        sb.append(": " + SootUtilities.getMethod(u).getName() + "@" + SootUtilities.getMethod(u).getDeclaringClass().getName());
        return sb.toString();
    }

    @Override
    public String toXMLAttrsString(Unit u) {
        SootMethod m = SootUtilities.getMethod(u);
        String file = ((SourceFileTag)m.getDeclaringClass().getTags().get(0)).getSourceFile();
        int line = ((LineNumberTag)u.getTag("LineNumberTag")).getLineNumber();
        int mIdx = domM.indexOf(m);
        return "file=\"" + file + "\" " + "line=\"" + line + "\" " + "Mid=\"M" + mIdx + "\"";
    	//return "";
    }
    
    public void parseAnnotations(Unit u, int indx){
        Set<String> annotationName = LogicBloxAnnotExporter.annotationName;
        Map<Integer, List<Trio<String, String, String>>> pgmPtAnnot = LogicBloxAnnotExporter.pgmPtAnnot;
        if(pgmPtAnnot.containsKey(indx))
            return;
        List<Trio<String, String, String>> annots = new ArrayList<Trio<String, String, String>>();
        for(Tag t : u.getTags()){
            if(t instanceof VisibilityAnnotationTag){
                Map<String,List<Pair<String,String>>> parsed = SootUtilities.parseVisibilityAnnotationTag((VisibilityAnnotationTag)t);
                for(String annotName : parsed.keySet()){
                    annotationName.add(annotName);
                    List<Pair<String,String>> keyValues = parsed.get(annotName);
                    for(Pair<String,String> p : keyValues){
                        annots.add(new Trio<String,String,String>(annotName,p.val0,p.val1));
                    }
                }
            }else if(t instanceof LineNumberTag){
                LineNumberTag lnt = (LineNumberTag)t;
                annotationName.add("LineNumberTag");
                annots.add(new Trio<String,String,String>("LineNumberTag","LineNumber",Integer.toString(lnt.getLineNumber())) );
            }
        }
        pgmPtAnnot.put(indx, annots);
    }
}
