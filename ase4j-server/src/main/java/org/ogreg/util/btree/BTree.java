package org.ogreg.util.btree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * An effective in-memory B+ tree implementation.
 *
 * @param   <T>  The type of the stored keys
 *
 * @author  Gergely Kiss
 */
public class BTree {

    /** The order of the balanced tree. */
    private final int order;

    /** The tree's root. */
    Node root;

    public BTree(int order) {

        if ((order < 4) || ((order % 2) != 0)) {
            throw new IllegalArgumentException("Invalid order: " + order +
                ". BTree must have an even order greater than 2.");
        }

        this.order = order;
        root = new BTLeaf();
    }

    /**
     * Gets the value associated to the given key.
     *
     * @param   key
     *
     * @return  The value for the given key, or {@link Long#MIN_VALUE} if the key is not stored
     */
    public int get(String key) {
        Node node = root;

        while (true) {

            if (node instanceof BTLeaf) {
                BTLeaf n = (BTLeaf) node;

                int idx = Collections.binarySearch(n.keys, key);

                return (idx < 0) ? Integer.MIN_VALUE : n.values.get(idx);
            } else {
                BTNode n = (BTNode) node;
                int idx = Collections.binarySearch(n.keys, key);
                idx = (idx < 0) ? (-idx - 1) : idx;

                node = n.children.get(idx);
            }
        }
    }

    /**
     * Sets the given value to the key.
     *
     * @param  key
     * @param  value
     */
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
            int idx = Collections.binarySearch(n.keys, key);

            if (idx < 0) {
                idx = -idx - 1;

                // Value insert
                insert(n.keys, idx, key);
                insert(n.values, idx, value);
            } else {

                // Value update
                n.values.set(idx, value);
            }
        } else {
            BTNode n = (BTNode) node;
            int idx = Collections.binarySearch(n.keys, key);
            idx = (idx < 0) ? (-idx - 1) : idx;

            Node child = n.children.get(idx);

            if (child.size() >= order) {
                splitChildren(n, idx, child);

                // If the key to be inserted is greater than the split node's median, then we must
                // insert the value in the next child (because a child was just now added at idx)
                if (key.compareTo(n.keys.get(idx)) > 0) {
                    child = n.children.get(idx + 1);
                }
                // Otherwise we must still re-get the child at the calculated idx
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
            m.keys.addAll(n.keys.subList(0, median));
            m.values.addAll(n.values.subList(0, median));
            n.keys = n.keys.subList(median, n.size());
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

        @Override public String toString() {
            return keys.toString();
        }
    }

    // BT Leaf
    private final class BTLeaf extends Node {
        private List<String> keys = new ArrayList<String>();
        private List<Integer> values = new ArrayList<Integer>();

        public String getLastKey() {
            return keys.get(keys.size() - 1);
        }

        public int size() {
            return values.size();
        }

        @Override public String toString() {
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
        }
    }
}
