package org.ogreg.ostore.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ogreg.common.nio.BaseIndexedStore;
import org.ogreg.common.nio.NioUtils;

/**
 * File based property store.
 * 
 * @author Gergely Kiss
 */
class FilePropertyStore<T> extends BaseIndexedStore<T> {
	private static final byte[] MAGIC = new byte[] { 'P', 'R', 'O', 'P' };
	private Class<T> type;

	@Override
	protected void writeHeader(FileChannel channel) throws IOException {
		super.writeHeader(channel);

		// Writing magic bytes
		channel.write(ByteBuffer.wrap(MAGIC));

		// Writing serialized type
		NioUtils.serializeTo(channel, type);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void readHeader(FileChannel channel) throws IOException {
		super.readHeader(channel);

		// Reading magic bytes
		channel.read(ByteBuffer.allocate(4));

		// Reading serialized type
		Class<T> serializedType = NioUtils.deserializeFrom(channel, Class.class);

		// Sanity checks
		if (type != null && !type.equals(serializedType)) {
			throw new IOException("Incompatible property store. Serialized type is "
					+ serializedType.getName() + ", but the store type is " + type.getName());
		}

		type = serializedType;
	}

	public Class<T> getType() {
		return type;
	}

	public void setType(Class<T> type) {
		this.type = type;
	}
}
