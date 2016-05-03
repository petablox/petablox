package petablox.android.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class LazyMap<K,V> {
	private final Map<K,V> realMap = new HashMap<K,V>();

	public abstract V lazyFill(K key);

	public V get(K key) {
		if (!realMap.containsKey(key)) {
			realMap.put(key, lazyFill(key));
		}
		return realMap.get(key);
	}

	public void clear() {
		realMap.clear();
	}

	public Collection<V> values() {
		return realMap.values();
	}
}
