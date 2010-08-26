package org.ogreg.util;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A trie node storing ints.
 * 
 * @author Gergely Kiss
 * @see Trie
 */
final class IntTrieNode implements Serializable {
	private static final long serialVersionUID = 7217023895828169766L;

	final IntTrie parent;
	final int offset;
	int count;

	int value;
	IntTrieNode[] children = null;

	IntTrieNode(IntTrie parent, int offset, int count, int value) {
		this.parent = parent;
		this.offset = offset;
		this.count = count;
		this.value = value;
	}

	int get(byte[] word, int offset, int length) {
		IntTrieNode node = this;

		byte[] contents = parent.contents;
		int wordPos = 0;

		while (node != null) {
			int pos = node.getLongestMatch(contents, word, wordPos);
			int wps = pos + wordPos;

			if (wps == length) {
				return pos == node.count ? node.value : Integer.MIN_VALUE;
			}

			if (node.children == null) {
				return Integer.MIN_VALUE;
			}

			int charPos = word[offset + wps];

			node = node.children[charPos];
			wordPos = wps;
		}

		return Integer.MIN_VALUE;
	}

	void set(byte[] word, int value) {
		IntTrieNode node = this;

		byte[] contents = parent.contents;
		int wordPos = 0;
		int pos;
		int wps;

		while (true) {
			pos = node.getLongestMatch(contents, word, wordPos);
			wps = pos + wordPos;

			// No match - adding the new word
			if (pos == 0) {
				int charPos = word[wps];

				if (node.children == null) {
					node.children = new IntTrieNode[parent.maxChildren];
				}

				if (node.children[charPos] == null) {
					// No node at this child, creating new one
					node.children[charPos] = parent.create(word, wordPos, value);
				} else {
					// Continue adding to current child
					node = node.children[charPos];
					continue;
				}
			}
			// We have a match
			else {
				int wordLen = word.length - wordPos;
				int prefixLen = node.count;

				if (pos < prefixLen) {
					// ... and there are remaining chars - splitting the
					// node (creating: a non-word prefix with two children)
					if (pos < wordLen) {
						IntTrieNode child1 = parent.create(word, wps, value);
						IntTrieNode child2 = node.subtrie(pos, node.value);
						child2.children = node.children;

						node.count = pos; // Keeping the first half
						node.value = Integer.MIN_VALUE;
						node.children = new IntTrieNode[parent.maxChildren];
						node.children[child1.byteAt(0)] = child1;
						node.children[child2.byteAt(0)] = child2;
					}
					// ...and we have an exact match - splitting the node
					// (creating: a prefix which is the same as the new
					// word, with one child)
					else {
						IntTrieNode child2 = node.subtrie(pos, node.value);
						child2.children = node.children;

						node.count = pos; // Keeping the first half
						node.value = value;
						node.children = new IntTrieNode[parent.maxChildren];
						node.children[child2.byteAt(0)] = child2;
					}
				}
				// Match is as long as the prefix
				else {
					// ...and there are remaining chars - adding remainder
					// to children
					if (pos < wordLen) {
						int charPos = word[wps];

						if (node.children == null) {
							node.children = new IntTrieNode[parent.maxChildren];
						}

						if (node.children[charPos] == null) {
							node.children[charPos] = parent.create(word, wps, value);
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

	final byte byteAt(int i) {
		return parent.contents[offset + i];
	}

	private IntTrieNode subtrie(int beginIndex, int value) {
		return (beginIndex == 0) ? this : new IntTrieNode(parent, offset + beginIndex, count
				- beginIndex, value);
	}

	@Override
	public String toString() {
		return Arrays.toString(Arrays.copyOfRange(parent.contents, offset, offset + count));
	}

	/**
	 * Returns the longest matching position of the two strings.
	 * 
	 * @param word
	 * @return
	 */
	private final int getLongestMatch(byte[] contents, byte[] word, int wordOffset) {
		int len = Math.min(count, word.length - wordOffset);
		int po = this.offset;

		for (int i = 0; i < len; i++) {
			if (contents[po + i] != word[wordOffset + i]) {
				return i;
			}
		}
		return len;
	}
}