package org.ogreg.common.nio.serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;

/**
 * {@link Double} serializer.
 * 
 * @author Gergely Kiss
 */
class DoubleSerializer implements NioSerializer<Double> {

	@Override
	public void serialize(Double value, ByteBuffer dest) throws IOException {
		dest.putDouble(value);
	}

	@Override
	public Double deserialize(ByteBuffer source) throws IOException {
		return source.getDouble();
	}

	@Override
	public int sizeOf(Double value) {
		return 8;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return 8;
	}
}
