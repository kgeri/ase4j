package org.ogreg.common.nio.serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;

/**
 * {@link Integer} serializer.
 * 
 * @author Gergely Kiss
 */
class IntegerSerializer implements NioSerializer<Integer> {

	@Override
	public void serialize(Integer value, ByteBuffer dest) throws IOException {
		dest.putInt(value);
	}

	@Override
	public Integer deserialize(ByteBuffer source) throws IOException {
		return source.getInt();
	}

	@Override
	public int sizeOf(Integer value) {
		return 4;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return 4;
	}
}
