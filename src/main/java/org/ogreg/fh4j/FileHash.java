package org.ogreg.fh4j;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.ogreg.common.nio.MappedFileSupport;
import org.ogreg.common.nio.NioUtils;

/**
 * A hash map stored in a mapped file.
 * 
 * <p>
 * This implementation makes it possible to have very large hash maps (2Gb total
 * size at most) with O(1) put and get amortized times. The map is stored in a
 * memory mapped file, so it does not use heap memory.
 * </p>
 * 
 * <p>
 * The map maintains a hash table of size <code>capacity</code>, which is always
 * a power of 2. The table contains file positions of the first bucket for every
 * table index. At these positions are entries, which have the form of:<br>
 * <br>
 * <code>nextPosition (4 bytes) + serialized key (ks bytes) + serialized value (vs bytes)</code>
 * <br>
 * <br>
 * If nextPosition is not 0, then it means the next bucket's file position, so
 * this way the buckets form a linked list.
 * </p>
 * 
 * <p>
 * Whenever an entry gets removed, the map removes it from the bucket list (or
 * the table), and adds it to a deleted list. The list of deleted entries works
 * exactly the same as the bucket list, except it starts from a special position
 * in the file (just before the hash table). When a new entry is added to the
 * map, it first checks for available deleted entries, and reuses one of them.
 * </p>
 * 
 * <p>
 * There are also some drawbacks:
 * </p>
 * 
 * <ul>
 * <li>The total map size may not exceed 2GB. This is because the maximum number
 * of memory mapped bytes is {@link Integer#MAX_VALUE}.</li>
 * <li>The key and value types of the map must be serialized and deserialized at
 * every {@link #put(Object, Object)} and {@link #get(Object)} operation. The
 * map must use a special kind of serialization interfaced by {@link Serializer}
 * s, because the size of the map entries must be fixed.</li>
 * <li>The total map size can be calculated with this formula:
 * <code>24 (header) + capacity * 4 + (capacity *
 *     loadFactor * (4 + keySize + valueSize))</code></li>
 * </ul>
 * 
 * @param <K>
 *            The type of the keys in the map
 * @param <V>
 *            The type of the values in the map
 * 
 * @author Gergely Kiss
 */
public class FileHash<K, V> extends MappedFileSupport {
	private static final byte[] MAGIC = new byte[] { 'F', 'H', '4', 'J' };

    // Header: MAGIC + table size + bucket size + key size + value size +
	// deleted entry chain start
	private static final int HEADER_LEN = MAGIC.length + 4 + 4 + 4 + 4 + 4;

    // Default load factor
	private static final float LOAD_FACTOR = 0.8F;

    static int baseCapacity = 1024;

