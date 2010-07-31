package org.ogreg.common.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Common interface for NIO-based entity serializers.
 * 
 * @param <T> The serialized entity type
 * @author Gergely Kiss
 */
public interface NioSerializer<T> {

	/**
	 * Serializes the <code>value</code> to the given {@link ByteBuffer}.
	 * <p>
	 * Precondition: it is safe to assume that the buffer is initialized, sized
	 * and positioned, so that this serialization method only needs to layout
	 * the data structure sequentially. The size of the buffer is determined
	 * using the {@link #sizeOf(Object)} method of this serializer.
	 * </p>
	 * 
	 * @param value The entity to serialize
	 * @param dest The target byte buffer to serialize to
	 * @throws IOException on serialization failure
	 */
	void serialize(T value, ByteBuffer dest) throws IOException;

	/**
	 * Deserializes an entity from the given {@link ByteBuffer}.
	 * <p>
	 * Preconditions: it is safe to assume that the buffer is initialized, sized
	 * and positioned so that this deserialization method only needs to read the
	 * data structure sequentially. The size of the buffer is determined using
	 * the {@link #sizeOf(FileChannel, long)} method of this serializer.
	 * </p>
	 * 
	 * @param source The source buffer to serialize from
	 * @return The deserialized Java entity
	 * @throws IOException on deserialization failure
	 */
	T deserialize(ByteBuffer source) throws IOException;

	/**
	 * The implementation must calculate the serialized size of the specified
	 * entity in bytes.
	 * 
	 * @param value The entity to serialize
	 * @return The number of bytes this entity needs for storage
	 * @see #serialize(Object, ByteBuffer)
	 */
	int sizeOf(T value);

	/**
	 * The implementation must determine the serialized size of an entity on the
	 * specified position of a channel, in bytes.
	 * 
	 * @param channel The channel to read the entity size from
	 * @param pos The position to start reading from
	 * @return The size of the serialized entity in bytes
	 * @throws IOException if file access failed
	 * @see #deserialize(ByteBuffer)
	 */
	int sizeOf(FileChannel channel, long pos) throws IOException;
}