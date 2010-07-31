package org.ogreg.ostore;

import java.io.File;
import java.util.Map;

import org.ogreg.common.ConfigurationException;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.index.UniqueIndex;

/**
 * Common interface for configurable object stores.
 * <p>
 * Every object store initialized by the {@link ObjectStoreManager} must
 * implement this interface, however, the different store implementations may
 * ignore the calls, or throw {@link ConfigurationException} if they do not
 * support an operation.
 * </p>
 * 
 * @author Gergely Kiss
 */
public interface ConfigurableObjectStore<T> extends ObjectStore<T> {

	void init(Class<T> type, File storageDir, Map<String, String> params);

	void initProperty(Class<?> type, String propertyName);

	void initIdProperty(Class<?> type, String propertyName, UniqueIndex index);

	void initExtension(Class<?> type, String propertyName);
}
