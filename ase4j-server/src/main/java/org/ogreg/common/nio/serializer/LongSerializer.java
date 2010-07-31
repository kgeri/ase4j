package org.ogreg.common.nio.serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;

/**
 * {@link Long} serializer.
 * 
 * @author Gergely Kiss
 */
class LongSerializer implements NioSerializer<Long> {

	@Override
	public void serialize(Long value, ByteBuffer dest) throws IOException {
		dest.putLong(value);
	}

	@Override
	public Long deserialize(ByteBuffer source) throws IOException {
		return source.getLong();
	}

	@Override
	public int sizeOf(Long value) {
		return 8;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return 8;
	}
}
