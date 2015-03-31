package chord.project.analyses.tdbu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;

/**
 * This class is specified from current package since I didn't strictly follow
 * the specification of SortedSet, so use with caution.
 * 
 * @author xin
 * 
 * @param <E>
 */
public class SortedArraySet<E> extends ArrayList<E> implements SortedSet<E> {
	private Comparator<? super E> cmp;

	public SortedArraySet(Comparator<? super E> cmp) {
		this.cmp = cmp;
	}

	public SortedArraySet(SortedSet<E> sortedSet) {
		for (E e : sortedSet)
			super.add(e);
		this.cmp = sortedSet.comparator();
	}

	private SortedArraySet(List<E> other, Comparator<? super E> cmp) {
		super(other);
		this.cmp = cmp;
	}

	@Override
	public Comparator<? super E> comparator() {
		return cmp;
	}

	@Override
	public SortedSet<E> subSet(E fromElement, E toElement) {
		int fromIdx = this.indexOf(fromElement);
		int toIdx = this.indexOf(toElement);
		return new SortedArraySet<E>(super.subList(fromIdx, toIdx), this.cmp);// Inconsistent
																				// with
																				// jdk
																				// spec
	}

	@Override
	public SortedSet<E> headSet(E toElement) {
		int toIdx = this.indexOf(toElement);
		return new SortedArraySet<E>(super.subList(0, toIdx), this.cmp);// Inconsistent
																		// with
																		// jdk
																		// spec
	}

	@Override
	public SortedSet<E> tailSet(E fromElement) {
		int fromIdx = this.indexOf(fromElement);
		return new SortedArraySet<E>(super.subList(fromIdx, this.size()),
				this.cmp);// Inconsistent with jdk spec
	}

	@Override
	public E first() {
		return this.get(0);
	}

	@Override
	public E last() {
		return this.get(this.size() - 1);
	}

	@Override
	public boolean add(E e) {
		int insertIdx = this.size();
		for (int i = 0; i < this.size(); i++) {
			int c = cmp.compare(this.get(i), e);
			if (c == 0)
				return false;
			if (c > 0) {
				insertIdx = i;
				break;
			}
		}
		this.add(insertIdx, e);
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean ret = false;
		for (E e : c)
			ret |= this.add(e);
		return ret;
	}

}
