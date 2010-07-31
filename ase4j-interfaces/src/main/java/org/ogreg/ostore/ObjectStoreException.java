package org.ogreg.ostore;

/**
 * Exception class signalling an error in object storage.
 * <p>
 * Received when the store has failed to load or store an object.
 * </p>
 * 
 * @author Gergely Kiss
 */
public class ObjectStoreException extends Exception {
	private static final long serialVersionUID = 6349307737327477105L;

	public ObjectStoreException(String message) {
		super(message);
	}

	public ObjectStoreException(Throwable cause) {
		super(cause);
	}

	public ObjectStoreException(String message, Throwable cause) {
		super(message, cause);
	}
}
