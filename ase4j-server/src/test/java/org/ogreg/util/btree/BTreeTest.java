package org.ogreg.util.btree;

import org.ogreg.test.TestUtils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import java.util.List;

/**
 * Test for the B-Tree.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class BTreeTest {

	/**
	 * Tests some basic inserts in an even-order tree.
	 */
	public void testInsert01() {
		BTree<String, Integer> bt = new BTree<String, Integer>(4);

		// Growing the leaf
		bt.set("010", 1);
		bt.set("020", 2);
		bt.set("030", 3);
		bt.set("040", 4);
		assertEquals(bt.root.dump(), "[010=1, 020=2, 030=3, 040=4]");

		// Leaf split (middle) and new root created
		bt.set("025", 5);
		assertEquals(bt.root.dump(), "[[010=1, 020=2], [025=5, 030=3, 040=4]]");

		// Growing the right of the right side of the tree
		// Leaf split (last) and new child added to root
		bt.set("050", 6);
		bt.set("060", 7);
		assertEquals(bt.root.dump(), "[[010=1, 020=2], [025=5, 030=3], [040=4, 050=6, 060=7]]");

		// Growing the left of the right side of the tree
		// Leaf split (first) and new child added to root
		bt.set("035", 8);
		bt.set("034", 9);
		assertEquals(bt.root.dump(),
				"[[010=1, 020=2], [025=5, 030=3], [034=9, 035=8, 040=4], [050=6, 060=7]]");

		// Growing the right of the right side of the tree
		// Node split (last) and new root created
		bt.set("070", 10);
		assertEquals(bt.root.dump(),
				"[[[010=1, 020=2], [025=5, 030=3]], [[034=9, 035=8, 040=4], [050=6, 060=7, 070=10]]]");

		// Testing key update
		bt.set("010", -1);

		assertEquals(bt.get("010"), Integer.valueOf(-1));
		assertEquals(bt.get("020"), Integer.valueOf(2));
		assertEquals(bt.get("030"), Integer.valueOf(3));
		assertEquals(bt.get("040"), Integer.valueOf(4));
		assertEquals(bt.get("025"), Integer.valueOf(5));
		assertEquals(bt.get("050"), Integer.valueOf(6));
		assertEquals(bt.get("060"), Integer.valueOf(7));
		assertEquals(bt.get("035"), Integer.valueOf(8));
		assertEquals(bt.get("034"), Integer.valueOf(9));
		assertEquals(bt.get("070"), Integer.valueOf(10));
		assertEquals(bt.get("xyz"), null);
	}

	/**
	 * Tests inserts with semi-random data.
	 */
	public void testInsert02() {
		BTree<String, Integer> bt = new BTree<String, Integer>(10);
		List<String> words = TestUtils.randomWords(5000, 31);

		for (int i = 0; i < words.size(); i++) {
			bt.set(words.get(i), i);
		}

		for (int i = 0; i < words.size(); i++) {
			assertEquals(bt.get(words.get(i)), Integer.valueOf(i));
		}
	}

	/**
	 * Tests some corner cases.
	 */
	public void testCoverage01() {

		// The order of the tree must be greater than 2
		try {
			new BTree<String, String>(2);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}

		// The order of the tree must be even
		try {
			new BTree<String, String>(3);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}
}
