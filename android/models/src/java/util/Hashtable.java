class Hashtable<K, V>
{
    private K key;
    private V value;

    public  V get(java.lang.Object k)
    {
		return value;
    }

    public  V put(K k, V v)
    {
		this.key = k;
		this.value = v;
		return this.value;
    }

	public java.util.Collection<V> values() {
		java.util.ArrayList list = new java.util.ArrayList<V>();
		list.add(value);
		return list;
	}
}
