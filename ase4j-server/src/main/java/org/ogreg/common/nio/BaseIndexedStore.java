package org.ogreg.common.nio;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.utils.FileUtils;

/**
 * Base class for file based storage of entities of type <code>T</code>, using
 * {@link IndexEntries}.
 * <p>
 * The store uses a single file to store entities of type <code>T</code>. It may
 * be <code>any</code> Java type, but a {@link NioSerializer} implementation
 * must be provided.
 * </p>
 * <p>
 * Layout: a header (h bytes, see {@link #getMagicBytes()}), an
 * {@link IndexEntries} block ( 8 + n * 8 ) bytes, and finally the serialized
 * entities (m bytes)
 * </p>
 * <p>
 * For performance reasons, the store is <b>append-only</b>.<br>
 * This means that when {@link #add(int, Object)}-ing an entity to a
 * pre-existent identifier, the entity <i>will be appended to the end of the
 * file<i>, regardless of whether or not it would fit into its previous place.<br>
 * </p>
 * 
 * @param <T> The type of the stored entities
 * @author Gergely Kiss
 * @see IndexEntries
 */
public abstract class BaseIndexedStore<T> implements Closeable, Flushable {
	private File storageFile;
	private FileChannel storageChannel;

	// Temporary, preinitialized buffer for reading and writing (1M)
	private ByteBuffer buffer = ByteBuffer.allocateDirect(1 * 1024 * 1024);

	private IndexEntries index = new IndexEntries();

	/** The serializer implementation used to serialize the indexed entities. */
	protected NioSerializer<T> serializer;

	/**
	 * The implementation may provider its custom header deserialization code
	 * here.
	 * <p>
	 * This method is when reopening an already created storage.
	 * </p>
	 * 
	 * @param channel The channel to deserialize the header from
	 * @throws IOException on storage failure
	 */
	protected void readHeader(FileChannel channel) throws IOException {
	}

	/**
	 * The implementation may provider its custom header serialization code
	 * here.
	 * <p>
	 * This method is invoked when creating a new storage (including reindexing
	 * a storage) and also when flushing the storage.
	 * </p>
	 * 
	 * @param channel The channel to serialize the header to
	 * @throws IOException on storage failure
	 */
	protected void writeHeader(FileChannel channel) throws IOException {
	}

	/**
	 * The implementation may provide its index capacity for newly created
	 * stores here.
	 * 
	 * @return
	 */
	protected int getBaseCapacity() {
		return 4;
	}

	/**
	 * The implementation may override this function to perform operations
	 * before flushing.
	 * 
	 * @throws IOException
	 */
	protected void onBeforeFlush() throws IOException {
	}

	/**
	 * The implementation may override this function to perform operations
	 * before closing.
	 */
	protected void onBeforeClose() {
	}

	public void setSerializer(NioSerializer<T> serializer) {
		this.serializer = serializer;
	}

	/**
	 * Opens the store in the specified file. Creates the store if the file was
	 * empty.
	 * 
	 * @param file The store file to open or create
	 * @throws IOException if file access failed
	 */
	public synchronized void open(File file) throws IOException {

		if (storageChannel != null) {
			close();
		}

		boolean existed = file.exists();

		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		storageChannel = raf.getChannel();
		storageFile = file;

		if (!existed) {
			writeHeader(storageChannel);
		} else {
			readHeader(storageChannel);
		}

		index.map(storageChannel, storageChannel.position(), getBaseCapacity());

		if (!existed) {
			flush();
		}
	}

	/**
	 * Gets the entity specified by its key.
	 * 
	 * @param key The identifier of the entity
	 * @return The loaded entity, or null if the store does not contain an
	 *         entity with the specified id
	 * @throws IOException if file access failed
	 */
	public T get(int key) throws IOException {
		long pos = index.get(key);

		if (pos == 0) {
			// Not found in index
			return null;
		} else {
			// Found in index, loading
			return load(pos);
		}
	}

	/**
	 * Adds the entity specified by its identifier.
	 * <p>
	 * Important: does <b>not</b> check if the entity has already been added. If
	 * it already exists, a new entity will be added and the previous one is
	 * marked for deletion.
	 * </p>
	 * 
	 * @param key The identifier of the entity
	 * @param entity The entity to add
	 * @throws IOException if file access failed
	 */
	public void add(int key, T entity) throws IOException {

		// Checking index size and growing if necessary
		if (key >= index.getCapacity()) {
			reindex(key + 1);
		}

		long pos = append(entity);
		index.set(key, pos);
	}

	/**
	 * Updates the entity specified by its identifier.
	 * <p>
	 * Overwrites the entity if its serialized size is not greater than the old
	 * entity's size. Appends the entity at the end of the file if the previous
	 * condition fails, or if no entity has been set for the given key.
	 * </p>
	 * 
	 * @param key The identifier of the entity
	 * @param entity The entity to update
	 * @throws IOException if file access failed
	 */
	public void update(int key, T entity) throws IOException {
		long pos = index.get(key);
		int newSize = serializer.sizeOf(entity);
		int oldSize = serializer.sizeOf(storageChannel, pos);

		// Entity grown or never existed -> append
		if (pos == 0 || newSize > oldSize) {
			add(key, entity);
		}
		// Entity kept size (or even shrunk) -> overwrite
		else {
			update(entity, newSize, pos);
		}
	}

