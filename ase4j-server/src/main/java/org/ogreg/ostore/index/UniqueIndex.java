package org.ogreg.ostore.index;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.ogreg.ostore.ObjectStoreException;

/**
 * Common interface for unique indices.
 * <p>
 * Unique indices must have a default constructor, and they should implement
 * some sort of serialization.
 * </p>
 * 
 * @author Gergely Kiss
 */
public interface UniqueIndex {

	/**
	 * The index must initialize itself with this method from the given file and
	 * index parameters.
	 * 
	 * @param indexFile
	 * @param params
	 * @throws ObjectStoreException if the initialization has failed
	 */
	void loadFrom(File indexFile, Map<String, String> params) throws IOException;

	/**
	 * The index must persist or update itself with this method at the given
	 * file.
	 * 
	 * @param indexFile
	 * @throws ObjectStoreException if persistence has failed
	 */
	void saveTo(File indexFile) throws IOException;

	/**
	 * Returns the key of the indexed field value, or null, if there is no such
	 * field.
	 * 
	 * @param value The value to search for, never null
	 * @return The identifier of the entity, or null if it was not found
	 */
	Long getKey(Object value);

	/**
	 * Sets the given identifier to the indexed field value.
	 * 
	 * @param value The value to index, never null
	 * @param identifier The identifier to index
	 */
	void setKey(Object value, long identifier);
}
