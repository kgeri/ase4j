package org.ogreg.common.nio.serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;

/**
 * {@link Float} serializer.
 * 
 * @author Gergely Kiss
 */
class FloatSerializer implements NioSerializer<Float> {

	@Override
	public void serialize(Float value, ByteBuffer dest) throws IOException {
		dest.putFloat(value);
	}

	@Override
	public Float deserialize(ByteBuffer source) throws IOException {
		return source.getFloat();
	}

	@Override
	public int sizeOf(Float value) {
		return 4;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return 4;
	}
}