	/**
	 * Appends the entity at the end of the store.
	 * 
	 * @param entity The entity to append
	 * @return The appended entity's file position
	 * @throws IOException if file access failed
	 */
	private synchronized long append(T entity) throws IOException {
		long lastPos = storageChannel.size();

		int size = serializer.sizeOf(entity);
		ByteBuffer buf = getBufferOf(size);

		buf.clear();
		serializer.serialize(entity, buf);
		buf.flip().limit(size);

		storageChannel.write(buf, lastPos);

		return lastPos;
	}

	/**
	 * Overwrites the entity at a given place in the store.
	 * 
	 * @param entity The entity to update
	 * @param size The entity's size
	 * @param position The position to overwrite
	 * @throws IOException if file access failed
	 */
	private synchronized void update(T entity, int size, long position) throws IOException {
		ByteBuffer buf = getBufferOf(size);

		buf.clear();
		serializer.serialize(entity, buf);
		buf.flip().limit(size);

		storageChannel.write(buf, position);
	}

	/**
	 * The implementation may use this method to load entities from the
	 * specified file position.
	 * <p>
	 * File positions can be obtained from the {@link #index}.
	 * </p>
	 * 
	 * @param pos The file position to load the entity from
	 * @return The loaded entity
	 * @throws IOException
	 */
	private synchronized T load(long pos) throws IOException {
		int size = serializer.sizeOf(storageChannel, pos);
		ByteBuffer buf = getBufferOf(size);

		buf.clear().limit(size);
		storageChannel.read(buf, pos);
		buf.flip();

		return serializer.deserialize(buf);
	}

	/**
	 * Grows the store to the specified size and/or compresseses the data in the
	 * store.
	 * <p>
	 * This method alleviates two problems:
	 * <ol>
	 * <li>As the store is append only, whenever an indexed entry is updated it
	 * means a new entry will be appended to the end of the storage file, and so
	 * garbage is left on the previous position.</li>
	 * <li>Because the index table is at the beginning of the storage file,
	 * whenever the store runs out of free index entries, the index table must
	 * be grown, the indexed entities must be moved in the file - and reindexed
	 * so their new positions are in the index table</li>
	 * </ol>
	 * </p>
	 * <p>
	 * The method does the following:
	 * <ul>
	 * <li>Opens a new storage file</li>
	 * <li>Creates an index table with 2^n capacity, so that it's at least
	 * <code>targetSize</code></li>
	 * <li>Copies every entry from the current store, and saves their positions
	 * in the new index table</li>
	 * <li>Switches the store to use the current store file</li>
	 * </ul>
	 * </p>
	 * 
	 * @param targetSize
	 * @throws IOException
	 */
	public synchronized void reindex(int targetSize) throws IOException {
		int capacity = index.getCapacity();
		int maxKey = index.getMaxKey();

		while (capacity < targetSize) {
			capacity <<= 1;
		}

		flush();

		// Opening a new store at a temp file
		File dir = storageFile.getParentFile();
		File tmpFile = File.createTempFile(storageFile.getName() + "_grow", "", dir);

		RandomAccessFile raf = new RandomAccessFile(tmpFile, "rw");
		FileChannel targetChannel = raf.getChannel();
		IndexEntries targetIndex = new IndexEntries();

		writeHeader(targetChannel);
		targetIndex.map(targetChannel, targetChannel.position(), capacity);

		// Appending data to the target channel
		targetChannel.position(targetChannel.size());
		for (int i = 0; i <= maxKey; i++) {
			long storagePos = index.get(i);

			if (storagePos == 0) {
				continue;
			}

			int size = serializer.sizeOf(storageChannel, storagePos);

			targetIndex.set(i, targetChannel.position());
			storageChannel.transferTo(storagePos, size, targetChannel);
		}

		targetIndex.flush();
		targetIndex.unmap();
		targetChannel.close();
		raf.close();

		// Finally: switching
		close();
		FileUtils.renameTo(tmpFile, storageFile);
		open(storageFile);
	}

	@Override
	public synchronized void flush() throws IOException {
		onBeforeFlush();

		// Flushing header
		storageChannel.position(0);
		writeHeader(storageChannel);

		index.flush();
	}

	@Override
	public synchronized void close() throws IOException {
		onBeforeClose();

		if (index != null) {
			index.unmap();
		}

		if (storageChannel != null) {
			storageChannel.close();
			storageChannel = null;
		}
	}

	/**
	 * Returns the number of stored entities in this store.
	 * <p>
	 * Please note that this is an <b>estimate</b>, which uses the
	 * {@link IndexEntries#maxKey} property. It is not guaranteed that every
	 * index key is used, although it is most likely.
	 * </p>
	 * 
	 * @return
	 */
	public int getSize() {
		return index.getMaxKey();
	}

	/**
	 * Returns the capacity of the index (the maximum number of stored elements
	 * without a reindex).
	 * 
	 * @return
	 */
	public int getCapacity() {
		return index.getCapacity();
	}

	private ByteBuffer getBufferOf(int size) {

		if (size > buffer.capacity()) {
			return ByteBuffer.allocate(size);
		} else {
			return buffer;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}
}
