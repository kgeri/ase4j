package org.ogreg.util;


/**
 * Common array manipulation methods.
 *
 * @author  Gergely Kiss
 */
public abstract class Arrays {

    /**
     * Searches the specified array of longs for the specified value using the binary search algorithm. The array must
     * be sorted ascending prior to making this call. If it is not sorted, the results are undefined. If the array
     * contains multiple elements with the specified value, there is no guarantee which one will be found.
     *
     * @param   array      The array to be searched
     * @param   fromIndex  The start index in the array
     * @param   toIndex    The stop index in the array
     * @param   key        The value to be searched for
     *
     * @return  The index of the search key, if it is contained in the array; otherwise, <tt>(-(<i>insertion point</i>)
     *          - 1)</tt>. The <i>insertion point</i> is defined as the point at which the key would be inserted into
     *          the array: the index of the first element greater than the key, or <tt>a.length</tt> if all elements in
     *          the array are less than the specified key. Note that this guarantees that the return value will be &gt;=
     *          0 if and only if the key is found.
     */
    public static int binarySearch(int[] array, int fromIndex, int toIndex, int key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = array[mid];

            if (midVal < key) {
                low = mid + 1;
            } else if (midVal > key) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }

        return -(low + 1); // key not found.
    }

    /**
     * Inserts <code>src</code> at <code>pos</code> in the array <code>dest</code>.
     *
     * <p>The array may or may not be filled completely - the <code>length</code> attribute specifies the number of
     * filled elements (for optimization). If the array is full, then the last element will be shifted out after the
     * insert.</p>
     *
     * @param   dest
     * @param   length
     * @param   pos
     * @param   src
     *
     * @throws  ArrayIndexOutOfBoundsException  if pos >= length or pos >= dest.length or length or pos are negative
     */
    public static void insert(int[] dest, int length, int pos, int src) {
        length = (length >= dest.length) ? (dest.length - pos - 1) : (length - pos);
        System.arraycopy(dest, pos, dest, pos + 1, length);
        dest[pos] = src;
    }

    /**
     * Inserts <code>src</code> at <code>pos</code> in the array <code>dest</code>.
     *
     * <p>The array may or may not be filled completely - the <code>length</code> attribute specifies the number of
     * filled elements (for optimization). If the array is full, then the last element will be shifted out after the
     * insert.</p>
     *
     * @param   dest
     * @param   length
     * @param   pos
     * @param   src
     *
     * @throws  ArrayIndexOutOfBoundsException  if pos >= length or pos >= dest.length or length or pos are negative
     */
    public static void insert(float[] dest, int length, int pos, float src) {
        length = (length >= dest.length) ? (dest.length - pos - 1) : (length - pos);
        System.arraycopy(dest, pos, dest, pos + 1, length);
        dest[pos] = src;
    }
}
