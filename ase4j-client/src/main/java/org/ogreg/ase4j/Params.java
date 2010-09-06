package org.ogreg.ase4j;

import org.ogreg.ase4j.AssociationStore.Operation;

import java.io.Serializable;


/**
 * Association parameters.
 *
 * <p>Association parameters specify the conditions for adding or querying associations.</p>
 *
 * @author  Gergely Kiss
 */
public class Params implements Serializable {
    private static final long serialVersionUID = 1090624754798154827L;
    private static final Params DEFAULT_PARAMS = new Params();

    /** The operation to use when adding associations. */
    public final Operation op;

    public Params() {
        this(Operation.AVG);
    }

    public Params(Operation op) {
        this.op = op;
    }

    public static Params ensureNotNull(Params params) {
        return (params == null) ? DEFAULT_PARAMS : params;
    }
}
