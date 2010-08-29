package edu.ogreg.util.btree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A clean, in-memory B+ tree implementation for educational purposes.
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
 * @author Gergely Kiss
 */
public class BTree {
	private final int order;
	Node root;

	public BTree(int order) {
		this.order = order;
		root = new BTLeaf();
	}

	// GET
	public int get(String key) {
		Node node = root;

		while (true) {

			if (node instanceof BTLeaf) {
				BTLeaf n = (BTLeaf) node;

				int idx = Collections.binarySearch(n.values, key);

				return (idx < 0) ? Integer.MIN_VALUE : n.values.get(idx).value;
			} else {
				BTNode n = (BTNode) node;
				int idx = Collections.binarySearch(n.keys, key);
				idx = (idx < 0) ? (-idx - 1) : idx;

				node = n.children.get(idx);
			}
		}
	}

	// SET
	public void set(String key, int value) {

		if (root.size() >= order) {
			Node node = root;
			BTNode newRoot = new BTNode();
			newRoot.children.add(node);

			splitChildren(newRoot, 0, node);
			setNonFull(newRoot, key, value);

			root = newRoot;
		} else {
			setNonFull(root, key, value);
		}
	}

	private void setNonFull(Node node, String key, int value) {

		if (node instanceof BTLeaf) {
			BTLeaf n = (BTLeaf) node;
			int idx = Collections.binarySearch(n.values, key);

			if (idx < 0) {
				idx = -idx - 1;

				// Value insert
				insert(n.values, idx, new BTEntry(key, value));
			} else {

				// Value update
				n.values.get(idx).value = value;
			}
		} else {
			BTNode n = (BTNode) node;
			int idx = Collections.binarySearch(n.keys, key);
			idx = (idx < 0) ? (-idx - 1) : idx;

			Node child = n.children.get(idx);

			if (child.size() >= order) {
				splitChildren(n, idx, child);

				// If the key to be inserted is greater than the split node's
				// median, then we must
				// insert the value in the next child (because a child was just
				// now added at idx)
				if (key.compareTo(n.keys.get(idx)) > 0) {
					child = n.children.get(idx + 1);
				}
				// Otherwise we must still re-get the child at the calculated
				// idx
				else {
					child = n.children.get(idx);
				}
			}

			setNonFull(child, key, value);
		}
	}

	private void splitChildren(BTNode parent, int idx, Node node) {
		Node newNode;
		String medKey;

		if (node instanceof BTLeaf) {
			BTLeaf n = (BTLeaf) node;
			BTLeaf m = new BTLeaf();

			int median = n.size() / 2;
			m.values.addAll(n.values.subList(0, median));
			n.values = n.values.subList(median, n.size());

			newNode = m;
			medKey = m.getLastKey();
		} else {
			BTNode n = (BTNode) node;
			BTNode m = new BTNode();

			int ksize = n.size() - 1;
			int kmedian = ksize / 2;
			medKey = n.keys.get(kmedian);

			m.keys.addAll(n.keys.subList(0, kmedian));
			n.keys = n.keys.subList(kmedian + 1, ksize);

			int median = n.size() / 2;
			m.children.addAll(n.children.subList(0, median));
			n.children = n.children.subList(median, n.size());

			newNode = m;
		}

		insert(parent.children, idx, newNode);
		insert(parent.keys, idx, medKey);
	}

	private static <T> void insert(List<? super T> list, int idx, T value) {
		list.add(idx, value);
	}

	static String dump(Node node) {
		return (node instanceof BTLeaf) ? node.toString() : ((BTNode) node).children.toString();
	}

	// Base Node
	abstract class Node {
		public abstract int size();
	}

	// BT Inner node
	private final class BTNode extends Node {
		private List<String> keys = new ArrayList<String>();
		private List<Node> children = new ArrayList<Node>();

		public int size() {
			return children.size();
		}

		@Override
		public String toString() {
			return keys.toString();
		}
	}

	// BT Leaf
	private final class BTLeaf extends Node {
		private List<BTEntry> values = new ArrayList<BTEntry>();

		public String getLastKey() {
			return values.get(values.size() - 1).key;
		}

		public int size() {
			return values.size();
		}

		@Override
		public String toString() {
			return values.toString();
		}
	}

	// BT Entry
	private final class BTEntry implements Comparable<String> {
		private final String key;
		private int value;

		public BTEntry(String key, int value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public int compareTo(String o) {
			return key.compareTo(o);
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}
	}
}
