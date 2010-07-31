package org.ogreg.fh4j;

import org.ogreg.common.utils.Bits;

/**
 * Serializer for longs.
 * 
 * @author Gergely Kiss
 */
public class LongSerializer implements Serializer<Long> {

	@Override
	public int getSize() {
		return 8;
	}

	@Override
	public Long read(byte[] s) throws IllegalArgumentException {
		return Bits.readLong(s, 0);
	}

	@Override
	public void write(Long value, byte[] dest) throws IllegalArgumentException {
		long v = value.longValue();
		Bits.writeLong(v, dest, 0);
	}
}
