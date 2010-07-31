package org.ogreg.ase4j.criteria;

/**
 * A NOT expression.
 * 
 * @author Gergely Kiss
 */
class NotExpression implements Expression {
	private static final long serialVersionUID = -680220817807119749L;

	final Expression expression;

	public NotExpression(Expression expression) {
		this.expression = expression;
	}
}
