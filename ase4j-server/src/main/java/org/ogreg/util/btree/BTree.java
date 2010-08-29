package org.ogreg.util.btree;

/**
 * A generic, effective in-memory B+ tree implementation.
 * <p>
 * This implementation is mostly based on <a href=
 * "http://homepages.ius.edu/rwisman/C455/html/notes/Chapter18/BT-Ops.htm"
 * >B-tree operations</a>. The implementation uses agressive node-splitting, to
 * eliminate the complexity of key propagation.
 * </p>
 * <p>
 * Please note that deletion is not yet implemented.
 * </p>
 * 
 * @param <T> The type of the stored keys
 * @author Gergely Kiss
 */
public class BTree<K extends Comparable<K>, V> {

	/** The order of the balanced tree. */
	private final int order;

	/** The tree's root. */
	BTNode<K, V> root;

	/**
	 * Constructs a BTree with the specified <code>order</code>.
	 * <p>
	 * The <code>order</code> of the tree shows how many children a
	 * {@link BTNode} may have at most.
	 * </p>
	 * 
	 * @param order
	 */
	public BTree(int order) {

		if ((order < 4) || ((order % 2) != 0)) {
			throw new IllegalArgumentException("Invalid order: " + order
					+ ". BTree must have an even order greater than 2.");
		}

		this.order = order;
		root = new BTNode<K, V>(true);
	}

	/**
	 * Gets the value associated to the given key.
	 * 
	 * @param key
	 * @return The value for the given key, or null if the key is not stored
	 */
	public V get(K key) {
		BTNode<K, V> node = root;

		while (true) {
			int idx = node.indexOf(key);

			if (node.isLeaf()) {
				return (idx < 0) ? null : node.values[idx];
			} else {
				idx = (idx < 0) ? (-idx - 1) : idx;
				node = node.children[idx];
			}
		}
	}

	/**
	 * Sets the given value to the key.
	 * 
	 * @param key
	 * @param value
	 */
	public void set(K key, V value) {

		if (root.size() >= order) {
			// Splitting root
			BTNode<K, V> node = root;
			BTNode<K, V> newRoot = new BTNode<K, V>(false);
			newRoot.addChild(0, node);

			splitChildren(newRoot, 0, node);
			setNonFull(newRoot, key, value);

			root = newRoot;
		} else {
			setNonFull(root, key, value);
		}
	}

	/**
	 * Performs a key-value insert, assuming that no nodes are full below
	 * <code>node</code>.
	 * <p>
	 * Sets the <code>value</code> on <code>key</code> recursively, agressively
	 * splitting every full node while traversing down the tree.
	 * </p>
	 * 
	 * @param node
	 * @param key
	 * @param value
	 */
	private void setNonFull(BTNode<K, V> node, K key, V value) {
		int idx = node.indexOf(key);

		if (node.isLeaf()) {
			// Inserting or updating node value if it's a leaf
			if (idx < 0) {
				idx = -idx - 1;

				// Value insert
				node.addValue(idx, key, value);
			} else {

				// Value update
				node.values[idx] = value;
			}
		} else {
			idx = (idx < 0) ? (-idx - 1) : idx;

			BTNode<K, V> child = node.children[idx];

			// Splitting child if it is full
			if (child.size() >= order) {
				splitChildren(node, idx, child);

				// If the key to be inserted is greater than the split node's
				// median, then we must insert the value in the next child
				// (because a child was just now added at idx)
				if (key.compareTo(node.keys[idx]) > 0) {
					child = node.children[idx + 1];
				}
				// Otherwise we must still re-get the child at the calculated
				// index
				else {
					child = node.children[idx];
				}
			}

			setNonFull(child, key, value);
		}
	}

	/**
	 * Splits the <code>node</code> in half.
	 * 
	 * @param parent The parent of <code>node</code>
	 * @param idx The index at which <code>node</code> resides in its
	 *            <code>parent</code>'s children list
	 * @param node The node to split
	 */
	private void splitChildren(BTNode<K, V> parent, int idx, BTNode<K, V> node) {
		BTNode<K, V> newNode;
		K medKey;

		if (node.isLeaf()) {
			// For leaf nodes, we need to propagate the rightmost key
			BTNode<K, V> m = node.splitInHalf();
			medKey = m.keys[m.size() - 1];
			newNode = m;
		} else {
			// For intermal nodes we need to propagate the median key (also, it
			// will be left out from left and right nodes)
			medKey = node.keys[(node.size() - 1) / 2];
			BTNode<K, V> m = node.splitInHalf();
			newNode = m;
		}

		// Propagating new node to parent
		parent.addChild(idx, medKey, newNode);
	}
}
