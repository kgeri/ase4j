package org.ogreg.util;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.testng.annotations.Test;

/**
 * Tests the int selector data structure.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class IntSelectorTest {

	/**
	 * Tests random inserts.
	 */
	public void testInsert01() {
		int[] insert = new int[10000];

		for (int i = 0; i < insert.length; i++) {
			insert[i] = i;
		}

		shuffle(insert);

		IntSelector h = new IntSelector(5);

		for (int i = 0; i < insert.length; i++) {
			h.add(i, insert[i]);
		}

		Arrays.sort(h.vals);

		assertEquals("[9995, 9996, 9997, 9998, 9999]", Arrays.toString(h.vals));
	}

	/**
	 * Tests the case of not enough inserts.
	 */
	public void testInsert02() {
		IntSelector h = new IntSelector(5);
		h.add(1, 1);
		h.add(2, 2);
		h.add(3, 3);

		assertEquals("[1, 2, 3, 0, 0]", Arrays.toString(h.vals));
	}

	/**
	 * Tests the case of not enough inserts.
	 */
	public void testInsert03() {
		IntSelector h = new IntSelector(5);
		h.add(1, 3);
		h.add(2, 2);
		h.add(3, 1);

		assertEquals("[1, 3, 2, 0, 0]", Arrays.toString(h.vals));
	}

	/**
	 * Tests some coverage cases.
	 */
	public void testCoverage() {
		IntSelector h = new IntSelector(5);
		h.keys();
		h.values();
	}

	void shuffle(int[] arr) {
		Random r = new Random();
		int len = arr.length;

		for (int i = 0; i < len; i++) {
			int i1 = r.nextInt(len);
			int i2 = r.nextInt(len);

			int tmp = arr[i1];
			arr[i1] = arr[i2];
			arr[i2] = tmp;
		}
	}
}
