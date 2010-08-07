package org.ogreg.ase4j;

import org.ogreg.common.ConfigurationException;

import org.ogreg.ostore.ObjectStore;

import java.io.File;


/**
 * SPI interface for configurable association stores.
 *
 * <p>Every assocation store initialized by the {@link AssociationStoreManager} must implement this interface, however,
 * the different store implementations may ignore the calls, or throw {@link ConfigurationException} if they do not
 * support an operation.</p>
 *
 * @author  Gergely Kiss
 */
public interface ConfigurableAssociationStore<F, T> extends AssociationStore<F, T> {

    /**
     * The implementor may perform store initialization here.
     *
     * @param  from         The object store of the 'from' entities
     * @param  to           The object store of the 'to' entities
     * @param  storageFile  The storage data file (if file-based)
     */
    void init(ObjectStore<F> from, ObjectStore<T> to, File storageFile);
}
