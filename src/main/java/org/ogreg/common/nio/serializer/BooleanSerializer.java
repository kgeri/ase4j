package org.ogreg.common.nio.serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;

/**
 * {@link Boolean} serializer.
 * 
 * @author Gergely Kiss
 */
// TODO Veeery ineffective
class BooleanSerializer implements NioSerializer<Boolean> {

	@Override
	public void serialize(Boolean value, ByteBuffer dest) throws IOException {
		dest.put((byte) (value ? 1 : 0));
	}

	@Override
	public Boolean deserialize(ByteBuffer source) throws IOException {
		return Boolean.valueOf(source.get() > 0);
	}

	@Override
	public int sizeOf(Boolean value) {
		return 1;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return 1;
	}
}
