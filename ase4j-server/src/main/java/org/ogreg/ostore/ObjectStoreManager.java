package org.ogreg.ostore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.ogreg.common.BaseJaxbManager;
import org.ogreg.common.ConfigurationException;
import org.ogreg.common.utils.PropertyUtils;
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

	public ObjectStoreManager() {
		super(ObjectStorageConfig.class);
	}

	/**
	 * Creates and opens a new object store based on this configuration.
	 * <p>
	 * The property storage and other files will be created and opened.
	 * </p>
	 * 
	 * @param id The id of the storage
	 * @param storageDir The storage directory to use
	 * @return A newly initialized {@link ObjectStore}
	 * @throws ConfigurationException on storage init error
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ObjectStore createStore(String id, File storageDir) throws ConfigurationException {

		// Initializing the storage dir
		storageDir.mkdirs();

		// Checking for config
		Store cfg = getStorageConfigFor(id);
		InstanceTypeConfig typeConfig = cfg.getType();

		// Creating the store
		ConfigurableObjectStore store = createStore(typeConfig);

		// Initializing the store
		ClassConfig clazz = cfg.getClazz();
		Class storedType = forName(clazz.getName());
		Map<String, String> params = getParams(typeConfig == null ? null : typeConfig
				.getParameter());

		store.init(storedType, storageDir, params);

		for (Object property : clazz.getIdOrCompositeIdAndProperty()) {
			// Business key
			if (property instanceof IdConfig) {
				IdConfig prop = (IdConfig) property;
				String propertyName = prop.getName();

				UniqueIndex idx = loadOrCreateUniqueIndex(storageDir, propertyName, prop.getIndex());
				store.initIdProperty(storedType, propertyName, idx);
			}
			// Composite Business key
			else if (property instanceof CompositeIdConfig) {
				throw new UnsupportedOperationException("composite-id is not supported yet");
			}
			// Basic property
			else if (property instanceof PropertyConfig) {
				PropertyConfig prop = (PropertyConfig) property;
				store.initProperty(storedType, prop.getName());
			}
			// Extension
			else if (property instanceof ExtensionConfig) {
				ExtensionConfig prop = (ExtensionConfig) property;
				store.initExtension(storedType, prop.getName());
			}
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

	@Override
	public void add(ObjectStorageConfig config) throws ConfigurationException {
		String packageName = config.getPackage();
		if (packageName == null) {
			packageName = "";
		} else {
			packageName += ".";
		}

		for (Store store : config.getStore()) {
			try {
				// Pre-checking config
				ClassConfig clazz = store.getClazz();
				Class.forName(packageName + clazz.getName());

				configuredStores.put(store.getId(), store);
			} catch (ClassNotFoundException e) {
				throw new ConfigurationException(e);
			}
		}
	}

	protected Store getStorageConfigFor(String id) throws ConfigurationException {
		Store store = configuredStores.get(id);

		if (store == null) {
			throw new ConfigurationException("No object storage found for identifier: " + id
					+ " Please check the configuration.");
		}

		return store;
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