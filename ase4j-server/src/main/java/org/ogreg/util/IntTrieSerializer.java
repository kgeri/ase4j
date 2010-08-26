package org.ogreg.util;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.NioUtils;
import org.ogreg.common.nio.serializer.SerializerManager;

/**
 * A fast NIO-based {@link IntTrie} serializer.
 * <p>
 * Since Java Serialization uses a HashMap when determining whether an instance
 * has been serialized or not, serializing a {@link IntTrie} which has many
 * {@link IntTrieNode}s may be very ineffective.
 * </p>
 * <p>
 * This implementation on the other hand iterates over all the
 * {@link IntTrieNode}s and determines and stores all the strings (actually byte
 * arrays) this trie represents. Similarly, deserialization means reading these
 * strings and rebuilding the whole {@link IntTrie} in memory. While it is much
 * more redundant, this approach has the following advantages:
 * <ul>
 * <li>It's a lot <b>faster</b>
 * <li>Uses <b>constant memory</b> while serializing and deserializing
 * <li>Later implementation may be able to <b>monitor progress</b>
 * <li>Storage representation is more natural (list of strings), which makes
 * <b>partial loading</b> and <b>error correction</b> possible
 * </ul>
 * </p>
 * <p>
 * Please note that for this to work, the type of the {@link IntTrie} must have
 * a {@link NioSerializer}.
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
public class IntTrieSerializer {
	// The maximum size a trie node may occupy
	private static final int MAX_TRIENODE_SIZE = 4096;

	// Allocate 512k buffer
	private final ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);

	/**
	 * Stores the given <code>trie</code> to the <code>channel</code>.
	 * 
	 * @param <T>
	 * @param trie The trie to serialize
	 * @param channel The target channel
	 * @throws IOException on write error
	 */
	public synchronized void serialize(IntTrie trie, FileChannel channel) throws IOException {
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
	public synchronized IntTrie deserialize(FileChannel channel, IntTrieSerializerListener listener)
			throws IOException {
		TrieDictionary dict = NioUtils.deserializeFrom(channel, TrieDictionary.class);
		IntTrie trie = new IntTrie(dict);

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
	private void serialize(TrieNodes prefix, IntTrieNode node, FileChannel dest) throws IOException {

		if (node.value != Integer.MIN_VALUE) {
			write(prefix, node, dest);
		}

		IntTrieNode[] children = node.children;

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
	private void deserialize(IntTrie trie, FileChannel src, IntTrieSerializerListener listener)
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
	 * Appends the current key-value pair on the given channel.
	 * 
	 * @param key
	 * @param value
	 * @param dest
	 * @throws IOException
	 */
	public synchronized void write(byte[] key, int value, FileChannel dest) throws IOException {
		buf.clear();

		// Writing key length
		buf.putInt(key.length);

		// Writing key
		buf.put(key);

		// Writing value
		buf.putInt(value);

		buf.flip();
		dest.write(buf);
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
	private void write(TrieNodes prefix, IntTrieNode node, FileChannel dest) throws IOException {

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
			IntTrieNode pn = prefix.nodes[i];
			buf.put(pn.parent.contents, pn.offset, pn.count);
		}
		buf.put(node.parent.contents, node.offset, node.count);

		// Writing value
		buf.putInt(node.value);
	}

	/**
	 * Reads a node key and value in the format specified in
	 * {@link #write(TrieNodes, IntTrieNode, FileChannel)}, then creates and
	 * adds a new IntTrie node.
	 * 
	 * @param trie
	 * @param listener
	 * @throws IOException on channel read error
	 */
	private void read(IntTrie trie, IntTrieSerializerListener listener) throws IOException {
		// Reading key length
		int n = buf.getInt();

		// Reading key
		byte[] key = new byte[n];
		buf.get(key);

		// Reading value
		int value = buf.getInt();

		// Adding trie entry
		trie.set(key, value);
		listener.onEntryRead(key, value);
	}

	// Fast IntTrieNode queue
	private final class TrieNodes {
		IntTrieNode[] nodes = new IntTrieNode[16];
		int size = 0;

		public void push(IntTrieNode node) {
			if (size >= nodes.length) {
				IntTrieNode[] copy = new IntTrieNode[nodes.length * 2];
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
	// access to the IntTrieNode, but the StringStore also has to know when a
	// new key-value pair has been red (to rebuild its index)
	public interface IntTrieSerializerListener {
		void onEntryRead(byte[] key, int value);
	}
}
