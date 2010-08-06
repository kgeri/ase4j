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

    /** The entity's field accessors keyed by the field names. */
    private final Map<String, PropertyHandler> handlers = new HashMap<String, PropertyHandler>();

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

    /** The Java type stored by this Object Store. */
    protected Class<T> storedType;

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

    @Override public void init(Class<T> type, EntityAccessor accessor, File storageDir,
        Map<String, String> params) {
        this.storedType = type;
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
            for (PropertyHandler handler : handlers.values()) {
                String propertyName = handler.propertyName;
                Object value = accessor.getFrom(entity, propertyName);

                if (value == null) {
                    continue;
                }

                // Updating indices
                UniqueIndex idx = uniqueIndices.get(propertyName);

                if (idx != null) {
                    idx.setKey(value, identifier);
                }

                handler.persistor.store(identifier, null, value);
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

            for (PropertyHandler handler : handlers.values()) {
                Object value = handler.persistor.load(identifier, null);
                accessor.setTo(result, handler.propertyName, value);
            }

            return result;
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
    }

    @Override public Object getField(long identifier, String fieldName)
        throws ObjectStoreException {
        String[] pathElements = PropertyUtils.splitFirstPathElement(fieldName);
        PropertyHandler handler = handlers.get(pathElements[0]);

        if (handler == null) {
            throw new ObjectStoreException("Property not found: " + storedType.getName() + "." +
                fieldName);
        }

        try {
            return handler.persistor.load(identifier, pathElements[1]);
        } catch (IOException e) {
            throw new ObjectStoreException(e);
        }
    }

    @Override public Long uniqueResult(String fieldName, Object value) throws ObjectStoreException {
        UniqueIndex idx = uniqueIndices.get(fieldName);

        if (idx == null) {
            throw new ObjectStoreException("Field " + storedType.getName() + "." + fieldName +
                " does not have a unique index specified.");
        }

        if (value == null) {
            throw new ObjectStoreException("The field " + storedType.getName() + "." + fieldName +
                " has a unique index, so its value should not be null.");
        }

        return idx.getKey(value);
    }

    @Override public void flush() throws IOException {

        for (PropertyHandler handler : handlers.values()) {
            handler.persistor.flush();
        }

        for (Entry<String, UniqueIndex> e : uniqueIndices.entrySet()) {
            flushUniqueIndex(e.getKey(), e.getValue());
        }
    }

    @Override public void close() throws IOException {

        for (PropertyHandler handler : handlers.values()) {
            handler.persistor.close();
        }
    }

    @Override public void setIdPropertyName(String propertyName) {
        uniqueFieldName = propertyName;
    }

    @Override public void addProperty(Class<?> propertyType, String propertyName) {

        if (handlers.containsKey(propertyName)) {
            throw new IllegalArgumentException("Property " + propertyName +
                " is already configured. You cannot configure a property with the same name twice.");
        }

        PropertyHandler handler = new PropertyHandler();
        handler.propertyName = propertyName;
        handler.persistor = createPersistor(propertyType, propertyName);
        handlers.put(propertyName, handler);
    }

    @Override public void addExtension(String propertyName) {

        if (handlers.containsKey(propertyName)) {
            throw new IllegalArgumentException("Extension " + propertyName +
                " is already configured. You cannot configure an extension with the same name twice.");
        }

        PropertyHandler handler = new PropertyHandler();
        handler.propertyName = propertyName;
        handler.persistor = createExtensionPersistor(propertyName);
        handlers.put(propertyName, handler);
    }

    @Override public void addIndex(String fieldName, UniqueIndex idx) {
        uniqueIndices.put(fieldName, idx);
    }

    private Object getBusinessKeyValue(T entity) throws ObjectStoreException {

        if (uniqueFieldName == null) {
            throw new IllegalStateException("The type " + storedType.getName() +
                " did not specify a business key. Please mark an appropriate business key with <id> before using this operation.");
        }

        try {
            return accessor.getFrom(entity, uniqueFieldName);
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * Property access and persistor VO.
     *
     * @author  Gergely Kiss
     */
    private class PropertyHandler {
        public String propertyName;
        public PropertyPersistor persistor;
    }
}
