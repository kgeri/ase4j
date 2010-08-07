package org.ogreg.ase4j;

import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;

import java.rmi.RemoteException;

import java.util.Collection;
import java.util.List;


/**
 * A grouped association store implementation.
 *
 * @param   <F>  The association 'from' type
 * @param   <T>  The association 'to' type
 *
 * @author  Gergely Kiss
 */
class GroupedAssociationStoreImpl<F, T> implements GroupedAssociationStore<F, T> {
    private final AssociationStoreManager manager;

    GroupedAssociationStoreImpl(AssociationStoreManager manager) {
        this.manager = manager;
    }

    @Override public void add(Group group, F from, T to, float value)
        throws AssociationStoreException {
        // TODO Auto-generated method stub

    }

    @Override public void addAll(Group group, Collection<Association<F, T>> froms, T to)
        throws AssociationStoreException, RemoteException {
        // TODO Auto-generated method stub

    }

    @Override public void addAll(Group group, Collection<Association<F, T>> assocs)
        throws AssociationStoreException, RemoteException {
        // TODO Auto-generated method stub

    }

    @Override public List<Association<F, T>> query(Group group, Query query)
        throws QueryExecutionException {

        // TODO Auto-generated method stub
        return null;
    }

    @Override public AssociationStoreMetadata getMetadata() {

        // TODO Auto-generated method stub
        return null;
    }
}
