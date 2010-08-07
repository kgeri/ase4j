package org.ogreg.ostore;

import org.ogreg.common.utils.PropertyUtils;

import org.ogreg.ostore.index.UniqueIndex;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Common base class for property-based object stores.
 *
 * @param   <T>
 *
 * @author  Gergely Kiss
 */
public abstract class BaseObjectStore<T> implements ConfigurableObjectStore<T>, Closeable {

    /** The entity's field persistors keyed by the field names. */
    private final Map<String, PropertyPersistor> persistors =
        new HashMap<String, PropertyPersistor>();

    /** The entity's unique indices keyed by their field names. */
    private final Map<String, UniqueIndex> uniqueIndices = new HashMap<String, UniqueIndex>();

    /**
     * The field accessor which has the business key.
     *
     * <p>Please note that it's not necessary for an object to have a business key (so this may be null), but some
     * operations will only work if there is one.</p>
     */
    // TODO Composite id
    private String uniqueFieldName;

    /** The property accessor used to access the stored type's properties. */
    private EntityAccessor accessor;

    /**
     * The implementor should return the next available identifier.
     *
     * <p>The implementation must be threadsafe.</p>
     *
     * @return
     */
    protected abstract long getNextId();

    /**
     * The implementor may flush the specified index here.
     *
     * @param   propertyName
     * @param   index
     *
     * @throws  IOException
     */
    protected abstract void flushUniqueIndex(String propertyName, UniqueIndex index)
        throws IOException;

    /**
     * The implementor should create an implementation specific simple property persistor here.
     *
     * @param   propertyType
     * @param   propertyName
     *
     * @return
     */
    protected abstract PropertyPersistor createPersistor(Class<?> propertyType,
        String propertyName);

    /**
     * The implementor should create an implementation specific extension persistor here.
     *
     * @param   propertyName
     *
     * @return
     */
    protected abstract PropertyPersistor createExtensionPersistor(String propertyName);

    @Override public void init(EntityAccessor accessor, File storageDir,
        Map<String, String> params) {
        this.accessor = accessor;
    }

    @Override public long save(T entity) throws ObjectStoreException {
        Object key = getBusinessKeyValue(entity);

        Long identifier = uniqueResult(uniqueFieldName, key);

        // Save
        if (identifier == null) {
            identifier = getNextId();

            // No updates here
            add(identifier, entity);
        }

        return identifier;
    }

    @Override public long saveOrUpdate(T entity) throws ObjectStoreException {
        Object key = getBusinessKeyValue(entity);

        Long identifier = uniqueResult(uniqueFieldName, key);

        // Save or update
        if (identifier == null) {
            identifier = getNextId();
        }

        // Update if necessary
        add(identifier, entity);

        return identifier;
    }

    @Override public void add(long identifier, T entity) throws ObjectStoreException {

        try {

            // Saving properties
            for (Entry<String, PropertyPersistor> e : persistors.entrySet()) {
                String propertyName = e.getKey();
                Object value = accessor.getFrom(entity, propertyName);

                if (value == null) {
                    continue;
                }

                // Updating indices
                UniqueIndex idx = uniqueIndices.get(propertyName);

                if (idx != null) {
                    idx.setKey(value, identifier);
                }

                e.getValue().store(identifier, null, value);
            }
        } catch (IllegalArgumentException e) {
            throw new ObjectStoreException(e);
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        } catch (IOException e) {
            throw new ObjectStoreException(e);
        }
    }

    @Override public T get(long identifier) throws ObjectStoreException {

        try {
            @SuppressWarnings("unchecked")
            T result = (T) accessor.newInstance();

            for (Entry<String, PropertyPersistor> e : persistors.entrySet()) {
                Object value = e.getValue().load(identifier, null);
                accessor.setTo(result, e.getKey(), value);
            }

            return result;
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
    }

    @Override public Object getField(long identifier, String fieldName)
        throws ObjectStoreException {
        String[] pathElements = PropertyUtils.splitFirstPathElement(fieldName);
        PropertyPersistor persistor = persistors.get(pathElements[0]);

        if (persistor == null) {
            throw new ObjectStoreException("Property not found: " + accessor.getTypeName() + "." +
                fieldName);
        }

        try {
            return persistor.load(identifier, pathElements[1]);
        } catch (IOException e) {
            throw new ObjectStoreException(e);
        }
    }

    @Override public Long uniqueResult(String fieldName, Object value) throws ObjectStoreException {
        UniqueIndex idx = uniqueIndices.get(fieldName);

        if (idx == null) {
            throw new ObjectStoreException("Field " + accessor.getTypeName() + "." + fieldName +
                " does not have a unique index specified.");
        }

        if (value == null) {
            throw new ObjectStoreException("The field " + accessor.getTypeName() + "." + fieldName +
                " has a unique index, so its value should not be null.");
        }

        return idx.getKey(value);
    }

    @Override public void flush() throws IOException {

        for (PropertyPersistor persistor : persistors.values()) {
            persistor.flush();
        }

        for (Entry<String, UniqueIndex> e : uniqueIndices.entrySet()) {
            flushUniqueIndex(e.getKey(), e.getValue());
        }
    }

    @Override public void close() throws IOException {

        for (PropertyPersistor persistor : persistors.values()) {
            persistor.close();
        }
    }

    @Override public void setIdPropertyName(String propertyName) {
        uniqueFieldName = propertyName;
    }

    @Override public void addProperty(Class<?> propertyType, String propertyName) {

        if (persistors.containsKey(propertyName)) {
            throw new IllegalArgumentException("Property " + propertyName +
                " is already configured. You cannot configure a property with the same name twice.");
        }

        persistors.put(propertyName, createPersistor(propertyType, propertyName));
    }

    @Override public void addExtension(String propertyName) {

        if (persistors.containsKey(propertyName)) {
            throw new IllegalArgumentException("Extension " + propertyName +
                " is already configured. You cannot configure an extension with the same name twice.");
        }

        persistors.put(propertyName, createExtensionPersistor(propertyName));
    }

    @Override public void addIndex(String fieldName, UniqueIndex idx) {
        uniqueIndices.put(fieldName, idx);
    }

    private Object getBusinessKeyValue(T entity) throws ObjectStoreException {

        if (uniqueFieldName == null) {
            throw new IllegalStateException("The type " + accessor.getTypeName() +
                " did not specify a business key. Please mark an appropriate business key with <id> before using this operation.");
        }

        try {
            return accessor.getFrom(entity, uniqueFieldName);
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
    }
}
