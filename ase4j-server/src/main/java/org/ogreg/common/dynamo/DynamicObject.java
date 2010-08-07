package org.ogreg.common.dynamo;

import java.io.Serializable;

import java.util.Arrays;


/**
 * Dynamic object.
 *
 * <p>When you wish to create an object instance of an unknown type, {@link DynamicObject} (dynamo) provides an
 * alternative to java.lang.reflection and ordinary {@link java.util.HashMap}s. This may be necessary for the following
 * reasons:</p>
 *
 * <ul>
 *   <li>Reflection <b>cumbersome</b> , if you intend to push the reflected type through an API (generics)</li>
 *   <li>For reflection to work, you <b>still need to have the class definition</b> on the classpath</li>
 *   <li>HashMaps are very flexible, but about 2 times <b>slower</b>, and use about <b>2 times the memory</b> than
 *     instantiating a reflected type</li>
 *   <li>HashMaps are <b>too flexible</b>, in that they provide no way of ensuring that a valid field is set, or the
 *     field value is of the required type (so you may also feel an urge to wrap the map in another class just to make
 *     things worse)</li>
 * </ul>
 *
 * <p>A {@link DynamicObject} is:</p>
 *
 * <ul>
 *   <li>Merely an Object array, and a type reference (so it only has +1 reference compared to a static type)</li>
 *   <li>Faster than initializing an object with reflection</li>
 *   <li>Has a type definition (singleton) which has all the information about typesafe getting and setting of values
 *   </li>
 * </ul>
 *
 * @author  Gergely Kiss
 */
public class DynamicObject implements Serializable {
    private static final long serialVersionUID = 2135375290758037268L;

    private final DynamicType type;
    private final Object[] fieldValues;

    public DynamicObject(DynamicType type) {
        this.type = type;
        this.fieldValues = new Object[type.fieldCount()];
    }

    /**
     * Sets the <code>field</code> to <code>value</code> on this object.
     *
     * @param   field  The name of the field to set
     * @param   value  The value to set
     *
     * @throws  IllegalAccessException  if the field does not exist in this {@link DynamicType}
     */
    public void set(String field, Object value) throws IllegalAccessException {
        fieldValues[type.fieldIndex(field)] = value;
    }

    /**
     * Gets the value of the <code>field</code> from this object.
     *
     * @param   field  The name of the field to get
     *
     * @return  The value of the field, null if it was never set
     *
     * @throws  IllegalAccessException  if the field does not exist in this {@link DynamicType}
     */
    public Object get(String field) throws IllegalAccessException {
        return fieldValues[type.fieldIndex(field)];
    }

    /**
     * Sets the <code>field</code> to <code>value</code> on this object typesafely.
     *
     * @param   field  The name of the field to set
     * @param   value  The value to set. You may set null to fields of any type
     *
     * @throws  IllegalAccessException  if the field does not exist in this {@link DynamicType}
     * @throws  ClassCastException      if <code>value</code> is not of the field type
     */
    public <T> void safeSet(String field, T value) throws IllegalAccessException {
        int i = type.fieldIndex(field);

        if (value != null) {
            type.typeCheck(i, value.getClass());
        }

        fieldValues[i] = value;
    }

    /**
     * Gets the value of the <code>field</code> from this object typesafely.
     *
     * @param   field      The name of the field to get
     * @param   fieldType  The expected type of the field
     *
     * @return  The value of the field, null if it was never set
     *
     * @throws  IllegalAccessException  if the field does not exist in this {@link DynamicType}
     * @throws  ClassCastException      if the field value is not of <code>fieldType</code>
     */
    public <T> T safeGet(String field, Class<T> fieldType) throws IllegalAccessException {
        Object value = fieldValues[type.fieldIndex(field)];

        return fieldType.cast(value);
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + Arrays.hashCode(fieldValues);

        return result;
    }

    @Override public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        DynamicObject other = (DynamicObject) obj;

        if (!Arrays.equals(fieldValues, other.fieldValues)) {
            return false;
        }

        return true;
    }
}
