package org.ogreg.ase4j;

import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;

import java.rmi.RemoteException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Grouped associative storage service interface.
 *
 * <p>Grouping is useful for accessing multiple association stores of the same schema at the same time.</p>
 *
 * @param   <F>  The type of the association source
 * @param   <T>  The type of the association target
 *
 * @author  Gergely Kiss
 */
public interface GroupedAssociationStore<F, T> {

    /**
     * Adds the association <code>from -> to = value</code> to the storage.
     *
     * <p>This method is the simplest way to add associations, though not very effective. It is useful for in-memory
     * association stores.</p>
     *
     * @param   group
     * @param   from
     * @param   to
     * @param   value
     *
     * @throws  AssociationStoreException  If the store failed to add the association
     */
    void add(Group group, F from, T to, float value) throws AssociationStoreException;

    /**
     * Adds all the associations <code>from -> to = value</code> to the storage.
     *
     * <p>The <code>to</code> part of the association is always the same. This method is useful for building an inverted
     * index (where many associations need to be persisted for the same target at a time).</p>
     *
     * @param   group
     * @param   froms
     * @param   to
     *
     * @throws  AssociationStoreException  If the store failed to add the association
     */
    void addAll(Group group, Collection<Association<F, T>> froms, T to)
        throws AssociationStoreException, RemoteException;

    /**
     * Adds all the associations to the storage.
     *
     * <p>This method is is the most performant when persisting massive amounts of associations.</p>
     *
     * @param   group
     * @param   assocs
     *
     * @throws  AssociationStoreException  If the store failed to add the association
     */
    void addAll(Group group, Collection<Association<F, T>> assocs) throws AssociationStoreException,
        RemoteException;

    /**
     * Returns the associations from the store, using the <code>query</code>.
     *
     * <p>Note: the collection returned by this function does <b>not</b> have to be sorted. It is only guaranteed to
     * contain exactly the associations which conform to the specified query.</p>
     *
     * @param   group
     * @param   query
     *
     * @return
     *
     * @throws  QueryExecutionException  If the store failed to query the associations
     *
     * @see     Query
     */
    List<Association<F, T>> query(Group group, Query query) throws QueryExecutionException;

    /**
     * Returns the metadata describing the current store.
     *
     * <p>Useful for getting various data type and schema information over RMI.</p>
     *
     * @return
     */
    AssociationStoreMetadata getMetadata();

    /**
     * Group access information.
     *
     * <p>Used to specify which association stores need to be accessed and with what multipliers.</p>
     *
     * @author  Gergely Kiss
     */
    public static class Group {

        /** The multipliers used when accessing the stores, keyed by the store ids. */
        private Map<String, Float> multiplier = new HashMap<String, Float>();

        /**
         * Sets the <code>multiplier</code> for <code>storeId</code>.
         *
         * @param  storeId
         * @param  multiplier
         */
        public void set(String storeId, Float multiplier) {
            this.multiplier.put(storeId, multiplier);
        }
    }
}
