package org.ogreg.common.nio.serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.NioUtils;

/**
 * UTF-8 {@link String} serializer.
 * 
 * @author Gergely Kiss
 */
class StringSerializer implements NioSerializer<String> {

	@Override
	public void serialize(String value, ByteBuffer dest) {
		dest.putInt(sizeOf(value));
		for (int i = 0; i < value.length(); i++) {
			dest.putChar(value.charAt(i));
		}
	}

	@Override
	public String deserialize(ByteBuffer source) {
		int size = source.getInt();
		size = (size - 4) / 2;

		char[] chrs = new char[size];
		for (int i = 0; i < size; i++) {
			chrs[i] = source.getChar();
		}

		return new String(chrs);
	}

	@Override
	public int sizeOf(String value) {
		return value.length() * 2 + 4;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return NioUtils.readInt(channel, pos);
	}
}
