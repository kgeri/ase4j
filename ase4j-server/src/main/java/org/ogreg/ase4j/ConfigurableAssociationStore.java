package org.ogreg.ase4j;

import java.io.File;

import org.ogreg.common.ConfigurationException;
import org.ogreg.ostore.ObjectStore;

/**
 * Common interface for configurable association stores.
 * <p>
 * Every assocation store initialized by the {@link AssociationStoreManager}
 * must implement this interface, however, the different store implementations
 * may ignore the calls, or throw {@link ConfigurationException} if they do not
 * support an operation.
 * </p>
 * 
 * @author Gergely Kiss
 */
public interface ConfigurableAssociationStore<F, T> extends AssociationStore<F, T> {

	void init(ObjectStore<F> from, ObjectStore<T> to, File storageFile);
}
