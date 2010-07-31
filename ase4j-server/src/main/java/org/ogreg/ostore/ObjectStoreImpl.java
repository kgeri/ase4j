package org.ogreg.ostore;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.ogreg.common.utils.PropertyPathUtils;
import org.ogreg.common.utils.SerializationUtils;
import org.ogreg.ostore.index.UniqueIndex;

/**
 * A generic, file-based implementation of the {@link ObjectStore} interface.
 * <p>
 * The objects are stored with their properties, in different files. Please see
 * {@link ObjectStore} for further documentation.
 * </p>
 * 
 * @author Gergely Kiss
 */
class ObjectStoreImpl<T> implements ObjectStore<T>, Closeable, Flushable {
	/** The entity's field accessors keyed by the field names. */
	private final Map<String, FieldAccessor> accessors = new HashMap<String, FieldAccessor>();

	/** The entity's unique indices keyed by their field names. */
	private final Map<String, UniqueIndex> uniqueIndices = new HashMap<String, UniqueIndex>();

	/** The storage dir of this Object Store. */
	private final File storageDir;

	/**
	 * The field which contains the business key.
	 * <p>
	 * Please note that it's not necessary for an object to have a business key,
	 * but some operations will only work if there is one.
	 * </p>
	 */
	private Field uniqueField;

	/** The Java type stored by this Object Store. */
	private final Class<T> storedType;

	/** The cached constructor of the stored Java type. */
	private final Constructor<T> storedCtor;

	/** The "sequence generator". */
	private AtomicInteger nextKey;

	ObjectStoreImpl(Class<T> type, File storageDir) {
		this.storedType = type;
		this.storageDir = storageDir;

		try {
			this.storedCtor = storedType.getDeclaredConstructor();
			storedCtor.setAccessible(true);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Stored type " + storedType.getName()
					+ " should have a default constructor", e);
		}

		try {
			nextKey = SerializationUtils.read(getSequenceFile(storageDir), AtomicInteger.class);
		} catch (IOException e) {
			nextKey = new AtomicInteger();
		}
	}

	protected Object getBusinessKey(T entity) throws ObjectStoreException {
		if (uniqueField == null) {
			throw new IllegalStateException(
					"The type "
							+ storedType.getName()
							+ " did not specify a business key. Please mark an appropriate business key with <id> before using this operation.");
		}

		try {
			return uniqueField.get(entity);
		} catch (IllegalArgumentException e) {
			throw new ObjectStoreException(e);
		} catch (IllegalAccessException e) {
			throw new ObjectStoreException(e);
		}
	}

	@Override
	public long save(T entity) throws ObjectStoreException {
		Object key = getBusinessKey(entity);

		Long identifier = uniqueResult(uniqueField.getName(), key);

		// Save
		if (identifier == null) {
			identifier = Long.valueOf(nextKey.incrementAndGet());

			// No updates here
			add(identifier, entity);
		}

		return identifier;
	}

	@Override
	public long saveOrUpdate(T entity) throws ObjectStoreException {
		Object key = getBusinessKey(entity);

		Long identifier = uniqueResult(uniqueField.getName(), key);

		// Save or update
		if (identifier == null) {
			identifier = Long.valueOf(nextKey.incrementAndGet());
		}

		// Update if necessary
		add(identifier, entity);

		return identifier;
	}

	@Override
	public void add(long identifier, T entity) throws ObjectStoreException {
		try {

			// Saving properties
			for (FieldAccessor accessor : accessors.values()) {
				Object value;
				value = accessor.getFrom(entity);

				if (value == null) {
					continue;
				}

				// Updating indices
				UniqueIndex idx = uniqueIndices.get(accessor.getFieldName());

				if (idx != null) {
					idx.setKey(value, identifier);
				}

				accessor.store(identifier, null, value);
			}
		} catch (IllegalArgumentException e) {
			throw new ObjectStoreException(e);
		} catch (IllegalAccessException e) {
			throw new ObjectStoreException(e);
		} catch (IOException e) {
			throw new ObjectStoreException(e);
		}
	}

	@Override
	public T get(long identifier) throws ObjectStoreException {
		try {
			T result = storedCtor.newInstance();

			for (FieldAccessor accessor : accessors.values()) {
				Object value = accessor.load(identifier, null);
				accessor.setTo(result, value);
			}

			return result;
		} catch (Exception e) {
			throw new ObjectStoreException(e);
		}
	}

	@Override
	public Object getField(long identifier, String fieldName) throws ObjectStoreException {
		String[] pathElements = PropertyPathUtils.splitFirstPathElement(fieldName);
		FieldAccessor accessor = accessors.get(pathElements[0]);

		if (accessor == null) {
			throw new ObjectStoreException("Field not found: " + storedType.getName() + "."
					+ fieldName);
		}

		try {
			return accessor.load(identifier, pathElements[1]);
		} catch (IOException e) {
			throw new ObjectStoreException(e);
		}
	}

	@Override
	public Long uniqueResult(String fieldName, Object value) throws ObjectStoreException {
		UniqueIndex idx = uniqueIndices.get(fieldName);

		if (idx == null) {
			throw new ObjectStoreException("Field " + storedType.getName() + "." + fieldName
					+ " does not have a unique index specified.");
		}

		if (value == null) {
			throw new ObjectStoreException("The field " + storedType.getName() + "." + fieldName
					+ " has a unique index, so its value should not be null.");
		}

		return idx.getKey(value);
	}

	/**
	 * Flushes the changes made to all of the property stores.
	 * 
	 * @throws IOException
	 */
	public void flush() throws IOException {
		SerializationUtils.write(getSequenceFile(storageDir), nextKey);

		for (FieldAccessor accessor : accessors.values()) {
			accessor.flush();
		}

		for (Entry<String, UniqueIndex> e : uniqueIndices.entrySet()) {
			UniqueIndex idx = e.getValue();
			idx.saveTo(getIndexFile(storageDir, e.getKey()));
		}
	}

	@Override
	public void close() throws IOException {
		for (FieldAccessor accessor : accessors.values()) {
			accessor.close();
		}
	}

	void addField(FieldAccessor accessor) {
		accessors.put(accessor.getFieldName(), accessor);
	}

	void addIndex(String fieldName, UniqueIndex idx) {
		uniqueIndices.put(fieldName, idx);
	}

	void setUniqueField(Field field) {
		uniqueField = field;
	}

	static File getIndexFile(File storageDir, String... propertyNames) {
		return new File(storageDir, PropertyPathUtils.pathToString(propertyNames) + ".uix");
	}

	static File getPropertyFile(File storageDir, String... propertyNames) {
		return new File(storageDir, PropertyPathUtils.pathToString(propertyNames) + ".prop");
	}

	static File getSequenceFile(File storageDir) {
		return new File(storageDir, "sequence");
	}

	static Pattern getExtensionFileNamePattern(String name) {
		return Pattern.compile(name + "\\.(.*?)\\.prop");
	}
}
