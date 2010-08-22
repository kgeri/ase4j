package org.ogreg.ase4j.criteria;

import org.ogreg.ase4j.AssociationStore.Operation;
import org.ogreg.ase4j.criteria.LogicalExpression.LogicalType;

import org.ogreg.common.Operator;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Base class for association query solvers.
 * 
 * @param <R> The type of the solver's results
 * @author Gergely Kiss
 */
public abstract class QuerySolver<R extends QueryResult> {

	/**
	 * Solves the specified query and returns the results.
	 * 
	 * @param query
	 * @return
	 * @throws QueryExecutionException If the query execution has failed
	 */
	public R solve(Query query) throws QueryExecutionException {

		if (query.select == null) {
			throw new IllegalArgumentException("The query select must not be null");
		}

		R results = select(query.select, query.params.op);

		if (results.isNegated()) {

			// The final result came out negated, we cannot return the universe
			throw new QueryExecutionException(
					"Your select evaluated to a negated result, which means that the result set would be too big. Please revise your selector: "
							+ query.select);
		}

		Expression filter = query.filter;

		if (filter != null) {
			List<Comparison> ret = new LinkedList<Comparison>();

			collectComparisons(query.filter, ret);

			results = filter(results, ret);
		}

		if (query.limit > 0) {
			results = limit(results, query.limit);
		}

		return results;
	}

	private void collectComparisons(Expression filter, List<Comparison> dest)
			throws QueryExecutionException {

		// Simple filters
		if (filter instanceof FieldExpression<?>) {
			FieldExpression<?> fe = (FieldExpression<?>) filter;
			dest.add(new Comparison(fe.fieldName, fe.op, fe.value));
		}
		// AND expressions
		else if ((filter instanceof LogicalExpression)
				&& (((LogicalExpression) filter).type == LogicalType.AND)) {

			for (Expression exp : ((LogicalExpression) filter).expressions) {
				collectComparisons(exp, dest);
			}
		} else {
			throw new QueryExecutionException(
					"Only simple filters and AND expressions are supported");
		}
	}

	/**
	 * Solves the given select expression and returns the result.
	 * 
	 * @param e
	 * @param op
	 * @return The solver's result
	 * @throws QueryExecutionException If the query execution has failed
	 */
	private R select(Expression e, Operation op) throws QueryExecutionException {

		if (e instanceof LogicalExpression) {
			LogicalExpression le = (LogicalExpression) e;

			// TODO query optimization by result set size

			Expression lhs = le.expressions.get(0);
			R leftResult = select(lhs, op);
			boolean isLeftNegated = leftResult.isNegated();

			if (le.type == LogicalType.AND) {

				for (int i = 1; i < le.expressions.size(); i++) {
					Expression rhs = le.expressions.get(i);
					R rightResult = select(rhs, op);
					boolean isRightNegated = rightResult.isNegated();

					if (isLeftNegated) {

						if (isRightNegated) {
							rightResult = union(leftResult, rightResult, op);
							rightResult.setNegated(true);
						} else {
							rightResult = minus(rightResult, leftResult);
						}
					} else {

						if (isRightNegated) {
							rightResult = minus(leftResult, rightResult);
						} else {
							rightResult = intersection(leftResult, rightResult, op);
						}
					}

					leftResult = rightResult;
					isLeftNegated = isRightNegated;
				}
			} else if (le.type == LogicalType.OR) {

				for (int i = 1; i < le.expressions.size(); i++) {
					Expression rhs = le.expressions.get(i);
					R rr = select(rhs, op);
					boolean isRightNegated = rr.isNegated();

					if (!isLeftNegated && !isRightNegated) {
						rr = union(leftResult, rr, op);
					} else {

						// Negation is not supported for OR expressions, because
						// the result set would be the universe
						throw new QueryExecutionException(
								"You have negated an OR expression, which means that the result set would be too big. Please revise your query at: "
										+ e);
					}

					leftResult = rr;
					isLeftNegated = isRightNegated;
				}
			}

			return leftResult;
		} else if (e instanceof PhraseExpression) {
			return query(((PhraseExpression) e).phrase);
		} else if (e instanceof NotExpression) {
			R r = select(((NotExpression) e).expression, op);
			r.setNegated(!r.isNegated());

			return r;
		}

		return solveExtended(e);
	}

	/**
	 * Returns true if <code>valueA</code> <code>op</code> <code>valueB</code>is
	 * true.
	 * 
	 * @param valueA
	 * @param op
	 * @param valueB
	 * @return
	 */
	// TODO How about error handling?
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected boolean evaluate(Object valueA, Operator op, Object valueB) {

		switch (op) {

		case EQ: {
			return (valueA == null) ? (valueB == null) : valueA.equals(valueB);
		}

		case NE: {
			return (valueA == null) ? (valueB != null) : (!valueA.equals(valueB));
		}

		case GE: {
			return (valueA == null) ? false : (((Comparable) valueA).compareTo(valueB) >= 0);
		}

		case GT: {
			return (valueA == null) ? false : (((Comparable) valueA).compareTo(valueB) > 0);
		}

		case LE: {
			return (valueA == null) ? false : (((Comparable) valueA).compareTo(valueB) <= 0);
		}

		case LT: {
			return (valueA == null) ? false : (((Comparable) valueA).compareTo(valueB) < 0);
		}

		case MATCHES: {
			return ((valueA == null) || (valueB == null)) ? (valueA == valueB) : ((Pattern) valueB)
					.matcher((CharSequence) valueA).matches();
		}
		}

		throw new UnsupportedOperationException("Unsupported operator: " + op);
	}

	/**
	 * Subclasses must provide the implementation to get query results using the
	 * given expression.
	 * 
	 * @param phrase
	 * @return
	 * @throws QueryExecutionException if the solver failed to get the results
	 */
	protected abstract R query(String phrase) throws QueryExecutionException;

	/**
	 * Subclasses must provide implementation for calculating the union of two
	 * results here.
	 * 
	 * @param valueA
	 * @param valueB
	 * @param op The operation to use when adding associations
	 * @return The union of the two values, never null
	 */
	protected abstract R union(R valueA, R valueB, Operation op);

	/**
	 * Subclasses must provide implementation for calculating the intersection
	 * of two results here.
	 * 
	 * @param valueA
	 * @param valueB
	 * @param op The operation to use when adding associations
	 * @return The intersection of the two values, never null
	 */
	protected abstract R intersection(R valueA, R valueB, Operation op);

	/**
	 * Subclasses must provide implementation for calculating the subtraction of
	 * two results here.
	 * 
	 * @param valueA
	 * @param valueB
	 * @return The subtraction of the two values, never null
	 */
	protected abstract R minus(R valueA, R valueB);

	/**
	 * Subclasses should provide implementation for limiting the result set.
	 * 
	 * @param results
	 * @param limit
	 * @return
	 */
	protected abstract R limit(R results, int limit);

	/**
	 * Subclasses should provide implementation for filtering the result set.
	 * 
	 * @param results
	 * @param filter
	 * @return
	 * @throws QueryExecutionException on filter failure
	 */
	protected abstract R filter(R results, List<Comparison> comparisons)
			throws QueryExecutionException;

	/**
	 * Subclasses may provide implementation for solving other Expression types
	 * here.
	 * 
	 * @param e
	 * @return
	 */
	protected R solveExtended(Expression e) {
		throw new UnsupportedOperationException();
	}

	public class Comparison {
		public final String fieldName;
		public final Operator op;
		public final Object value;

		public Comparison(String fieldName, Operator op, Object value) {
			this.fieldName = fieldName;
			this.op = op;
			this.value = value;
		}
	}
}
