package org.ogreg.ase4j;

import org.ogreg.ostore.ObjectStore;

import gnu.cajo.utils.extra.TransparentItemProxy;

/**
 * Client side association database connector.
 * 
 * @author Gergely Kiss
 */
public abstract class StorageClient {

	/**
	 * Retrieves a proxy for the association store <code>id</code> which should
	 * return associations between objects of type <code>from</code> and
	 * <code>to</code>.
	 * 
	 * @param url The url of the association store (eg.:
	 *            //localhost:1198/assocs/myAssocStore)
	 * @param from The from object type
	 * @param to The to object type
	 * @return
	 * @throws AssociationStoreException on storage connection error
	 */
	@SuppressWarnings("unchecked")
	public static <F, T> AssociationStore<F, T> lookup(String url, Class<F> from, Class<T> to)
			throws AssociationStoreException {
		try {
			return (AssociationStore<F, T>) TransparentItemProxy.getItem(url,
					new Class[] { AssociationStore.class });
		} catch (Exception e) {
			throw new AssociationStoreException(e);
		}
	}

	/**
	 * Retrieves a proxy for the object store <code>id</code> which should
	 * return associations between objects of type <code>from</code> and
	 * <code>to</code>.
	 * 
	 * @param url The url of the association store (eg.:
	 *            //localhost:1198/objects/myObjectStore)
	 * @param from The from object type
	 * @param to The to object type
	 * @return
	 * @throws AssociationStoreException on storage connection error
	 */
	@SuppressWarnings("unchecked")
	public static <T> ObjectStore<T> lookup(String url, Class<T> type)
			throws AssociationStoreException {
		try {
			return (ObjectStore<T>) TransparentItemProxy.getItem(url,
					new Class[] { ObjectStore.class });
		} catch (Exception e) {
			throw new AssociationStoreException(e);
		}
	}
}
