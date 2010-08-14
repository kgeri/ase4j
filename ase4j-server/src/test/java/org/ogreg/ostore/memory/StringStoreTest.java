package org.ogreg.ostore.memory;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;

import org.ogreg.test.FileTestSupport;
import org.testng.annotations.Test;

/**
 * String index tests.
 * 
 * @author Gergely Kiss
 */
@Test(groups = "correctness")
public class StringStoreTest {

	/**
	 * Tests saving entries to the string store.
	 */
	public void testAdd01() throws Exception {
		File dir = FileTestSupport.createTempDir("sstore");
		StringStore store = new StringStore();
		store.init(null, dir, new HashMap<String, String>());

		store.add(1, "aaa");
		store.add(2, "abc");
		store.add(3, "aab");
		store.flush();

		store = new StringStore();
		store.init(null, dir, new HashMap<String, String>());

		assertEquals(store.get(1), "aaa");
		assertEquals(store.get(2), "abc");
		assertEquals(store.get(3), "aab");
	}

	/**
	 * Tests some corner cases.
	 */
	public void testCoverage01() throws Exception {
		StringStore test = new StringStore();
		test.init(null, null, new HashMap<String, String>());

		// For coverage
		test.add(1, "aaa");
		test.save("ccc");
		test.saveOrUpdate("ccc");

		assertEquals(test.uniqueResult("", "aaa"), Long.valueOf(1));
		assertEquals(test.uniqueResult("", "bbb"), null);
		assertEquals(test.uniqueResult("", "ccc"), Long.valueOf(3));
		assertEquals(test.get(10), null);
		assertEquals(test.get(3), "ccc");
	}
}
