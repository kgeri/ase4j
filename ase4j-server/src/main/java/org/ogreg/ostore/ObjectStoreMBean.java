package org.ogreg.ostore;

import java.io.IOException;

/**
 * JMX interface for object stores.
 * 
 * @author Gergely Kiss
 */
public interface ObjectStoreMBean {

	/**
	 * Returns the number of currently stored objects (estimated).
	 * 
	 * @return
	 */
	long getObjectCount();

	/**
	 * Flushes the object store.
	 */
	void flush() throws IOException;
}
