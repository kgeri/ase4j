package org.ogreg.ostore;

import org.ogreg.common.utils.PropertyUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import java.util.HashMap;
import java.util.Map;


/**
 * Simple field-based entity accessor.
 *
 * @author  Gergely Kiss
 */
class FieldAccessor implements EntityAccessor {

    /** The entity type to access. */
    private Class<?> storedType;

    /** The cached constructor of the stored Java type. */
    private Constructor<?> storedCtor;

    /** The condigured fields for this accessor. */
    private final Map<String, Field> fields = new HashMap<String, Field>();

    FieldAccessor(Class<?> type) {

        try {
            this.storedType = type;
            this.storedCtor = type.getDeclaredConstructor();
            storedCtor.setAccessible(true);
        } catch (SecurityException e) {
            throw new IllegalArgumentException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Stored type " + type.getName() +
                " should have a default constructor", e);
        }
    }

    @Override public Object getFrom(Object source, String propertyName)
        throws IllegalAccessException {
        return fields.get(propertyName).get(source);
    }

    @Override public void setTo(Object source, String propertyName, Object value)
        throws IllegalArgumentException, IllegalAccessException {
        fields.get(propertyName).set(source, value);
    }

    @Override public void addProperty(String propertyName) {
        fields.put(propertyName, PropertyUtils.getField(storedType, propertyName));
    }

    @Override public Object newInstance() throws InstantiationException {

        try {
            return storedCtor.newInstance();
        } catch (Exception e) {
            throw new InstantiationException("Failed to instantiate " + storedType.getName() +
                ": " + e.getLocalizedMessage());
        }
    }
}
