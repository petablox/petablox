package petablox.program;

import java.util.List;

import petablox.util.tuple.object.Pair;

import java.util.ArrayList;
import soot.RefLikeType;
import soot.Unit;

/**
 * Resolved reflection information.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Reflect {
    private final List<Pair<Unit, List<RefLikeType>>> resolvedClsForNameSites;
    private final List<Pair<Unit, List<RefLikeType>>> resolvedObjNewInstSites;
    private final List<Pair<Unit, List<RefLikeType>>> resolvedConNewInstSites;
    private final List<Pair<Unit, List<RefLikeType>>> resolvedAryNewInstSites;

    public Reflect(
            List<Pair<Unit, List<RefLikeType>>> _resolvedClsForNameSites,
            List<Pair<Unit, List<RefLikeType>>> _resolvedObjNewInstSites,
            List<Pair<Unit, List<RefLikeType>>> _resolvedConNewInstSites,
            List<Pair<Unit, List<RefLikeType>>> _resolvedAryNewInstSites) {
        resolvedClsForNameSites = _resolvedClsForNameSites;
        resolvedObjNewInstSites = _resolvedObjNewInstSites;
        resolvedConNewInstSites = _resolvedConNewInstSites;
        resolvedAryNewInstSites = _resolvedAryNewInstSites;
    }
    public Reflect() {
        resolvedClsForNameSites = new ArrayList<Pair<Unit, List<RefLikeType>>>();
        resolvedObjNewInstSites = new ArrayList<Pair<Unit, List<RefLikeType>>>();
        resolvedConNewInstSites = new ArrayList<Pair<Unit, List<RefLikeType>>>();
        resolvedAryNewInstSites = new ArrayList<Pair<Unit, List<RefLikeType>>>();
    }
    /**
     * Provides a list containing each call to static method {@code Class forName(String s)}
     * defined in class {@code java.lang.Class}, along with the types of all classes
     * denoted by argument {@code s}.
     */
    public List<Pair<Unit, List<RefLikeType>>> getResolvedClsForNameSites() {
        return resolvedClsForNameSites;
    }
    /**
     * Provides a list containing each call to instance method {@code Object newInstance()}
     * defined in class {@code java.lang.Class}, along with the types of all classes
     * instantiated by it.
     */
    public List<Pair<Unit, List<RefLikeType>>> getResolvedObjNewInstSites() {
        return resolvedObjNewInstSites;
    }
    /**
     * Provides a list containing each call to instance method {@code Object newInstance(Object[])}
     * defined in class {@code java.lang.reflect.Constructor}, along with the types of all
     * classes instantiated by it.
     */
    public List<Pair<Unit, List<RefLikeType>>> getResolvedConNewInstSites() {
        return resolvedConNewInstSites;
    }
    /**
     * Provides a list containing each call to static method {@code Object newInstance(Class, int)}
     * defined in class {@code java.lang.reflect.Array}, along with the types of all classes 
     * instantiated by it.
     */
    public List<Pair<Unit, List<RefLikeType>>> getResolvedAryNewInstSites() {
        return resolvedAryNewInstSites;
    }
    public boolean addResolvedClsForNameSite(Unit q, RefLikeType c) {
        return add(resolvedClsForNameSites, q, c);
    }
    public boolean addResolvedObjNewInstSite(Unit q, RefLikeType c) {
        return add(resolvedObjNewInstSites, q, c);
    }
    public boolean addResolvedConNewInstSite(Unit q, RefLikeType c) {
        return add(resolvedConNewInstSites, q, c);
    }
    public boolean addResolvedAryNewInstSite(Unit q, RefLikeType c) {
        return add(resolvedAryNewInstSites, q, c);
    }
    private static boolean add(List<Pair<Unit, List<RefLikeType>>> l, Unit q, RefLikeType c) {
        for (Pair<Unit, List<RefLikeType>> p : l) {
            if (p.val0 == q) {
                List<RefLikeType> s = p.val1;
                if (s.contains(c))
                    return false;
                s.add(c);
                return true;
            }
        }
        List<RefLikeType> s = new ArrayList<RefLikeType>(2);
        s.add(c);
        l.add(new Pair<Unit, List<RefLikeType>>(q, s));
        return true;
    }
}
