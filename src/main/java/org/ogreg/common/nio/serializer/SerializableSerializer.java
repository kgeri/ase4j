package org.ogreg.common.nio.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.WeakHashMap;

import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.NioUtils;

/**
 * {@link NioSerializer} for {@link Serializable} types.
 * 
 * @author Gergely Kiss
 */
class SerializableSerializer implements NioSerializer<Object> {
	private final Map<Object, byte[]> serializedCache = new WeakHashMap<Object, byte[]>();

	@Override
	public void serialize(Object value, ByteBuffer dest) throws IOException {
		byte[] bytes = toByteArray(value);

		dest.putInt(bytes.length);
		dest.put(bytes);
	}

	@Override
	public Object deserialize(ByteBuffer source) throws IOException {
		byte[] bytes = new byte[source.limit()];
		source.get(bytes);

		// Skipping 4 bytes which stored the size
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes, 4, bytes.length - 4);
		ObjectInputStream ois = new ObjectInputStream(bais);

		try {
			return ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}

	@Override
	public int sizeOf(Object value) {
		return toByteArray(value).length + 4;
	}

	@Override
	public int sizeOf(FileChannel channel, long pos) throws IOException {
		return NioUtils.readInt(channel, pos) + 4;
	}

	private byte[] toByteArray(Object value) {
		byte[] serialized = serializedCache.get(value);

		if (serialized == null) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);

				oos.writeObject(value);
				oos.flush();

				serialized = baos.toByteArray();
				serializedCache.put(value, serialized);
			} catch (IOException e) {
				// It is safe to assume that an IOE will never occur because
				// the underlying OutputStream is a BAOS
				throw new IllegalStateException("Unexpected error", e);
			}
		}

		return serialized;
	}
}
