package chord.project;

/**
 * A base class for Chord's runtime exceptions.
 */
public class ChordException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public ChordException() {
	}

	public ChordException(String message) {
		super(message);
	}

	public ChordException(Throwable cause) {
		super(cause);
	}

	public ChordException(String message, Throwable cause) {
		super(message, cause);
	}

	public ChordException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
