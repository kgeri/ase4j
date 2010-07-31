package org.ogreg.common.nio.serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.NioSerializer;

/**
 * {@link Character} serializer.
 * 
 * @author Gergely Kiss
 */
class CharacterSerializer implements NioSerializer<Character> {

	@Override
	public void serialize(Character value, ByteBuffer dest) throws IOException {
		dest.putChar(value);
	}

	@Override
	public Character deserialize(ByteBuffer source) throws IOException {
		return source.getChar();
	}

	@Override
	public int sizeOf(Character value) {
		return 2;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return 2;
	}
}
