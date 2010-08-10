package org.ogreg.ostore;

import org.ogreg.common.dynamo.DynamicType;

import java.io.Serializable;


/**
 * Object store description and schema information.
 *
 * @author  Gergely Kiss
 */
public class ObjectStoreMetadata implements Serializable {
    private static final long serialVersionUID = 4510331494411649651L;

    private final Class<?> staticType;
    private final DynamicType dynamicType;

    public ObjectStoreMetadata(Class<?> staticType, DynamicType dynamicType) {
        this.staticType = staticType;
        this.dynamicType = dynamicType;
    }

    /**
     * Returns the static type of the stored entity, or null if the storage mode is not <code>class</code>.
     *
     * @return
     *
     * @see
     */
    public Class<?> getStaticType() {
        return staticType;
    }

    /**
     * Returns the dynamic type of the stored entity, or null if the storage mode is not <code>dynamic</code>.
     *
     * @return
     *
     * @see
     */
    public DynamicType getDynamicType() {
        return dynamicType;
    }

    /**
     * Returns true if storage mode is <code>class</code> for this store.
     *
     * @return
     */
    public boolean isStatic() {
        return staticType != null;
    }

    /**
     * Returns true if storage mode is <code>dynamic</code> for this store.
     *
     * @return
     */
    public boolean isDynamic() {
        return dynamicType != null;
    }
}
