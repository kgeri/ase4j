package org.ogreg.common;

/**
 * Generic comparison operator types.
 * 
 * @author Gergely Kiss
 */
public enum Operator {
	/** Equals. */
	EQ,

	/** Not equals. */
	NE,

	/** Less than. */
	LT,

	/** Greater than. */
	GT,

	/** Less than or equals. */
	LE,

	/** Greater than or equals. */
	GE,

	/** Matches the regular expression (for {@link String} fields only). */
	MATCHES;
}