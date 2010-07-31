package org.ogreg.fh4j;

import java.nio.charset.Charset;

import org.ogreg.common.utils.Bits;

/**
 * Serializer for fixed size strings.
 * 
 * @author Gergely Kiss
 */
public class StringSerializer implements Serializer<String> {
	private static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * Specifies the maximum character length this serializer truncates the
	 * strings to.
	 */
	private final int length;

	public StringSerializer(int length) {
		this.length = length;
	}

	@Override
	public int getSize() {
		return length * 2 + 4;
	}

	@Override
	public String read(byte[] source) throws IllegalArgumentException {
		int len = Bits.readInt(source, 0);
		return new String(source, 4, len, UTF8);
	}

	@Override
	public void write(String value, byte[] dest) throws IllegalArgumentException {
		byte[] bytes = value.getBytes(UTF8);
		Bits.writeInt(value.length(), dest, 0);
		System.arraycopy(bytes, 0, dest, 4, Math.min(dest.length, bytes.length));
	}
}
