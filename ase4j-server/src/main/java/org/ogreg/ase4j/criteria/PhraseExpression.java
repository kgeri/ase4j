package org.ogreg.ase4j.criteria;

/**
 * A simple phrase expression.
 * 
 * @author Gergely Kiss
 */
class PhraseExpression implements Expression {
	private static final long serialVersionUID = -1035437398680278532L;

	final String phrase;

	public PhraseExpression(String phrase) {
		this.phrase = phrase;
	}
}
