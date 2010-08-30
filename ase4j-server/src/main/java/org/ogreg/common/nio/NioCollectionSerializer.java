package org.ogreg.common.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

/**
 * Support implementation for collection serializers using NIO.
 * 
 * @param <E> The serialized type
 * @author Gergely Kiss
 */
public abstract class NioCollectionSerializer<E> {

	// Allocate 512k buffer
	private final ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);

	/**
	 * The implementor should read exactly one chunk context-sensitively from
	 * the buffer. The number of bytes read may not exceed the given
	 * {@link SerializationContext#maxChunkSize}.
	 * 
	 * @param buf
	 * @param ctx
	 * @throws IOException on read error
	 */
	protected abstract void read(ByteBuffer buf, SerializationContext ctx) throws IOException;

	/**
	 * The implementor should write exactly one chunk context-sensitively to the
	 * buffer. The number of bytes written may not exceed the given
	 * {@link SerializationContext#maxChunkSize}.
	 * 
	 * @param buf
	 * @param ctx
	 * @throws IOException
	 */
	protected abstract void write(E elem, ByteBuffer buf, SerializationContext ctx)
			throws IOException;

	/**
	 * Serializes the contents with <code>process</code> using the given
	 * <code>ctx</code>.
	 * 
	 * @param it
	 * @param channel
	 * @throws IOException on write error
	 */
	protected synchronized void serialize(Iterator<E> it, FileChannel channel,
			SerializationContext ctx) throws IOException {
		buf.clear();

		while (it.hasNext()) {
			E elem = it.next();

			if (buf.remaining() < ctx.maxChunkSize) {
				buf.flip();
				channel.write(buf);
				buf.clear();
			}

			write(elem, buf, ctx);
		}

		buf.flip();
		channel.write(buf);
	}

	/**
	 * Deserializes the contents from <code>src</code> using the given
	 * <code>ctx</code>.
	 * 
	 * @param src
	 * @param ctx
	 * @throws IOException on read error
	 */
	protected synchronized void deserialize(FileChannel src, SerializationContext ctx)
			throws IOException {
		buf.clear();

		while (src.read(buf) != -1) {
			buf.flip();

			while (buf.remaining() > ctx.maxChunkSize) {
				read(buf, ctx);
			}

			buf.compact();
		}

		buf.flip();

		while (buf.hasRemaining()) {
			read(buf, ctx);
		}
	}

	/**
	 * Base class for storing serialization state.
	 * 
	 * @author Gergely Kiss
	 */
	public class SerializationContext {
		private int maxChunkSize = 4096;
		private SerializerListener<E> listener;

		public void setMaxChunkSize(int maxChunkSize) {
			this.maxChunkSize = maxChunkSize;
		}

		public void setListener(SerializerListener<E> listener) {
			this.listener = listener;
		}

		public void onEntryRead(E entry) {
			if (listener != null) {
				listener.onEntryRead(entry);
			}
		}
	}

	/**
	 * Deserialization listener.
	 * 
	 * @author Gergely Kiss
	 * @param <E>
	 */
	public interface SerializerListener<E> {
		void onEntryRead(E entry);
	}
}
