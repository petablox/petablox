package stamp.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A sequence formed by applying {@link #processElem(S)} to each element of
 * another sequence. {@link #processElem(S)} is itself expected to return a
 * sequence, so we flatten the final result.
 *
 * e.g. wrapping {@code ["1:2","3","4:5:6"]} with {@code s => s.split(":")}
 * would give {@code ["1","2","3","4","5","6"]}.
 */
public abstract class IterableWrapper<T,S> implements Iterable<T> {
	private final Iterable<S> childElems;

	public IterableWrapper(Iterable<S> childElems) {
		this.childElems = childElems;
	}

	public abstract Iterable<T> processElem(S childElem);

	public Iterator<T> iterator() {
		return new WrapperIterator();
	}

	private class WrapperIterator implements Iterator<T> {
		private final Iterator<S> childIter;
		private Iterator<T> wrappedIter = null;

		public WrapperIterator() {
			childIter = childElems.iterator();
		}

		public boolean hasNext() {
			while (wrappedIter == null || !wrappedIter.hasNext()) {
				if (!childIter.hasNext()) {
					return false;
				}
				wrappedIter = processElem(childIter.next()).iterator();
			}
			return true;
		}

		public T next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			return wrappedIter.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
