package org.ogreg.common.nio.serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;

/**
 * {@link Byte} serializer.
 * 
 * @author Gergely Kiss
 */
class ByteSerializer implements NioSerializer<Byte> {

	@Override
	public void serialize(Byte value, ByteBuffer dest) throws IOException {
		dest.put(value);
	}

	@Override
	public Byte deserialize(ByteBuffer source) throws IOException {
		return source.get();
	}

	@Override
	public int sizeOf(Byte value) {
		return 1;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return 1;
	}
}
