package org.ogreg.util;

public class SortedArray {
	private byte[][] keys = new byte[1][];
	private long[] values = new long[1];
	private int size = 1;
	private int capacity = 1;

	private TrieDictionary dictionary = TrieDictionary.EN;

	public SortedArray() {
		keys[0] = new byte[0];
		values[0] = 0;
	}

	public void set(String key, long value) {
		byte[] bk = dictionary.encode(key);
		int idx = binarySearch(keys, 0, size, bk);

		if (idx < 0) {
			if (size >= (capacity - 1)) {
				grow(size + 1);
			}

			idx = -idx - 1;
			insert(keys, size, idx, bk);
			insert(values, size, idx, value);
			size++;
		} else {
			values[idx] = value;
		}
	}

	public long get(String key) {
		byte[] bk = dictionary.encode(key);
		int idx = binarySearch(keys, 0, size, bk);
		return idx < 0 ? 0 : values[idx];
	}

	private void grow(int targetSize) {
		while (capacity < targetSize) {
			capacity <<= 1;
		}

		byte[][] nkeys = new byte[capacity][];
		long[] nvalues = new long[capacity];

		System.arraycopy(keys, 0, nkeys, 0, size);
		System.arraycopy(values, 0, nvalues, 0, size);

		keys = nkeys;
		values = nvalues;
	}

	private static final int binarySearch(byte[][] keys, int fromIndex, int toIndex, byte[] key) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			byte[] midVal = keys[mid];

			int cmp = compare(midVal, key);

			if (cmp < 0) {
				low = mid + 1;
			} else if (cmp > 0) {
				high = mid - 1;
			} else {
				return mid; // key found
			}
		}

		return -(low + 1); // key not found.
	}

	private static final void insert(byte[][] dest, int length, int pos, byte[] src) {
		length = (length >= dest.length) ? (dest.length - pos - 1) : (length - pos);
		System.arraycopy(dest, pos, dest, pos + 1, length);
		dest[pos] = src;
	}

	private static final void insert(long[] dest, int length, int pos, long src) {
		length = (length >= dest.length) ? (dest.length - pos - 1) : (length - pos);
		System.arraycopy(dest, pos, dest, pos + 1, length);
		dest[pos] = src;
	}

	private static int compare(byte[] a, byte[] b) {
		int l = Math.min(a.length, b.length);

		for (int i = 0; i < l; i++) {
			byte ai = a[i];
			byte bi = b[i];

			if (ai < bi) {
				return -1;
			} else if (ai > bi) {
				return 1;
			}
		}

		if (a.length < b.length) {
			return -1;
		} else if (b.length > a.length) {
			return 1;
		} else {
			return 0;
		}
	}
}
