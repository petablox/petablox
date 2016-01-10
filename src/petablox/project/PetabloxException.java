package petablox.project;

/**
 * A base class for Chord's runtime exceptions.
 */
public class PetabloxException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public PetabloxException() {
	}

	public PetabloxException(String message) {
		super(message);
	}

	public PetabloxException(Throwable cause) {
		super(cause);
	}

	public PetabloxException(String message, Throwable cause) {
		super(message, cause);
	}

	public PetabloxException(String message, Throwable cause,
							 boolean enableSuppression, boolean writableStackTrace) {
		//super(message, cause, enableSuppression, writableStackTrace);
	}
}
