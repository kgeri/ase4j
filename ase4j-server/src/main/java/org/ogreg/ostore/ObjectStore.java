package org.ogreg.ostore;

import java.io.Serializable;
import java.util.Date;

import org.ogreg.common.nio.NioSerializer;

/**
 * Service interface for storing and retrieving Java objects.
 * <p>
 * Every instance is stored as a set of key-value pairs representing their
 * fields. Every instance is assigned a unique identifier of type
 * <code>long</code>. Objects may be retrieved by their identifier, or any of
 * their properties.
 * <p>
 * </p>
 * It is possible to retrieve ony one field of the object, also it is possible
 * to retrieve a partialy resolved object instance.</p>
 * <p>
 * Restrictions:
 * <ul>
 * <li>An object store is intended to store only one type of object
 * <li>The stored type <code>T</code> <b>must have a default constructor</b>
 * <li>The stored type <code>T</code> may need to mark <b>exactly one</b> of its
 * fields with {@link BusinessKey}. The type of this field may be {@link String}
 * only (currently)
 * <li>Only fields of the type wich have a {@link NioSerializer} will be
 * persisted
 * <li>The object identifiers are not generated, the caller must provide them
 * <li>Only some types may have an index (all primitive types, {@link String},
 * {@link Date})
 * <li><b>null values are implicit</b>, that is if an object has null values,
 * nothing will be stored for that field, and when queried, it will be set to
 * null
 * </ul>
 * Good to know:
 * <ul>
 * <li>If you do not wish to persist a field, mark it with
 * <code>transient</code>
 * <li>The type <code>T</code> <b>does not</b> have to conform to the JavaBeans
 * specification, since direct field access will be used
 * <li>The type <code>T</code> <b>does not</b> have to be {@link Serializable},
 * unless you access this service through RMI of course
 * </ul>
 * </p>
 * 
 * @param <T> The type of the stored object. Must have a default constructor.
 * @author Gergely Kiss
 */
public interface ObjectStore<T> {

	/**
	 * Stores the given instance in the object store, or returns the stored
	 * instance's key.
	 * <p>
	 * Does <b>not</b> update the stored entity if it already exists.
	 * </p>
	 * <p>
	 * Important: requires a {@link BusinessKey} on the stored type.
	 * </p>
	 * 
	 * @param entity The object instance to store
	 * @return The identifier of the new or updated entity
	 * @throws ObjectStoreException on storage error
	 * @throws IllegalStateException if the type does not have a business key
	 */
	long save(T entity) throws ObjectStoreException;

	/**
	 * Stores or updates the given instance in the object store, or returns the
	 * stored instance's key.
	 * <p>
	 * Updates the stored entity if it already exists.
	 * </p>
	 * <p>
	 * Important: requires a {@link BusinessKey} on the stored type.
	 * </p>
	 * 
	 * @param entity The object instance to store
	 * @return The identifier of the new or updated entity
	 * @throws ObjectStoreException on storage error
	 * @throws IllegalStateException if the type does not have a business key
	 */
	long saveOrUpdate(T entity) throws ObjectStoreException;

	/**
	 * Adds the given instance in the object store.
	 * 
	 * @param identifier The identifier to use
	 * @param entity The entity to add
	 * @throws ObjectStoreException on storage error
	 */
	void add(long identifier, T entity) throws ObjectStoreException;

	/**
	 * Retrieves the object instance by its identifier.
	 * 
	 * @param identifier The unique identifier of the object
	 * @return The initialized object instance, or null if no object is stored
	 *         with the given identifier
	 * @throws ObjectStoreException on storage error
	 */
	T get(long identifier) throws ObjectStoreException;

	/**
	 * Retrieves the field value of an instance by its identifier and field
	 * name.
	 * 
	 * @param identifier The unique identifier of the object
	 * @param fieldName The field name to get the value from
	 * @return The field value, or null if no field is stored with the given
	 *         identifier and field name
	 * @throws ObjectStoreException on storage error
	 */
	Object getField(long identifier, String fieldName) throws ObjectStoreException;

	/**
	 * Retrieves the object identifier by one of the object's fields.
	 * 
	 * @param fieldName The name of the field to use for the query
	 * @param value The field value to look for
	 * @return The identifier of the object instance, or null if there is no
	 *         such object
	 * @throws ObjectStoreException on storage error
	 */
	Long uniqueResult(String fieldName, Object value) throws ObjectStoreException;
}
