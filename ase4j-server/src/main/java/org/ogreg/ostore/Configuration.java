package org.ogreg.ostore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.ogreg.common.nio.NioSerializer;
import org.ogreg.common.nio.NioUtils;
import org.ogreg.common.nio.serializer.SerializerManager;
import org.ogreg.ostore.config.BaseElementConfig;
import org.ogreg.ostore.config.ClassConfig;
import org.ogreg.ostore.config.CompositeIdConfig;
import org.ogreg.ostore.config.ExtensionConfig;
import org.ogreg.ostore.config.IdConfig;
import org.ogreg.ostore.config.IndexConfig;
import org.ogreg.ostore.config.IndexConfig.Parameter;
import org.ogreg.ostore.config.PropertyConfig;
import org.ogreg.ostore.config.Storage;
import org.ogreg.ostore.index.StringIndex;

/**
 * Object store configuration bean.
 * 
 * @author Gergely Kiss
 */
public class Configuration {
	/** Supported index types. */
	private static final Map<String, Class<? extends UniqueIndex>> SupportedIndices = new HashMap<String, Class<? extends UniqueIndex>>();

	static {
		// TODO More index types
		SupportedIndices.put("trie", StringIndex.class);
	}

	/** The configurations for the different stored Java types. */
	private final Map<Class<?>, ClassConfig> configuredTypes = new HashMap<Class<?>, ClassConfig>();

