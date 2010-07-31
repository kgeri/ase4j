package org.ogreg.common.nio.serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;

import org.ogreg.common.nio.NioSerializer;

/**
 * {@link Date} serializer.
 * 
 * @author Gergely Kiss
 */
class DateSerializer implements NioSerializer<Date> {

	@Override
	public void serialize(Date value, ByteBuffer dest) {
		dest.putLong(value.getTime());
	}

	@Override
	public Date deserialize(ByteBuffer source) {
		return new Date(source.getLong());
	}

	@Override
	public int sizeOf(Date value) {
		return 8;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return 8;
	}
}
