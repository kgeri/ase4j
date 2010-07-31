package org.ogreg.util;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A trie node.
 * 
 * @author Gergely Kiss
 * @see Trie
 */
final class TrieNode<T> implements Serializable {
	private static final long serialVersionUID = 7217023895828169766L;

	private final byte maxChildren;
	final byte[] prefix;
	final int offset;
	int count;

	T value;
	TrieNode<T>[] children = null;

	TrieNode(byte[] prefix, byte maxChildren, T value) {
		this.prefix = prefix;
		this.offset = 0;
		this.count = prefix.length;
		this.maxChildren = maxChildren;
		this.value = value;
	}

	private TrieNode(byte[] prefix, int offset, int count, byte maxChildren, T value) {
		this.prefix = prefix;
		this.offset = offset;
		this.count = count;
		this.maxChildren = maxChildren;
		this.value = value;
	}

	T get(byte[] word, int offset, int length) {
		TrieNode<T> node = this;
		int wordPos = 0;

		while (node != null) {
			int pos = node.getLongestMatch(word, offset, length, wordPos);
			int wps = pos + wordPos;

			if (wps == length) {
				return pos == node.count ? node.value : null;
			}

			if (node.children == null) {
				return null;
			}

			int charPos = word[offset + wps];

			node = node.children[charPos];
			wordPos = wps;
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	void set(TrieNode<T> word, int wordPos, T value) {
		TrieNode<T> node = this;

		byte[] wordPrefix = word.prefix;
		int wordOffset = word.offset;
		int wordCount = word.count;

		int pos;
		int wps;

		while (true) {
			pos = node.getLongestMatch(wordPrefix, wordOffset, wordCount, wordPos);
			wps = pos + wordPos;

			// No match - adding the new word
			if (pos == 0) {
				int charPos = word.byteAt(wps);

				if (node.children == null) {
					node.children = new TrieNode[maxChildren];
				}

				if (node.children[charPos] == null) {
					node.children[charPos] = word.subtrie(wordPos, value);
				} else {
					node = node.children[charPos];
					continue;
				}
			} else {
				int wordLen = wordCount - wordPos;
				int prefixLen = node.count;

				if (pos < prefixLen) {
					// ... and there are remaining chars - splitting the
					// node (creating: a non-word prefix with two children)
					if (pos < wordLen) {
						TrieNode<T> child1 = word.subtrie(wps, value);
						TrieNode<T> child2 = node.subtrie(pos, node.value);
						child2.children = node.children;

						node.count = pos; // Keeping the first half
						node.value = null;
						node.children = new TrieNode[maxChildren];
						node.children[child1.byteAt(0)] = child1;
						node.children[child2.byteAt(0)] = child2;
					}
					// ...and we have an exact match - splitting the node
					// (creating: a prefix which is the same as the new
					// word, with one child)
					else {
						TrieNode<T> child2 = node.subtrie(pos, node.value);
						child2.children = node.children;

						node.count = pos; // Keeping the first half
						node.value = value;
						node.children = new TrieNode[maxChildren];
						node.children[child2.byteAt(0)] = child2;
					}
				}
				// Match is as long as the prefix
				else {
					// ...and there are remaining chars - adding remainder
					// to children
					if (pos < wordLen) {
						int charPos = word.byteAt(wps);

						if (node.children == null) {
							node.children = new TrieNode[maxChildren];
						}

						if (node.children[charPos] == null) {
							node.children[charPos] = word.subtrie(wps, value);
						} else {
							node = node.children[charPos];
							wordPos = wps;
							continue;
						}
					}
					// ...and we have an exact match - updating value
					else {
						node.value = value;
					}
				}
			}

			break;
		}
	}

	byte byteAt(int i) {
		return prefix[offset + i];
	}

	private TrieNode<T> subtrie(int beginIndex, T value) {
		return (beginIndex == 0) ? this : new TrieNode<T>(prefix, offset + beginIndex, count
				- beginIndex, maxChildren, value);
	}

	@Override
	public String toString() {
		return Arrays.toString(Arrays.copyOfRange(prefix, offset, offset + count));
	}

	/**
	 * Returns the longest matching position of the two strings.
	 * 
	 * @param word
	 * @param offset
	 * @param length
	 * @param wordPos
	 * @return
	 */
	private int getLongestMatch(byte[] word, int offset, int length, int wordPos) {
		int len = Math.min(count, length - wordPos);
		int po = this.offset;
		byte[] p = prefix;

		for (int i = 0; i < len; i++) {
			if (p[po + i] != word[offset + i + wordPos]) {
				return i;
			}
		}
		return len;
	}
}