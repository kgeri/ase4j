package org.ogreg.util.btree;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * {@link BTree} node. May be initialized either as a leaf, or as an internal
 * node.
 * 
 * @param<K> The type of the keys
 * @param<V> The type of the values
 * @author Gergely Kiss
 */
class BTNode<K extends Comparable<K>, V> implements Serializable {
	private static final long serialVersionUID = 7765823881226901621L;

	/**
	 * The keys of the node (with size of n if it's a leaf node, and n-1 if it's
	 * an internal node).
	 */
	K[] keys;

	/**
	 * The values of the leaf node (size of n), or null if it's an internal
	 * node.
	 */
	V[] values = null;

	/**
	 * The children of the internal node (size of n), or null if it's a leaf
	 * node.
	 */
	BTNode<K, V>[] children = null;

	/**
	 * The right sibling of this leaf, or null if this node is an internal node.
	 */
	private BTNode<K, V> rightSibling = null;

	/**
	 * The left sibling of this leaf, or null if this node is an internal node.
	 */
	private BTNode<K, V> leftSibling = null;

	/** The currently used node values or children. */
	private int size = 0;

	/**
	 * Creates a {@link BTree} node.
	 * 
	 * @param leaf If true, the node will be initialized as a leaf node
	 */
	@SuppressWarnings("unchecked")
	public BTNode(boolean leaf) {
		this.size = 0;

		if (leaf) {
			keys = (K[]) new Comparable[4];
			values = (V[]) new Object[4];
		} else {
			keys = (K[]) new Comparable[3];
			children = (BTNode<K, V>[]) new BTNode[4];
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

			int median = size / 2;

			m.keys = Arrays.copyOfRange(keys, 0, median);
			m.values = Arrays.copyOfRange(values, 0, median);
			m.size = median;

			System.arraycopy(keys, median, keys, 0, size - median);
			System.arraycopy(values, median, values, 0, size - median);
			size = size - median;

			// Adjusting leaf sibling pointers after split
			m.rightSibling = this;
			if (leftSibling != null) {
				m.leftSibling = leftSibling;
				leftSibling.rightSibling = m;
			}
			leftSibling = m;

			return m;
		} else {
			BTNode<K, V> m = new BTNode<K, V>(false);

			int ksize = size - 1;
			int kmedian = ksize / 2;
			int median = size / 2;

			m.keys = Arrays.copyOfRange(keys, 0, kmedian);
			m.children = Arrays.copyOfRange(children, 0, median);
			m.size = median;

			System.arraycopy(keys, kmedian + 1, keys, 0, ksize - kmedian - 1);
			System.arraycopy(children, median, children, 0, size - median);
			size = size - median;

			return m;
		}
	}

	/**
	 * Adds the child <code>node</code> to this internal node at
	 * <code>idx</code> .
	 * 
	 * @param idx
	 * @param node
	 * @throws NullPointerException if this node is a leaf node
	 */
	public void addChild(int idx, BTNode<K, V> node) {
		children = insert(children, size, idx, node);
		size++;
	}

	/**
	 * Adds the child <code>node</code> with <code>key</code>to this internal
	 * node at <code>idx</code> .
	 * 
	 * @param idx
	 * @param node
	 * @throws NullPointerException if this node is a leaf node
	 */
	public void addChild(int idx, K key, BTNode<K, V> node) {
		children = insert(children, size, idx, node);
		keys = insert(keys, size - 1, idx, key);
		size++;
	}

	/**
	 * Returns the index of the specified key, or
	 * <code>(-(insertion point) - 1)</code> if it was not found.
	 * 
	 * @param key
	 * @return
	 */
	public final int indexOf(K key) {
		if (size == 0) {
			return -1;
		} else if (isLeaf()) {
			return Arrays.binarySearch(keys, 0, size, key);
		} else {
			return Arrays.binarySearch(keys, 0, size - 1, key);
		}
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
		keys = insert(keys, size, idx, key);
		values = insert(values, size, idx, value);
		size++;
	}

	/**
	 * Returns the size of this node.
	 * 
	 * @return The number of keys for leaves or the number of children for
	 *         internal nodes
	 */
	public int size() {
		return size;
	}

	protected String dump() {
		return toString();
	}

	@Override
	public String toString() {
		if (isLeaf()) {
			StringBuilder buf = new StringBuilder();
			buf.append("[");

			for (int i = 0; i < size; i++) {

				if (i > 0) {
					buf.append(", ");
				}

				buf.append(keys[i]).append("=").append(values[i]);
			}

			buf.append("]");

			return buf.toString();
		} else {
			return Arrays.toString(Arrays.copyOf(children, size));
		}
	}

	public boolean isLeaf() {
		return children == null;
	}

	public BTNode<K, V> getRightSibling() {
		return rightSibling;
	}

	public BTNode<K, V> getLeftSibling() {
		return leftSibling;
	}

	@SuppressWarnings("unchecked")
	static <T> T[] insert(T[] array, int size, int idx, T value) {
		T[] newarr = array;

		if (size + 1 > array.length) {
			newarr = (T[]) Array.newInstance(array.getClass().getComponentType(), size + 1);
			System.arraycopy(array, 0, newarr, 0, idx);
		}

		System.arraycopy(array, idx, newarr, idx + 1, size - idx);
		newarr[idx] = value;

		return newarr;
	}
}