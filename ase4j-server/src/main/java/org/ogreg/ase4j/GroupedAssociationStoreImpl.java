package org.ogreg.ase4j;

import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * A grouped association store implementation.
 *
 * @param   <F>  The association 'from' type
 * @param   <T>  The association 'to' type
 *
 * @author  Gergely Kiss
 */
class GroupedAssociationStoreImpl<F, T> implements GroupedAssociationStore<F, T> {

    /** The configured and initialized association stores. */
    private final Map<String, AssociationStore<F, T>> assocStores =
        new HashMap<String, AssociationStore<F, T>>();

    private final AssociationStoreManager manager;

    /** The identifier of the storage template which is used as a group element. */
    private String groupId;

    /**
     * The storage directory of the group.
     *
     * @see  AssociationStoreManager#getGroupedStore(String)
     */
    private File groupStorageDir;

    /** Storage metadata. */
    private AssociationStoreMetadata metadata;

    GroupedAssociationStoreImpl(AssociationStoreManager manager) {
        this.manager = manager;
    }

    public void init(String groupId, File groupStorageDir) {
        this.groupId = groupId;
        this.groupStorageDir = groupStorageDir;
    }

    @Override public void add(Group group, F from, T to, float value)
        throws AssociationStoreException {

        for (Entry<String, Float> e : group.getMultipliers().entrySet()) {
            Float mul = e.getValue();

            if (mul == null) {
                continue;
            }

            ensureStore(e.getKey()).add(from, to, value * mul);
        }
    }

    @Override public void addAll(Group group, Collection<Association<F, T>> froms, T to)
        throws AssociationStoreException, RemoteException {

        for (Entry<String, Float> e : group.getMultipliers().entrySet()) {
            Float mul = e.getValue();

            if (mul == null) {
                continue;
            }

            // TODO rounding instability?
            applyMultiplier(froms, e.getValue());
            ensureStore(e.getKey()).addAll(froms, to);
            applyMultiplier(froms, 1.0F / e.getValue());
        }
    }

    @Override public void addAll(Group group, Collection<Association<F, T>> assocs)
        throws AssociationStoreException, RemoteException {

        for (Entry<String, Float> e : group.getMultipliers().entrySet()) {
            Float mul = e.getValue();

            if (mul == null) {
                continue;
            }

            // TODO rounding instability?
            applyMultiplier(assocs, e.getValue());
            ensureStore(e.getKey()).addAll(assocs);
            applyMultiplier(assocs, 1.0F / e.getValue());
        }
    }

    @Override public List<Association<F, T>> query(Group group, Query query)
        throws QueryExecutionException {

        List<Association<F, T>> ret = new ArrayList<Association<F, T>>(group.getMultipliers()
                .size() * query.limit());

        for (Entry<String, Float> e : group.getMultipliers().entrySet()) {
            Float mul = e.getValue();

            if (mul == null) {
                continue;
            }

            List<Association<F, T>> results = ensureStore(e.getKey()).query(query);
            ret.addAll(results);
        }

        return ret;
    }

    @Override public AssociationStoreMetadata getMetadata() {
        return metadata;
    }

    private void applyMultiplier(Collection<Association<F, T>> original, float value) {

        for (Association<F, T> assoc : original) {
            assoc.value *= value;
        }
    }

    @SuppressWarnings("unchecked")
    private AssociationStore<F, T> ensureStore(String id) {
        AssociationStore<F, T> store = assocStores.get(id);

        if (store != null) {
            return store;
        }

        store = manager.createStore(groupId,
                AssociationStoreManager.getAssociatonStoreFile(groupStorageDir, id));

        assocStores.put(id, store);

        return store;
    }

    public void setMetadata(AssociationStoreMetadata metadata) {
        this.metadata = metadata;
    }
}