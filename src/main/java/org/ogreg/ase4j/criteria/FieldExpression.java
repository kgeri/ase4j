package org.ogreg.ase4j.criteria;

import org.ogreg.common.Operator;

/**
 * An expression which defines a comparison operation on an object field.
 * 
 * @author Gergely Kiss
 */
class FieldExpression<T> implements Expression {
	private static final long serialVersionUID = 5625271018569664196L;

	final String fieldName;
	final Operator op;
	final T value;

	public FieldExpression(String fieldName, Operator op, T value) {
		this.fieldName = fieldName;
		this.op = op;
		this.value = value;
	}
}
