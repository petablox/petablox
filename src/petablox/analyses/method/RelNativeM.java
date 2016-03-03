package petablox.analyses.method;

import soot.SootClass;
import soot.SootMethod;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing all native methods.
 *
 * @author Joe Cox (cox@cs.ucla.edu)
 */
@Petablox(
    name = "nativeM",
    sign = "M0"
)
public class RelNativeM extends ProgramRel implements IMethodVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (m.isNative()) {
            add(m);
        }
        
        /**
         * Some native methods are stubbed and had the native modifier removed
         * to play nice with Soot
         */
        if (m.getSignature().equals("<java.lang.Object: java.lang.Object clone()>") ||
            m.getSignature().equals("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>") ||
            m.getSignature().equals("<java.lang.reflect.Array: void set(java.lang.Object,int,java.lang.Object)>") ||
            m.getSignature().equals("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction)>") ||
            m.getSignature().equals("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)>") ||
            m.getSignature().equals("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction,java.security.AccessControlContext)>") ||
            m.getSignature().equals("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>")) {
            add(m);
        }
    }
}
