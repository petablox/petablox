package stamp.util;

/**
 * A sequence formed by applying {@link #mapElem(S)} to each element of
 * another sequence.
 */
public abstract class Mapper<T,S> extends IterableWrapper<T,S> {
	public Mapper(Iterable<S> childElems) {
		super(childElems);
	}

	public abstract T mapElem(S childElem);

	@Override
	public final Iterable<T> processElem(S childElem) {
		return new Cell<T>(mapElem(childElem));
	}
}
