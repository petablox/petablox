package chord.program;

import soot.RefLikeType;
import soot.SootMethod;
import chord.util.IndexSet;

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
}
