package org.ogreg.common.nio;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.serializer.SerializerManager;

/**
 * NIO support utilities.
 * 
 * @author Gergely Kiss
 */
public abstract class NioUtils {

	// The size of the memory block used to transfer bytes
	static int BLOCK_SIZE = 1024 * 1024;

	private static Class<?> DirectByteBufferClass;
	private static Method MappedByteBufferCleaner;

	static {

		try {
			DirectByteBufferClass = Class.forName("java.nio.DirectByteBuffer");

			Method m = DirectByteBufferClass.getMethod("cleaner");
			m.setAccessible(true);
			MappedByteBufferCleaner = m;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Unmaps the specified byte buffer.
	 * <p>
	 * {@link MappedByteBuffer}s are by design not unmappable for security
	 * reasons. This, however makes it impossible to delete the mapped file
	 * until the GC releases the mapping in some filesystems.
	 * </p>
	 * <p>
	 * You must understand the security implications before using this method.
	 * Please see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038 for
	 * more information.
	 * </p>
	 * 
	 * @param buffer
	 * @throws IOException
	 */
	public static synchronized void unmap(MappedByteBuffer buffer) throws IOException {

		if (buffer == null) {
			return;
		}

		try {
			Object cleaner = MappedByteBufferCleaner.invoke(buffer);

			if (cleaner == null) {

				// Already closed
				return;
			}

			cleaner.getClass().getMethod("clean").invoke(cleaner);
		} catch (Exception e) {
			throw new IOException("Failed to unmap buffer", e);
		}
	}

	/**
	 * Moves the bytes in a {@link FileChannel} from <code>from</code> to
	 * <code>to</code>.
	 * <p>
	 * This method may be used either to shrink or to grow the file.
	 * </p>
	 * <p>
	 * Please be advised that when growing the file, this method <b>does not</b>
	 * reset the newly created bytes to zero (ie. the previous content will
	 * remain there). If you must clear the contents of that area, please use
	 * {@link #fill(FileChannel, long, long, byte)}.
	 * </p>
	 * 
	 * @param channel
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	public static synchronized void move(FileChannel channel, long from, long to)
			throws IOException {

		if (from == to) {
			return;
		}

		ByteBuffer buf = ByteBuffer.allocateDirect(BLOCK_SIZE);

		long len = channel.size();
		long diff = to - from;
		long pos;

		// Growing the file
		if (diff > 0) {

			// Moving the big blocks
			for (pos = len - BLOCK_SIZE; pos >= from; pos -= BLOCK_SIZE) {
				buf.clear();
				channel.read(buf, pos);

				buf.clear();
				channel.write(buf, pos + diff);
			}

			// Moving the last block
			// Since MOVE_BLOCK_SIZE is an int, and we decreased pos with that -
			// this cast will not overflow
			int remaining = (int) (pos + BLOCK_SIZE - from);

			if (remaining > 0) {
				buf.clear().limit(remaining);
				channel.read(buf, from);

				buf.clear().limit(remaining);
				channel.write(buf, from + diff);
			}
		}
		// Shrinking the file
		else {

			// Moving the big blocks
			for (pos = from; pos < (len - BLOCK_SIZE); pos += BLOCK_SIZE) {
				buf.clear();
				channel.read(buf, pos);

				buf.clear();
				channel.write(buf, pos + diff);
			}

			// Moving the last block
			// Since MOVE_BLOCK_SIZE is an int, and we increased pos with that -
			// this cast will not overflow
			int remaining = (int) (len - pos);

			if (remaining > 0) {
				buf.clear().limit(remaining);
				channel.read(buf, pos);

				buf.clear().limit(remaining);
				channel.write(buf, pos + diff);
			}

			channel.truncate(len + diff);
		}
	}

	/**
	 * Fills the bytes in a {@link FileChannel} from <code>from</code>
	 * (inclusive) to <code>to</code> (exclusive) with the specified pattern.
	 * 
	 * @param channel
	 * @param from
	 * @param to
	 * @param pattern
	 * @throws IOException
	 */
	public static synchronized void fill(FileChannel channel, long from, long to, byte pattern)
			throws IOException {

		if (from > to) {
			return;
		}

		ByteBuffer buf = ByteBuffer.allocateDirect(BLOCK_SIZE);

		long len = channel.size();
		long pos;

		from = Math.max(from, 0);
		to = Math.min(to, len);

		// Initializing pattern
		for (int i = 0; i < buf.capacity(); i++) {
			buf.put(pattern);
		}

		// Filling the big blocks
		for (pos = from; pos < (to - BLOCK_SIZE); pos += BLOCK_SIZE) {
			buf.clear();
			channel.write(buf, pos);
		}

		// Filling the remaining bytes
		int remaining = (int) (to - pos);
		buf.clear().limit(remaining);
		channel.write(buf, pos);
	}

	/**
	 * Closes the {@link Closeable} quietly.
	 * 
	 * @param closeable
	 */
	public static final void closeQuietly(Closeable closeable) {

		if (closeable != null) {

			try {
				closeable.close();
			} catch (IOException e) {
				// Swallowing errors
			}
		}
	}

	/**
	 * Reads an integer from the channel at the current position.
	 * 
	 * @param channel
	 * @param position
	 * @return
	 * @throws IOException
	 */
	public static final int readInt(FileChannel channel) throws IOException {
		ByteBuffer singleBuf = ByteBuffer.allocate(4);

		channel.read(singleBuf);
		singleBuf.flip();

		return singleBuf.getInt();
	}

	/**
	 * Reads an integer from the channel at the specified position.
	 * 
	 * @param channel
	 * @param position
	 * @return
	 * @throws IOException
	 */
	public static final int readInt(FileChannel channel, long position) throws IOException {
		ByteBuffer singleBuf = ByteBuffer.allocate(4);

		channel.read(singleBuf, position);
		singleBuf.flip();

		return singleBuf.getInt();
	}

	/**
	 * Writes an integer to the channel at the current position.
	 * 
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static final void writeInt(FileChannel channel, int value) throws IOException {
		ByteBuffer singleBuf = ByteBuffer.allocate(4);

		singleBuf.putInt(value);
		singleBuf.flip();

		channel.write(singleBuf);
	}

	/**
	 * Reads a long from the channel at the current position.
	 * 
	 * @param channel
	 * @return
	 * @throws IOException
	 */
	public static final long readLong(FileChannel channel) throws IOException {
		ByteBuffer singleBuf = ByteBuffer.allocate(8);

		channel.read(singleBuf);
		singleBuf.flip();

		return singleBuf.getLong();
	}

	/**
	 * Writes a long to the channel at the current position.
	 * 
	 * @param channel
	 * @param value
	 * @throws IOException
	 */
	public static final void writeLong(FileChannel channel, long value) throws IOException {
		ByteBuffer singleBuf = ByteBuffer.allocate(8);

		singleBuf.putLong(value);
		singleBuf.flip();

		channel.write(singleBuf);
	}

	/**
	 * Serializes the <code>object</code> to the given <code>channel</code>
	 * using {@link NioSerializer}s.
	 * <p>
	 * Note: The channel must be positioned first.
	 * </p>
	 * 
	 * @param channel
	 * @param object
	 * @throws IOException on io failure
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final void serializeTo(FileChannel channel, Object object) throws IOException {
		NioSerializer s = SerializerManager.findSerializerFor(object.getClass());

		int size = s.sizeOf(object);

		ByteBuffer buf = ByteBuffer.allocate(size);

		s.serialize(object, buf);
		buf.flip().limit(size);

		channel.write(buf);
	}

	/**
	 * Deserializes an object of <code>type</code> from the given
	 * <code>channel</code> using {@link NioSerializer}.
	 * <p>
	 * Note: The channel must be positioned first. Only byte arrays serialized
	 * with {@link #serializeTo(FileChannel, Object)} can be deserialized. The
	 * channel position will be incremented.
	 * </p>
	 * 
	 * @param channel
	 * @param type The type of the object
	 * @return The deserialized object, never null
	 * @throws IOException on io or deserialization failure
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static final <T> T deserializeFrom(FileChannel channel, Class<T> type)
			throws IOException {
		NioSerializer s = SerializerManager.findSerializerFor(type);

		int size = s.sizeOf(channel, channel.position());
		ByteBuffer buf = ByteBuffer.allocate(size);

		channel.read(buf);
		buf.flip().limit(size);

		return (T) s.deserialize(buf);
	}
}
