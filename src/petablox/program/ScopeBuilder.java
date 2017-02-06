package petablox.program;

import java.util.HashSet;

import soot.RefLikeType;
import soot.SootClass;
import soot.SootMethod;
import petablox.util.IndexSet;

public interface ScopeBuilder {
    /**
     * Provides all methods in the input Java program that are deemed reachable by this scope builder.
     *
     * @return All methods in the input Java program that are deemed reachable by this scope builder.
     */
    public abstract IndexSet<SootMethod> getMethods();
    /**
     * Provides all classes in the input Java program that are deemed reachable by this scope builder.
     *
     * @return All classes in the input Java program that are deemed reachable by this scope builder.
     */
    public abstract IndexSet<RefLikeType> getClasses();
    /**
     * Provides reflection information in the input Java program that is resolved by this scope builder.
     *
     * @return Reflection information in the input Java program that is resolved by this scope builder.
     */
    public abstract Reflect getReflect();
    
    public abstract HashSet<SootMethod> getEntryMethods();
    public abstract HashSet<SootClass> getEntryClasses();
}
