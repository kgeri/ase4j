package org.ogreg.ase4j;

import java.io.IOException;

/**
 * JMX interface for association stores.
 * 
 * @author Gergely Kiss
 */
public interface AssociationStoreMBean {

	/**
	 * Returns the number of currently stored, compressed association blocks.
	 * 
	 * @return
	 */
	long getBlockCount();

	/**
	 * Returns the number of currently cached, compressed association blocks, or
	 * -1 if there is no cache.
	 * 
	 * @return
	 */
	long getCachedBlockCount();

	/**
	 * Returns the number of currently stored associations.
	 * 
	 * @return
	 */
	long getAssociationCount();

	/**
	 * Returns the number of cached associations, or -1 if there is no cache.
	 * 
	 * @return
	 */
	long getCachedAssociationCount();

	/**
	 * Returns the association block usage (estimated).
	 * <p>
	 * Block usage shows that how many percent of the AssociationBlock's bytes
	 * are used really for association storage - it does not take into account
	 * the overhead of the data structure, so it purely shows how many bytes are
	 * preallocated but not used.
	 * </p>
	 * 
	 * @return
	 */
	double getBlockUsage();

	/**
	 * Flushes the association store.
	 */
	void flush() throws IOException;
}
