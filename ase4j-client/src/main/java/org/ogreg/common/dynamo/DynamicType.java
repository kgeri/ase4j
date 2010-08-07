package org.ogreg.common.dynamo;

import java.io.Serializable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * {@link DynamicObject} type.
 *
 * <p>The purpose of this class is to store type and field name information about a dynamic type. Like with {@link
 * java.lang.Class}es, the {@link DynamicType}s are also singletons. You may use</p>
 *
 * @author  Gergely Kiss
 */
public final class DynamicType implements Serializable {
    private static final long serialVersionUID = 944089596559004323L;

    // DynamicType singleton storage
    private static final Map<String, DynamicType> types = new HashMap<String, DynamicType>();

    /** The types of the fields. */
    private Class<?>[] fieldTypes;

    /** The field indexes keyed by the field names. */
    private Map<String, Integer> fields;

    /** The dynamic type name. */
    private String name;

    /**
     * Creates a new dynamic object type.
     *
     * @param  fields  The names and types of the fields of the object
     */
    private DynamicType(String name, Map<String, Class<?>> fields) {
        this.name = name;
        this.fields = new LinkedHashMap<String, Integer>(fields.size());
        this.fieldTypes = new Class<?>[fields.size()];

        int i = 0;

        for (Entry<String, Class<?>> e : fields.entrySet()) {
            this.fields.put(e.getKey(), i);
            this.fieldTypes[i] = e.getValue();
            i++;
        }
    }

    /**
     * Returns the name of this dynamic type.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the index of the given <code>field</code>.
     *
     * @param   field  The name of the field
     *
     * @return  The index where field values may be found in the {@link DynamicObject}'s values array
     *
     * @throws  DynamicAccessException  if the field does not exist in this type
     */
    final int fieldIndex(String field) throws DynamicAccessException {
        Integer f = fields.get(field);

        if (f == null) {
            throw new DynamicAccessException("Field '" + field +
                "' was not found on dynamic type: " + toString());
        }

        return f;
    }

    /**
     * Performs a type check on the specified <code>fieldIndex</code> with the specified <code>type</code>.
     *
     * @param   fieldIndex
     * @param   type
     *
     * @throws  ClassCastException  if <code>fieldType</code> is not assignable from <code>type</code>
     */
    final void typeCheck(int fieldIndex, Class<?> type) {
        Class<?> fieldType = fieldTypes[fieldIndex];

        if (!fieldType.isAssignableFrom(type)) {
            throw new ClassCastException("Cannot cast " + type.getName() + " to " +
                fieldType.getName());
        }
    }

    /**
     * Returns the number of the fields in this type.
     *
     * @return
     */
    final int fieldCount() {
        return fields.size();
    }

    @Override public String toString() {
        return new StringBuilder("dynamo ").append(fields).toString();
    }

    /**
     * Defines the dynamic type of <code>name</code> and <code>fields</code>.
     *
     * @param   name
     * @param   fields
     *
     * @return
     */
    public static DynamicType define(String name, Map<String, Class<?>> fields) {
        DynamicType type = types.get(name);

        if (type == null) {
            type = new DynamicType(name, fields);
            types.put(name, type);
        }

        return type;
    }

    /**
     * Defines the dynamic type of <code>name</code> and <code>fields</code>.
     *
     * @param   name
     * @param   fields
     *
     * @return  The dynamic type, never null
     *
     * @throws  ClassNotFoundException  if no type was defined with the specified <code>name</code>
     */
    public static DynamicType forName(String name) throws ClassNotFoundException {
        DynamicType type = types.get(name);

        if (type == null) {
            throw new ClassNotFoundException("DynamicType was not found: " + name +
                " Please use the define method to define dynamic types prior to usage.");
        }

        return type;
    }
}
