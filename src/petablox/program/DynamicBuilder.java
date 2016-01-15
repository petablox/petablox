package petablox.program;

import java.util.List;
import petablox.util.IndexSet;
import soot.RefType;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootMethod;
 

/**
 * Dynamic analysis-based scope builder.
 *
 * Constructs scope by running the given Java program on the given input,
 * observing which classes are loaded (either using JVMTI or load-time bytecode
 * instrumentation, depending upon whether property {@code petablox.use.jvmti}
 * is set to true or false, respectively), and then regarding all methods
 * declared in those classes as reachable.
 *
 * This scope builder does not currently resolve any reflection; use RTA instead.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DynamicBuilder implements ScopeBuilder {
    private IndexSet<SootMethod> methods;
    private IndexSet<RefLikeType> classes;

    @Override
    public IndexSet<SootMethod> getMethods() {
        if (methods != null)
            return methods;
        Program program = Program.g();
        List<String> classNames = program.getDynamicallyLoadedClasses();
        //HostedVM.initialize();
        methods = new IndexSet<SootMethod>();
        for (String s : classNames) {
            RefType rc = (RefType) program.loadClass(s);
            SootClass c = rc.getSootClass();
            classes.add(rc);
            List<SootMethod> meths = c.getMethods();
            for(SootMethod m : meths){
            	if(!m.isAbstract())
            		m.releaseActiveBody();
            	methods.add(m);
            }
        }
        return methods;
    }
    
    @Override
    public IndexSet<RefLikeType> getClasses(){
    	return classes;
    }

    /*
     * Returns an empty reflect. Dynamic scope doesn't do reflection analysis.
     * Instead, Program.java uses the concrete classes that got created at run time.
     */
    @Override
    public Reflect getReflect() {
        return new Reflect();
    }
}
