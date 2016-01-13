package petablox.program;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import petablox.project.Config;
import petablox.project.Messages;
import petablox.util.IndexSet;
import petablox.util.Timer;
import petablox.util.soot.ICFG;
import petablox.util.soot.SootMethodWrapper;
import petablox.util.soot.SootUtilities;
import soot.ArrayType;
import soot.Hierarchy;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootResolver;
import soot.Type;
import soot.Unit;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewExpr;
import soot.toolkits.graph.Block;

/**
 * Class Hierarchy Analysis (CHA) based scope builder.
 *
 * This scope builder currently does not resolve any reflection; use RTA instead.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CHA implements ScopeBuilder {
    private static final String MAIN_CLASS_NOT_DEFINED =
        "ERROR: Property petablox.main.class must be set to specify the main class of program to be analyzed.";
    private static final String MAIN_METHOD_NOT_FOUND =
        "ERROR: Could not find main class '%s' or main method in that class.";

    public static final boolean DEBUG = false;

    private IndexSet<RefLikeType> classes;

    // classes whose clinit and super class/interface clinits have been processed
    private Set<SootClass> classesVisitedForClinit;

    // methods deemed reachable so far
    private IndexSet<SootMethod> methods;

    // worklist for methods that have been seen but whose cfg isn't processed yet
    private List<SootMethod> methodWorklist;

    private SootClass javaLangObject;

    private final ClassHierarchy ch;

    public CHA(ClassHierarchy _ch) {
        ch = _ch;
    }

    @Override
    public List<SootMethodWrapper> getAugmentedMethods(){
        return new ArrayList<SootMethodWrapper>();
    }

    @Override
    public IndexSet<SootMethod> getMethods() {
        if (methods != null)
            return methods;
        System.out.println("ENTER: CHA");
        Timer timer = new Timer();
        timer.init();
        classes = new IndexSet<RefLikeType>();
        classesVisitedForClinit = new HashSet<SootClass>();
        methods = new IndexSet<SootMethod>();
        methodWorklist = new ArrayList<SootMethod>();
        javaLangObject =  null; 
        		//SootResolver.v().makeClassRef("java.lang.Object");
        String mainClassName = Config.mainClassName;
        if (mainClassName == null)
            Messages.fatal(MAIN_CLASS_NOT_DEFINED);
        
        SootClass mainClass = Scene.v().loadClassAndSupport(mainClassName);
        Scene.v().loadNecessaryClasses();
        mainClass.setApplicationClass();
        
        prepareClass(mainClass.getType());
        SootMethod mainMethod = mainClass.getMethod("void main(java.lang.String[])");
        if (mainMethod == null)
            Messages.fatal(MAIN_METHOD_NOT_FOUND, mainClassName);
        visitClinits(mainClass);
        visitMethod(mainMethod);
        while (!methodWorklist.isEmpty()) {
            SootMethod m = methodWorklist.remove(methodWorklist.size() - 1);
            try{
            	if(!m.hasActiveBody())
            		m.retrieveActiveBody();
            }catch(Exception e){
            	continue;
            }
            ICFG cfg = SootUtilities.getCFG(m);
            if (DEBUG) System.out.println("Processing CFG of method: " + m);
            processCFG(cfg);
        }
        System.out.println("LEAVE: CHA");
        timer.done();
        System.out.println("Time: " + timer.getInclusiveTimeStr());
        return methods;
    }
    
    @Override
    public IndexSet<RefLikeType> getClasses(){
    	return classes;
    }
    
    private void visitMethod(SootMethod m) {
        if (methods.add(m)) {
            if (!m.isAbstract()) {
                if (DEBUG) System.out.println("\tAdding method: " + m);
                methodWorklist.add(m);
            }
        }
    }

    private void processCFG(ICFG cfg) {
    	Hierarchy h = new Hierarchy();
    	for(Block bb : cfg.reversePostOrder()){
    		Iterator<Unit> itr = bb.iterator();
    		while(itr.hasNext()){
    			Unit u = itr.next();
    			if(SootUtilities.isInvoke(u)){
    				InvokeExpr ie = SootUtilities.getInvokeExpr(u);
    				if (DEBUG) System.out.println("Unit: " + u);
    				SootMethod n = ie.getMethod();
    				SootClass c = n.getDeclaringClass();
    				visitClass(c.getType());
    				visitMethod(n);
    				if(ie instanceof VirtualInvokeExpr || ie instanceof SpecialInvokeExpr ||
    						ie instanceof InterfaceInvokeExpr){
    					List<SootMethod> dispatch = h.resolveAbstractDispatch(c, n);
    					for(SootMethod m : dispatch){
    						visitClass(m.getDeclaringClass().getType());
    						visitMethod(m);
    					}
    				}else
    					assert(ie instanceof StaticInvokeExpr);	
    			}else if(u instanceof JAssignStmt){
    				JAssignStmt jas = (JAssignStmt)u;
    				if(SootUtilities.isStaticGet(jas)){
    					if (DEBUG) System.out.println("Unit: " + u);
                        SootField f = jas.getFieldRef().getField();
                        SootClass c = f.getDeclaringClass();
                        visitClass(c.getType());
    				}else if(SootUtilities.isStaticPut(jas)){
    					if (DEBUG) System.out.println("Unit: " + u);
                        SootField f = jas.getFieldRef().getField();
                        SootClass c = f.getDeclaringClass();
                        visitClass(c.getType());
    				}else if(SootUtilities.isNewStmt(jas)){
        				if(DEBUG) System.out.println("Unit "+u);
        				JNewExpr jne = ((JNewExpr)jas.rightBox.getValue());
        				SootClass c = ((RefType)jne.getType()).getSootClass();
        				visitClass(c.getType());
        			}else if(SootUtilities.isNewArrayStmt(jas)){
        				if(DEBUG) System.out.println("Unit "+u);
        				JNewArrayExpr jne = ((JNewArrayExpr)jas.rightBox.getValue());
        				visitClass(jne.getType());
        			}	
    			}
    		}
    	}
    }

    private void prepareClass(Type t) {
    	if(t instanceof ArrayType)
    		return;
    	if(t instanceof RefType){
    		RefType r = (RefType)t;
    		SootClass c = ((RefType)r).getSootClass();
        	
    		if (classes.add(r)) {
                if (DEBUG) System.out.println("\tAdding class: " + r);
                c = r.getSootClass();
                Scene.v().loadClassAndSupport(c.getName());
                //c.setApplicationClass();
                if(c.hasSuperclass()){
                	SootClass d = c.getSuperclass();
                	if (d == null)
                		assert (c == javaLangObject);
                	else
                		prepareClass(d.getType());
                }
                for (SootClass i : c.getInterfaces())
                    prepareClass(i.getType());
            }
    	}
    }

    private void visitClass(Type t) {
        prepareClass(t);
        if (t instanceof ArrayType)
            return;
        RefType r = (RefType)t;
        SootClass c = r.getSootClass();
        visitClinits(c);
    }

    private void visitClinits(SootClass c) {
    	if (classesVisitedForClinit.add(c)) {
            List<SootMethod> methods = c.getMethods();
            for (SootMethod m : methods){
            	if(m.getName().contains("<clinit>")){
            		visitMethod(m);
            	}
            }
            if(c.hasSuperclass()){
	            SootClass superClass = c.getSuperclass();
	            if(superClass != null)
	            	visitClinits(superClass);
            }
            for(SootClass i : c.getInterfaces())
            	visitClinits(i);
        }
    }

    @Override
    public Reflect getReflect() {
        return new Reflect();
    }
}
