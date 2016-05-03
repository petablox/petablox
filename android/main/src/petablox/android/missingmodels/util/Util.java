package petablox.android.missingmodels.util;

import java.util.*;

public class Util {
	public static class Counter<K> {
		private Map<K,Integer> counts = new HashMap<K,Integer>();

		public int getCount(K k) {
			Integer count = this.counts.get(k);
			if(count == null) {
				count = 0;
				this.counts.put(k, count);
			}
			return count;
		}

		public void increment(K k) {
			this.setCount(k, this.getCount(k)+1);
		}

		public void setCount(K k, int count) {
			this.counts.put(k, count);
		}

		public Set<K> keySet() {
			return this.counts.keySet();
		}

		public Set<Map.Entry<K,Integer>> entrySet() {
			return this.counts.entrySet();
		}

		public List<K> sortedKeySet() {
			List<K> keys = new ArrayList<K>(this.keySet());
			Collections.sort(keys, new Comparator<K>() {
				@Override public int compare(K k1, K k2) {
					int i1 = getCount(k1);
					int i2 = getCount(k2);
					if(i1 < i2) {
						return 1;
					} else if(i1 == i2) {
						return 0;
					} else {
						return -1;
					}
				}
			});
			return keys;
		}
	}

	public static class MultivalueMap<K,V> extends HashMap<K,Set<V>> {
		private static final long serialVersionUID = -6390444829513305915L;

		public void add(K k, V v) {
			ensure(k).add(v);
		}
		
		public Set<V> ensure(K k) {
			Set<V> vSet = super.get(k);
			if(vSet == null) {
				super.put(k, vSet = new HashSet<V>());
			}
			return vSet;
		}

		@Override
		public Set<V> get(Object k) {
			Set<V> vSet = super.get(k);
			return vSet == null ? new HashSet<V>() : vSet;
		}
	}

	public static class Pair<X,Y> {
		private X x;
		private Y y;

		public Pair(X x, Y y) {
			this.x = x;
			this.y = y;
		}
		
		public X getX() {
			return this.x;
		}

		public Y getY() {
			return this.y;
		}

		@Override
		public String toString() {
			return "[" + this.x.toString() + "," + this.y.toString() + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((x == null) ? 0 : x.hashCode());
			result = prime * result + ((y == null) ? 0 : y.hashCode());
			return result;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Pair other = (Pair) obj;
			if (x == null) {
				if (other.x != null)
					return false;
			} else if (!x.equals(other.x))
				return false;
			if (y == null) {
				if (other.y != null)
					return false;
			} else if (!y.equals(other.y))
				return false;
			return true;
		}
	}
}
