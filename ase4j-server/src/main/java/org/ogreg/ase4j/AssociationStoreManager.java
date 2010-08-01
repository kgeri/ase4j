package org.ogreg.ase4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.ogreg.ase4j.file.FileAssociationStoreImpl;
import org.ogreg.common.BaseJaxbManager;
import org.ogreg.common.ConfigurationException;
import org.ogreg.config.AssociationStorageConfig.Store;
import org.ogreg.config.Associationstore;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreManager;

/**
 * Association store configurator service.
 * <p>
 * The configuration XML resources must comply to associationstore.xsd
 * </p>
 * 
 * @author Gergely Kiss
 */
public class AssociationStoreManager extends BaseJaxbManager<Associationstore> {
	/** The configurations for the different stores. */
	private final Map<String, Store> configuredStores = new HashMap<String, Store>();

	/** The configured and initialized object stores. */
	private final Map<String, ObjectStore<?>> objectStores = new HashMap<String, ObjectStore<?>>();

	private final ObjectStoreManager objectStoreManager = new ObjectStoreManager();

	/** The storage directory for object and association stores. */
	private File dataDir;

	public AssociationStoreManager() {
		super(Associationstore.class);
	}

	@Override
	public void add(Associationstore config) throws ConfigurationException {
		objectStoreManager.add(config.getObjects());

		for (Store store : config.getAssociations().getStore()) {
			configuredStores.put(store.getId(), store);
		}
	}

	/**
	 * Creates and opens a new association store based on this configuration.
	 * <p>
	 * The object storage and other files will be created and opened.
	 * </p>
	 * 
	 * @param id The id of the storage
	 * @return A newly initialized {@link AssociationStore}
	 * @throws ConfigurationException on storage init error
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public AssociationStore createStore(String id) {
		// Checking for config
		Store cfg = getStorageConfigFor(id);

		// Creating the store
		// TODO support more store types
		ConfigurableAssociationStore store = new FileAssociationStoreImpl();

		// Initializing the store
		ObjectStore from = getOrCreateStore(cfg.getFromStore());
		ObjectStore to = getOrCreateStore(cfg.getToStore());
		File storageFile = getAssociatonStoreFile(dataDir, id);

		store.init(from, to, storageFile);

		return store;
	}

	/**
	 * Returns all the configured association store ids.
	 * 
	 * @return
	 */
	public Set<String> getConfiguredStores() {
		return configuredStores.keySet();
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

	@SuppressWarnings("rawtypes")
	private ObjectStore getOrCreateStore(String id) {
		ObjectStore store = objectStores.get(id);

		if (store == null) {
			store = objectStoreManager.createStore(id, getObjectStoreFile(dataDir, id));
			objectStores.put(id, store);
		}

		return store;
	}

	private Store getStorageConfigFor(String id) throws ConfigurationException {
		Store store = configuredStores.get(id);

		if (store == null) {
			throw new ConfigurationException("No association storage found for identifier: " + id
					+ " Please check the configuration.");
		}

		return store;
	}

	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	public static File getObjectStoreFile(File storageDir, String id) {
		File dir = new File(storageDir, id);

		if (!dir.exists() && !dir.mkdirs()) {
			throw new ConfigurationException("Failed to create storage dir: " + dir);
		}

		return dir;
	}

	public static File getAssociatonStoreFile(File storageDir, String id) {
		return new File(storageDir, id);
	}
}
