package org.ogreg.ase4j;

import org.ogreg.ase4j.file.FileAssociationStoreImpl;

import org.ogreg.common.BaseJaxbManager;
import org.ogreg.common.ConfigurationException;

import org.ogreg.config.AssociationStorageConfig.Store;
import org.ogreg.config.Associationstore;

import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreManager;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;


/**
 * Association store configurator service.
 *
 * <p>The configuration XML resources must comply to associationstore.xsd</p>
 *
 * @author  Gergely Kiss
 */
public class AssociationStoreManager extends BaseJaxbManager<Associationstore> {

    /** The configurations for the different stores. */
    private final Map<String, Store> configuredStores = new HashMap<String, Store>();

    /** The configured and initialized association stores. */
    private final Map<String, AssociationStore<?, ?>> assocStores =
        new HashMap<String, AssociationStore<?, ?>>();

    /** The object store manager. */
    private final ObjectStoreManager objectStoreManager = new ObjectStoreManager();

    /** The storage directory for object and association stores. */
    private File dataDir;

    public AssociationStoreManager() {
        super(Associationstore.class);
    }

    @Override public void add(Associationstore config) throws ConfigurationException {
        objectStoreManager.add(config.getObjects());

        for (Store store : config.getAssociations().getStore()) {
            configuredStores.put(store.getId(), store);
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

        ConfigurableAssociationStore cstore = createStore(id);
        assocStores.put(id, cstore);

        return cstore;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    ConfigurableAssociationStore createStore(String id) {

        // Checking for config
        Store cfg = getStorageConfigFor(id);

        // Creating the store
        // TODO support more store types
        ConfigurableAssociationStore store = new FileAssociationStoreImpl();

        // Initializing the store
        String frId = cfg.getFromStore();
        String toId = cfg.getToStore();

        ObjectStore from = objectStoreManager.getStore(frId, getObjectStoreFile(dataDir, frId));
        ObjectStore to = objectStoreManager.getStore(toId, getObjectStoreFile(dataDir, toId));
        File storageFile = getAssociatonStoreFile(dataDir, id);

        store.init(from, to, storageFile);

        return store;
    }

    /**
     * Flushes the specified store.
     *
     * @param   id
     *
     * @throws  IOException  if the manager has failed to flush the store
     */
    public synchronized void flushStore(String id) throws IOException {
        AssociationStore<?, ?> store = assocStores.get(id);

        if (store instanceof Flushable) {
            ((Flushable) store).flush();
        }

        assocStores.remove(id);
    }

    /**
     * Closes the specified store and removes it from the store cache.
     *
     * @param   id
     *
     * @throws  IOException  if the manager has failed to close the store
     */
    public synchronized void closeStore(String id) throws IOException {
        AssociationStore<?, ?> store = assocStores.get(id);

        if (store instanceof Closeable) {
            ((Closeable) store).close();
        }

        assocStores.remove(id);
    }

    public ObjectStoreManager getObjectManager() {
        return objectStoreManager;
    }

    /**
     * Returns all the configured association store ids.
     *
     * @return
     */
    public Map<String, AssociationStore<?, ?>> getConfiguredStores() {
        return assocStores;
    }

    private Store getStorageConfigFor(String id) throws ConfigurationException {
        Store store = configuredStores.get(id);

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
}
