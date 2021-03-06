package org.ogreg.util;

import java.io.Serializable;

/**
 * A fast and memory efficient Trie data structure.
 * <p>
 * It differs from the original implementation in that it stores String prefixes
 * instead of characters. Also, the prefixes are stored in a very efficient
 * encoded format: every character of a prefix String is mapped to its index and
 * stored in a byte array. The mapping is done exactly once, before an insertion
 * or a search.
 * </p>
 * <p>
 * The storage of the mapped byte arrays is similar to Java's String
 * implementation - whenever a byte array needs to be split, only its offset
 * and/or length field will be modified.
 * </p>
 * <p>
 * Thanks to these optimizations, the Trie is only 5 times slower than a HashMap
 * (both insertion and search), and allocates only about 1.2 times the memory
 * (based on 1M random string insertions of length 1-32, real life scenarios
 * such as URL sets may provide better results).
 * </p>
 * Please note that you must provide a {@link TrieDictionary} to use this data
 * structure, by default the {@link TrieDictionary#EN} is specified. </p>
 * <p>
 * Please note that this implementation currently does not support deletion.
 * </p>
 * <p>
 * Please note that despite this implementation is {@link Serializable}, it may
 * be ineffective to use Java Serialization for loading and saving {@link Trie}
 * s. For a fast, NIO-based solution please see {@link TrieSerializer}.
 * </p>
 * 
 * @author Gergely Kiss
 * @see http://en.wikipedia.org/wiki/Trie
 * @see TrieDictionary
 */
public class Trie<T> implements Serializable {
	private static final long serialVersionUID = 8556320988440764488L;

	final TrieDictionary dict;
	byte maxChildren;
	TrieNode<T> root;

	byte[] contents = new byte[1];
	private int size = 1;
	private int capacity = 1;

	/**
	 * Creates a Trie based on the English dictionary.
	 */
	public Trie() {
		this(TrieDictionary.EN);
	}

	/**
	 * Creates a Trie based on the specified dictionary.
	 * 
	 * @param dict
	 */
	public Trie(TrieDictionary dict) {
		this.dict = dict;
		this.maxChildren = dict.size();
		this.root = new TrieNode<T>(this, 1, 0, null);
	}

	TrieNode<T> create(byte[] word, int offset, T value) {
		int wordLen = word.length - offset;

		if (size + wordLen >= capacity) {
			grow(size + wordLen);
		}

		int noffset = size;
		System.arraycopy(word, offset, contents, noffset, wordLen);
		size += wordLen;

		return new TrieNode<T>(this, noffset, wordLen, value);
	}

	/**
	 * Sets the given value to the word in the trie.
	 * 
	 * @param word
	 */
	public void set(String word, T value) {

		if ((word == null) || (word.length() == 0)) {
			return;
		}

		root.set(dict.encode(word), value);
	}

	/**
	 * Sets the given value to the encoded word in the trie.
	 * 
	 * @param word
	 */
	public void set(byte[] word, T value) {

		if ((word == null) || (word.length == 0)) {
			return;
		}

		root.set(word, value);
	}

	/**
	 * Returns the value associated with the given word, or null of there is no
	 * such association.
	 * 
	 * @param word
	 * @return
	 */
	public T get(String word) {
		byte[] bytes = dict.encode(word);

		return root.get(bytes, 0, bytes.length);
	}

	/**
	 * Returns all the words in the trie.
	 * 
	 * @return
	 */
	public void getWords(Callback<String> processor) {
		getWords("", root, processor);
	}

	public TrieDictionary getDictionary() {
		return dict;
	}

	private void getWords(String prefix, TrieNode<T> node, Callback<String> processor) {
		String value = prefix + dict.decode(contents, node.offset, node.count);

		if (node.value != null) {
			processor.callback(value);
		}

		TrieNode<T>[] children = node.children;

		if (children == null) {
			return;
		}

		for (int i = 0; i < children.length; i++) {

			if (children[i] != null) {
				getWords(value, children[i], processor);
			}
		}
	}

	private void grow(int targetSize) {
		while (capacity < targetSize) {
			capacity <<= 1;
		}

		byte[] ncontents = new byte[capacity];

		System.arraycopy(contents, 0, ncontents, 0, size);
		contents = ncontents;
	}
}
