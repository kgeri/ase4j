package org.ogreg.ase4j.file;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.ogreg.ase4j.Association;
import org.ogreg.ase4j.AssociationStore;
import org.ogreg.ase4j.AssociationStoreException;
import org.ogreg.ase4j.criteria.Query;
import org.ogreg.ase4j.criteria.QueryExecutionException;
import org.ogreg.ostore.ObjectStore;
import org.ogreg.ostore.ObjectStoreException;

/**
 * A file-based implementation of the association store.
 * 
 * @author Gergely Kiss
 */
public class FileAssociationStoreImpl<T> implements AssociationStore<String, T>, Closeable, Flushable {
	public static final float VALUE_MUL = 1000;

	/** The index of the from entities. */
	private ObjectStore<String> fromStore;

	/** The object store of the to entities. */
	private ObjectStore<T> toStore;

	/** The file used to store the associations. */
	private File storageFile;

	private CachedBlockStore assocs = new CachedBlockStore();
	private AssociationSolver solver = new AssociationSolver(this);

	/**
	 * Initializes the storage using the given parameters.
	 * 
	 * @throws IOException
	 */
	@PostConstruct
	public void init() {

		try {

			if (fromStore == null) {
				throw new AssertionError("The field fromIndex must be set");
			} else if (toStore == null) {
				throw new AssertionError("The field toStore must be set");
			}

			close();

			assocs.open(storageFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void add(String from, T to, float value) throws AssociationStoreException {
		try {
			Long fi = fromStore.save(from);
			Long ti = toStore.save(to);

			AssociationBlock a = new AssociationBlock(fi.intValue());
			a.merge(ti.intValue(), (int) (value * VALUE_MUL));

			assocs.merge(a);
		} catch (IOException e) {
			throw new AssociationStoreException(e);
		} catch (ObjectStoreException e) {
			throw new AssociationStoreException(e);
		}
	}

	@Override
	public void addAll(Collection<Association<String, T>> froms, T to)
			throws AssociationStoreException {
		try {
			Long ti = toStore.save(to);

			for (Association<String, ?> assoc : froms) {
				String from = assoc.from;
				Long fi = fromStore.save(from);

				int v = (int) (assoc.value * VALUE_MUL);

				AssociationBlock a = new AssociationBlock(fi.intValue());
				a.merge(ti.intValue(), v);

				this.assocs.merge(a);
			}
		} catch (IOException e) {
			throw new AssociationStoreException(e);
		} catch (ObjectStoreException e) {
			throw new AssociationStoreException(e);
		}
	}

	@Override
	public void addAll(Collection<Association<String, T>> assocs) throws AssociationStoreException {
		Map<String, List<Association<String, T>>> byFrom = new HashMap<String, List<Association<String, T>>>(
				assocs.size() / 2);

		for (Association<String, T> a : assocs) {
			List<Association<String, T>> l = byFrom.get(a.from);

			if (l == null) {
				l = new LinkedList<Association<String, T>>();
				byFrom.put(a.from, l);
			}

			l.add(a);
		}

		try {

			for (Entry<String, List<Association<String, T>>> e : byFrom.entrySet()) {
				String from = e.getKey();
				Long fi = fromStore.save(from);

				AssociationBlock a = new AssociationBlock(fi.intValue());

				for (Association<String, T> assoc : e.getValue()) {
					int v = (int) (assoc.value * VALUE_MUL);
					Long ti = toStore.save(assoc.to);

					// TODO This could be more effective
					a.merge(ti.intValue(), v);
				}

				this.assocs.merge(a);
			}
		} catch (IOException e) {
			throw new AssociationStoreException(e);
		} catch (ObjectStoreException e) {
			throw new AssociationStoreException(e);
		}
	}

	@Override
	public List<Association<String, T>> query(Query query) throws QueryExecutionException {
		List<Association<String, T>> ret = new ArrayList<Association<String, T>>(query.limit());

		// Solving the query
		AssociationResultBlock results = solver.solve(query);

		try {
			int[] tos = results.tos;
			int[] values = results.values;

			for (int i = 0; i < results.size; i++) {
				T to = toStore.get(tos[i]);
				float value = (float) values[i] / VALUE_MUL;

				ret.add(new Association<String, T>(null, to, value));
			}
		} catch (ObjectStoreException e) {
			throw new QueryExecutionException(e);
		}

		return ret;
	}

	AssociationBlock getAssociation(int from) throws IOException {
		return assocs.get(from);
	}

	@Override
	public synchronized void flush() throws IOException {
		assocs.flush();
	}

	@Override
	@PreDestroy
	public synchronized void close() throws IOException {
		assocs.close();
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	public ObjectStore<String> getFromStore() {
		return fromStore;
	}

	public void setFromStore(ObjectStore<String> fromIndex) {
		this.fromStore = fromIndex;
	}

	public ObjectStore<T> getToStore() {
		return toStore;
	}

	public void setToStore(ObjectStore<T> toStore) {
		this.toStore = toStore;
	}

	public File getStorageFile() {
		return storageFile;
	}

	public void setStorageFile(File storageFile) {
		this.storageFile = storageFile;
	}
}
