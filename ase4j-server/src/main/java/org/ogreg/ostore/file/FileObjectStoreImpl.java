package org.ogreg.ostore.file;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogreg.common.ConfigurationException;
import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.serializer.SerializerManager;
import org.ogreg.common.utils.PropertyUtils;
import org.ogreg.common.utils.SerializationUtils;
import org.ogreg.ostore.ConfigurableObjectStore;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreException;
import org.ogreg.ostore.ObjectStoreManager;
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
public class FileObjectStoreImpl<T> implements ConfigurableObjectStore<T>, Closeable {

	/** The entity's field accessors keyed by the field names. */
	private final Map<String, FieldAccessor> accessors = new HashMap<String, FieldAccessor>();

	/** The entity's unique indices keyed by their field names. */
	private final Map<String, UniqueIndex> uniqueIndices = new HashMap<String, UniqueIndex>();

	/** The storage dir of this Object Store. */
	private File storageDir;

	/**
	 * The field which contains the business key.
	 * <p>
	 * Please note that it's not necessary for an object to have a business key,
	 * but some operations will only work if there is one.
	 * </p>
	 */
	private Field uniqueField;

	/** The Java type stored by this Object Store. */
	private Class<T> storedType;

	/** The cached constructor of the stored Java type. */
	private Constructor<T> storedCtor;

	/** The "sequence generator". */
	private AtomicInteger nextKey;

	@Override
	public void init(Class<T> type, File storageDir, Map<String, String> params) {
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
			nextKey = SerializationUtils.read(ObjectStoreManager.getSequenceFile(storageDir),
					AtomicInteger.class);
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
		String[] pathElements = PropertyUtils.splitFirstPathElement(fieldName);
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
	@Override
	public void flush() throws IOException {
		SerializationUtils.write(ObjectStoreManager.getSequenceFile(storageDir), nextKey);

		for (FieldAccessor accessor : accessors.values()) {
			accessor.flush();
		}

		for (Entry<String, UniqueIndex> e : uniqueIndices.entrySet()) {
			UniqueIndex idx = e.getValue();
			idx.saveTo(ObjectStoreManager.getIndexFile(storageDir, e.getKey()));
		}
	}

	@Override
	public void close() throws IOException {
		for (FieldAccessor accessor : accessors.values()) {
			accessor.close();
		}
	}

	private void addField(FieldAccessor accessor) {
		accessors.put(accessor.getFieldName(), accessor);
	}

	private void addIndex(String fieldName, UniqueIndex idx) {
		uniqueIndices.put(fieldName, idx);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void initProperty(Class<?> type, String propertyName) {

		try {
			Field field = getField(type, propertyName);

			Class<?> fieldType = field.getType();

			// TODO Custom config for serializers (through property type)
			NioSerializer<?> s = SerializerManager.findSerializerFor(fieldType);

			FilePropertyStore pstore = new FilePropertyStore();
			pstore.setType(fieldType);
			pstore.setSerializer(s);
			pstore.open(ObjectStoreManager.getPropertyFile(storageDir, field.getName()));

			// TODO Indices for properties

			addField(new FieldPropertyAccessor(field, pstore));
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public void initIdProperty(Class<?> type, String propertyName, UniqueIndex index) {
		addIndex(propertyName, index);
		uniqueField = getField(type, propertyName);
		initProperty(type, propertyName);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void initExtension(Class<?> type, String propertyName) {
		Field field = getField(type, propertyName);

		final Pattern extPattern = ObjectStoreManager.getExtensionFileNamePattern(propertyName);

		File[] extStoreFiles = storageDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return extPattern.matcher(name).matches();
			}
		});

		FieldExtensionAccessor accessor = new FieldExtensionAccessor(storageDir, field);

		if (extStoreFiles != null && extStoreFiles.length > 0) {
			try {

				for (File extStore : extStoreFiles) {
					Matcher m = extPattern.matcher(extStore.getName());
					if (m.matches()) {
						String ename = m.group(1);
						FilePropertyStore pstore = new FilePropertyStore();
						pstore.open(extStore);
						pstore.setSerializer(SerializerManager.findSerializerFor(pstore.getType()));

						accessor.addField(new FieldExtensionAccessor.ValueAccessor(ename, pstore));
					}
				}
			} catch (IOException e) {
				throw new ConfigurationException(e);
			}
		}

		addField(accessor);
	}

	/**
	 * Returns the declared field of the <code>type</code> by its
	 * <code>name</code>.
	 * <p>
	 * As a side effect, the field will be made accessible.
	 * </p>
	 * 
	 * @param type The type which declares the field
	 * @param name The name of the declared field
	 * @return
	 * @throws ConfigurationException
	 */
	static Field getField(Class<?> type, String name) {
		try {
			Field field = type.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (SecurityException e) {
			throw new ConfigurationException(e);
		} catch (NoSuchFieldException e) {
			throw new ConfigurationException(e);
		}
	}
}
