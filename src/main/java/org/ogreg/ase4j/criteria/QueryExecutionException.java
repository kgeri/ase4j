package org.ogreg.ase4j.criteria;

/**
 * Signals an error in query execution.
 * <p>
 * Thrown when the query execution has failed for some reason - most probably
 * because the result set to return would be too big.
 * </p>
 * 
 * @author Gergely Kiss
 */
public class QueryExecutionException extends Exception {
	private static final long serialVersionUID = 5604473864785457225L;

	public QueryExecutionException(String message) {
		super(message);
	}

	public QueryExecutionException(Throwable cause) {
		super(cause);
	}
}
