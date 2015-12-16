package chord.util;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import chord.bddbddb.Dom;

/**
 * A bitset implementation of Set interface specially for Chord Dom objects. The
 * operations between DomBitSets with the same Dom should be significantly
 * faster than normal Set operations.
 * 
 * @author xin
 * 
 * @param <E>
 */
public class DomBitSet<E> implements Set<E> {
	private Dom<E> dom;
	private BitSet bitSet;

	public DomBitSet(Dom<E> dom) {
		this.dom = dom;
		bitSet = new BitSet(dom.size());
	}

	public DomBitSet(DomBitSet<E> that){
		this.dom = that.dom;
		this.bitSet = (BitSet)that.bitSet.clone();
	}
	
	@Override
	public int size() {
		return bitSet.cardinality();
	}

	@Override
	public boolean isEmpty() {
		return bitSet.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		int index = dom.indexOf(o);
		if (index >= 0)
			return bitSet.get(index);
		return false;
	}

	@Override
	public Iterator<E> iterator() {
		return new DomIterator();
	}

	@Override
	public Object[] toArray() {
		Object[] ret = new Object[this.size()];
		Iterator<E> iter = this.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = iter.next();
		}
		return ret;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		int size = size();
		T[] ret = a.length >= size ? a : (T[]) java.lang.reflect.Array
				.newInstance(a.getClass().getComponentType(), size);
		Iterator<E> iter = this.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = (T) iter.next();
		}
		return ret;
	}

	@Override
	public boolean add(E e) {
		int idx = dom.indexOf(e);
		if (idx < 0)
			return false;
		if (bitSet.get(idx))
			return false;
		bitSet.set(idx);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		int idx = dom.indexOf(o);
		if (idx < 0)
			return false;
		if (!bitSet.get(idx))
			return false;
		bitSet.clear(idx);
		return true;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (c instanceof DomBitSet) {
			DomBitSet that = (DomBitSet) c;
			if (that.dom != dom)// we assume over the same type, all the
								// DomBitSet share the same Dom
				return false;
			BitSet bsCopy = (BitSet) bitSet.clone();
			bsCopy.or(that.bitSet);
			if (bsCopy.equals(bitSet))
				return true;
			return false;
		}
		for (Object o : c)
			if (!this.contains(o))
				return false;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (c instanceof DomBitSet) {
			DomBitSet that = (DomBitSet) c;
			if (that.dom != dom)// we assume over the same type, all the
								// DomBitSet share the same Dom
				return false;
			BitSet bsCopy = (BitSet) bitSet.clone();
			bitSet.or(that.bitSet);
			if (bsCopy.equals(bitSet))
				return false;
			return true;
		}
		boolean added = false;
		for (E o : c)
			added |= this.add(o);
		return added;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (c instanceof DomBitSet) {
			DomBitSet that = (DomBitSet) c;
			if (that.dom != dom)// we assume over the same type, all the
								// DomBitSet share the same Dom
				return false;
			BitSet bsCopy = (BitSet) bitSet.clone();
			bitSet.and(that.bitSet);
			if (bsCopy.equals(bitSet))
				return false;
			return true;
		}
		BitSet newBS = new BitSet(dom.size());
		for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
			if (c.contains(dom.get(i)))
				newBS.set(i);
		}
		if (newBS.equals(bitSet))
			return false;
		bitSet = newBS;
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (c instanceof DomBitSet) {
			DomBitSet that = (DomBitSet) c;
			if (that.dom != dom)// we assume over the same type, all the
								// DomBitSet share the same Dom
				return false;
			BitSet bsCopy = (BitSet) bitSet.clone();
			bitSet.andNot(that.bitSet);
			if (bsCopy.equals(bitSet))
				return false;
			return true;
		}
		boolean removed = false;
		for (Object o : c) {
			int idx = dom.indexOf(o);
			if (idx >= 0)
				if (bitSet.get(idx)) {
					removed = true;
					bitSet.clear(idx);
				}
		}
		return removed;
	}

	@Override
	public void clear() {
		bitSet.clear();
	}

	public String toString() {
		Iterator<E> i = iterator();
		if (!i.hasNext())
			return "[]";

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (;;) {
			E e = i.next();
			sb.append(e == this ? "(this Collection)" : e);
			if (!i.hasNext())
				return sb.append(']').toString();
			sb.append(", ");
		}
	}
	
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Set))
            return false;
        Collection c = (Collection) o;
        if (c.size() != size())
            return false;
        try {
            return containsAll(c);
        } catch (ClassCastException unused)   {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    public int hashCode() {
        int h = 0;
        Iterator<E> i = iterator();
        while (i.hasNext()) {
            E obj = i.next();
            if (obj != null)
                h += obj.hashCode();
        }
        return h;
    }


	class DomIterator implements Iterator<E> {
		int counter = 0;

		@Override
		public boolean hasNext() {
			return bitSet.nextSetBit(counter) != -1;
		}

		@Override
		public E next() {
			int nextIdx = bitSet.nextSetBit(counter);
			if (nextIdx == -1)
				throw new IndexOutOfBoundsException();
			counter = nextIdx + 1;
			return dom.get(nextIdx);
		}

		@Override
		public void remove() {
			int lastIdx = counter - 1;
			int lastIdx1 = bitSet.nextSetBit(lastIdx);
			if (lastIdx1 != lastIdx)
				throw new RuntimeException(
						"Iterator.remove() must be called after Iterator.next()");
			bitSet.clear(lastIdx);
		}

	}

}
