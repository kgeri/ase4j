package org.ogreg.common.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/**
 * Index entry for storing absolute file positions for any content.
 * <p>
 * Important:
 * <ul>
 * <li>The index entries should precede the stored content in the storage file
 * <li>The maximum number of indexed entries is 2GB / 8 bytes = 2^28 (an index
 * entry uses 8 bytes). This is a limitation of NIO mapped byte buffers.
 * <li>The key used in the index <b>is in fact its array index</b>. The</li>
 * </ul>
 * </p>
 * <p>
 * The index capacity specifies the maximum number of entries the index may
 * hold. After reaching the capacity, the index may be grown using
 * {@link #grow(int)}. On growing, the indexed contents will be shifted, and the
 * index entries are updated with the new file positions.
 * </p>
 * 
 * @author Gergely Kiss
 * @see BaseIndexedStore
 */
final class IndexEntries {

	// Header offset (the header contains the index capacity and maxKey)
	private static final int ENTRY_OFFSET = 4 + 4;

	// Base capacity of the newly created indices
	private static int baseCapacity = 1024;

	/** The current capacity of the index. */
	private int capacity = baseCapacity;

	/** The greatest identifier stored in the index. */
	private int maxKey;

	/** A NIO byte buffer for the index for fast memory-mapped access. */
	private MappedByteBuffer buffer;

	/**
	 * Sets the indexed value at the specified position.
	 * 
	 * @param key
	 * @param value
	 * @throws IOException
	 */
	public void set(int key, long value) throws IOException {

		if (key >= capacity) {
			throw new IndexOutOfBoundsException("Not enough capacity for key: " + key + " (>= "
					+ capacity + ")");
		}

		buffer.putLong(ENTRY_OFFSET + (key * 8), value);

		if (key > maxKey) {
			// TODO This may cause problems because it's not threadsafe...
			maxKey = key;
		}
	}

	/**
	 * Returns the indexed value at the specified position.
	 * 
	 * @param key
	 * @return The indexed file position for the key, or 0 if it does not exist.
	 */
	public long get(int key) {
		return (key >= capacity) ? 0 : buffer.getLong(ENTRY_OFFSET + (key * 8));
	}

	/**
	 * Maps the index on the specified file channel.
	 * <p>
	 * Creates the index with {@link #baseCapacity} if it didn't exist before.
	 * </p>
	 * 
	 * @param channel The file channel to map the index to
	 * @param offset The offset where the index should start
	 * @throws IOException if the channel is inaccessible
	 */
	public void map(FileChannel channel, long offset) throws IOException {
		map(channel, offset, baseCapacity);
	}

	/**
	 * Maps the index on the specified file channel.
	 * <p>
	 * Creates the index with <code>baseCapacity</code> if it didn't exist
	 * before.
	 * </p>
	 * 
	 * @param channel The file channel to map the index to
	 * @param offset The offset where the index should start
	 * @throws IOException if the channel is inaccessible
	 */
	public void map(FileChannel channel, long offset, int baseCapacity) throws IOException {
		flush();
		unmap();

		if (channel.size() >= (offset + ENTRY_OFFSET)) {
			ByteBuffer tmp = ByteBuffer.allocate(8);
			channel.read(tmp, offset);
			this.capacity = tmp.getInt(0);
			this.maxKey = tmp.getInt(4);
		} else {
			this.capacity = baseCapacity;
			this.maxKey = 0;
		}

		if (capacity <= 0) {
			throw new IllegalArgumentException("Bogus index capacity: " + capacity);
		}

		buffer = channel.map(MapMode.READ_WRITE, offset, ENTRY_OFFSET + (capacity * 8));
	}

	public void unmap() throws IOException {
		if (buffer != null) {
			NioUtils.unmap(buffer);
			buffer = null;
		}
	}

	public void flush() {

		if (buffer != null) {
			buffer.putInt(0, capacity);
			buffer.putInt(4, maxKey);
			buffer.force();
		}
	}

	public int getCapacity() {
		return capacity;
	}

	public int getMaxKey() {
		return maxKey;
	}
}
