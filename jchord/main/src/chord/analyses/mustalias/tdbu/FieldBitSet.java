package chord.analyses.mustalias.tdbu;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import joeq.Class.jq_Field;
import chord.analyses.field.DomF;

public class FieldBitSet implements Set<jq_Field> {
	public static DomF domF;
	protected BitSet bitSet;

	public FieldBitSet(){
		bitSet = new BitSet(domF.size());
	}
	public FieldBitSet(Set<jq_Field> notInFSet) {
		this();
		if(notInFSet instanceof FieldBitSet){
			FieldBitSet fbs = (FieldBitSet)notInFSet;
			this.addAll(fbs);
			return;
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		if(!(o instanceof jq_Field))
			return false;
		jq_Field f = (jq_Field)o;
		int fIndx = domF.indexOf(f);
		return bitSet.get(fIndx);
	}

	@Override
	public Iterator<jq_Field> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(jq_Field e) {
		int fIndx = domF.indexOf(e);
		if(bitSet.get(fIndx))
			return false;
		bitSet.set(fIndx);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		if(!(o instanceof jq_Field))
			return false;
		jq_Field f = (jq_Field)o;
		int fIndx = domF.indexOf(f);
		if(bitSet.get(fIndx)){
			bitSet.set(fIndx);
			return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if(c instanceof FieldBitSet){
			BitSet cBitSetCopy = (BitSet)((FieldBitSet)c).bitSet.clone();
			cBitSetCopy.andNot(bitSet);
			if(cBitSetCopy.isEmpty())
				return true;
			return false;
		}
		throw new UnsupportedOperationException();
	}

	public boolean intersects(FieldBitSet set) {
		BitSet cBitSetCopy = (BitSet)set.bitSet.clone();
		cBitSetCopy.and(bitSet);
		if(cBitSetCopy.isEmpty())
			return false;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends jq_Field> c) {
		if(c instanceof FieldBitSet){
			BitSet cBitSet = (BitSet)((FieldBitSet)c).bitSet;
			BitSet bitSetCopy = (BitSet)bitSet.clone();
			bitSet.or(cBitSet);
			return !bitSet.equals(bitSetCopy);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if(c instanceof FieldBitSet){
			BitSet cBitSet = (BitSet)((FieldBitSet)c).bitSet;
			BitSet bitSetCopy = (BitSet)bitSet.clone();
			bitSet.and(cBitSet);
			return !bitSet.equals(bitSetCopy);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if(c instanceof FieldBitSet){
			BitSet cBitSet = (BitSet)((FieldBitSet)c).bitSet;
			BitSet bitSetCopy = (BitSet)bitSet.clone();
			bitSet.andNot(cBitSet);
			return !bitSet.equals(bitSetCopy);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		StringBuffer sv = new StringBuffer();
		sv.append("FieldBitSet [");
		int startIndex = bitSet.nextSetBit(0);
		while(startIndex >= 0){
			sv.append(domF.get(startIndex)+",");
			startIndex = bitSet.nextSetBit(startIndex+1);
		}
		sv.append("]");
		return sv.toString();
	}

	@Override
	public int size() {
		return bitSet.cardinality();
	}

	@Override
	public void clear() {
		bitSet.clear();
	}

	@Override
	public boolean isEmpty() {
		return bitSet.isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bitSet == null) ? 0 : bitSet.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FieldBitSet other = (FieldBitSet) obj;
		if (bitSet == null) {
			if (other.bitSet != null)
				return false;
		} else if (!bitSet.equals(other.bitSet))
			return false;
		return true;
	}	
	
}
