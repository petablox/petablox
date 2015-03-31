package conf_analyzer.stubs;

import java.util.*;

public class ModelList<E> implements List<E>, Queue<E>, Deque<E>, SortedSet<E>, NavigableSet<E> {
	
	static class ModelListIter<E> implements ListIterator<E>{
		E contents;

		public ModelListIter(E contents2) {
			contents = contents2;
		}

		@Override
		public void add(E o) {
			contents = o;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public boolean hasPrevious() {
			return false;
		}

		@Override
		public E next() {
			return contents;
		}

		@Override
		public int nextIndex() {
			return 0;
		}

		@Override
		public E previous() {
			return contents;
		}

		@Override
		public int previousIndex() {
			return 0;
		}

		@Override
		public void remove() {
			
		}

		@Override
		public void set(E o) {
			contents = o;			
		}
	}
	
	E contents;

	@Override
	public boolean add(E o) {
		contents = o;
		return false;
	}

	@Override
	public void add(int index, E element) {
		contents = element;		
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if(c instanceof ModelList) {
			contents = ((ModelList<E>) c).contents;
		}
		return contents.equals(null); //to enforce data dependency on contents
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		if(c instanceof ModelList) {
			contents = ((ModelList<E>) c).contents;
		}
		return contents.equals(null); //to enforce data dependency on contents
	}

	@Override
	public void clear() {
		
	}

	@Override
	public boolean contains(Object o) {
		return contents.equals(o); //to enforce data dependency on contents
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void ensureCapacity(int i) {}

	@Override
	public E get(int index) {
		return contents;
	}
	

	public E getFirst() {
		return contents;
	}


	@Override
	public int indexOf(Object o) {
		return contents.hashCode(); //a data dependence on content
	}

	@Override
	public boolean isEmpty() {
		return contents.equals(null);
	}

	@Override
	public Iterator<E> iterator() {
		return new ModelListIter<E>(contents);
	}

	@Override
	public int lastIndexOf(Object o) {
		return contents.hashCode(); //a data dependence on content
	}

	@Override
	public ListIterator<E> listIterator() {
		return new ModelListIter<E>(contents);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new ModelListIter<E>(contents);
	}

	@Override
	public boolean remove(Object o) {
		return contents.equals(o);
	}

	@Override
	public E remove(int index) {
		return contents;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	@Override
	public E set(int index, E element) {
		contents = element;
		return contents;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return this;
	}

	@Override
	public Object[] toArray() {
		Object[] cont = new Object[1];
		cont[0] = contents;
		return cont;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		T[] cont = a;
		cont[0] = (T) contents;
		return (T[]) cont;
	}

	@Override
	public E element() {
		return contents;
	}

	@Override
	public boolean offer(E o) {
		contents = o;
		return false;
	}

	@Override
	public E peek() {
		return contents;
	}

	@Override
	public E poll() {
		return contents;
	}

	@Override
	public E remove() {
		return contents;
	}

	/* Queue methods. */
//	@Override
	public boolean hasNext() {
		return contents.equals(null); //to enforce data dependency on contents
	}

//	@Override
	public E next() {
		return contents;
	}
/*
	@Override
	public void remove() {
		
	} */

	@Override
	public void addFirst(E arg0) {
		contents = arg0;
	}

	@Override
	public void addLast(E arg0) {
		contents = arg0;
	}

	@Override
	public Iterator<E> descendingIterator() {
		return new ModelListIter<E>(contents);
	}

	@Override
	public E getLast() {
		return contents;
	}

	@Override
	public boolean offerFirst(E arg0) {
		contents = arg0;
		return false;
	}

	@Override
	public boolean offerLast(E arg0) {
		contents = arg0;
		return false;
	}

	@Override
	public E peekFirst() {
		return contents;
	}

	@Override
	public E peekLast() {
		return contents;
	}

	@Override
	public E pollFirst() {
		return contents;
	}

	@Override
	public E pollLast() {
		return contents;
	}

	@Override
	public E pop() {
		return contents;
	}

	@Override
	public void push(E arg0) {
		contents = arg0;
	}

	@Override
	public E removeFirst() {
		return contents;
	}

	@Override
	public boolean removeFirstOccurrence(Object arg0) {
		return contents.equals(arg0);
	}

	@Override
	public E removeLast() {
		return contents;
	}

	@Override
	public boolean removeLastOccurrence(Object arg0) {
		return contents.equals(arg0);
	}

	@Override
	public Comparator<? super E> comparator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public E first() {
		return contents;
	}

	@Override
	public SortedSet<E> headSet(E toElement) {
		return this;
	}

	@Override
	public E last() {
		return contents;
	}

	@Override
	public SortedSet<E> subSet(E fromElement, E toElement) {
		return this;
	}

	@Override
	public SortedSet<E> tailSet(E fromElement) {
		return this;
	}
	
	public void trimToSize() {
	}

	@Override
	public E ceiling(E arg0) {
		return contents;
	}

	@Override
	public NavigableSet<E> descendingSet() {
		return this;
	}

	@Override
	public E floor(E arg0) {
		return contents;
	}

	@Override
	public NavigableSet<E> headSet(E arg0, boolean arg1) {
		return this;
	}

	@Override
	public E higher(E arg0) {
		return contents;
	}

	@Override
	public E lower(E arg0) {
		return contents;
	}

	@Override
	public NavigableSet<E> subSet(E arg0, boolean arg1, E arg2, boolean arg3) {
		return this;
	}

	@Override
	public NavigableSet<E> tailSet(E arg0, boolean arg1) {
		return this;
	}

}
