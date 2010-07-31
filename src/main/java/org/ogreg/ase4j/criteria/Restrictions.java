package org.ogreg.ase4j.criteria;

import java.util.regex.Pattern;

import org.ogreg.ase4j.criteria.LogicalExpression.LogicalType;
import org.ogreg.common.Operator;

/**
 * Helper class for creating association queries from code.
 * 
 * @author Gergely Kiss
 */
public abstract class Restrictions {

	/**
	 * Creates a simple phrase query.
	 * 
	 * @param phrase
	 * @return
	 */
	public static Expression phrase(String phrase) {
		return new PhraseExpression(phrase);
	}

	/**
	 * Creates a NOT expression.
	 * 
	 * @param phrase
	 * @return
	 */
	public static Expression not(String phrase) {
		return new NotExpression(phrase(phrase));
	}

	/**
	 * Creates a NOT expression.
	 * 
	 * @param expression
	 * @return
	 */
	public static Expression not(Expression expression) {
		return new NotExpression(expression);
	}

	/**
	 * Creates an AND expression.
	 * 
	 * @param first
	 * @param second
	 * @param more
	 * @return
	 */
	public static Expression and(Expression first, Expression... more) {
		LogicalExpression ret = new LogicalExpression(LogicalType.AND);
		ret.expressions.add(first);

		if (more != null) {

			for (Expression e : more) {
				ret.expressions.add(e);
			}
		}

		return ret;
	}

	/**
	 * Creates an OR expression.
	 * 
	 * @param first
	 * @param more
	 * @return
	 */
	public static Expression or(Expression first, Expression... more) {
		LogicalExpression ret = new LogicalExpression(LogicalType.OR);
		ret.expressions.add(first);

		if (more != null) {

			for (Expression e : more) {
				ret.expressions.add(e);
			}
		}

		return ret;
	}

	/**
	 * Restricts that <code>propertyName</code> should be equal to
	 * <code>value</code>.
	 * 
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static <T> Expression eq(String propertyName, T value) {
		return new FieldExpression<T>(propertyName, Operator.EQ, value);
	}

	/**
	 * Restricts that <code>propertyName</code> should not be equal to
	 * <code>value</code>.
	 * 
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static <T> Expression ne(String propertyName, T value) {
		return new FieldExpression<T>(propertyName, Operator.NE, value);
	}

	/**
	 * Restricts that <code>propertyName</code> should be greater than or equal
	 * to <code>value</code>.
	 * 
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static <T extends Comparable<T>> Expression ge(String propertyName, T value) {
		return new FieldExpression<T>(propertyName, Operator.GE, value);
	}

	/**
	 * Restricts that <code>propertyName</code> should be greater than
	 * <code>value</code>.
	 * 
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static <T extends Comparable<T>> Expression gt(String propertyName, T value) {
		return new FieldExpression<T>(propertyName, Operator.GT, value);
	}

	/**
	 * Restricts that <code>propertyName</code> should be less than or equal to
	 * <code>value</code>.
	 * 
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static <T extends Comparable<T>> Expression le(String propertyName, T value) {
		return new FieldExpression<T>(propertyName, Operator.LE, value);
	}

	/**
	 * Restricts that <code>propertyName</code> should be less than
	 * <code>value</code>.
	 * 
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static <T extends Comparable<T>> Expression lt(String propertyName, T value) {
		return new FieldExpression<T>(propertyName, Operator.LT, value);
	}

	/**
	 * Restricts that <code>propertyName</code> should match <code>value</code>.
	 * 
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static Expression matches(String propertyName, Pattern value) {
		return new FieldExpression<Pattern>(propertyName, Operator.MATCHES, value);
	}
}
