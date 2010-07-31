package org.ogreg.ostore;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.serializer.SerializerManager;
import org.ogreg.common.utils.PropertyPathUtils;

/**
 * Property field accessor for extended properties (maps).
 * 
 * @author Gergely Kiss
 */
class FieldExtensionAccessor implements FieldAccessor {
	private final Field field;
	private final File storageDir;
	private final Map<String, FieldAccessor> accessors = new HashMap<String, FieldAccessor>();

	public FieldExtensionAccessor(File storageDir, Field field) {
		this.storageDir = storageDir;
		this.field = field;
	}

	@Override
	public Object load(long identifier, String propertyPath) throws IOException {
		if (propertyPath == null) {
			// Full map query
			Map<String, Object> ret = new HashMap<String, Object>();

			for (FieldAccessor accessor : accessors.values()) {
				Object value = accessor.load(identifier, null);
				ret.put(accessor.getFieldName(), value);
			}

			return ret;
		} else {
			// Object picking
			String[] path = PropertyPathUtils.splitFirstPathElement(propertyPath);
			FieldAccessor accessor = accessors.get(path[0]);

			if (accessor == null) {
				return null;
			}

			return accessor.load(identifier, path[1]);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void store(long identifier, String propertyPath, Object value) throws IOException {
		Map<String, Object> map = (Map<String, Object>) value;

		if (propertyPath == null) {
			// Full map update
			for (Entry<String, Object> e : map.entrySet()) {
				String ename = e.getKey();
				Object evalue = e.getValue();

				ensureHasAccessor(ename, evalue).store(identifier, null, evalue);
			}
		} else {
			// Object picking
			String[] path = PropertyPathUtils.splitFirstPathElement(propertyPath);
			ensureHasAccessor(path[0], value).store(identifier, path[1], value);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private FieldAccessor ensureHasAccessor(String name, Object value) throws IOException {
		FieldAccessor accessor = accessors.get(name);

		// Creating property store if it didn't exist
		if (accessor == null) {
			Class<? extends Object> etype = value.getClass();
			NioSerializer<?> s = SerializerManager.findSerializerFor(etype);

			PropertyStore pstore = new PropertyStore();
			pstore.setType(etype);
			pstore.setSerializer(s);
			pstore.open(ObjectStoreImpl.getPropertyFile(storageDir, getFieldName(), name));

			accessor = new ValueAccessor(name, pstore);
			addField(accessor);
		}

		return accessor;
	}

	@Override
	public String getFieldName() {
		return field.getName();
	}

	@Override
	public Object getFrom(Object source) throws IllegalArgumentException, IllegalAccessException {
		return field.get(source);
	}

	@Override
	public void setTo(Object source, Object value) throws IllegalArgumentException,
			IllegalAccessException {
		field.set(source, value);
	}

	@Override
	public void close() throws IOException {
		for (FieldAccessor accessor : accessors.values()) {
			accessor.close();
		}
	}

	@Override
	public void flush() throws IOException {
		for (FieldAccessor accessor : accessors.values()) {
			accessor.flush();
		}
	}

	public void addField(FieldAccessor accessor) {
		accessors.put(accessor.getFieldName(), accessor);
	}

	// Map value accessor
	static class ValueAccessor implements FieldAccessor {
		private final String name;
		private final PropertyStore<Object> store;

		public ValueAccessor(String name, PropertyStore<Object> pstore) {
			this.name = name;
			this.store = pstore;
		}

		@Override
		public Object load(long identifier, String propertyPath) throws IOException {
			// TODO long ids may be supported later...
			// TODO Field name check
			return store.get((int) identifier);
		}

		@Override
		public void store(long identifier, String propertyPath, Object value) throws IOException {
			// TODO long ids may be supported later...
			// TODO Field name check
			store.update((int) identifier, value);
		}

		@Override
		public String getFieldName() {
			return name;
		}

		@Override
		public Object getFrom(Object source) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setTo(Object source, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() throws IOException {
			store.close();
		}

		@Override
		public void flush() throws IOException {
			store.flush();
		}
	}
}
