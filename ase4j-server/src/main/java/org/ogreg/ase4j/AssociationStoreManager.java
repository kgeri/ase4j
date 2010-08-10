package org.ogreg.ase4j;

import org.ogreg.ase4j.file.FileAssociationStoreImpl;

import org.ogreg.common.BaseJaxbManager;
import org.ogreg.common.ConfigurationException;
import org.ogreg.common.utils.FileUtils;

import org.ogreg.config.AssociationStorageConfig.Group;
import org.ogreg.config.Associationstore;
import org.ogreg.config.StoreConfig;

import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreManager;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Association store configurator service.
 *
 * <p>The configuration XML resources must comply to associationstore.xsd</p>
 *
 * @author  Gergely Kiss
 */
public class AssociationStoreManager extends BaseJaxbManager<Associationstore> {

    /** The configurations for the different stores. */
    private final Map<String, StoreConfig> configuredStores = new HashMap<String, StoreConfig>();

    /** The configured and initialized association stores. */
    private final Map<String, AssociationStore<?, ?>> assocStores =
        new HashMap<String, AssociationStore<?, ?>>();

    /** The configured grouped association stores. */
    private final Map<String, GroupedAssociationStore<?, ?>> groupedStores =
        new HashMap<String, GroupedAssociationStore<?, ?>>();

    /** The object store manager. */
    private final ObjectStoreManager objectStoreManager = new ObjectStoreManager();

    /** The storage directory for object and association stores. */
    private File dataDir;

    public AssociationStoreManager() {
        super(Associationstore.class);
    }

    @Override public void add(Associationstore config) throws ConfigurationException {
        objectStoreManager.add(config.getObjects());

        for (StoreConfig store : config.getAssociations().getStore()) {
            configuredStores.put(store.getId(), store);
        }

        for (StoreConfig store : config.getAssociations().getGroup()) {
            configuredStores.put(store.getId(), store);
        }
    }

    /**
     * Configures and initializes all of the association storage.
     *
     * @throws  ConfigurationException  on storage init error
     *
     * @see     #getConfiguredStores() and {@link #getConfiguredGroupedStores()}
     */
    public void configureAll() {

        for (Entry<String, StoreConfig> e : configuredStores.entrySet()) {

            if (e.getValue() instanceof Group) {
                getGroupedStore(e.getKey());
            } else {
                getStore(e.getKey());
            }
        }
    }

    /**
     * Creates and opens a new association store based on this configuration, or returns an already initialized assoc
     * store instance.
     *
     * <p>The object storage and other files will be created and opened.</p>
     *
     * @param   id  The id of the storage
     *
     * @return  A newly initialized {@link AssociationStore}
     *
     * @throws  ConfigurationException  on storage init error
     */
    @SuppressWarnings("rawtypes")
    public AssociationStore getStore(String id) {
        AssociationStore<?, ?> store = assocStores.get(id);

        if (store != null) {
            return store;
        }

        ConfigurableAssociationStore cstore = createStore(id, getAssociatonStoreFile(dataDir, id));

        assocStores.put(id, cstore);

        return cstore;
    }

    /**
     * Creates and opens a new association store group based on this configuration, or returns an already initialized
     * assoc store group instance.
     *
     * <p>The object storage and other files will be created and opened.</p>
     *
     * @param   id  The id of the group
     *
     * @return  A newly initialized {@link GroupedAssociationStore}
     *
     * @throws  ConfigurationException  on storage init error
     */
    @SuppressWarnings("rawtypes")
    public GroupedAssociationStore getGroupedStore(String id) {
        GroupedAssociationStore<?, ?> store = groupedStores.get(id);

        if (store != null) {
            return store;
        }

        // Checking for config
        StoreConfig cfg = getStorageConfigFor(id);

        // Creating the grouped store
        GroupedAssociationStoreImpl gstore = new GroupedAssociationStoreImpl(this);

        // Initializing the store
        File groupDir = getGroupedStoreFile(dataDir, id);

        try {
            FileUtils.mkdirs(groupDir);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }

        gstore.init(id, groupDir);

        // Setting metadata
        ObjectStore from = getObjectStore(cfg.getFromStore());
        ObjectStore to = getObjectStore(cfg.getToStore());
        gstore.setMetadata(new AssociationStoreMetadata(from.getMetadata(), to.getMetadata()));

        groupedStores.put(id, gstore);

        return gstore;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    ConfigurableAssociationStore createStore(String id, File storageFile) {

        // Checking for config
        StoreConfig cfg = getStorageConfigFor(id);

        // Creating the store
        // TODO support more store types
        ConfigurableAssociationStore cstore = new FileAssociationStoreImpl();

        // Initializing the store
        ObjectStore from = getObjectStore(cfg.getFromStore());
        ObjectStore to = getObjectStore(cfg.getToStore());

        cstore.init(from, to, storageFile);

        // Setting metadata
        cstore.setMetadata(new AssociationStoreMetadata(from.getMetadata(), to.getMetadata()));

        return cstore;
    }

    @SuppressWarnings("rawtypes")
    private ObjectStore getObjectStore(String id) {
        return objectStoreManager.getStore(id, getObjectStoreFile(dataDir, id));
    }

    /**
     * Flushes the specified store.
     *
     * @param   id
     *
     * @throws  IOException  if the manager has failed to flush the store
     */
    public synchronized void flushStore(String id) throws IOException {
        Object store = assocStores.get(id);
        store = (store == null) ? groupedStores.get(id) : store;

        if (store instanceof Flushable) {
            ((Flushable) store).flush();
        }
    }

    /**
     * Closes the specified store and removes it from the store cache.
     *
     * @param   id
     *
     * @throws  IOException  if the manager has failed to close the store
     */
    public synchronized void closeStore(String id) throws IOException {
        Object store = assocStores.get(id);
        store = (store == null) ? groupedStores.get(id) : store;

        if (store instanceof Closeable) {
            ((Closeable) store).close();
        }

        assocStores.remove(id);
        groupedStores.remove(id);
    }

    public ObjectStoreManager getObjectManager() {
        return objectStoreManager;
    }

    /**
     * Returns all the configured association stores.
     *
     * @return
     */
    public Map<String, AssociationStore<?, ?>> getConfiguredStores() {
        return assocStores;
    }

    /**
     * Returns all the configured grouped association stores.
     *
     * @return
     */
    public Map<String, GroupedAssociationStore<?, ?>> getConfiguredGroupedStores() {
        return groupedStores;
    }

    /**
     * Returns the storage configuration for the specified storage <code>id</code>.
     *
     * @param   id
     *
     * @return
     *
     * @throws  ConfigurationException  if the <code>id</code> is not configured
     */
    private StoreConfig getStorageConfigFor(String id) throws ConfigurationException {
        StoreConfig store = configuredStores.get(id);

        if (store == null) {
            throw new ConfigurationException("No association storage found for identifier: " + id +
                " Please check the configuration.");
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

    public static File getGroupedStoreFile(File storageDir, String id) {
        return new File(storageDir, "grp-" + id);
    }
}
