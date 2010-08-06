package org.ogreg.ostore;

import org.ogreg.ostore.ObjectStore;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;


/**
 * SPI interface for {@link ObjectStore} property persistors.
 *
 * <p>Property persistors are used for maintaining the connection between the persistent and in-memory state of an
 * object field. The persistor can load and store property values in an object storage.</p>
 *
 * @author  Gergely Kiss
 */
public interface PropertyPersistor extends Closeable, Flushable {

    /**
     * Returns the field value for the object with the given identifier from the object store.
     *
     * @param   identifier    The identifier of the object to access
     * @param   propertyPath  The path of the property to access. If null, then this field should be accessed, otherwise
     *                        the call must be delegated to the child accessors (if any)
     *
     * @return
     *
     * @throws  IOException  on storage error
     */
    Object load(long identifier, String propertyPath) throws IOException;

    /**
     * Updates the field value for the object with the given identifier in the object store.
     *
     * @param   identifier    The identifier of the object to update
     * @param   propertyPath  The path of the property to update. If null, then this field should be updated, otherwise
     *                        the call must be delegated to the child accessors (if any)
     * @param   value         The value to update to
     *
     * @throws  IOException  on storage error
     */
    void store(long identifier, String propertyPath, Object value) throws IOException;
}
