package petablox.analyses.method;

import soot.SootClass;
import soot.SootMethod;
import soot.tagkit.AnnotationElem;
import soot.tagkit.AnnotationStringElem;
import soot.tagkit.AnnotationTag;
import soot.tagkit.SourceFileTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.tagkit.VisibilityParameterAnnotationTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import petablox.logicblox.LogicBloxAnnotExporter;
import petablox.program.Program;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;
import petablox.util.ArraySet;
import petablox.util.Utils;
import petablox.util.soot.SootUtilities;
import petablox.util.tuple.object.Pair;
import petablox.util.tuple.object.Quad;
import petablox.util.tuple.object.Trio;

/**
 * Domain of methods.
 * <p>
 * The 0th element in this domain is the main method of the program.
 * <p>
 * The 1st element in this domain is the <tt>start()</tt> method of class <tt>java.lang.Thread</tt>,
 * if this method is reachable from the main method of the program.
 * <p>
 * The above two methods are the entry-point methods of the implicitly created main thread and each
 * explicitly created thread, respectively.  Due to Petablox's emphasis on concurrency, these methods
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
        int indx = -1;
    	Program program = Program.g();
        SootMethod mainMethod = program.getMainMethod();
        assert (mainMethod != null);
        indx = getOrAdd(mainMethod);
        parseAnnotations(mainMethod,indx);
        SootMethod startMethod = program.getThreadStartMethod();
        if (startMethod != null){
           indx = getOrAdd(startMethod);
           parseAnnotations(startMethod,indx);
        }
    }

    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        int indx = getOrAdd(m);
        parseAnnotations(m, indx);
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
        String file = "null";
        SourceFileTag sft = (SourceFileTag)m.getTag("SourceFileTag");
        if(sft!=null){
        	file = sft.getSourceFile();
        }
        int line = m.getNumber(); 
        return "sign=\"" + sign + "\" file=\"" + file + "\" line=\"" + line + "\"";
    }
    
    public void parseAnnotations(SootMethod m, int indx){
    	Map<Integer, List<Quad<String, Integer, String, String>>> methParamAnnot = LogicBloxAnnotExporter.methParamAnnot;
    	Map<Integer, List<Trio<String, String, String>>> methRetAnnot =  LogicBloxAnnotExporter.methRetAnnot;
    	Set<String> annotationName = LogicBloxAnnotExporter.annotationName;
    	if(methParamAnnot.containsKey(indx)){
    		return;
    	}
    	List<Quad<String, Integer, String, String>> paramAnnots = new ArrayList<Quad<String, Integer, String, String>>();
    	List<Trio<String, String, String>> retAnnot = new ArrayList<Trio<String, String, String>>();
    	int paramIndx = 1;
    	for(Tag t : m.getTags()){
    		if(t instanceof VisibilityAnnotationTag){
    			Map<String,List<Pair<String,String>>> parsed = SootUtilities.parseVisibilityAnnotationTag((VisibilityAnnotationTag)t);
    			for(String annotName : parsed.keySet()){
    				annotationName.add(annotName);
    				List<Pair<String,String>> keyValues = parsed.get(annotName);
    				for(Pair<String,String> p : keyValues){
    					retAnnot.add(new Trio<String,String,String>(annotName,p.val0,p.val1));
    				}
    			}
    		}else if(t instanceof VisibilityParameterAnnotationTag){
    			List<VisibilityAnnotationTag> vtags = ((VisibilityParameterAnnotationTag) t).getVisibilityAnnotations();
    			for(int i=0;i<vtags.size();i++){
    				VisibilityAnnotationTag v = vtags.get(i);
    				Map<String,List<Pair<String,String>>> parsed = SootUtilities.parseVisibilityAnnotationTag(v);
    				for(String annotName : parsed.keySet()){
    					annotationName.add(annotName);
    					List<Pair<String,String>> keyValues = parsed.get(annotName);
    					for(Pair<String,String> p : keyValues){
    						paramAnnots.add(new Quad<String,Integer,String,String>(annotName,paramIndx,p.val0,p.val1));
    					}
    				}
    				paramIndx++;
    			}
    		}
    	}
    	methParamAnnot.put(indx, paramAnnots);
    	methRetAnnot.put(indx, retAnnot);
    }
}
