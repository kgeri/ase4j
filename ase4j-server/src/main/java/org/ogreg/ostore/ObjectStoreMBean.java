package org.ogreg.ostore;

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
}
