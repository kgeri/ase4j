package org.ogreg.ostore;

import org.ogreg.common.BaseJaxbManager;
import org.ogreg.common.ConfigurationException;
import org.ogreg.common.utils.FileUtils;
import org.ogreg.common.utils.MBeanUtils;
import org.ogreg.common.utils.PropertyUtils;

import org.ogreg.config.BasePropertyConfig;
import org.ogreg.config.ClassConfig;
import org.ogreg.config.CompositeIdConfig;
import org.ogreg.config.ExtensionConfig;
import org.ogreg.config.IdConfig;
import org.ogreg.config.IndexConfig;
import org.ogreg.config.InstanceTypeConfig;
import org.ogreg.config.ObjectStorageConfig;
import org.ogreg.config.ObjectStorageConfig.Store;
import org.ogreg.config.Parameter;
import org.ogreg.config.PropertyConfig;

import org.ogreg.ostore.file.FileObjectStoreImpl;
import org.ogreg.ostore.index.StringIndex;
import org.ogreg.ostore.index.UniqueIndex;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Object store configurator service. *
 * <p>
 * The configuration XML resources must comply to objectstore.xsd
 * </p>
 * 
 * @author Gergely Kiss
 */
public class ObjectStoreManager extends BaseJaxbManager<ObjectStorageConfig> {

	/** Supported index types. */
	private static final Map<String, Class<? extends UniqueIndex>> SupportedIndices = new HashMap<String, Class<? extends UniqueIndex>>();

	static {

		// TODO More index types
		SupportedIndices.put("trie", StringIndex.class);
	}

	/** The configurations for the different stores. */
	private final Map<String, Store> configuredStores = new HashMap<String, Store>();

	/** The configured and initialized object stores. */
	private final Map<String, ObjectStore<?>> objectStores = new HashMap<String, ObjectStore<?>>();

	public ObjectStoreManager() {
		super(ObjectStorageConfig.class);
	}

