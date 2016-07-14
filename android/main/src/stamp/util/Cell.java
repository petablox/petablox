package stamp.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A single-element container.
 */
public class Cell<T> implements Iterable<T> {
	private final T elem;

	public Cell(T elem) {
		this.elem = elem;
	}

	public T get() {
		return elem;
	}

	public Iterator<T> iterator() {
		return new SingleElemIterator();
	}

	private class SingleElemIterator implements Iterator<T> {
		private boolean retrieved = false;

		public boolean hasNext() {
			return !retrieved;
		}

		public T next() {
			if (retrieved) {
				throw new NoSuchElementException();
			}
			retrieved = true;
			return elem;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
