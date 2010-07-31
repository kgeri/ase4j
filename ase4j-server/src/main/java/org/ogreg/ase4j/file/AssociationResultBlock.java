package org.ogreg.ase4j.file;

import org.ogreg.ase4j.criteria.QueryResult;

/**
 * A type for storing association query results.
 * 
 * @author Gergely Kiss
 */
// While very similar to an {@link AssociationBlock}, it also contains the
// current state of negation and misses the control fields.
class AssociationResultBlock implements QueryResult {

	/** True if the result is negated. */
	private boolean negated = false;

	/**
	 * The <code>to</code> parts of the associations sorted numerically,
	 * ascending. Has the same order as {@link #values}.
	 */
	int[] tos;

	/**
	 * The <code>value</code>s of the associations. Has the same order as
	 * {@link #tos}.
	 */
	int[] values;

	/** The number of associations currently stored in the row. */
	int size;

	public AssociationResultBlock(int size) {
		this.size = size;
		this.tos = new int[size];
		this.values = new int[size];
	}

	public AssociationResultBlock(int[] tos, int[] values, int size) {
		this.size = size;
		this.tos = tos;
		this.values = values;
	}

	@Override
	public boolean isNegated() {
		return negated;
	}

	@Override
	public void setNegated(boolean negated) {
		this.negated = negated;
	}

}
