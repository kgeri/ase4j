package org.ogreg.ase4j;

/**
 * Common association helper methods.
 * 
 * @author Gergely Kiss
 */
public abstract class AssociationUtils {

	/**
	 * Updates the association.
	 * 
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	public static final int update(int oldValue, int newValue) {
		// TODO make it a bit more sophisticated...
		return (newValue + oldValue) / 2;
	}
}
