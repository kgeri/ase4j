package org.ogreg.ase4j;

import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;

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
 * @param <F> The association 'from' type
 * @param <T> The association 'to' type
 * @author Gergely Kiss
 */
// TODO Made public because of Cajo - need to test RMI access
public class GroupedAssociationStoreImpl<F, T> implements AssociationStore<F, T>, Flushable,
		Closeable {

	/** The configured and initialized association stores. */
	private final Map<String, AssociationStore<F, T>> assocStores = new HashMap<String, AssociationStore<F, T>>();

	private final AssociationStoreManager manager;

	/** The identifier of the storage template which is used as a group element. */
	private String groupId;

	/**
	 * The storage directory of the group.
	 * 
	 * @see AssociationStoreManager#getGroupedStore(String)
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

	@Override
	public void add(F from, T to, float value, Params params) throws AssociationStoreException {
		GroupedParams group = GroupedParams.ensureNotNull(params);

		for (Entry<String, Float> e : group.getMultipliers().entrySet()) {
			Float mul = e.getValue();

			ensureStore(e.getKey()).add(from, to, value * mul, null);
		}
	}

	@Override
	public void addAll(Collection<Association<F, T>> froms, T to, Params params)
			throws AssociationStoreException, RemoteException {
		GroupedParams group = GroupedParams.ensureNotNull(params);

		for (Entry<String, Float> e : group.getMultipliers().entrySet()) {
			Float mul = e.getValue();

			// TODO rounding instability?
			applyMultiplier(froms, mul);
			ensureStore(e.getKey()).addAll(froms, to, null);
			applyMultiplier(froms, 1.0F / mul);
		}
	}

	@Override
	public void addAll(Collection<Association<F, T>> assocs, Params params)
			throws AssociationStoreException, RemoteException {
		GroupedParams group = GroupedParams.ensureNotNull(params);

		for (Entry<String, Float> e : group.getMultipliers().entrySet()) {
			float mul = e.getValue();

			// TODO rounding instability?
			applyMultiplier(assocs, mul);
			ensureStore(e.getKey()).addAll(assocs, null);
			applyMultiplier(assocs, 1.0F / mul);
		}
	}

	@Override
	public List<Association<F, T>> query(Query query) throws QueryExecutionException {
		GroupedParams group = GroupedParams.ensureNotNull(query.params());

		List<Association<F, T>> ret = new ArrayList<Association<F, T>>(group.getMultipliers()
				.size() * query.limit());

		for (Entry<String, Float> e : group.getMultipliers().entrySet()) {
			float mul = e.getValue();

			List<Association<F, T>> results = ensureStore(e.getKey()).query(query);
			applyMultiplier(results, mul);

			ret.addAll(results);
		}

		return ret;
	}

	@Override
	public AssociationStoreMetadata getMetadata() {
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

	@Override
	public void flush() throws IOException {

		// TODO Error handling how?
		for (AssociationStore<?, ?> store : assocStores.values()) {

			if (store instanceof Flushable) {
				((Flushable) store).flush();
			}
		}
	}

	@Override
	public void close() throws IOException {

		// TODO Error handling how?
		for (AssociationStore<?, ?> store : assocStores.values()) {

			if (store instanceof Closeable) {
				((Closeable) store).close();
			}
		}

		assocStores.clear();
	}
}
