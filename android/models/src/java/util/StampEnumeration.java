package java.util;

public class StampEnumeration<E> implements Enumeration<E>
{
	private E f;

	public StampEnumeration(E e) {
		this.f = e;
	}

	public boolean hasMoreElements() {
		return true;
	}

	public E nextElement() {
		return this.f;
	}
}