package org.ogreg.ase4j;

import java.io.Serializable;


/**
 * Represents an association.
 *
 * <p>Associations are edges of a graph which has nodes of type <code>F</code> and <code>T</code>. Every association has
 * a strength, which is represented as a float value. Assocations are comparable, but please not that their natural
 * order is <b>descending</b>.</p>
 *
 * @param   <F>  The type of the association source
 * @param   <T>  The type of the association target
 *
 * @author  Gergely Kiss
 */
public final class Association<F, T> implements Serializable, Comparable<Association<F, T>> {
    private static final long serialVersionUID = -9143244773679471224L;

    public final F from;
    public final T to;
    public float value;

    public Association(F from, T to, float value) {
        this.from = from;
        this.to = to;
        this.value = value;
    }

    @Override public int compareTo(Association<F, T> o) {
        return Float.compare(o.value, value);
    }
}
