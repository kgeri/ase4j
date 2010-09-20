package org.ogreg.ostore.memory;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.ogreg.test.FileTestSupport;
import org.ogreg.test.TestUtils;
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
		store.save("aaa");
		store.saveOrUpdate("abc");
		store.flush();

		store = new StringStore();
		store.init(null, dir, new HashMap<String, String>());

		assertEquals(store.get(1), "aaa");
		assertEquals(store.get(2), "abc");
		assertEquals(store.get(3), "aab");
	}

	/**
	 * Tests saving random entries to the string store.
	 */
	public void testAdd02() throws Exception {
		File dir = FileTestSupport.createTempDir("sstore");
		StringStore store = new StringStore();
		store.init(null, dir, new HashMap<String, String>());

		int WORDS = 10000;
		List<String> words = TestUtils.randomWords(WORDS, 31);

		for (String word : words) {
			store.save(word);
		}
		store.flush();

		store = new StringStore();
		store.init(null, dir, new HashMap<String, String>());

		for (int i = 0; i < words.size(); i++) {
			assertEquals(store.get(i), words.get(i));
		}
	}

	/**
	 * Tests some corner cases.
	 */
	public void testCoverage01() throws Exception {
		File dir = FileTestSupport.createTempDir("sstore");
		StringStore test = new StringStore();
		test.init(null, dir, new HashMap<String, String>());

		// For coverage
		test.add(1, "aaa");
		test.save("ccc");
		test.saveOrUpdate("ccc");
		test.save("ddd");
		test.add(5, "eee");

		assertEquals(test.uniqueResult("", "aaa"), Long.valueOf(1));
		assertEquals(test.uniqueResult("", "bbb"), null);
		assertEquals(test.uniqueResult("", "ccc"), Long.valueOf(2));
		assertEquals(test.uniqueResult("", "ddd"), Long.valueOf(3));
		assertEquals(test.uniqueResult("", "eee"), Long.valueOf(5));
		assertEquals(test.get(10), null);
		assertEquals(test.get(2), "ccc");
	}
}
