package org.ogreg.ase4j.criteria;

/**
 * Common interface for expression solver results which support negation.
 * 
 * @author Gergely Kiss
 */
public interface QueryResult {

	/**
	 * Sets or clears the negated flag on the result.
	 * 
	 * @param negated
	 */
	void setNegated(boolean negated);

	/**
	 * Returns true if the result is negated.
	 * 
	 * @return
	 */
	boolean isNegated();
}
