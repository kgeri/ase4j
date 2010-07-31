package org.ogreg.common.nio.serializer;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.ogreg.common.nio.NioSerializer;

/**
 * Serializer implementation manager.
 * 
 * @author Gergely Kiss
 */
public abstract class SerializerManager {
	static final Charset UTF8 = Charset.forName("UTF-8");
	static final Map<Class<?>, NioSerializer<?>> DefaultSerializers = new HashMap<Class<?>, NioSerializer<?>>();

	/** Default serializer for any Java type. */
	private static final NioSerializer<?> DefaultSerializer = new SerializableSerializer();

	static {
		DefaultSerializers.put(String.class, new StringSerializer());
		DefaultSerializers.put(Date.class, new DateSerializer());

		DefaultSerializers.put(Long.class, new LongSerializer());
		DefaultSerializers.put(Integer.class, new IntegerSerializer());
		DefaultSerializers.put(Short.class, new ShortSerializer());
		DefaultSerializers.put(Character.class, new CharacterSerializer());
		DefaultSerializers.put(Byte.class, new ByteSerializer());
		DefaultSerializers.put(Float.class, new FloatSerializer());
		DefaultSerializers.put(Double.class, new DoubleSerializer());
		DefaultSerializers.put(Boolean.class, new BooleanSerializer());

		DefaultSerializers.put(long.class, new LongSerializer());
		DefaultSerializers.put(int.class, new IntegerSerializer());
		DefaultSerializers.put(short.class, new ShortSerializer());
		DefaultSerializers.put(char.class, new CharacterSerializer());
		DefaultSerializers.put(byte.class, new ByteSerializer());
		DefaultSerializers.put(float.class, new FloatSerializer());
		DefaultSerializers.put(double.class, new DoubleSerializer());
		DefaultSerializers.put(boolean.class, new BooleanSerializer());
	}

	/**
	 * Locates a serializer for the given type.
	 * 
	 * @param type The type to serialize.
	 * @return A serializer for <code>type</code>, never null
	 * @throws NoSerializerFoundException if the type is not serializable
	 */
	@SuppressWarnings("unchecked")
	public static <T> NioSerializer<T> findSerializerFor(Class<T> type) {
		// TODO Extension point?
		NioSerializer<?> ret;

		ret = SerializerManager.DefaultSerializers.get(type);

		if (ret != null) {
			return (NioSerializer<T>) ret;
		}

		if (!Serializable.class.isAssignableFrom(type)) {
			throw new NoSerializerFoundException(type.getName());
		}

		return (NioSerializer<T>) DefaultSerializer;
	}

	/**
	 * Runtime error which signals that there were no serializers found for a
	 * given type.
	 * 
	 * @author Gergely Kiss
	 */
	public static class NoSerializerFoundException extends RuntimeException {
		private static final long serialVersionUID = -6906093696767781530L;

		public NoSerializerFoundException(String typeName) {
			super(typeName);
		}
	}
}
