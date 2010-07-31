package org.ogreg.fh4j;

/**
 * A common interface for object which can serialize a given type to and from
 * byte arrays.
 * 
 * @author Gergely Kiss
 * 
 */
public interface Serializer<T> {

	/**
	 * Deserializes the value from the <code>source</code> array.
	 * 
	 * @param source
	 * 
	 * @return
	 * 
	 * @throws IllegalArgumentException
	 *             if there is a size mismatch between <code>source</code> and
	 *             the serialized image size of the type T
	 */
	T read(byte[] source) throws IllegalArgumentException;

	/**
	 * Serializes the given <code>value</code> to a byte array.
	 * 
	 * @param value
	 * @param dest
	 * 
	 * @throws IllegalArgumentException
	 *             if there is a size mismatch between <code>dest</code> and the
	 *             serialized image size of the type T
	 */
	void write(T value, byte[] dest) throws IllegalArgumentException;

	/**
	 * Returns the number of bytes needed to serialize a type T object.
	 * 
	 * @return
	 */
	int getSize();
}
