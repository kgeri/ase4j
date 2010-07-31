package org.ogreg.ase4j.file;

import java.io.Serializable;

import org.ogreg.ase4j.AssociationUtils;
import org.ogreg.util.Arrays;

/**
 * A compressed association block storing all the associations of one subject.
 * <p>
 * Used for file based association storage.
 * </p>
 * 
 * @author Gergely Kiss
 */
class AssociationBlock implements Serializable {
	private static final long serialVersionUID = -1225839956910848754L;

	static int baseCapacity = 64;

	/**
	 * The original capacity of the association row. This is used to indicate
	 * growing.
	 */
	int originalCapacity;

	/** The current capacity of the assocition row. */
	int capacity;

	/** The number of associations currently stored in the row. */
	int size;

	/** The common <code>from</code> part of the associations. */
	int from;

	/**
	 * The <code>to</code> parts of the associations sorted numerically,
	 * ascending. Has the same order as {@link #values}.
	 */
	int[] tos;

	/**
	 * The <code>value</code>s of the associations. Has the same order as
	 * {@link #tos}.
	 */
	int[] values;

	// Helper fields
	/** True, if the association row has changed (dirty flag for updating). */
	boolean changed;

	public AssociationBlock(int from) {
		this(baseCapacity, 0, from);
	}

	public AssociationBlock(int capacity, int size, int from) {
		this.originalCapacity = capacity;
		this.capacity = capacity;
		this.size = size;
		this.from = from;

		this.tos = new int[capacity];
		this.values = new int[capacity];
	}

	/**
	 * Merges all the associations from <code>value</code> to this row.
	 * 
	 * @param value
	 */
	public void merge(AssociationBlock value) {

		if (value.from != from) {
			throw new IllegalArgumentException(
					"Cannot merge associations, because the from subject differs (" + from + " != "
							+ value.from + ")");
		}

		// Safety check
		if ((size + value.size) >= (capacity - 1)) {
			grow(size + value.size);
		}

		for (int i = 0; i < value.size; i++) {
			merge(value.tos[i], value.values[i]);
		}
	}

	/**
	 * Merges the given association to this row.
	 * 
	 * @param to
	 * @param value
	 */
	public void merge(int to, int value) {

		// Determining if to already exists
		int tidx = Arrays.binarySearch(tos, 0, size, to);

		// Insert
		if (tidx < 0) {

			if (size >= (capacity - 1)) {
				grow(size + 1);
			}

			tidx = -tidx - 1;

			Arrays.insert(tos, size, tidx, to);
			Arrays.insert(values, size, tidx, value);
			size++;
			changed = true;
		}
		// Update
		else {
			values[tidx] = AssociationUtils.update(values[tidx], value);
			changed |= value != values[tidx];
		}
	}

	/**
	 * Returns the strength of the association with <code>to</code>.
	 * 
	 * @param to
	 * @return
	 */
	public int get(int to) {
		int tidx = Arrays.binarySearch(tos, 0, size, to);

		return (tidx < 0) ? 0 : values[tidx];
	}

	public AssociationResultBlock asResult() {
		return new AssociationResultBlock(tos, values, size);
	}

	protected void grow(int targetSize) {

		if (capacity >= targetSize) {
			return;
		}

		while (capacity < targetSize) {
			capacity <<= 1;
		}

		int[] ntos = new int[capacity];
		int[] nvalues = new int[capacity];

		System.arraycopy(tos, 0, ntos, 0, size);
		System.arraycopy(values, 0, nvalues, 0, size);

		tos = ntos;
		values = nvalues;
	}

	public int size() {
		return size;
	}

	public int capacity() {
		return capacity;
	}

	public boolean isChanged() {
		return changed;
	}

	public boolean isGrown() {
		return originalCapacity < capacity;
	}
}
