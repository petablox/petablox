package petablox.program;

import petablox.util.soot.SootMethodWrapper;
import soot.RefLikeType;
import soot.SootMethod;
import petablox.util.IndexSet;

import java.util.List;

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
    /**
     * Provides the order in which all methods are visited by the scope builder
     * to be used by aggressive resuse scope.
     *
     * @return Augmented methods with pre-order and post-order numbering
     */
    public abstract List<SootMethodWrapper> getAugmentedMethods();
}
