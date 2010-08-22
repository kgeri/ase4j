package org.ogreg.ase4j.criteria;

import java.io.Serializable;

import org.ogreg.ase4j.AssociationStore.Operation;
import org.ogreg.ase4j.Params;

/**
 * An association query. An association query consists of two parts: a select
 * and a filter. The select part marks the 'from' subjects in a logical
 * expressions to get the associations from, and the filter narrows the search
 * among these 'to' targets.
 * 
 * @author Gergely Kiss
 */
public class Query implements Serializable {
	private static final long serialVersionUID = 411032445059348593L;

	/**
	 * The select expression.
	 * <p>
	 * The select expression specifies the subjects which need to be queried for
	 * all of their associations.
	 * </p>
	 */
	final Expression select;

	/**
	 * The filter expression.
	 * <p>
	 * The filter expression specifies how to filter the association targets.
	 * </p>
	 */
	Expression filter;

	/**
	 * The maximum number of results to return, or 0 if unlimited.
	 * <p>
	 * Default: 1000
	 * </p>
	 */
	int limit = 1000;

	/**
	 * The association parameters to use for querying the associations.
	 * <p>
	 * The default operation is {@link Operation#SUM}.
	 * </p>
	 */
	final Params params;

	/**
	 * Creates the query using the <code>select</code> as a selector.
	 * <p>
	 * Please see the {@link Restrictions} class on how to build an expression.
	 * Please note that it depends on the {@link QuerySolver} implementation
	 * whether or not it will accept a particular expression as a selector.
	 * </p>
	 * 
	 * @param select
	 * @return
	 */
	public Query(Expression select) {
		this(select, new Params(Operation.SUM));
	}

	/**
	 * Creates the query using the <code>select</code> as a selector and
	 * <code>params</code> as the association parameters.
	 * <p>
	 * Please see the {@link Restrictions} class on how to build an expression.
	 * Please note that it depends on the {@link QuerySolver} implementation
	 * whether or not it will accept a particular expression as a selector.
	 * </p>
	 * 
	 * @param select
	 * @return
	 */
	public Query(Expression select, Params params) {
		this.select = select;
		this.params = params;
	}

	/**
	 * Assigns the <code>filter</code> expression to this query.
	 * <p>
	 * Please see the {@link Restrictions} class on how to build an expression.
	 * Please note that it depends on the {@link QuerySolver} implementation
	 * whether or not it will accept a particular expression as a filter.
	 * </p>
	 * 
	 * @param filter
	 * @return
	 */
	public Query filter(Expression filter) {
		this.filter = filter;
		return this;
	}

	/**
	 * Sets the maximum number of elements to return.
	 * <p>
	 * This may greatly enhance query execution time, since only a fraction of
	 * the associations need to be materialized.
	 * </p>
	 * 
	 * @param select
	 * @return
	 */
	public Query limit(int limit) {
		this.limit = limit;
		return this;
	}

	/**
	 * Returns the association limit for this query.
	 * 
	 * @return
	 */
	public int limit() {
		return limit;
	}

	/**
	 * Returns the association parameters for this query.
	 * 
	 * @return
	 */
	public Params params() {
		return params;
	}
}