	/**
	 * Adds the given Object Store configuration file to the current
	 * configuration.
	 * <p>
	 * The file must comply to the XML schema
	 * http://ase4j.googlecode.com/schema/objectstore.xsd
	 * </p>
	 * 
	 * @param configurationFile The source file to read
	 * @throws ObjectStoreException If the configuration has failed
	 */
	public void add(File configurationFile) throws ObjectStoreException {
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(configurationFile);
			add(fis);
		} catch (FileNotFoundException e) {
			throw new ObjectStoreException(e);
		} finally {
			NioUtils.closeQuietly(fis);
		}
	}

	/**
	 * Adds the given Object Store configuration resource to the current
	 * configuration.
	 * <p>
	 * The resource must comply to the XML schema
	 * http://ase4j.googlecode.com/schema/objectstore.xsd
	 * </p>
	 * 
	 * @param configurationResource The resource to read
	 * @throws ObjectStoreException If the configuration has failed
	 */
	public void add(String configurationResource) throws ObjectStoreException {
		FileInputStream fis = null;

		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			InputStream is = null;

			if (contextClassLoader != null) {
				is = contextClassLoader.getResourceAsStream(configurationResource);
			}
			if (is == null) {
				is = getClass().getClassLoader().getResourceAsStream(configurationResource);
			}

			if (is == null) {
				throw new ObjectStoreException("Resource was not found: " + configurationResource);
			}

			add(is);
		} finally {
			NioUtils.closeQuietly(fis);
		}
	}

	/**
	 * Adds the given Object Store configuration to the current configuration.
	 * <p>
	 * The source must comply to the XML schema
	 * http://ase4j.googlecode.com/schema/objectstore.xsd
	 * </p>
	 * 
	 * @param configurationStream The source stream to read
	 * @throws ObjectStoreException If the configuration has failed
	 */
	public void add(InputStream configurationStream) throws ObjectStoreException {
		try {
			JAXBContext jc = JAXBContext.newInstance(Storage.class.getPackage().getName());
			Unmarshaller um = jc.createUnmarshaller();

			Storage storage = (Storage) um.unmarshal(configurationStream);

			add(storage);
		} catch (JAXBException e) {
			throw new ObjectStoreException(e);
		}
	}

	private void add(Storage storage) throws ObjectStoreException {
		String packageName = storage.getPackage();
		if (packageName == null) {
			packageName = "";
		} else {
			packageName += ".";
		}

		for (ClassConfig clazz : storage.getClazz()) {
			try {
				Class<?> type = Class.forName(packageName + clazz.getName());
				configuredTypes.put(type, clazz);
			} catch (ClassNotFoundException e) {
				throw new ObjectStoreException(e);
			}
		}
	}

	/**
	 * Creates and opens a new object store based on this configuration.
	 * <p>
	 * The property storage and other files will be created and opened.
	 * </p>
	 * 
	 * @param type The type of the stored objects
	 * @param storageDir The storage directory to use
	 * @return A newly initialized {@link ObjectStore}
	 * @throws ObjectStoreException on storage init error
	 */
	public <T> ObjectStore<T> createStore(Class<T> type, File storageDir)
			throws ObjectStoreException {
		// Initializing the storage dir
		storageDir.mkdirs();

		// Creating the store
		ObjectStoreImpl<T> store = new ObjectStoreImpl<T>(type, storageDir);

		// Initializing the store
		init(type, store, storageDir);

		return store;
	}

	/**
	 * Initializes the object store.
	 * 
	 * @throws ObjectStoreException On storage init error
	 */
	protected void init(Class<?> type, ObjectStoreImpl<?> store, File storageDir)
			throws ObjectStoreException {
		ClassConfig clazz = getClassConfigFor(type);

		for (Object property : clazz.getIdOrCompositeIdAndProperty()) {
			// Business key
			if (property instanceof IdConfig) {
				IdConfig prop = (IdConfig) property;

				UniqueIndex idx = loadOrCreateUniqueIndex(storageDir, prop.getName(),
						prop.getIndex());

				store.addIndex(prop.getName(), idx);
				store.setUniqueField(getField(type, prop.getName()));

				addProperty(type, prop, store, storageDir);
			}
			// Composite Business key
			else if (property instanceof CompositeIdConfig) {
				throw new UnsupportedOperationException("composite-id is not supported yet");
			}
			// Basic property
			else if (property instanceof PropertyConfig) {
				PropertyConfig prop = (PropertyConfig) property;
				addProperty(type, prop, store, storageDir);
			}
			// Extension
			else if (property instanceof ExtensionConfig) {
				ExtensionConfig prop = (ExtensionConfig) property;
				addExtension(type, prop, store, storageDir);
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addProperty(Class<?> type, BaseElementConfig cfg, ObjectStoreImpl<?> store,
			File storageDir) throws ObjectStoreException {
		Field field = getField(type, cfg.getName());

		Class<?> fieldType = field.getType();

		// TODO Custom config for serializers (through property type)
		NioSerializer<?> s = SerializerManager.findSerializerFor(fieldType);

		try {
			PropertyStore pstore = new PropertyStore();
			pstore.setType(fieldType);
			pstore.setSerializer(s);
			pstore.open(ObjectStoreImpl.getPropertyFile(storageDir, field.getName()));

			// TODO Indices for properties

			store.addField(new FieldPropertyAccessor(field, pstore));
		} catch (IOException e) {
			throw new ObjectStoreException(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addExtension(Class<?> type, ExtensionConfig cfg, ObjectStoreImpl<?> store,
			File storageDir) throws ObjectStoreException {
		String propertyName = cfg.getName();
		Field field = getField(type, propertyName);

		final Pattern extPattern = ObjectStoreImpl.getExtensionFileNamePattern(propertyName);

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
						PropertyStore pstore = new PropertyStore();
						pstore.open(extStore);
						pstore.setSerializer(SerializerManager.findSerializerFor(pstore.getType()));

						accessor.addField(new FieldExtensionAccessor.ValueAccessor(ename, pstore));
					}
				}
			} catch (IOException e) {
				throw new ObjectStoreException(e);
			}
		}

		store.addField(accessor);
	}

	protected ClassConfig getClassConfigFor(Class<?> type) throws ObjectStoreException {
		ClassConfig clazz = configuredTypes.get(type);

		if (clazz == null) {
			throw new ObjectStoreException("No storage info found for type: " + type.getName()
					+ " Please check the configuration.");
		}

		return clazz;
	}

	private Field getField(Class<?> type, String name) throws ObjectStoreException {
		try {
			Field field = type.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (SecurityException e) {
			throw new ObjectStoreException(e);
		} catch (NoSuchFieldException e) {
			throw new ObjectStoreException(e);
		}
	}

	/**
	 * Returns the supported index type by its name (eg.: 'trie') or tries to
	 * find a type with the given class name.
	 * 
	 * @param type
	 * @return The index type
	 * @throws ObjectStoreException If the type name is not supported and is not
	 *             a valid class name.
	 */
	@SuppressWarnings("unchecked")
	private Class<? extends UniqueIndex> getIndexType(String type) throws ObjectStoreException {
		Class<?> ret = SupportedIndices.get(type);

		try {
			if (ret == null) {
				ret = Class.forName(type);
			}

			if (!UniqueIndex.class.isAssignableFrom(ret)) {
				throw new ObjectStoreException("Index type " + ret.getName() + " should implement "
						+ UniqueIndex.class.getName());
			}
		} catch (ClassNotFoundException e) {
			throw new ObjectStoreException(e);
		}

		return (Class<? extends UniqueIndex>) ret;
	}

	private UniqueIndex loadOrCreateUniqueIndex(File storageDir, String fieldName, IndexConfig type)
			throws ObjectStoreException {

		try {
			// TODO Rebuild index from PropertyStore if the file does not exist

			Class<? extends UniqueIndex> indexType = getIndexType(type.getType());
			File indexFile = ObjectStoreImpl.getIndexFile(storageDir, fieldName);
			UniqueIndex index = indexType.newInstance();

			Map<String, String> params = new HashMap<String, String>();
			for (Parameter param : type.getParameter()) {
				params.put(param.getKey(), param.getValue());
			}

			// Initializing the index
			index.loadFrom(indexFile, params);

			return index;
		} catch (InstantiationException e) {
			throw new ObjectStoreException(e);
		} catch (IllegalAccessException e) {
			throw new ObjectStoreException(e);
		} catch (IOException e) {
			throw new ObjectStoreException(e);
		}
	}
}