	/**
	 * Creates and opens a new object store based on this configuration, or
	 * returns an already initialized object store instance.
	 * <p>
	 * The property storage and other files will be created and opened.
	 * </p>
	 * 
	 * @param id The id of the storage
	 * @param storageDir The storage directory to use
	 * @return A newly initialized {@link ObjectStore}
	 * @throws ConfigurationException on storage init error
	 */
	@SuppressWarnings("rawtypes")
	public synchronized ObjectStore getStore(String id, File storageDir)
			throws ConfigurationException {
		ObjectStore<?> store = objectStores.get(id);

		if (store != null) {
			return store;
		}

		ConfigurableObjectStore cstore = createStore(id, storageDir);
		objectStores.put(id, cstore);

		return cstore;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ConfigurableObjectStore createStore(String id, File storageDir) {
		ConfigurableObjectStore store;

		try {

			// Initializing the storage dir
			FileUtils.mkdirs(storageDir);
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}

		// Checking for config
		Store cfg = getStorageConfigFor(id);
		String mode = cfg.getMode();
		InstanceTypeConfig typeConfig = cfg.getType();
		ClassConfig clazz = cfg.getClazz();

		Map<String, String> params = getParams((typeConfig == null) ? null : typeConfig
				.getParameter());

		// Creating the store
		store = createStore(typeConfig);

		// Setting accessor based on store mode
		EntityAccessor accessor;

		String dynamicType = clazz.getName();

		if ("class".equals(mode)) {
			Class<?> staticType = forName(dynamicType);
			accessor = new FieldAccessor(staticType);
			store.setMetadata(new ObjectStoreMetadata(staticType, null));
		} else if ("dynamic".equals(mode)) {
			accessor = new DynamoAccessor(dynamicType);
			// Setting dynamic metadata after all properties are processed
		} else {
			throw new ConfigurationException("Unsupported store mode: " + mode);
		}

		// Initializing the store
		store.init(accessor, storageDir, params);

		// Processing properties
		Map<String, Class<?>> properties = new LinkedHashMap<String, Class<?>>();

		for (Object property : clazz.getIdOrCompositeIdAndProperty()) {

			// Business key
			if (property instanceof IdConfig) {
				IdConfig prop = (IdConfig) property;
				String propertyName = prop.getName();
				Class<?> propertyType = getPropertyType(cfg, prop);

				UniqueIndex idx = loadOrCreateUniqueIndex(storageDir, propertyName, prop.getIndex());
				store.setIdPropertyName(propertyName);
				store.addProperty(propertyType, propertyName);
				store.addIndex(propertyName, idx);
				properties.put(propertyName, propertyType);
			}
			// Composite Business key
			else if (property instanceof CompositeIdConfig) {
				throw new UnsupportedOperationException("composite-id is not supported yet");
			}
			// Basic property
			else if (property instanceof PropertyConfig) {
				PropertyConfig prop = (PropertyConfig) property;
				String propertyName = prop.getName();
				Class<?> propertyType = getPropertyType(cfg, prop);

				store.addProperty(propertyType, propertyName);
				properties.put(propertyName, propertyType);
			}
			// Extension
			else if (property instanceof ExtensionConfig) {
				ExtensionConfig prop = (ExtensionConfig) property;
				String propertyName = prop.getName();

				store.addExtension(propertyName);
				properties.put(propertyName, Map.class);
			}
		}

		accessor.setProperties(properties);

		// Dynamic stores get a dynamo accessor
		if ("dynamic".equals(cfg.getMode())) {
			store.setMetadata(new ObjectStoreMetadata(null, ((DynamoAccessor) accessor).getType()));
		}

		// Registering as an MBean (the store must have its own interface which
		// extends OSMB and is named conventionally)
		if (store instanceof ObjectStoreMBean) {
			MBeanUtils.register(store, id);
		}

		return store;
	}

	@SuppressWarnings("rawtypes")
	private ConfigurableObjectStore createStore(InstanceTypeConfig config) {
		ConfigurableObjectStore store;

		try {

			if (config == null) {

				// FileObjectStore is the default
				store = new FileObjectStoreImpl();
			} else {

				// But any store is supported
				store = (ConfigurableObjectStore) Class.forName(config.getType()).newInstance();
			}
		} catch (InstantiationException e) {
			throw new ConfigurationException(e);
		} catch (IllegalAccessException e) {
			throw new ConfigurationException(e);
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException(e);
		}

		return store;
	}

	/**
	 * Flushes the specified store.
	 * 
	 * @param id
	 * @throws IOException if the manager has failed to flush the store
	 */
	public synchronized void flushStore(String id) throws IOException {
		ObjectStore<?> store = objectStores.get(id);

		if (store instanceof Flushable) {
			((Flushable) store).flush();
		}

		objectStores.remove(id);
	}

	/**
	 * Closes the specified store and removes it from the store cache.
	 * 
	 * @param id
	 * @throws IOException if the manager has failed to close the store
	 */
	public synchronized void closeStore(String id) throws IOException {
		ObjectStore<?> store = objectStores.get(id);

		if (store instanceof Closeable) {
			((Closeable) store).close();
		}

		objectStores.remove(id);
	}

	@Override
	public void add(ObjectStorageConfig config) throws ConfigurationException {
		String packageName = config.getPackage();

		if (packageName == null) {
			packageName = "";
		} else {
			packageName += ".";
		}

		for (Store store : config.getStore()) {
			configuredStores.put(store.getId(), store);
		}
	}

	/**
	 * Returns the currently initialized object stores.
	 * <p>
	 * Note: should be called after at least one {@link #createStore(String)}.
	 * </p>
	 * 
	 * @return
	 */
	public Map<String, ObjectStore<?>> getObjectStores() {
		return objectStores;
	}

	private Store getStorageConfigFor(String id) throws ConfigurationException {
		Store store = configuredStores.get(id);

		if (store == null) {
			throw new ConfigurationException("No object storage found for identifier: " + id
					+ " Please check the configuration.");
		}

		return store;
	}

	/**
	 * Returns the property type by the configuration.
	 * <p>
	 * When the object store is in <code>class</code> mode (so the entity type
	 * is on the classpath), the property type may be deduced from the entity
	 * type using reflection. Otherwise a configuration error is thrown, unless
	 * the config specifies the correct type.
	 * </p>
	 * 
	 * @param mode
	 * @param storedType
	 * @param propConfig
	 * @return
	 * @throws ConfigurationException if the type can not be determined
	 */
	private Class<?> getPropertyType(Store cfg, BasePropertyConfig propConfig) {

		String propertyName = propConfig.getName();
		String type = propConfig.getType();

		if ("class".equals(cfg.getMode())) {
			return PropertyUtils.getField(forName(cfg.getClazz().getName()), propertyName)
					.getType();
		} else if (type == null) {
			throw new ConfigurationException("Failed to determine property type for '"
					+ propertyName
					+ "'. Attribute 'type' is not set, and the store mode is not 'class'.");
		}

		return forName(type);
	}

	/**
	 * Returns the supported index type by its name (eg.: 'trie') or tries to
	 * find a type with the given class name.
	 * 
	 * @param type
	 * @return The index type
	 * @throws ConfigurationException If the type name is not supported and is
	 *             not a valid class name.
	 */
	@SuppressWarnings("unchecked")
	private Class<? extends UniqueIndex> getIndexType(String type) throws ConfigurationException {
		Class<?> ret = SupportedIndices.get(type);

		try {

			if (ret == null) {
				ret = Class.forName(type);
			}

			if (!UniqueIndex.class.isAssignableFrom(ret)) {
				throw new ConfigurationException("Index type " + ret.getName()
						+ " should implement " + UniqueIndex.class.getName());
			}
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException(e);
		}

		return (Class<? extends UniqueIndex>) ret;
	}

	private UniqueIndex loadOrCreateUniqueIndex(File storageDir, String fieldName, IndexConfig type)
			throws ConfigurationException {

		try {
			// TODO Rebuild index from property store if the file does not exist

			Class<? extends UniqueIndex> indexType = getIndexType(type.getType());
			File indexFile = getIndexFile(storageDir, fieldName);
			UniqueIndex index = indexType.newInstance();

			Map<String, String> params = getParams(type.getParameter());

			// Initializing the index
			index.loadFrom(indexFile, params);

			return index;
		} catch (InstantiationException e) {
			throw new ConfigurationException(e);
		} catch (IllegalAccessException e) {
			throw new ConfigurationException(e);
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}
	}

	private Map<String, String> getParams(List<Parameter> parameters) {
		Map<String, String> ret = new HashMap<String, String>();

		if (parameters != null) {

			for (Parameter param : parameters) {
				ret.put(param.getKey(), param.getValue());
			}
		}

		return ret;
	}

	public static File getIndexFile(File storageDir, String... propertyNames) {
		return new File(storageDir, PropertyUtils.pathToString(propertyNames) + ".uix");
	}

	public static File getSequenceFile(File storageDir) {
		return new File(storageDir, "sequence");
	}

	public static File getPropertyFile(File storageDir, String... propertyNames) {
		return new File(storageDir, PropertyUtils.pathToString(propertyNames) + ".prop");
	}

	public static Pattern getExtensionFileNamePattern(String name) {
		return Pattern.compile(name + "\\.(.*?)\\.prop");
	}
}