    private final Iterable<Entry<K, V>> FileHashIterable = new Iterable<Entry<K, V>>() {
		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new FileHashEntryIterator();
		}
	};

    /** The size of the hash table. */
	private int capacity;

    /**
	 * The number of buckets used in the file (bs), not including the deleted
	 * entries.
	 */
	private int size = 0;

    /** The fixed serialized size of the keys. */
	private int keySize;

    /** The fixed serialized size of the values. */
	private int valSize;

    /** The file position of the first deleted entry of the chain. */
	private int firstDelPos = 0;

    /** The maximum size the map will tolerate without a resize. */
	private int maxSize;

    private final Serializer<K> keySerializer;
	private final Serializer<V> valSerializer;

    public FileHash(Serializer<K> keySerializer, Serializer<V> valSerializer) {
		this(keySerializer, valSerializer, baseCapacity);
	}

    public FileHash(Serializer<K> keySerializer, Serializer<V> valSerializer, int capacity) {
		this.keySerializer = keySerializer;
		this.valSerializer = valSerializer;
		this.capacity = capacity;
		this.keySize = keySerializer.getSize();
		this.valSize = valSerializer.getSize();
		this.maxSize = (int) (capacity * LOAD_FACTOR);
	}

    /**
	 * Gets the value of the specified key from this table.
	 * 
	 * @param key
	 * 
	 * @return The value associated with <code>key</code>, or null if none
	 *         exists
	 */
	public V get(K key) {
		byte[] keyBytes = new byte[keySize];
		keySerializer.write(key, keyBytes);

        int i = indexFor(key.hashCode(), capacity);

        int firstPos = getFirstBucket(i);
		int pos = getEntryPosition(keyBytes, firstPos);

        if (pos == 0) {

            // Key was not found
			return null;
		} else {

            // Value found for key
			return getValue(pos);
		}
	}

    /**
	 * Puts the value on the specified key in this table.
	 * 
	 * @param key
	 * @param value
	 * 
	 * @throws IOException
	 */
	public void put(K key, V value) throws IOException {
		byte[] keyBytes = new byte[keySize];
		byte[] valueBytes = new byte[valSize];

        keySerializer.write(key, keyBytes);
		valSerializer.write(value, valueBytes);

        ensureSize(size + 1);

        int i = indexFor(key.hashCode(), capacity);

        int firstPos = getFirstBucket(i);
		int pos = getEntryPosition(keyBytes, firstPos);

        if (pos == 0) {

            // Not found in buckets, adding a new entry as the first bucket
			int next = addEntry(keyBytes, valueBytes);
			setFirstBucket(i, next);
			setNextBucket(next, firstPos);
		} else {

            // Sets the value at the given entry position
			buffer.position(pos + 4 + keySize);
			buffer.put(valueBytes, 0, valSize);
		}
	}

    /**
	 * Removes the entry specified by <code>key</code> from the map.
	 * 
	 * @param key
	 * 
	 * @return The old value associated with <code>key</code>, or null if there
	 *         was none
	 */
	public V remove(K key) {
		byte[] keyBytes = new byte[keySize];
		byte[] valueBytes = new byte[valSize];
		keySerializer.write(key, keyBytes);

        int i = indexFor(key.hashCode(), capacity);

        int firstPos = getFirstBucket(i);
		int pos = firstPos;
		int prev = 0;

        byte[] currentKey = new byte[keySize];

        // Finding bucket for hash index
		for (; pos != 0; pos = getNextBucket(pos)) {

            // Loads the key at the given entry position
			buffer.position(pos + 4);
			buffer.get(currentKey, 0, keySize);

            if (equals(currentKey, keyBytes)) {

                // Found the key, saving the value
				buffer.position(pos + 4 + keySize);
				buffer.get(valueBytes, 0, valSize);

                break;
			}

            prev = pos;
		}

        if (pos == 0) {

            // Key not found, do nothing
			return null;
		} else {
			int next = getNextBucket(pos);

            if (prev != 0) {

                // Removing from the middle of the chain
				setNextBucket(prev, next);
			} else {

                // Removing from the start of the chain
				setFirstBucket(i, next);
			}

            // Adding to start of deleted chain
			if (firstDelPos != 0) {
				setNextBucket(pos, firstDelPos);
			}

            firstDelPos = pos;
			size--;

            return valSerializer.read(valueBytes);
		}
	}

    /**
	 * Returns the current size of the map.
	 * 
	 * @return
	 */
	public int size() {
		return size;
	}

    /**
	 * Traverses the buckets starting from <code>firstPos</code>, and returns
	 * the position of the entry by the given <code>key</code>.
	 * 
	 * @param keyBytes
	 * @param firstPos
	 * 
	 * @return
	 */
	private int getEntryPosition(byte[] keyBytes, int firstPos) {
		byte[] currentKey = new byte[keySize];

        // Finding bucket for hash index
		for (int pos = firstPos; pos != 0; pos = getNextBucket(pos)) {

            // Loads the key at the given entry position
			buffer.position(pos + 4);
			buffer.get(currentKey, 0, keySize);

            if (equals(currentKey, keyBytes)) {

                // Found the key, returning the entry position
				return pos;
			}
		}

        return 0;
	}

    /**
	 * Puts an entry at the end of the entries, or reuses a deleted entry.
	 * 
	 * @param keyBytes
	 * @param valueBytes
	 * 
	 * @return The file position of the inserted entry
	 * 
	 * @throws IOException
	 */
	private int addEntry(byte[] keyBytes, byte[] valueBytes) throws IOException {

        // Empty deleted chain, create a new entry
		if (firstDelPos == 0) {

            // Last entry will be after the header + table + all entries
			int lastEntryPos = HEADER_LEN + (capacity * 4) + (size * (4 + keySize + valSize));

            setEntry(lastEntryPos, keyBytes, valueBytes);
			size++;

            return lastEntryPos;
		}
		// Reusing deleted entry and removing it from the deleted chain
		else {
			int pos = firstDelPos;
			firstDelPos = getNextBucket(firstDelPos);

            setEntry(pos, keyBytes, valueBytes);
			setNextBucket(pos, 0);

            size++;

            return pos;
		}
	}

    private final K getKey(int pos) {
		byte[] currentKey = new byte[keySize];
		buffer.position(pos + 4);
		buffer.get(currentKey, 0, keySize);

        return keySerializer.read(currentKey);
	}

    private final V getValue(int pos) {
		byte[] currentVal = new byte[valSize];
		buffer.position(pos + 4 + keySize);
		buffer.get(currentVal, 0, valSize);

        return valSerializer.read(currentVal);
	}

    /**
	 * Ensures that the map has the capacity to efficiently contain at least
	 * <code>size</code> elements.
	 * 
	 * @param size
	 * 
	 * @throws IOException
	 */
	private synchronized void ensureSize(int size) throws IOException {

        if (size <= maxSize) {
			return;
		}

        File cloneFile = null;
        FileHash<K, V> clone = null;

		int newCapacity = capacity;

        while ((newCapacity * LOAD_FACTOR) < size) {
			newCapacity <<= 1;
		}

        try {

            // Cloning the map to a bigger one
			cloneFile = File.createTempFile("fhcopy", ".dat");
			clone = new FileHash<K, V>(keySerializer, valSerializer, newCapacity);
			clone.open(cloneFile);

            for (Entry<K, V> e : entries()) {
				clone.put(e.getKey(), e.getValue());
			}

            // Overwriting current contents
            NioUtils.unmap(buffer);
			NioUtils.unmap(clone.buffer);

            clone.channel.position(0);
			channel.transferFrom(clone.channel, 0, clone.channel.size());
			buffer = map(channel);
			this.size = clone.size;
		} finally {

            // Cleaning up
            if (clone != null) {
				clone.close();
			}

            if (cloneFile != null) {
				cloneFile.delete();
			}
        }
	}

    public Iterable<Entry<K, V>> entries() {
		return FileHashIterable;
	}

    /**
	 * Overwrites the entry at the specified position.
	 * 
	 * @param pos
	 * @param keyBytes
	 * @param valueBytes
	 */
	private final void setEntry(int pos, byte[] keyBytes, byte[] valueBytes) {
		buffer.position(pos);
		buffer.putInt(0);
		buffer.put(keyBytes);
		buffer.put(valueBytes);
	}

    /**
	 * Returns the first bucket's position from the table index, or 0 if there
	 * are no buckets for this index yet.
	 * 
	 * @param index
	 * 
	 * @return
	 */
	private final int getFirstBucket(int index) {
		return buffer.getInt(HEADER_LEN + (index * 4));
	}

    /**
	 * Sets the first bucket's position at the table index.
	 * 
	 * @param index
	 * @param pos
	 * 
	 * @return
	 */
	private final void setFirstBucket(int index, int pos) {
		buffer.putInt(HEADER_LEN + (index * 4), pos);
	}

    /**
	 * Returns the following bucket's file position.
	 * 
	 * @param pos
	 * 
	 * @return
	 */
	private final int getNextBucket(int pos) {
		return buffer.getInt(pos);
	}

    /**
	 * Sets the following bucket's file position.
	 * 
	 * @param pos
	 * @param next
	 */
	private final void setNextBucket(int pos, int next) {
		buffer.putInt(pos, next);
	}

    static final int indexFor(int h, int length) {
		h ^= (h >>> 20) ^ (h >>> 12);
		h ^= (h >>> 7) ^ (h >>> 4);

        return h & (length - 1);
	}

    /**
	 * Returns true if the two byte arrays are equal.
	 * 
	 * <p>
	 * Does not perform a length check.
	 * </p>
	 * 
	 * @param source
	 * @param target
	 * 
	 * @return
	 */
	private static final boolean equals(byte[] source, byte[] target) {

        for (int i = 0; i < source.length; i++) {

            if (source[i] != target[i]) {
				return false;
			}
		}

        return true;
	}

    @Override
	public synchronized void flush() throws IOException {
		buffer.putInt(4, capacity);
		buffer.putInt(8, size);
		buffer.putInt(12, keySize);
		buffer.putInt(16, valSize);
		buffer.putInt(20, firstDelPos);
		super.flush();
	}

    @Override
	protected MappedByteBuffer map(FileChannel channel) throws IOException {
		MappedByteBuffer map;

        // Initializing new hash
		if (channel.size() <= HEADER_LEN) {
			map = channel.map(MapMode.READ_WRITE, 0, HEADER_LEN + (capacity * 4)
					+ (maxSize * (4 + keySize + valSize)));

            map.position(0);
			map.put(MAGIC);
			map.putInt(capacity);
			map.putInt(size);
			map.putInt(keySize);
			map.putInt(valSize);
			map.putInt(0);
		} else {
			map = channel.map(MapMode.READ_WRITE, 0, channel.size());

            map.position(4);
			this.capacity = map.getInt();
			this.size = map.getInt();
			this.keySize = map.getInt();
			this.valSize = map.getInt();
			this.maxSize = (int) (capacity * LOAD_FACTOR);
			this.firstDelPos = map.getInt();
		}

        return map;
	}

    // An iterator for file hash entries
    final class FileHashEntryIterator implements Iterator<Entry<K, V>> {
		private int idx = 0;
		private int pos = getFirstBucket(0);

        @Override
		public boolean hasNext() {

            while (pos == 0) {
				idx++;

                if (idx >= capacity) {
					return false;
				}

                pos = getFirstBucket(idx);
			}

            return true;
		}

        @Override
		public Entry<K, V> next() {

            if (!hasNext()) {
				throw new NoSuchElementException();
			}

            EntryImpl e = new EntryImpl();
			e.key = getKey(pos);
			e.value = getValue(pos);

            pos = getNextBucket(pos);

            return e;
		}

        @Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

    final class EntryImpl implements Entry<K, V> {
		private K key;
		private V value;

        @Override
		public K getKey() {
			return key;
		}

        @Override
		public V getValue() {
			return value;
		}

        @Override
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}
	}
}
