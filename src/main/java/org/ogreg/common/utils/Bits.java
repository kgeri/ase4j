package org.ogreg.common.utils;

/**
 * Common bit manipulation utils.
 * 
 * @author Gergely Kiss
 */
public abstract class Bits {

	/**
	 * Reads the <code>source</code> as an int value, starting from
	 * <code>offset</code>, without any length checks, in Big Endian.
	 * 
	 * @param source
	 * @param offset
	 * @return
	 */
	public static int readInt(byte[] source, int offset) {
		return ((((int) source[offset + 3] & 0xff) << 24)
				| (((int) source[offset + 2] & 0xff) << 16)
				| (((int) source[offset + 1] & 0xff) << 8) | (((int) source[offset + 0] & 0xff) << 0));
	}

	/**
	 * Writes the <code>value</code> to the <code>dest</code> array, without any
	 * length checks, in Big Endian.
	 * 
	 * @param value
	 * @param dest
	 * @param offset
	 */
	public static void writeInt(int value, byte[] dest, int offset) {
		dest[offset + 3] = (byte) (value >> 24);
		dest[offset + 2] = (byte) (value >> 16);
		dest[offset + 1] = (byte) (value >> 8);
		dest[offset + 0] = (byte) (value >> 0);
	}

	/**
	 * Reads the <code>source</code> as a long value, without any length checks,
	 * in Big Endian.
	 * 
	 * @param source
	 * @param offset
	 * @return
	 */
	public static long readLong(byte[] source, int offset) {
		return ((((long) source[offset + 7] & 0xff) << 56)
				| (((long) source[offset + 6] & 0xff) << 48)
				| (((long) source[offset + 5] & 0xff) << 40)
				| (((long) source[offset + 4] & 0xff) << 32)
				| (((long) source[offset + 3] & 0xff) << 24)
				| (((long) source[offset + 2] & 0xff) << 16)
				| (((long) source[offset + 1] & 0xff) << 8) | (((long) source[offset + 0] & 0xff) << 0));
	}

	/**
	 * Writes the <code>value</code> to the <code>dest</code> array, without any
	 * length checks, in Big Endian.
	 * 
	 * @param value
	 * @param dest
	 * @param offset
	 */
	public static void writeLong(long value, byte[] dest, int offset) {
		dest[offset + 7] = (byte) (value >> 56);
		dest[offset + 6] = (byte) (value >> 48);
		dest[offset + 5] = (byte) (value >> 40);
		dest[offset + 4] = (byte) (value >> 32);
		dest[offset + 3] = (byte) (value >> 24);
		dest[offset + 2] = (byte) (value >> 16);
		dest[offset + 1] = (byte) (value >> 8);
		dest[offset + 0] = (byte) (value >> 0);
	}
}
