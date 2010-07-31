package org.ogreg.ostore;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * Common interface for {@link ObjectStore} field accessors.
 * <p>
 * Field accessors are used for maintaining the connection between the
 * persistent and in-memory state of an object field. The accessor can load and
 * store property values in an object storage, and also can get and set property
 * values on a Java object instance.
 * </p>
 * 
 * @author Gergely Kiss
 */
interface FieldAccessor extends Closeable, Flushable {

	/**
	 * Returns the field value for the object with the given identifier from the
	 * object store.
	 * 
	 * @param identifier The identifier of the object to access
	 * @param propertyPath The path of the property to access. If null, then
	 *            this field should be accessed, otherwise the call must be
	 *            delegated to the child accessors (if any)
	 * @return
	 * @throws IOException on storage error
	 */
	Object load(long identifier, String propertyPath) throws IOException;

	/**
	 * Updates the field value for the object with the given identifier in the
	 * object store.
	 * 
	 * @param identifier The identifier of the object to update
	 * @param propertyPath The path of the property to update. If null, then
	 *            this field should be updated, otherwise the call must be
	 *            delegated to the child accessors (if any)
	 * @param value The value to update to
	 * @throws IOException on storage error
	 */
	void store(long identifier, String propertyPath, Object value) throws IOException;

	/**
	 * Returns the accessed field's name.
	 * 
	 * @return
	 */
	String getFieldName();

	/**
	 * Returns the field value from the specified object.
	 * 
	 * @param source
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	Object getFrom(Object source) throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Sets the field value on the specified object.
	 * 
	 * @param source
	 * @param value
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	void setTo(Object source, Object value) throws IllegalArgumentException, IllegalAccessException;

}