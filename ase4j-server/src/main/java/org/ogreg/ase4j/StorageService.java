package org.ogreg.ase4j;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
	private final Map<String, AssociationStore<?, ?>> associationStores = new HashMap<String, AssociationStore<?, ?>>();

	private boolean running = false;

	public void start() throws Exception {
		running = true;

		log.info("Starting up storage...");
		// TODO PerformanceTimer - CJC maybe?
		long before = System.currentTimeMillis();

		// Reading configuration
		String dataPath = System.getProperty("dataDir", "data");
		String schemaPath = System.getProperty("schema", "schema.xml");
		int rmiPort = Integer.getInteger("rmiPort", 1198);

		File dataDir = new File(dataPath);
		File schemaFile = new File(schemaPath);

		// TODO check permissions?
		log.debug("Using data dir: {}", dataDir);

		// Initializing Cajo
		Remote.config(null, rmiPort, null, 0);

		// Initializing storage
		// TODO Load multiple configs?
		storeManager = new AssociationStoreManager();
		storeManager.add(schemaFile);
		storeManager.setDataDir(dataDir);

		// Publishing association storage
		log.debug("Configuring association storage");
		int assocStores = 0;

		for (String id : storeManager.getConfiguredStores()) {
			String serviceLoc = "assocs/" + id;

			try {
				@SuppressWarnings("rawtypes")
				AssociationStore store = storeManager.createStore(id);
				ItemServer.bind(store, serviceLoc);
				associationStores.put(id, store);
				assocStores++;
				log.debug("Successfully initialized assoc store at: {}", serviceLoc);
			} catch (Exception e) {
				log.error("Failed to initialize assoc store: {} ({})", id, e.getLocalizedMessage());
				log.debug("Failure trace", e);
			}
		}

		log.info("Initialized {}/{} assoc stores successfully", assocStores, storeManager
				.getConfiguredStores().size());

		// Publishing object storage
		log.debug("Configuring object storage");
		int objectStores = 0;

		for (Entry<String, ObjectStore<?>> en : storeManager.getObjectStores().entrySet()) {
			String id = en.getKey();
			String serviceLoc = "objects/" + id;

			try {
				ItemServer.bind(en.getValue(), serviceLoc);
				objectStores++;
				log.debug("Successfully initialized object store at: {}", serviceLoc);
			} catch (Exception e) {
				log.error("Failed to initialize object store: {} ({})", id, e.getLocalizedMessage());
				log.debug("Failure trace", e);
			}
		}

		// TODO PerformanceTimer
		long time = System.currentTimeMillis() - before;
		log.info("Initialized {}/{} object stores successfully", objectStores, storeManager
				.getObjectStores().size());

		log.info("Startup completed in {} ms, waiting for requests", time);

		// Adding shutdown hook (Ctrl+C)
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdown();
			}
		});

		// Starting console
		Thread console = new Thread("Console") {
			@Override
			public void run() {
				try {
					System.out.println("Enter 'q' or Ctrl+C to quit");
					while (running) {
						int b = System.in.read();
						if (b < 0 || b == 'q') {
							shutdown();
						}
					}
				} catch (IOException e) {
					log.error("Failed to read from System.in");
				}
			}
		};
		console.setDaemon(true);
		console.start();

		while (running) {
			Thread.sleep(100);
		}
	}

	public synchronized void shutdown() {
		if (!running) {
			return;
		}

		// TODO PerformanceTimer
		long before = System.currentTimeMillis();

		log.debug("Shutting down assoc storage");
		for (Entry<String, AssociationStore<?, ?>> e : associationStores.entrySet()) {
			String id = e.getKey();
			AssociationStore<?, ?> store = e.getValue();

			shutdownStore(id, store);
		}

		log.debug("Shutting down object storage");
		for (Entry<String, ObjectStore<?>> e : storeManager.getObjectStores().entrySet()) {
			String id = e.getKey();
			ObjectStore<?> store = e.getValue();

			shutdownStore(id, store);
		}

		// TODO PerformanceTimer
		long time = System.currentTimeMillis() - before;
		log.info("Shutdown completed in {} ms. Bye!", time);

		// Shutting down RMI
		Remote.shutdown();

		running = false;
	}

	void shutdownStore(String id, Object store) {
		try {
			if (store instanceof Flushable) {
				((Flushable) store).flush();
			}
			log.debug("Successfully flushed storage: {}", id);
		} catch (Exception e) {
			log.error("Failed to flush: {} ({}), trying to close", id);
			log.debug("Failure trace", e);
		}

		try {
			if (store instanceof Closeable) {
				((Closeable) store).close();
			}
			log.debug("Successfully closed storage: {}", id);
		} catch (Exception e) {
			log.error("Failed to close: {} ({})", id);
			log.debug("Failure trace", e);
		}
	}

	public static void main(String[] args) {
		try {
			new StorageService().start();
		} catch (Throwable e) {
			log.error("Unexpected storage failure", e);
		}
	}
}
