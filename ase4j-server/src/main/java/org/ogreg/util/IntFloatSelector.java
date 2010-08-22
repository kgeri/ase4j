package org.ogreg.util;

import java.util.Arrays;

/**
 * An int-float selector.
 * <p>
 * This is a data structure for determining the top k elements of n int-float
 * pairs in O(n * log(k)) time. It is based on a binary heap.
 * </p>
 * <p>
 * The {@link #add(int, int)} method should be used to fill this selector with
 * key-value pairs, then its {@link #keys()} and {@link #values()} functions may
 * be used to get the top k key-value pairs.
 * </p>
 * <p>
 * Note: key-value pairs returned by this selector are <b>not</b> ordered. They
 * are only guaranteed to be the <code>k</code> largest value elements.
 * </p>
 * 
 * @author Gergely Kiss
 */
public class IntFloatSelector {
	int[] keys;
	float[] vals;
	private int size = 0;

	public IntFloatSelector(int maxSize) {
		keys = new int[maxSize];
		vals = new float[maxSize];

		Arrays.fill(keys, Integer.MIN_VALUE);
	}

	/**
	 * Adds the int-float pair to the selector.
	 * 
	 * @param key
	 * @param val
	 */
	public void add(int key, float val) {
		if (size == keys.length) {

			// Shortcut for low values when the heap is full
			if (val <= vals[0]) {
				return;
			}

			removeMin();
		}

		vals[size] = val;
		keys[size] = key;

		siftUp(size);

		size++;
	}

	/**
	 * Returns the keys.
	 * <p>
	 * Note: the backing array is returned, which may be larger than
	 * {@link #size}.
	 * </p>
	 * 
	 * @return
	 */
	public int[] keys() {
		return keys;
	}

	/**
	 * Returns the values.
	 * <p>
	 * Note: the backing array is returned, which may be larger than
	 * {@link #size}.
	 * </p>
	 * 
	 * @return
	 */
	public float[] values() {
		return vals;
	}

	/**
	 * Returns the number of stored elements in the selector.
	 * 
	 * @return
	 */
	public int size() {
		return size;
	}

	/**
	 * Removes the minimum value from the heaps.
	 */
	private void removeMin() {
		vals[0] = vals[size - 1];
		keys[0] = keys[size - 1];

		siftDown(0);

		size--;
	}

	/**
	 * Sifts the specified index down in both the keys and values heaps until
	 * its heap property is restored.
	 * 
	 * @param index
	 */
	private void siftDown(int index) {
		int rindex;
		int lindex;
		int minindex;

		while (true) {
			rindex = rightChildIndex(index);
			lindex = leftChildIndex(index);

			// Determining minimum value index from left and right children
			if (rindex >= size) {
				if (lindex >= size) {
					// No children
					break;
				} else {
					// Only left children, left is minimum
					minindex = lindex;
				}
			} else {
				if (vals[lindex] <= vals[rindex]) {
					// Left child is smaller
					minindex = lindex;
				} else {
					// Right child is smaller
					minindex = rindex;
				}
			}

			// Determining if any of the children is smaller than us
			if (vals[index] > vals[minindex]) {
				// Key swap
				int minKey = keys[minindex];
				keys[minindex] = keys[index];
				keys[index] = minKey;

				// Value swap
				float minVal = vals[minindex];
				vals[minindex] = vals[index];
				vals[index] = minVal;

				// Moving to next index
				index = minindex;
			} else {
				break;
			}
		}
	}

	/**
	 * Sifts the specified index up in both the keys and values heaps until its
	 * heap property is restored.
	 * 
	 * @param index
	 */
	private void siftUp(int index) {
		float value;
		int parentIndex;
		float parentValue;

		while (index != 0) {
			parentIndex = parentIndex(index);

			parentValue = vals[parentIndex];
			value = vals[index];

			if (parentValue > value) {
				// Key swap
				int parentKey = keys[parentIndex];
				keys[parentIndex] = keys[index];
				keys[index] = parentKey;

				// Value swap
				vals[parentIndex] = value;
				vals[index] = parentValue;

				// Moving to next index
				index = parentIndex;
			} else {
				break;
			}
		}
	}

	/**
	 * Returns the parent index of the specified index.
	 * 
	 * @param index
	 * @return
	 */
	private int parentIndex(int index) {
		return (index - 1) / 2;
	}

	/**
	 * Returns the right child's index of the specified index.
	 * 
	 * @param index
	 * @return
	 */
	private int rightChildIndex(int index) {
		return 2 * index + 2;
	}

	/**
	 * Returns the left child's index of the specified index.
	 * 
	 * @param index
	 * @return
	 */
	private int leftChildIndex(int index) {
		return 2 * index + 1;
	}
}
