package chord.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import chord.util.tuple.object.Pair;

/** Mostly deprecated utilities. (After some point I decided I can't stand not
having guava.) */
public final class CollectionUtil {
  @Deprecated // use com.google.common.collect.Lists.newArrayList
	public static <T> ArrayList<T> newArrayList() { return new ArrayList<T>(); }

  @Deprecated // use com.google.common.collect.Sets.newHashSet
	public static <T> HashSet<T> newHashSet() { return new HashSet<T>(); }

  @Deprecated // use com.google.common.collect.Sets.newTreeSet
	public static <T> TreeSet<T> newTreeSet() { return new TreeSet<T>(); }

  @Deprecated // use com.google.common.collect.Maps.newHashMap
	public static <K,V> HashMap<K,V> newHashMap() { return new HashMap<K,V>(); }

	public static <F, S> Pair<F, S> mkPair(F f, S s) { return new Pair<F,S>(f, s); }

  @Deprecated // com.google.common.primitives.Ints.lexicographicalComparator().compare()
  public static int compare(int[] xs, int[] ys) {
    int n = Math.min(xs.length, ys.length);
    int i;
    for (i = 0; i < n; ++i)
      if (xs[i] != ys[i]) return compare(xs[i], ys[i]);
    return compare(xs.length, ys.length);
  }

  // NOTE: Integer.compare in Java 7, but we use older
  @Deprecated // com.google.common.primitives.Ints.compare
  public static int compare(int x, int y) {
    return (x < y)? -1 : ((x == y)? 0 : 1);
  }

  // NOTE: Boolean.compare in Java 7, but we use older
  @Deprecated // com.google.common.primitives.Booleans.compare
  public static int compare(boolean x, boolean y){
	  return (x == y) ? 0 : (x ? 1 : -1);
  }

  // NOTE: Long.compare in Java 7, but we use older
  @Deprecated // com.google.common.primitives.Longs.compare
  public static int compare(long x, long y) {
    return (x < y) ? -1 : ((x == y) ? 0 : 1);
  }

  @Deprecated // use com.google.common.collect.Lists.newArrayList
	public static <T> ArrayList<T> newArrayList(Collection<T> xs) {
		ArrayList<T> r = newArrayList();
		r.addAll(xs);
		return r;
	}

	private CollectionUtil() { /* no instance */ }
}
