package org.ogreg.ostore;

import org.ogreg.common.ConfigurationException;

import org.ogreg.ostore.index.UniqueIndex;

import java.io.File;

import java.util.Map;


/**
 * SPI interface for configurable object stores.
 *
 * <p>Every object store initialized by the {@link ObjectStoreManager} must implement this interface, however, the
 * different store implementations may ignore the calls, or throw {@link ConfigurationException} if they do not support
 * an operation.</p>
 *
 * @author  Gergely Kiss
 */
public interface ConfigurableObjectStore<T> extends ObjectStore<T> {

    /**
     * The implementor may perform store initialization here.
     *
     * @param  accessor    The entity accessor for this type
     * @param  storageDir  The storage data dir (if file-based)
     * @param  params      Implementation specific storage parameters
     */
    void init(EntityAccessor accessor, File storageDir, Map<String, String> params);

    /**
     * Sets the given property as the id of the stored type.
     *
     * @param  propertyName
     */
    void setIdPropertyName(String propertyName);

    /**
     * Configures a new property in the store.
     *
     * @param   propertyType
     * @param   propertyName
     *
     * @throws  UnsupportedOperationException  if the implementor does not support properties
     */
    void addProperty(Class<?> propertyType, String propertyName);

    /**
     * Configures a new extension in the store.
     *
     * @param   propertyName
     *
     * @throws  UnsupportedOperationException  if the implementor does not support extensions
     */
    void addExtension(String propertyName);

    /**
     * Adds an index to the specified property.
     *
     * <p>Does nothing if the implementor does not support indices.</p>
     *
     * @param  fieldName
     * @param  idx
     */
    void addIndex(String fieldName, UniqueIndex idx);

    /**
     * Sets the store's metadata.
     *
     * @param  object
     */
    void setMetadata(ObjectStoreMetadata metadata);
}
