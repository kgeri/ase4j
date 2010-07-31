package org.ogreg.common.nio.serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;

/**
 * {@link Short} serializer.
 * 
 * @author Gergely Kiss
 */
class ShortSerializer implements NioSerializer<Short> {

	@Override
	public void serialize(Short value, ByteBuffer dest) throws IOException {
		dest.putShort(value);
	}

	@Override
	public Short deserialize(ByteBuffer source) throws IOException {
		return source.getShort();
	}

	@Override
	public int sizeOf(Short value) {
		return 2;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return 2;
	}
}
