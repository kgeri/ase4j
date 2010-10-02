package org.ogreg.ase4j.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ogreg.ase4j.AssociationStore.Operation;
import org.ogreg.common.nio.BaseIndexedStore;
import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.NioUtils;

/**
 * File based, cached association block storage.
 * <p>
 * Because the layout the {@link AssociationBlock} uses, it is not efficient to
 * store association one-by-one. Associations merged in this store are cached
 * until they reach a critical amount ({@link #maxCached}), after which they are
 * {@link #flush()}-ed to disk.
 * </p>
 * 
 * @author Gergely Kiss
 */
class CachedBlockStore extends BaseIndexedStore<AssociationBlock> {
	private static final byte[] MAGIC = new byte[] { 'A', 'S', '4', 'J' };

	static final NioSerializer<AssociationBlock> Serializer = new AssociationBlockSerializer();

	// The base capacity of a newly created association store
	static int baseCapacity = 1024;

	/** The in-memory working set of the association blocks. */
	private WorkingSet workingSet = new WorkingSet();

	/**
	 * The maximum number of cached associations.
	 * <p>
	 * Default: 1 million
	 * </p>
	 */
	private int maxCached = 1000000;

	/** The number of currently stored associations. */
	private long associationCount = 0;

	public CachedBlockStore() {
		setSerializer(Serializer);
	}

	@Override
	protected void writeHeader(FileChannel channel) throws IOException {
		super.writeHeader(channel);

		// Writing magic bytes
		channel.write(ByteBuffer.wrap(MAGIC));

		// Writing association count
		NioUtils.writeLong(channel, associationCount);
	}

	@Override
	protected void readHeader(FileChannel channel) throws IOException {
		super.readHeader(channel);

		// Reading magic bytes
		channel.read(ByteBuffer.allocate(4));

		// Reading association count
		associationCount = NioUtils.readLong(channel);
	}

	/**
	 * Merges all the associations to the store.
	 * <p>
	 * Grows and/or updates the current associations in the store.
	 * </p>
	 * 
	 * @param assocs
	 * @param op The operation to use for adding associations
	 * @throws IOException in case of a storage failure
	 */
	public void merge(AssociationBlock assocs, Operation op) throws IOException {
		workingSet.merge(assocs, op);
	}

	/**
	 * Returns the associations starting from <code>from</code>.
	 * 
	 * @param from
	 * @return The asspciations row, or null if it was not found for the given
	 *         <code>from</code>
	 * @throws IOException in case of a storage failure
	 */
	@Override
	public AssociationBlock get(int from) throws IOException {
		AssociationBlock assocs = super.get(from);

		// TODO Caching?

		return assocs;
	}

	/**
	 * Returns the association strength between <code>from</code> and
	 * <code>to</code>.
	 * 
	 * @param from
	 * @param to
	 * @return
	 * @throws IOException in case of a storage failure
	 */
	public float get(int from, int to) throws IOException {
		AssociationBlock assocs = get(from);

		return (assocs == null) ? 0 : assocs.get(to);
	}

	/**
	 * Sets the association cache size.
	 * <p>
	 * A total of <code>maxCached</code> associations will be stored in memory
	 * before the cache buffer is flushed to disk. Since an association uses
	 * only about 8 bytes (plus block overhead), this can probably be set to
	 * >1M.
	 * </p>
	 * 
	 * @param maxCached
	 */
	public void setMaxCached(int maxCached) {
		this.maxCached = maxCached;
	}

	@Override
	protected void onBeforeFlush() throws IOException {
		flushWorkingSet();
	}

	synchronized void flushWorkingSet() throws IOException {

		// Flushing the cache
		for (AssociationBlock assocs : workingSet.blocks.values()) {
			AssociationBlock stored = super.get(assocs.from);

			if (stored == null) {
				update(assocs.from, assocs);
			} else {
				// TODO: faster writeback, avoid to read the whole block in
				// memory again
				stored.merge(assocs, Operation.OVERWRITE);
				update(stored.from, stored);
			}
		}

		workingSet.clear();
	}

	@Override
	protected int getBaseCapacity() {
		return baseCapacity;
	}

	long getAssociationCount() {
		return associationCount;
	}

	// NIO Serializer for association blocks
	private static class AssociationBlockSerializer implements NioSerializer<AssociationBlock> {

		@Override
		public void serialize(AssociationBlock value, ByteBuffer dest) {
			IntBuffer iview = dest.asIntBuffer();
			FloatBuffer fview = dest.asFloatBuffer();

			iview.put(value.capacity);
			iview.put(value.size);
			iview.put(value.from);
			iview.put(value.tos);

			fview.position(iview.position());
			fview.put(value.values);

			// Resetting changedness
			value.changed = false;
			value.originalCapacity = value.capacity;
		}

		@Override
		public AssociationBlock deserialize(ByteBuffer source) {
			IntBuffer iview = source.asIntBuffer();
			FloatBuffer fview = source.asFloatBuffer();

			int capacity = iview.get();
			int size = iview.get();
			int from = iview.get();

			AssociationBlock assocs = new AssociationBlock(capacity, size, from);

			iview.get(assocs.tos);

			fview.position(iview.position());
			fview.get(assocs.values);

			assocs.changed = false;

			return assocs;
		}

		@Override
		public int sizeOf(AssociationBlock value) {
			return sizeOf(value.capacity);
		}

		@Override
		public int sizeOf(FileChannel channel, long pos) throws IOException {
			return sizeOf(NioUtils.readInt(channel, pos));
		}

		private final int sizeOf(int capacity) {
			// Size of an association:
			// 4 + 4 + 4 + capacity * 4 + capacity * 4
			// capacity + size + from + tos + values
			return 12 + capacity * 8;
		}
	}

	// In-memory working set of AssociationBlocks
	private class WorkingSet {
		private long associationCount;
		private int blockCount;
		private final Map<Integer, AssociationBlock> blocks = new ConcurrentHashMap<Integer, AssociationBlock>();

		synchronized void merge(AssociationBlock assocs, Operation op) throws IOException {
			if (associationCount + assocs.size > maxCached) {
				flushWorkingSet();
			}

			AssociationBlock ws = blocks.get(assocs.from);

			if (ws == null) {
				// Reading the stored association ensures the operation order
				AssociationBlock stored = CachedBlockStore.super.get(assocs.from);
				AssociationBlock merged = stored == null ? assocs : assocs.interMerge(stored, op);

				blocks.put(assocs.from, merged);
				blockCount++;
				associationCount += assocs.size;
			} else {
				synchronized (ws) {
					associationCount += ws.merge(assocs, op);
				}
			}
		}

		public void clear() {
			blocks.clear();
			this.blockCount = 0;
			this.associationCount = 0;
		}
	}
}
