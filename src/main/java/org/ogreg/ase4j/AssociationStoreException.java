package org.ogreg.ase4j;

/**
 * Exception class signalling an error in association storage.
 * 
 * <p>
 * Received when the store has failed to load or save an association.
 * </p>
 * 
 * @author Gergely Kiss
 */
public class AssociationStoreException extends Exception {
	private static final long serialVersionUID = 5769610247740613990L;

    public AssociationStoreException(Throwable cause) {
		super(cause);
	}

    public AssociationStoreException(String message, Throwable cause) {
		super(message, cause);
	}
}
