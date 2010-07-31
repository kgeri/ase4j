package org.ogreg.ase4j;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.Arrays;

import org.testng.annotations.Test;

/**
 * Tests for the {@link AssociationUtils} class.
 * 
 * @author Gergely Kiss
 * 
 */
@Test(groups = "correctness")
public class AssociationUtilsTest extends AssociationUtils {

	/**
	 * Tests that the {@link org.ogreg.util.Arrays#insert(int[], int, int, int)}
	 * function works as expected;
	 */
	public void testInsert() {
		{
			// Inserting 0 at the first pos
			int[] src = new int[] { 1, 2, 3, -1, -1 };
			org.ogreg.util.Arrays.insert(src, 3, 0, 0);
			assertEquals("[0, 1, 2, 3, -1]", Arrays.toString(src));
		}

		{
			// Inserting 0 at the second pos
			int[] src = new int[] { 1, 2, 3, -1, -1 };
			org.ogreg.util.Arrays.insert(src, 3, 1, 0);
			assertEquals("[1, 0, 2, 3, -1]", Arrays.toString(src));
		}

		{
			// Inserting 0 at the last pos
			int[] src = new int[] { 1, 2, 3, -1, -1 };
			org.ogreg.util.Arrays.insert(src, 5, 4, 0);
			assertEquals("[1, 2, 3, -1, 0]", Arrays.toString(src));
		}

		// Bogus insert - overindexed length
		try {
			int[] src = new int[] {};
			org.ogreg.util.Arrays.insert(src, 1, 0, 0);
			fail("Expected ArrayIndexOutOfBoundsException");
		} catch (ArrayIndexOutOfBoundsException e) {
		}

		// Bogus insert - overindexed pos to length
		try {
			int[] src = new int[] { 0 };
			org.ogreg.util.Arrays.insert(src, 1, 1, 0);
			fail("Expected ArrayIndexOutOfBoundsException");
		} catch (ArrayIndexOutOfBoundsException e) {
		}

		// Bogus insert - overindexed pos
		try {
			int[] src = new int[] {};
			org.ogreg.util.Arrays.insert(src, 0, 1, 0);
			fail("Expected ArrayIndexOutOfBoundsException");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}
}
