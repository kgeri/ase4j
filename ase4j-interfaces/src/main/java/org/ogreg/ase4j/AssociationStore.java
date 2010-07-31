package org.ogreg.ase4j;

import java.util.Collection;
import java.util.List;

import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;

/**
 * Associative storage service interface.
 * 
 * @param <F> The type of the association source
 * @param <T> The type of the association target
 * @author Gergely Kiss
 */
public interface AssociationStore<F, T> {

	/**
	 * Adds the association <code>from -> to = value</code> to the storage.
	 * 
	 * @param from
	 * @param to
	 * @param value
	 * @throws AssociationStoreException If the store failed to add the
	 *             association
	 */
	void add(F from, T to, float value) throws AssociationStoreException;

	/**
	 * Adds all the associations <code>from -> to = value</code> to the storage.
	 * <p>
	 * The <code>to</code> part of the association is always the same. This
	 * method is for simplifying the task of building an inverted index.
	 * </p>
	 * 
	 * @param froms
	 * @param to
	 * @throws AssociationStoreException If the store failed to add the
	 *             association
	 */
	void addAll(Collection<Association<F, T>> froms, T to) throws AssociationStoreException;

	/**
	 * Adds all the associations to the storage.
	 * 
	 * @param assocs
	 * @throws AssociationStoreException If the store failed to add the
	 *             association
	 */
	void addAll(Collection<Association<F, T>> assocs) throws AssociationStoreException;

	/**
	 * Returns the associations from the store, using the <code>query</code>.
	 * <p>
	 * Note: the collection returned by this function does <b>not</b> have to be
	 * sorted. It is only guaranteed to contain exactly the associations which
	 * conform to the specified query.
	 * </p>
	 * 
	 * @param query
	 * @return
	 * @throws QueryExecutionException If the store failed to query the
	 *             associations
	 * @see Query
	 */
	List<Association<F, T>> query(Query query) throws QueryExecutionException;
}
