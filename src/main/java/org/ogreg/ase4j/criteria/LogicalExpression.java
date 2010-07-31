package org.ogreg.ase4j.criteria;

import java.util.ArrayList;
import java.util.List;

/**
 * AND expression.
 * 
 * @author Gergely Kiss
 */
class LogicalExpression implements Expression {
	private static final long serialVersionUID = -2227060076868438272L;

	enum LogicalType {
		AND, OR;
	}

	final List<Expression> expressions = new ArrayList<Expression>();
	final LogicalType type;

	public LogicalExpression(LogicalType type) {
		this.type = type;
	}
}
