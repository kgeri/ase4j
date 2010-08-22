package org.ogreg.ase4j.file;

import java.io.IOException;
import java.util.List;

import org.ogreg.ase4j.AssociationStore.Operation;
import org.ogreg.ase4j.criteria.QueryExecutionException;
import org.ogreg.ase4j.criteria.QuerySolver;
import org.ogreg.ostore.ObjectStoreException;
import org.ogreg.util.IntFloatSelector;

/**
 * Association query solver for {@link AssociationBlock}s.
 * <p>
 * Note: {@link AssociationResultBlock}s created by this implementation may
 * <b>not</b> be ordered.
 * </p>
 * 
 * @author Gergely Kiss
 */
class FileAssociationSolver extends QuerySolver<AssociationResultBlock> {
	private final FileAssociationStoreImpl<?, ?> store;

	public FileAssociationSolver(FileAssociationStoreImpl<?, ?> store) {
		this.store = store;
	}

	@Override
	protected AssociationResultBlock query(String phrase) throws QueryExecutionException {
		AssociationResultBlock a = new AssociationResultBlock(0);

		try {
			// TODO Field name? See: StringIndex.uniqueResult
			Long k = store.getFromStore().uniqueResult("", phrase);

			if (k != null) {
				AssociationBlock assoc = store.getAssociation(k.intValue());

				if (assoc != null) {
					a = assoc.asResult();
				}
			}
		} catch (IOException e) {
			throw new QueryExecutionException(e);
		} catch (ObjectStoreException e) {
			throw new QueryExecutionException(e);
		}

		return a;
	}

	@Override
	protected AssociationResultBlock intersection(AssociationResultBlock valueA,
			AssociationResultBlock valueB, Operation op) {
		int[] ta = valueA.tos;
		int[] tb = valueB.tos;

		int la = valueA.size;
		int lb = valueB.size;
		int len = Math.min(la, lb);

		AssociationResultBlock row = new AssociationResultBlock(len);

		int a, b, cnt = 0;

		for (int i = 0, j = 0; (i < la) && (j < lb);) {
			a = ta[i];
			b = tb[j];

			// Note: arrays are sorted in _ascending_ order
			if (a < b) {
				i++;
			} else if (a > b) {
				j++;
			} else {
				row.tos[cnt] = a;
				row.values[cnt] = op.calculate(valueA.values[i], valueB.values[j]);
				cnt++;
				i++;
				j++;
			}
		}

		row.size = cnt;

		return row;
	}

	@Override
	protected AssociationResultBlock minus(AssociationResultBlock valueA,
			AssociationResultBlock valueB) {
		int[] ta = valueA.tos;
		int[] tb = valueB.tos;

		int la = valueA.size;
		int lb = valueB.size;
		int len = la;

		AssociationResultBlock row = new AssociationResultBlock(len);

		int a, b, i = 0, cnt = 0;

		for (int j = 0; (i < la) && (j < lb);) {
			a = ta[i];
			b = tb[j];

			// Note: arrays are sorted in _ascending_ order
			if (a < b) {
				row.tos[cnt] = a;
				row.values[cnt] = valueA.values[i];
				cnt++;
				i++;
			} else if (a > b) {
				j++;
			} else {
				i++;
				j++;
			}
		}

		for (; i < la; i++, cnt++) {
			row.tos[cnt] = ta[i];
			row.values[cnt] = valueA.values[i];
		}

		row.size = cnt;

		return row;
	}

	@Override
	protected AssociationResultBlock union(AssociationResultBlock valueA,
			AssociationResultBlock valueB, Operation op) {
		int[] ta = valueA.tos;
		int[] tb = valueB.tos;

		int la = valueA.size;
		int lb = valueB.size;
		int len = la + lb;

		AssociationResultBlock row = new AssociationResultBlock(len);

		int i = 0, j = 0, a, b, cnt = 0;

		while ((i < la) && (j < lb)) {
			a = ta[i];
			b = tb[j];

			// Note: arrays are sorted in _ascending_ order
			if (a < b) {
				row.tos[cnt] = a;
				row.values[cnt] = valueA.values[i];
				cnt++;
				i++;
			} else if (a > b) {
				row.tos[cnt] = b;
				row.values[cnt] = valueB.values[j];
				cnt++;
				j++;
			} else {
				row.tos[cnt] = a;
				row.values[cnt] = op.calculate(valueA.values[i], valueB.values[j]);
				cnt++;
				i++;
				j++;
			}
		}

		for (; i < la; i++, cnt++) {
			row.tos[cnt] = ta[i];
			row.values[cnt] = valueA.values[i];
		}

		for (; j < lb; j++, cnt++) {
			row.tos[cnt] = tb[j];
			row.values[cnt] = valueB.values[j];
		}

		row.size = cnt;

		return row;
	}

	@Override
	protected AssociationResultBlock filter(AssociationResultBlock results,
			List<Comparison> comparisons) throws QueryExecutionException {

		try {
			AssociationResultBlock filtered = new AssociationResultBlock(results.size);

			int cnt = 0;
			for (int i = 0; i < results.size; i++) {

				boolean skip = false;
				for (Comparison comparison : comparisons) {
					Object value = store.getToStore()
							.getField(results.tos[i], comparison.fieldName);

					if (!evaluate(value, comparison.op, comparison.value)) {
						skip = true;
						break;
					}
				}

				if (skip) {
					continue;
				}

				filtered.tos[cnt] = results.tos[i];
				filtered.values[cnt] = results.values[i];
				cnt++;
			}

			filtered.size = cnt;
			return filtered;
		} catch (ObjectStoreException e) {
			throw new QueryExecutionException(e);
		}

	}

	@Override
	protected AssociationResultBlock limit(AssociationResultBlock results, int limit) {
		IntFloatSelector selector = new IntFloatSelector(limit);

		for (int i = 0; i < results.size; i++) {
			selector.add(results.tos[i], results.values[i]);
		}

		return new AssociationResultBlock(selector.keys(), selector.values(), selector.size());
	}
}
