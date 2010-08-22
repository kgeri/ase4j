package org.ogreg.ase4j.file;

import static org.testng.Assert.assertEquals;

import org.ogreg.ase4j.AssociationStore.Operation;
import org.ogreg.ase4j.file.AssociationResultBlock;
import org.ogreg.ase4j.file.AssociationBlock;
import org.ogreg.ase4j.file.FileAssociationSolver;
import org.testng.annotations.Test;

/**
 * Association row solver tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class FileAssociationSolverTest {
	private static final Operation OP = Operation.AVG;
	FileAssociationSolver solver = new FileAssociationSolver(null);

	/**
	 * Tests {@link AssociationBlock} intersection calculations.
	 */
	public void testIntersection() {

		// At the beginning, corner case
		equals(solver.intersection(row("2,3"), row("2"), OP), row("2"));

		// At the beginning, overlap
		equals(solver.intersection(row("2,3"), row("1,2"), OP), row("2"));

		// In the middle
		equals(solver.intersection(row("1,2,3"), row("2,3,4"), OP), row("2,3"));

		// At the end, overlap
		equals(solver.intersection(row("2,3"), row("3,4"), OP), row("3"));

		// At the end, corner case
		equals(solver.intersection(row("2,3"), row("3"), OP), row("3"));
	}

	/**
	 * Tests {@link AssociationBlock} subtraction calculations.
	 */
	public void testSubtraction() {

		// At the beginning, corner case
		equals(solver.minus(row("2,3"), row("2")), row("3"));

		// At the beginning, overlap
		equals(solver.minus(row("2,3"), row("1,2")), row("3"));

		// In the middle
		equals(solver.minus(row("1,2,3"), row("2")), row("1,3"));

		// At the end, overlap
		equals(solver.minus(row("2,3"), row("3,4")), row("2"));

		// At the end, corner case
		equals(solver.minus(row("2,3"), row("3")), row("2"));
	}

	/**
	 * Tests {@link AssociationBlock} union calculations.
	 */
	public void testUnion() {

		// Full overlap
		equals(solver.union(row("2,3,4"), row("2,3,4"), OP), row("2,3,4"));

		// No overlap
		equals(solver.union(row("2,3"), row("4,5"), OP), row("2,3,4,5"));

		// Overlap in the middle
		equals(solver.union(row("2,3"), row("3,4"), OP), row("2,3,4"));

		// At the beginning, corner case
		equals(solver.union(row("2,4"), row("2"), OP), row("2,4"));

		// At the end, corner case
		equals(solver.union(row("4"), row("2,4"), OP), row("2,4"));
	}

	void equals(AssociationResultBlock actual, AssociationResultBlock expected) {
		assertEquals(actual.size, expected.size, "row size");

		for (int i = 0; i < actual.size; i++) {
			assertEquals(actual.tos[i], expected.tos[i], "tos field");
		}

		for (int i = 0; i < actual.size; i++) {
			assertEquals(actual.values[i], expected.values[i], "values field");
		}
	}

	AssociationResultBlock row(String tosAndValues) {
		return row(tosAndValues, tosAndValues);
	}

	AssociationResultBlock row(String tos, String values) {
		String[] t = tos.split(",");
		String[] v = tos.split(",");

		assertEquals(t.length, v.length, "Array lengths must match");

		AssociationResultBlock row = new AssociationResultBlock(t.length);

		for (int i = 0; i < t.length; i++) {
			row.tos[i] = Integer.parseInt(t[i]);
			row.values[i] = Integer.parseInt(v[i]);
		}

		return row;
	}
}
