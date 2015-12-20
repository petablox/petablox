package petablox.program;

import java.util.List;

import petablox.util.tuple.object.Pair;

import java.util.ArrayList;
import java.util.Collections;

import soot.RefType;
import soot.Unit;

/**
 * Resolved reflection information.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Reflect {
    private final List<Pair<Unit, List<RefType>>> resolvedClsForNameSites;
    private final List<Pair<Unit, List<RefType>>> resolvedObjNewInstSites;
    private final List<Pair<Unit, List<RefType>>> resolvedConNewInstSites;
    private final List<Pair<Unit, List<RefType>>> resolvedAryNewInstSites;

    public Reflect(
            List<Pair<Unit, List<RefType>>> _resolvedClsForNameSites,
            List<Pair<Unit, List<RefType>>> _resolvedObjNewInstSites,
            List<Pair<Unit, List<RefType>>> _resolvedConNewInstSites,
            List<Pair<Unit, List<RefType>>> _resolvedAryNewInstSites) {
        resolvedClsForNameSites = _resolvedClsForNameSites;
        resolvedObjNewInstSites = _resolvedObjNewInstSites;
        resolvedConNewInstSites = _resolvedConNewInstSites;
        resolvedAryNewInstSites = _resolvedAryNewInstSites;
    }
    public Reflect() {
        resolvedClsForNameSites = new ArrayList<Pair<Unit, List<RefType>>>();
        resolvedObjNewInstSites = new ArrayList<Pair<Unit, List<RefType>>>();
        resolvedConNewInstSites = new ArrayList<Pair<Unit, List<RefType>>>();
        resolvedAryNewInstSites = new ArrayList<Pair<Unit, List<RefType>>>();
    }
    /**
     * Provides a list containing each call to static method {@code Class forName(String s)}
     * defined in class {@code java.lang.Class}, along with the types of all classes
     * denoted by argument {@code s}.
     */
    public List<Pair<Unit, List<RefType>>> getResolvedClsForNameSites() {
        return resolvedClsForNameSites;
    }
    /**
     * Provides a list containing each call to instance method {@code Object newInstance()}
     * defined in class {@code java.lang.Class}, along with the types of all classes
     * instantiated by it.
     */
    public List<Pair<Unit, List<RefType>>> getResolvedObjNewInstSites() {
        return resolvedObjNewInstSites;
    }
    /**
     * Provides a list containing each call to instance method {@code Object newInstance(Object[])}
     * defined in class {@code java.lang.reflect.Constructor}, along with the types of all
     * classes instantiated by it.
     */
    public List<Pair<Unit, List<RefType>>> getResolvedConNewInstSites() {
        return resolvedConNewInstSites;
    }
    /**
     * Provides a list containing each call to static method {@code Object newInstance(Class, int)}
     * defined in class {@code java.lang.reflect.Array}, along with the types of all classes 
     * instantiated by it.
     */
    public List<Pair<Unit, List<RefType>>> getResolvedAryNewInstSites() {
        return resolvedAryNewInstSites;
    }
    public boolean addResolvedClsForNameSite(Unit q, RefType c) {
        return add(resolvedClsForNameSites, q, c);
    }
    public boolean addResolvedObjNewInstSite(Unit q, RefType c) {
        return add(resolvedObjNewInstSites, q, c);
    }
    public boolean addResolvedConNewInstSite(Unit q, RefType c) {
        return add(resolvedConNewInstSites, q, c);
    }
    public boolean addResolvedAryNewInstSite(Unit q, RefType c) {
        return add(resolvedAryNewInstSites, q, c);
    }
    private static boolean add(List<Pair<Unit, List<RefType>>> l, Unit q, RefType c) {
        for (Pair<Unit, List<RefType>> p : l) {
            if (p.val0 == q) {
                List<RefType> s = p.val1;
                if (s.contains(c))
                    return false;
                s.add(c);
                return true;
            }
        }
        List<RefType> s = new ArrayList<RefType>(2);
        s.add(c);
        l.add(new Pair<Unit, List<RefType>>(q, s));
        return true;
    }
}
