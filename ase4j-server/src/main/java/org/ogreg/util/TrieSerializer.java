package org.ogreg.util;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.NioUtils;
import org.ogreg.common.nio.serializer.SerializerManager;

/**
 * A fast NIO-based {@link Trie} serializer.
 * <p>
 * Since Java Serialization uses a HashMap when determining whether an instance
 * has been serialized or not, serializing a {@link Trie} which has many
 * {@link TrieNode}s may be very ineffective.
 * </p>
 * <p>
 * This implementation on the other hand iterates over all the {@link TrieNode}s
 * and determines and stores all the strings (actually byte arrays) this trie
 * represents. Similarly, deserialization means reading these strings and
 * rebuilding the whole {@link Trie} in memory. While it is much more redundant,
 * this approach has the following advantages:
 * <ul>
 * <li>It's a lot <b>faster</b>
 * <li>Uses <b>constant memory</b> while serializing and deserializing
 * <li>Later implementation may be able to <b>monitor progress</b>
 * <li>Storage representation is more natural (list of strings), which makes
 * <b>partial loading</b> and <b>error correction</b> possible
 * </ul>
 * </p>
 * <p>
 * Please note that for this to work, the type of the {@link Trie} must have a
 * {@link NioSerializer}.
 * </p>
 * <p>
 * Please note that the buffer limit is currently {@link #MAX_TRIENODE_SIZE}
 * bytes, so trying to serialize keys and values larger than that will throw a
 * {@link BufferOverflowException}. This is by design. Tries are not meant to
 * store such huge strings or values, please use an
 * {@link org.ogreg.ostore.ObjectStore} for that.
 * </p>
 * 
 * @author Gergely Kiss
 * @see SerializerManager
 */
public class TrieSerializer<T> {
	// The maximum size a trie node may occupy
	private static final int MAX_TRIENODE_SIZE = 4096;

	// Allocate 512k buffer
	private final ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);

	/** The serializer for the trie's values. */
	private final NioSerializer<T> valueSerializer;

	public TrieSerializer(Class<T> valueType) {
		this.valueSerializer = SerializerManager.findSerializerFor(valueType);
	}

	/**
	 * Stores the given <code>trie</code> to the <code>channel</code>.
	 * 
	 * @param <T>
	 * @param trie The trie to serialize
	 * @param channel The target channel
	 * @throws IOException on write error
	 */
	public synchronized void serialize(Trie<T> trie, FileChannel channel) throws IOException {
		NioUtils.serializeTo(channel, trie.dict);

		buf.clear();
		serialize(new TrieNodes(), trie.root, channel);

		buf.flip();
		channel.write(buf);
	}

	/**
	 * Loads a trie from the given <code>channel</code>.
	 * 
	 * @param channel
	 * @return
	 * @throws IOException on read error
	 */
	public synchronized Trie<T> deserialize(FileChannel channel, TrieSerializerListener<T> listener)
			throws IOException {
		TrieDictionary dict = NioUtils.deserializeFrom(channel, TrieDictionary.class);
		Trie<T> trie = new Trie<T>(dict);

		buf.clear();
		deserialize(trie, channel, listener);
		return trie;
	}

	/**
	 * Recursively serializes the given trie prefix and actual trie node.
	 * 
	 * @param prefix
	 * @param node
	 * @param dest
	 * @throws IOException
	 */
	private void serialize(TrieNodes prefix, TrieNode<T> node, FileChannel dest) throws IOException {

		if (node.value != null) {
			write(prefix, node, dest);
		}

		TrieNode<T>[] children = node.children;

		if (children == null) {
			return;
		}

		prefix.push(node);
		for (int i = 0; i < children.length; i++) {

			if (children[i] != null) {
				serialize(prefix, children[i], dest);
			}
		}
		prefix.pop();
	}

	/**
	 * Deserializes trie contents from <code>src</code> to the given
	 * <code>trie</code>.
	 * 
	 * @param trie
	 * @param src
	 * @throws IOException on read error
	 */
	private void deserialize(Trie<T> trie, FileChannel src, TrieSerializerListener<T> listener)
			throws IOException {

		while (src.read(buf) != -1) {
			buf.flip();

			while (buf.remaining() > MAX_TRIENODE_SIZE) {
				read(trie, listener);
			}

			buf.compact();
		}

		buf.flip();

		while (buf.hasRemaining()) {
			read(trie, listener);
		}
	}

	/**
	 * Writes the current node key and value in the following format:
	 * <ul>
	 * <li>int[1] node key length (n)
	 * <li>byte[n] node key
	 * <li>T node value
	 * </ul>
	 * 
	 * @param prefix
	 * @param node
	 * @param dest
	 * @throws IOException on channel write error
	 * @throws BufferOverflowException if the key or value length is over 4096
	 *             bytes
	 */
	private synchronized void write(TrieNodes prefix, TrieNode<T> node, FileChannel dest)
			throws IOException {

		if (buf.remaining() < MAX_TRIENODE_SIZE) {
			buf.flip();
			dest.write(buf);
			buf.clear();
		}

		// Writing key length
		int n = 0;
		for (int i = 0; i < prefix.size; i++) {
			n += prefix.nodes[i].count;
		}
		n += node.count;
		buf.putInt(n);

		// Writing key
		for (int i = 0; i < prefix.size; i++) {
			TrieNode<?> pn = prefix.nodes[i];
			buf.put(pn.prefix, pn.offset, pn.count);
		}
		buf.put(node.prefix, node.offset, node.count);

		// Writing value
		valueSerializer.serialize(node.value, buf);
	}

	/**
	 * Reads a node key and value in the format specified in
	 * {@link #write(TrieNodes, TrieNode, FileChannel)}, then creates and adds a
	 * new Trie node.
	 * 
	 * @param trie
	 * @param listener
	 * @throws IOException on channel read error
	 */
	private void read(Trie<T> trie, TrieSerializerListener<T> listener) throws IOException {
		// Reading key length
		int n = buf.getInt();

		// Reading key
		byte[] key = new byte[n];
		buf.get(key);

		// Reading value
		T value = valueSerializer.deserialize(buf);

		// Adding trie entry
		trie.set(key, value);
		listener.onEntryRead(key, value);
	}

	// Fast TrieNode queue
	private final class TrieNodes {
		TrieNode<?>[] nodes = new TrieNode<?>[16];
		int size = 0;

		public void push(TrieNode<?> node) {
			if (size >= nodes.length) {
				TrieNode<?>[] copy = new TrieNode<?>[nodes.length * 2];
				System.arraycopy(nodes, 0, copy, 0, nodes.length);
				nodes = copy;
			}
			nodes[size++] = node;
		}

		public void pop() {
			nodes[--size] = null;
		}
	}

	// TODO Reconsider this... problem is that only the TrieSerializer has
	// access to the TrieNode, but the StringStore also has to know when a new
	// key-value pair has been red (to rebuild its index)
	public interface TrieSerializerListener<T> {
		void onEntryRead(byte[] key, T value);
	}
}
