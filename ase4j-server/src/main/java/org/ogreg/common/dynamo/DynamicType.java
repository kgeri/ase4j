package org.ogreg.common.dynamo;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link DynamicObject} type.
 * <p>
 * The purpose of this class is to store type and field name information about a
 * dynamic type. Like with {@link java.lang.Class}es, the {@link DynamicType}
 * should also be a singleton (though it is not necessary).
 * </p>
 * 
 * @author Gergely Kiss
 */
public final class DynamicType {
	/** The types of the fields. */
	private final Class<?>[] fieldTypes;

	/** The field indexes keyed by the field names. */
	private final Map<String, Integer> fields;

	/**
	 * Creates a new dynamic object type.
	 * 
	 * @param fieldNames The names of the fields of the object
	 * @param fieldTypes The types of the fields of the object
	 */
	public DynamicType(String[] fieldNames, Class<?>[] fieldTypes) {
		this.fields = new HashMap<String, Integer>(fieldNames.length);
		this.fieldTypes = new Class<?>[fieldNames.length];

		for (int i = 0; i < fieldNames.length; i++) {
			this.fields.put(fieldNames[i], i);
			this.fieldTypes[i] = fieldTypes[i];
		}
	}

	/**
	 * Returns the name of this dynamic type. The name of a dynamic type is
	 * composed of its field names.
	 * <p>
	 * Ex.: [url, date, size]
	 * </p>
	 * 
	 * @return
	 */
	public String getName() {
		return fields.toString();
	}

	/**
	 * Returns the index of the given <code>field</code>.
	 * 
	 * @param field The name of the field
	 * @return The index where field values may be found in the
	 *         {@link DynamicObject}'s values array
	 * @throws IllegalAccessException if the field does not exist in this type
	 */
	final int fieldIndex(String field) throws IllegalAccessException {
		Integer f = fields.get(field);
		if (f == null) {
			throw new IllegalAccessException("Field '" + field
					+ "' was not found on dynamic type: " + toString());
		}
		return f;
	}

	/**
	 * Performs a type check on the specified <code>fieldIndex</code> with the
	 * specified <code>type</code>.
	 * 
	 * @param fieldIndex
	 * @param type
	 * @throws ClassCastException if <code>fieldType</code> is not assignable
	 *             from <code>type</code>
	 */
	final void typeCheck(int fieldIndex, Class<?> type) {
		Class<?> fieldType = fieldTypes[fieldIndex];
		if (!fieldType.isAssignableFrom(type)) {
			throw new ClassCastException("Cannot cast " + type.getName() + " to "
					+ fieldType.getName());
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

	@Override
	public String toString() {
		return new StringBuilder("dynamo ").append(fields).toString();
	}
}
