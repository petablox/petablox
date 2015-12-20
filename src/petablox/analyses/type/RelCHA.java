package petablox.analyses.type;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import soot.SootResolver;
import soot.ArrayType;
import soot.NullType;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import petablox.analyses.invk.StubRewrite;
import petablox.analyses.method.DomM;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.analyses.ProgramRel;
import petablox.util.IndexSet;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m1,t,m2) such that method m2 is the
 * resolved method of an invokevirtual or invokeinterface call with
 * resolved method m1 on an object of concrete class t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "cha",
    sign = "M1,T1,M0:M0xM1_T1"
)
public class RelCHA extends ProgramRel {
	private SootMethod getMethodItr(SootClass c,String subsign){
		SootMethod ret = null;
		while(true){
			try{
				ret= c.getMethod(subsign);
				break;
			}catch(Exception e){
				if(!c.hasSuperclass()){
					System.out.println("WARN: RelCHA: Method "+subsign+" not found");
					break;
				}else{
					c = c.getSuperclass();
				}
			}
		}
		return ret;
	}
    public void fill() {
        DomM domM = (DomM) doms[0];
        Program program = Program.g();

        // Set of instance methods that belong to in-scope classes where at least
        // one object of the class exists in the program.
        Set<SootMethod> objClsInstanceMethods = new HashSet<SootMethod>();
        IndexSet<RefLikeType> classes = program.getClasses();
        //SootClass objCls = (SootClass) program.getClass("java.lang.Object");
        SootClass objCls = SootResolver.v().makeClassRef("java.lang.Object");
        Iterator<SootMethod> it = objCls.methodIterator();
        while (it.hasNext()) {
        	SootMethod m = it.next();
            // only add methods deemed reachable
            if (!m.isStatic() && domM.contains(m))
                objClsInstanceMethods.add(m);
        }
        for (RefLikeType r : classes) {
        	if(r instanceof NullType)
        		continue;
        	if(r instanceof ArrayType){
        		for(SootMethod m : objClsInstanceMethods)
        			add(m, r, StubRewrite.maybeReplaceVirtCallDest(m, m, domM));
        		continue;
        	}
            SootClass c = ((RefType)r).getSootClass();
            Iterator<SootMethod> cit = c.methodIterator();
            while (cit.hasNext()) {
            	SootMethod m = cit.next();
            	if (m.isStatic()) // we are looking at instance methods only
            		continue;
                if (m.isPrivate()) //not in CHA
                    continue;
                if (m.getName().contains("<init>"))
                    continue;
                if (!domM.contains(m))
                    continue;
                String subsig = m.getSubSignature();
                if (c.isInterface()) {
                    for (RefLikeType s : classes) {
                        if (s instanceof ArrayType || s instanceof NullType)
                            continue;
                        SootClass d = ((RefType)s).getSootClass();
                        if (d.isInterface() || d.isAbstract())
                            continue;
                        if (d.implementsInterface(c.getName())) {
                            SootMethod n = this.getMethodItr(d,subsig);
                            if(n==null){
                            	System.out.println("WARN: RelCHA Method not found:"+subsig+" Class:"+d+" Interface:"+c);
                            	continue;
                            }
                            // There seems to be some consistency issue for some interfaces which have added additional
                            // methods in Java 8. Hence the assertion below is commented out.
                            // Ideally the assertion should be there since it is a serious problem
                            //assert (n != null);
                            if (domM.contains(n)) {
                                // rewrite dest, after resolution
                                add(m, d.getType(), StubRewrite.maybeReplaceVirtCallDest(m,n, domM));
                            }
                        }
                    }
                } else { //class, not interface
                    for (RefLikeType s : classes) {
                    	if (s instanceof ArrayType || s instanceof NullType)
                            continue;
                        SootClass d = ((RefType)s).getSootClass();
                        if (d.isInterface() || d.isAbstract())
                            continue;
                        if (SootUtilities.extendsClass(d,c)) {
                            SootMethod n = this.getMethodItr(d, subsig);
                            if(n==null){
                            	System.out.println("WARN: RelCHA Method not found:"+subsig+" Class:"+d+" Interface:"+c);
                            	continue;
                            }
                            //assert (n != null);
                            if (domM.contains(n)) {
                                // rewrite dest, after resolution
                                add(m, d.getType(), StubRewrite.maybeReplaceVirtCallDest(m,n, domM));
                                add(m, d.getType(), n);
                            }
                        }
                    }
                }
            }
        }
    }
}
