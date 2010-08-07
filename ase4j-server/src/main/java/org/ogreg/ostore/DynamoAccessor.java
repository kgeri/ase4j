package org.ogreg.ostore;

import org.ogreg.common.dynamo.DynamicObject;
import org.ogreg.common.dynamo.DynamicType;

import java.util.Map;


/**
 * {@link DynamicObject}-based accessor.
 *
 * @author  Gergely Kiss
 */
class DynamoAccessor implements EntityAccessor {
    private final String storedTypeName;
    private DynamicType type;

    public DynamoAccessor(String storedTypeName) {
        this.storedTypeName = storedTypeName;
    }

    @Override public Object getFrom(Object source, String propertyName)
        throws IllegalAccessException {
        return ((DynamicObject) source).get(propertyName);
    }

    @Override public void setTo(Object source, String propertyName, Object value)
        throws IllegalAccessException {
        ((DynamicObject) source).set(propertyName, value);
    }

    @Override public Object newInstance() throws InstantiationException {
        return new DynamicObject(type);
    }

    @Override public void setProperties(Map<String, Class<?>> properties) {
        type = DynamicType.define(storedTypeName, properties);
    }

    @Override public String getTypeName() {
        return type.toString();
    }

    public DynamicType getType() {
        return type;
    }
}
