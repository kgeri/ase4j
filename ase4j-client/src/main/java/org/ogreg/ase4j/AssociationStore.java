package org.ogreg.ase4j;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;

/**
 * Associative storage service interface.
 * <p>
 * Please note that the {@link Params} parameter in this interface may be
 * required to be a subclass of {@link Params} (such {@link GroupedParams}). For
 * detailed documentation, please see the scpecific {@link AssociationStore}
 * implementations.
 * </p>
 * 
 * @param <F> The type of the association source
 * @param <T> The type of the association target
 * @author Gergely Kiss
 */
public interface AssociationStore<F, T> {

	/**
	 * Adds the association <code>from -> to = value</code> to the storage.
	 * <p>
	 * This method is the simplest way to add associations, though not very
	 * effective. It is useful for in-memory association stores.
	 * </p>
	 * 
	 * @param from
	 * @param to
	 * @param value
	 * @param params The association parameters to use for the addition, or null
	 *            if the defaults should be used
	 * @throws AssociationStoreException If the store failed to add the
	 *             association
	 */
	void add(F from, T to, float value, Params params) throws AssociationStoreException;

	/**
	 * Adds all the associations <code>from -> to = value</code> to the storage.
	 * <p>
	 * The <code>to</code> part of the association is always the same. This
	 * method is useful for building an inverted index (where many associations
	 * need to be persisted for the same target at a time).
	 * </p>
	 * 
	 * @param froms
	 * @param to
	 * @param params The association parameters to use for the addition, or null
	 *            if the defaults should be used
	 * @throws AssociationStoreException If the store failed to add the
	 *             association
	 */
	void addAll(Collection<Association<F, T>> froms, T to, Params params)
			throws AssociationStoreException, RemoteException;

	/**
	 * Adds all the associations to the storage.
	 * <p>
	 * This method is is the most performant when persisting massive amounts of
	 * associations.
	 * </p>
	 * 
	 * @param assocs
	 * @param params The association parameters to use for the addition, or null
	 *            if the defaults should be used
	 * @throws AssociationStoreException If the store failed to add the
	 *             association
	 */
	void addAll(Collection<Association<F, T>> assocs, Params params)
			throws AssociationStoreException, RemoteException;

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

	/**
	 * Returns the metadata describing the current store.
	 * <p>
	 * Useful for getting various data type and schema information over RMI.
	 * </p>
	 * 
	 * @return
	 */
	AssociationStoreMetadata getMetadata();

	/**
	 * Provides different computation mechanisms when two or more associations
	 * are added.
	 * 
	 * @author Gergely Kiss
	 */
	public enum Operation {
		/**
		 * Average (default).
		 * <p>
		 * When an association already exists, the value will be the average of
		 * the old value and the new value:
		 * </p>
		 * <code>
		 * v = ( v<sub>o</sub> + v<sub>n</sub> ) / 2
		 * </code>
		 */
		AVG {
			@Override
			public float calculate(float oldValue, float newValue) {
				return (oldValue + newValue) / 2;
			}
		},

		/**
		 * Addition.
		 * <p>
		 * When an association already exists, the value will be the sum of the
		 * old value and the new value:
		 * </p>
		 * <code>
		 * v = v<sub>o</sub> + v<sub>n</sub>
		 * </code>
		 */
		SUM {
			@Override
			public float calculate(float oldValue, float newValue) {
				return oldValue + newValue;
			}
		},

		/**
		 * Logarithmic addition.
		 * <p>
		 * When an association already exists, the value be calculated as
		 * follows:
		 * </p>
		 * <code>
		 * v = ln( e<sup>v<sub>o</sub></sup> + v<sub>n</sub> - v<sub>o</sub>)
		 * </code>
		 */
		LOGSUM {
			@Override
			public float calculate(float oldValue, float newValue) {
				return (float) Math.log(Math.exp(oldValue) + newValue - oldValue);
			}
		},

		/**
		 * Overwrite.
		 * <p>
		 * When an association already exists, the value will overwritten by the
		 * new value:
		 * </p>
		 * <code>
		 * v = v<sub>n</sub>
		 * </code>
		 */
		OVERWRITE {
			@Override
			public float calculate(float oldValue, float newValue) {
				return newValue;
			}
		};

		public float calculate(float oldValue, float newValue) {
			throw new AbstractMethodError();
		}
	}
}
