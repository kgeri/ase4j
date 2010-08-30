package org.ogreg.util.btree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.Map.Entry;

import org.ogreg.common.nio.NioCollectionSerializer;
import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.NioUtils;

/**
 * Serializer implementation for {@link BTree}s.
 * 
 * @author Gergely Kiss
 * @param <K>
 * @param <V>
 */
public class BTreeSerializer<K extends Comparable<K>, V> extends
		NioCollectionSerializer<Entry<K, V>> {
	private static final int MAX_ENTRY_SIZE = 4096;

	private final NioSerializer<K> keySerializer;
	private final NioSerializer<V> valueSerializer;

	public BTreeSerializer(NioSerializer<K> keySerializer, NioSerializer<V> valueSerializer) {
		this.keySerializer = keySerializer;
		this.valueSerializer = valueSerializer;
	}

	public synchronized void serialize(BTree<K, V> tree, FileChannel channel) throws IOException {
		NioUtils.writeInt(channel, tree.order);

		SerializationContext ctx = new BTreeSerializationContext(tree);
		ctx.setMaxChunkSize(MAX_ENTRY_SIZE);

		serialize(tree.iterator(), channel, ctx);
	}

	public synchronized BTree<K, V> deserialize(FileChannel channel,
			SerializerListener<Entry<K, V>> listener) throws IOException {
		int order = NioUtils.readInt(channel);

		BTree<K, V> tree = new BTree<K, V>(order);

		SerializationContext ctx = new BTreeSerializationContext(tree);
		ctx.setMaxChunkSize(MAX_ENTRY_SIZE);
		ctx.setListener(listener);

		deserialize(channel, ctx);

		return tree;
	}

	@Override
	protected void read(ByteBuffer buf, SerializationContext ctx) throws IOException {
		@SuppressWarnings("unchecked")
		BTreeSerializationContext bc = (BTreeSerializationContext) ctx;

		K key = keySerializer.deserialize(buf);
		V value = valueSerializer.deserialize(buf);

		// TODO This could be more effective
		bc.tree.set(key, value);
		bc.onEntryRead(new AbstractMap.SimpleEntry<K, V>(key, value));
	}

	@Override
	protected void write(Entry<K, V> elem, ByteBuffer buf, SerializationContext ctx)
			throws IOException {
		keySerializer.serialize(elem.getKey(), buf);
		valueSerializer.serialize(elem.getValue(), buf);
	}

	public void writeImmediately(K key, V value, FileChannel channel) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(keySerializer.sizeOf(key)
				+ valueSerializer.sizeOf(value));

		keySerializer.serialize(key, buf);
		valueSerializer.serialize(value, buf);

		buf.flip();
		channel.write(buf);
	}

	private class BTreeSerializationContext extends SerializationContext {
		private final BTree<K, V> tree;

		public BTreeSerializationContext(BTree<K, V> tree) {
			this.tree = tree;
		}
	}
}
