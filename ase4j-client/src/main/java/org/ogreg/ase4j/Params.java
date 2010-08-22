package org.ogreg.ase4j;

import java.io.Serializable;

import org.ogreg.ase4j.AssociationStore.Operation;

/**
 * Association parameters.
 * <p>
 * Association parameters specify the conditions for adding or querying
 * associations.
 * </p>
 * 
 * @author Gergely Kiss
 */
public class Params implements Serializable {
	private static final long serialVersionUID = 1090624754798154827L;
	private static final Params DEFAULT_PARAMS = new Params();

	/** The operation to use when adding associations. */
	public final AssociationStore.Operation op;

	public Params() {
		this(Operation.AVG);
	}

	public Params(AssociationStore.Operation op) {
		this.op = op;
	}

	public static Params ensureNotNull(Params params) {
		return params == null ? DEFAULT_PARAMS : params;
	}
}