package org.ogreg.ase4j;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.File;
import java.util.Map.Entry;

import org.ogreg.ostore.ObjectStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Association storage service entry point.
 * 
 * @author Gergely Kiss
 */
public class StorageService {
	private static final Logger log = LoggerFactory.getLogger(StorageService.class);

	private AssociationStoreManager storeManager;

	public void start() throws Exception {
		log.info("Starting storage...");

		// Reading configuration
		String dataPath = System.getProperty("dataDir", "data");
		String schemaPath = System.getProperty("schema", "schema.xml");
		int rmiPort = Integer.getInteger("rmiPort", 1198);

		File dataDir = new File(dataPath);
		File schemaFile = new File(schemaPath);

		// Initializing Cajo
		Remote.config(null, rmiPort, null, 0);

		// Initializing storage manager
		// TODO Load multiple configs
		storeManager = new AssociationStoreManager();
		storeManager.add(schemaFile);
		storeManager.setDataDir(dataDir);

		// Publishing association storage
		log.info("Configuring association storage");
		for (String id : storeManager.getConfiguredStores()) {
			publish(id);
		}

		// Publishing object storage
		log.info("Configuring object storage");
		for (Entry<String, ObjectStore<?>> e : storeManager.getObjectStores().entrySet()) {
			publish(e.getKey(), e.getValue());
		}

		log.info("Storage startup complete, waiting for requests");
	}

	private void publish(String assocStoreId) {
		try {
			log.debug("Configuring assoc store: {}", assocStoreId);
			@SuppressWarnings("rawtypes")
			AssociationStore store = storeManager.createStore(assocStoreId);
			ItemServer.bind(store, "assocs/" + assocStoreId);
		} catch (Exception e) {
			log.error("Failed to configure assoc store: {} ({})", assocStoreId,
					e.getLocalizedMessage());
			log.debug("Failure trace", e);
		}
	}

	private void publish(String objectStoreId, ObjectStore<?> store) {
		try {
			log.debug("Configuring object store: {}", objectStoreId);
			ItemServer.bind(store, "objects/" + objectStoreId);
		} catch (Exception e) {
			log.error("Failed to configure object store: {} ({})", objectStoreId,
					e.getLocalizedMessage());
			log.debug("Failure trace", e);
		}
	}

	public static void main(String[] args) throws Exception {
		new StorageService().start();
	}
}
