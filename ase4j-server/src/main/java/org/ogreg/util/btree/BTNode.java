package org.ogreg.util.btree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link BTree} node. May be initialized either as a leaf, or as an internal
 * node.
 * 
 * @param<K> The type of the keys
 * @param<V> The type of the values
 * @author Gergely Kiss
 */
final class BTNode<K extends Comparable<K>, V> {
	/**
	 * The keys of the node (with size of n if it's a leaf node, and n-1 if it's
	 * an internal node).
	 */
	private List<K> keys = new ArrayList<K>();

	/**
	 * The values of the leaf node (size of n), or null if it's an internal
	 * node.
	 */
	private List<V> values = null;

	/**
	 * The children of the internal node (size of n), or null if it's a leaf
	 * node.
	 */
	private List<BTNode<K, V>> children = null;

	/**
	 * Creates a {@link BTree} node.
	 * 
	 * @param leaf If true, the node will be initialized as a leaf node
	 */
	public BTNode(boolean leaf) {
		if (leaf) {
			values = new ArrayList<V>();
		} else {
			children = new ArrayList<BTNode<K, V>>();
		}
	}

	/**
	 * Splits the leaf or internal node in half.
	 * <p>
	 * Moves the left half of the node's keys and values or children in a new
	 * node, and returns the new node. Handles the internal node's key list
	 * specially.
	 * </p>
	 * 
	 * @return
	 */
	public BTNode<K, V> splitInHalf() {
		if (isLeaf()) {
			BTNode<K, V> m = new BTNode<K, V>(true);

			int ksize = keys.size();
			int median = ksize / 2;

			m.keys.addAll(keys.subList(0, median));
			m.values.addAll(values.subList(0, median));
			keys = keys.subList(median, ksize);
			values = values.subList(median, ksize);

			return m;
		} else {
			BTNode<K, V> m = new BTNode<K, V>(false);

			int ksize = keys.size();
			int kmedian = ksize / 2;

			m.keys.addAll(keys.subList(0, kmedian));
			keys = keys.subList(kmedian + 1, ksize);

			int size = children.size();
			int median = size / 2;
			m.children.addAll(children.subList(0, median));
			children = children.subList(median, size);

			return m;
		}
	}

	/**
	 * Returns this internal node's child at <code>idx</code>.
	 * 
	 * @param idx
	 * @return
	 * @throws NullPointerException if the node is a leaf node
	 */
	public BTNode<K, V> getChild(int idx) {
		return children.get(idx);
	}

	/**
	 * Adds the child <code>node</code> to this internal at <code>idx</code> .
	 * 
	 * @param idx
	 * @param node
	 * @throws NullPointerException if the node is a leaf node
	 */
	public void addChild(int idx, BTNode<K, V> node) {
		children.add(idx, node);
	}

	/**
	 * Returns the index of the specified key, or
	 * <code>(-(insertion point) - 1)</code> if it was not found.
	 * 
	 * @param key
	 * @return
	 */
	public int indexOf(K key) {
		return Collections.binarySearch(keys, key);
	}

	/**
	 * Returns the key at <code>idx</code>.
	 * 
	 * @param idx
	 * @return
	 */
	public K getKey(int idx) {
		return keys.get(idx);
	}

	/**
	 * Returns the last key of this node.
	 * 
	 * @return
	 */
	public K getLastKey() {
		return keys.get(keys.size() - 1);
	}

	/**
	 * Adds a new key at <code>idx</code>.
	 * 
	 * @param idx
	 * @param key
	 */
	public void addKey(int idx, K key) {
		keys.add(idx, key);
	}

	/**
	 * Returns this leaf's value at <code>idx</code>.
	 * 
	 * @param idx
	 * @return
	 * @throws NullPointerException if this is not a leaf node
	 */
	public V getValue(int idx) {
		return values.get(idx);
	}

	/**
	 * Sets this leaf's value at <code>idx</code>.
	 * 
	 * @param idx
	 * @param value
	 * @throws NullPointerException if this is not a leaf node
	 */
	public void setValue(int idx, V value) {
		values.set(idx, value);
	}

	/**
	 * Adds a new <code>value</code> on <code>key</code> to this leaf at
	 * <code>idx</code>.
	 * 
	 * @param idx
	 * @param key
	 * @param value
	 * @throws NullPointerException if this is not a leaf node
	 */
	public void addValue(int idx, K key, V value) {
		keys.add(idx, key);
		values.add(idx, value);
	}

	/**
	 * Returns the size of this node.
	 * 
	 * @return The number of keys for leaves or the number of children for
	 *         internal nodes
	 */
	public int size() {
		return isLeaf() ? keys.size() : children.size();
	}

	protected String dump() {
		return toString();
	}

	@Override
	public String toString() {
		if (isLeaf()) {
			StringBuilder buf = new StringBuilder();
			buf.append("[");

			for (int i = 0; i < keys.size(); i++) {

				if (i > 0) {
					buf.append(", ");
				}

				buf.append(keys.get(i)).append("=").append(values.get(i));
			}

			buf.append("]");

			return buf.toString();
		} else {
			return children.toString();
		}
	}

	public boolean isLeaf() {
		return children == null;
	}
}